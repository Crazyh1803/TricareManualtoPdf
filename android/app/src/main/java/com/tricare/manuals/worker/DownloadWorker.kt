package com.tricare.manuals.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tricare.manuals.data.db.ManualDao
import com.tricare.manuals.data.db.SectionDao
import com.tricare.manuals.data.model.Section
import com.tricare.manuals.data.network.TocParser
import com.tricare.manuals.data.network.TricareWebClient
import com.tricare.manuals.data.repository.ManualRepository
import com.tricare.manuals.util.appDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val webClient: TricareWebClient,
    private val tocParser: TocParser,
    private val manualDao: ManualDao,
    private val sectionDao: SectionDao
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_MANUAL_CODE = "manual_code"
        const val KEY_FORMAT = "format"
        const val KEY_CHANGE_NUM = "change_num"
        const val KEY_PROGRESS_CURRENT = "current"
        const val KEY_PROGRESS_TOTAL = "total"
        const val KEY_MANUAL_PROGRESS = "manual"
        const val KEY_ERROR_REASON = "error_reason"
        const val KEY_ETA_SECONDS = "eta_seconds"
        const val KEY_FILE_PATH = "file_path"

        private val WIFI_ONLY_KEY = booleanPreferencesKey("wifi_only_downloads")
        private const val BASE_TOC_URL = "https://manuals.health.mil/pages/ManualToc.aspx?Manual="
        private const val CHANNEL_ID = "tricare_download"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val code = inputData.getString(KEY_MANUAL_CODE) ?: return Result.failure(
            workDataOf(KEY_ERROR_REASON to "Missing manual code")
        )
        val format = inputData.getString(KEY_FORMAT) ?: "md"
        val changeNum = inputData.getInt(KEY_CHANGE_NUM, 0)

        setForeground(createForegroundInfo(code, changeNum, 0, 0, null))

        val prefs = applicationContext.appDataStore.data.first()
        val wifiOnly = prefs[WIFI_ONLY_KEY] ?: false
        if (wifiOnly && !isOnWifi()) {
            return Result.failure(workDataOf(KEY_ERROR_REASON to "WiFi required but not connected"))
        }

        return withContext(Dispatchers.IO) {
            try {
                setProgress(workDataOf(
                    KEY_MANUAL_PROGRESS to code,
                    KEY_PROGRESS_CURRENT to 0,
                    KEY_PROGRESS_TOTAL to 0,
                    KEY_ETA_SECONDS to -1L
                ))

                // 1. Fetch the main TOC page for the requested change
                val tocUrl = "$BASE_TOC_URL$code&Change=$changeNum"
                val tocHtml = webClient.fetchHtml(tocUrl)
                    ?: return@withContext Result.failure(
                        workDataOf(KEY_ERROR_REASON to "Failed to fetch TOC: $tocUrl")
                    )

                // 2. Find chapter TOC links
                val allLinks = tocParser.parseChapterTocUrls(tocHtml, tocUrl)
                val chapterTocLinks = allLinks.filter { url ->
                    val filename = url.substringAfterLast('/').substringBefore('?')
                    tocParser.isChapterToc(filename)
                }

                // 3. Collect all section URLs — pre-seed seen with chapter TOC URLs
                //    so they are never added as section content (deduplication).
                val seenUrls = chapterTocLinks.toMutableSet()
                val sectionUrls = mutableListOf<String>()

                // Direct section links from the main TOC
                allLinks.forEach { url ->
                    val filename = url.substringAfterLast('/').substringBefore('?')
                    if (!tocParser.isChapterToc(filename) && seenUrls.add(url)) {
                        sectionUrls.add(url)
                    }
                }

                // Section links from each chapter TOC
                for (chapterUrl in chapterTocLinks) {
                    delay(randomDelay())
                    val chapterHtml = webClient.fetchHtml(chapterUrl) ?: continue
                    tocParser.parseChapterTocUrls(chapterHtml, chapterUrl).forEach { url ->
                        val filename = url.substringAfterLast('/').substringBefore('?')
                        if (!tocParser.isChapterToc(filename) && seenUrls.add(url)) {
                            sectionUrls.add(url)
                        }
                    }
                }

                val sortedUrls = tocParser.naturalSort(sectionUrls)

                if (sortedUrls.isEmpty()) {
                    return@withContext Result.failure(
                        workDataOf(
                            KEY_ERROR_REASON to
                                "No sections found for $code Change $changeNum. " +
                                "TOC had ${allLinks.size} links, " +
                                "${chapterTocLinks.size} chapter TOCs. " +
                                "First link: ${allLinks.firstOrNull() ?: "none"}"
                        )
                    )
                }

                val total = sortedUrls.size
                setProgress(workDataOf(
                    KEY_MANUAL_PROGRESS to code,
                    KEY_PROGRESS_CURRENT to 0,
                    KEY_PROGRESS_TOTAL to total,
                    KEY_ETA_SECONDS to -1L
                ))
                setForeground(createForegroundInfo(code, changeNum, 0, total, null))

                // 4. Download each section
                //    Always extract markdown content regardless of output format so
                //    both MD and PDF outputs have real readable text.
                val sections = mutableListOf<Section>()
                var current = 0
                val loopStartMs = System.currentTimeMillis()
                var lastNotifUpdateMs = 0L

                for (url in sortedUrls) {
                    delay(randomDelay())
                    val html = webClient.fetchHtml(url) ?: continue
                    val filename = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
                    val title = tocParser.extractTitle(html)
                    val contentMd = tocParser.htmlToMarkdown(html)

                    sections.add(Section(
                        manualCode = code,
                        change = changeNum,
                        filename = filename,
                        title = title,
                        sortOrder = current,
                        contentMd = contentMd
                    ))

                    current++

                    val elapsedMs = System.currentTimeMillis() - loopStartMs
                    val avgMsPerSection = elapsedMs / current
                    val etaSeconds = avgMsPerSection * (total - current) / 1000L

                    setProgress(workDataOf(
                        KEY_MANUAL_PROGRESS to code,
                        KEY_PROGRESS_CURRENT to current,
                        KEY_PROGRESS_TOTAL to total,
                        KEY_ETA_SECONDS to etaSeconds
                    ))

                    val now = System.currentTimeMillis()
                    if (now - lastNotifUpdateMs >= 3000) {
                        setForeground(createForegroundInfo(code, changeNum, current, total, etaSeconds))
                        lastNotifUpdateMs = now
                    }
                }

                // 5. Write output file to the public Downloads folder.
                //    Android 10+ (API 29+): MediaStore.Downloads — no permission needed.
                //    Android 9 and below: direct file write (WRITE_EXTERNAL_STORAGE granted by user).
                val ext = if (format == "pdf") "pdf" else "md"
                val fileName = "${code}_change${changeNum}.$ext"

                val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeViaMediaStore(fileName, code, changeNum, sections)
                } else {
                    writeViaLegacy(fileName, code, changeNum, sections)
                } ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR_REASON to "Failed to write file to Downloads folder")
                )

                // 6. Mark the manual as downloaded in the DB.
                //    Do this RIGHT AFTER the file is written so the card updates
                //    correctly even if the section-cache insert below fails.
                manualDao.updateDownloadInfo(
                    code, changeNum, format, filePath, System.currentTimeMillis()
                )

                // 7. Cache section content in DB for in-app reader (best-effort).
                //    A failure here does NOT roll back the download — the file is
                //    already on disk and the manual is already marked complete.
                try {
                    sectionDao.clearSections(code, changeNum)
                    sectionDao.insertSections(sections)
                } catch (e: Exception) {
                    // Non-fatal: reader will fall back to the file on disk
                }

                Result.success(workDataOf(
                    KEY_MANUAL_PROGRESS to code,
                    KEY_PROGRESS_CURRENT to total,
                    KEY_PROGRESS_TOTAL to total,
                    KEY_ETA_SECONDS to 0L,
                    KEY_FILE_PATH to filePath
                ))

            } catch (e: Exception) {
                Result.failure(workDataOf(KEY_ERROR_REASON to (e.message ?: "Unknown error")))
            }
        }
    }

    /** Android 10+ (API 29+): write via MediaStore so the file lands in the real Downloads
     *  folder without needing WRITE_EXTERNAL_STORAGE. Returns the file-system path on success.
     *
     *  Re-download strategy: if an entry with the same display name already exists, update it
     *  in-place with write-truncate ("wt") rather than delete + re-insert.  Delete + re-insert
     *  can leave the old file visible (if the delete fails silently) while Android names the new
     *  file "TOT5_change53 (1).md", so the user sees stale content in Downloads. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeViaMediaStore(
        fileName: String, code: String, changeNum: Int, sections: List<Section>
    ): String? {
        val resolver = applicationContext.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val cv = ContentValues()

        // Look for any existing entries with this name (there should be at most one, but loop
        // to handle edge cases where duplicates accumulated from earlier app versions).
        var targetUri: android.net.Uri? = null
        resolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val entryUri = android.content.ContentUris.withAppendedId(collection, id)
                if (targetUri == null) {
                    // Keep the first match as the update target
                    targetUri = entryUri
                } else {
                    // Delete any extra duplicates so we don't accumulate stale entries
                    resolver.delete(entryUri, null, null)
                }
            }
        }

        val uri: android.net.Uri
        if (targetUri != null) {
            // Update existing entry in-place: mark pending so it's hidden while we write
            uri = targetUri!!
            cv.put(MediaStore.Downloads.IS_PENDING, 1)
            resolver.update(uri, cv, null, null)
        } else {
            // No existing entry — insert a new one
            cv.apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/markdown")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            uri = resolver.insert(collection, cv) ?: return null
        }

        return try {
            // "wt" = write-truncate: clears existing bytes before writing new content
            resolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { w ->
                writeContent(w, code, changeNum, sections)
            }
            cv.clear()
            cv.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, cv, null, null)

            // Get the real file-system path; fall back to constructing it if DATA is null
            @Suppress("DEPRECATION")
            resolver.query(uri, arrayOf(MediaStore.Downloads.DATA), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                ?: "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName"
        } catch (e: Exception) {
            // Only delete the URI if we freshly inserted it; leave pre-existing entries alone
            if (targetUri == null) resolver.delete(uri, null, null)
            null
        }
    }

    /** Android 9 and below: direct write to the public Downloads folder.
     *  Requires WRITE_EXTERNAL_STORAGE, which the user granted before enqueueing. */
    private fun writeViaLegacy(
        fileName: String, code: String, changeNum: Int, sections: List<Section>
    ): String? {
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            file.bufferedWriter().use { w -> writeContent(w, code, changeNum, sections) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun writeContent(
        w: BufferedWriter, code: String, changeNum: Int, sections: List<Section>
    ) {
        w.write("# ${ManualRepository.KNOWN_MANUALS.find { it.code == code }?.name ?: code}\n")
        w.write("Change $changeNum — ${sections.size} sections\n\n")
        w.write("---\n\n")
        w.write("## Table of Contents\n\n")
        sections.forEachIndexed { i, s -> w.write("${i + 1}. ${s.title}\n") }
        w.write("\n---\n\n")
        for (section in sections) {
            w.write("## ${section.title}\n\n")
            w.write(section.contentMd ?: "")
            w.write("\n\n---\n\n")
        }
    }

    private fun createForegroundInfo(
        code: String,
        changeNum: Int,
        current: Int,
        total: Int,
        etaSeconds: Long?
    ): ForegroundInfo {
        val manualName = ManualRepository.KNOWN_MANUALS
            .find { it.code == code }?.name ?: code

        val contentText = when {
            total == 0 -> "Change $changeNum · Fetching table of contents…"
            current >= total -> "Change $changeNum · Saving…"
            else -> {
                val eta = etaSeconds?.takeIf { it >= 0 }?.let { " · ${formatEta(it)}" } ?: ""
                "Change $changeNum · $current / $total sections$eta"
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Manual Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "TRICARE manual download progress" }
            applicationContext
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $manualName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(maxOf(total, 1), current, total == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun formatEta(seconds: Long): String = when {
        seconds >= 3600 -> "~${seconds / 3600} hr remaining"
        seconds >= 60   -> "~${seconds / 60} min remaining"
        else            -> "< 1 min remaining"
    }

    private fun isOnWifi(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun randomDelay(): Long = (2000L + (Math.random() * 3000).toLong())
}
