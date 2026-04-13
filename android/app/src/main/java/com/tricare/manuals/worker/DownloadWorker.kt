package com.tricare.manuals.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
import android.os.Environment
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

        // Promote to a foreground service so the download survives the user
        // navigating away from the app or switching to another app entirely.
        setForeground(createForegroundInfo(code, changeNum, 0, 0, null))

        // Check WiFi-only preference
        val prefs = applicationContext.appDataStore.data.first()
        val wifiOnly = prefs[WIFI_ONLY_KEY] ?: false
        if (wifiOnly && !isOnWifi()) {
            return Result.failure(workDataOf(KEY_ERROR_REASON to "WiFi required but not connected"))
        }

        return withContext(Dispatchers.IO) {
            try {
                setProgress(
                    workDataOf(
                        KEY_MANUAL_PROGRESS to code,
                        KEY_PROGRESS_CURRENT to 0,
                        KEY_PROGRESS_TOTAL to 0,
                        KEY_ETA_SECONDS to -1L
                    )
                )

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

                // 3. Collect all section URLs from each chapter TOC
                val sectionUrls = mutableListOf<String>()
                for (chapterUrl in chapterTocLinks) {
                    delay(randomDelay())
                    val chapterHtml = webClient.fetchHtml(chapterUrl) ?: continue
                    val sectionLinks = tocParser.parseChapterTocUrls(chapterHtml, chapterUrl)
                        .filter { url ->
                            val filename = url.substringAfterLast('/').substringBefore('?')
                            !tocParser.isChapterToc(filename)
                        }
                    sectionUrls.addAll(sectionLinks)
                }

                // Also include direct section links from the main TOC
                val directSectionLinks = allLinks.filter { url ->
                    val filename = url.substringAfterLast('/').substringBefore('?')
                    !tocParser.isChapterToc(filename)
                }
                sectionUrls.addAll(directSectionLinks)

                val sortedUrls = tocParser.naturalSort(sectionUrls.distinct())

                if (sortedUrls.isEmpty()) {
                    return@withContext Result.failure(
                        workDataOf(
                            KEY_ERROR_REASON to
                                "No sections found for $code Change $changeNum " +
                                "(${allLinks.size} links on TOC page; " +
                                "first link: ${allLinks.firstOrNull() ?: "none"})"
                        )
                    )
                }

                val total = sortedUrls.size
                setProgress(
                    workDataOf(
                        KEY_MANUAL_PROGRESS to code,
                        KEY_PROGRESS_CURRENT to 0,
                        KEY_PROGRESS_TOTAL to total,
                        KEY_ETA_SECONDS to -1L
                    )
                )
                setForeground(createForegroundInfo(code, changeNum, 0, total, null))

                // 4. Download each section
                val sections = mutableListOf<Section>()
                var current = 0
                val loopStartMs = System.currentTimeMillis()
                var lastNotifUpdateMs = 0L

                for (url in sortedUrls) {
                    delay(randomDelay())
                    val html = webClient.fetchHtml(url) ?: continue
                    val filename = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
                    val title = tocParser.extractTitle(html)
                    val contentMd = if (format == "md") tocParser.htmlToMarkdown(html) else null

                    sections.add(
                        Section(
                            manualCode = code,
                            change = changeNum,
                            filename = filename,
                            title = title,
                            sortOrder = current,
                            contentMd = contentMd
                        )
                    )

                    current++

                    // Compute estimated time remaining based on average time per section so far
                    val elapsedMs = System.currentTimeMillis() - loopStartMs
                    val avgMsPerSection = elapsedMs / current
                    val etaSeconds = avgMsPerSection * (total - current) / 1000L

                    setProgress(
                        workDataOf(
                            KEY_MANUAL_PROGRESS to code,
                            KEY_PROGRESS_CURRENT to current,
                            KEY_PROGRESS_TOTAL to total,
                            KEY_ETA_SECONDS to etaSeconds
                        )
                    )

                    // Update the foreground notification at most once every 3 seconds
                    // to avoid flooding the notification shade with rapid updates.
                    val now = System.currentTimeMillis()
                    if (now - lastNotifUpdateMs >= 3000) {
                        setForeground(createForegroundInfo(code, changeNum, current, total, etaSeconds))
                        lastNotifUpdateMs = now
                    }
                }

                // 5. Write to public Downloads folder so files are accessible
                // in the file browser and can be shared / uploaded elsewhere.
                val manualsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                manualsDir.mkdirs()

                val filePath: String
                if (format == "md") {
                    val outputFile = File(manualsDir, "${code}_change${changeNum}.md")
                    outputFile.bufferedWriter().use { writer ->
                        for (section in sections) {
                            writer.write("# ${section.title}\n\n")
                            writer.write(section.contentMd ?: "")
                            writer.write("\n\n---\n\n")
                        }
                    }
                    filePath = outputFile.absolutePath

                    // Save sections to DB
                    sectionDao.clearSections(code, changeNum)
                    sectionDao.insertSections(sections)
                } else {
                    // PDF placeholder: write text content to file
                    val outputFile = File(manualsDir, "${code}_change${changeNum}.pdf")
                    outputFile.bufferedWriter().use { writer ->
                        for (section in sections) {
                            writer.write("${section.title}\n\n")
                            writer.write(section.contentMd ?: "")
                            writer.write("\n\n")
                        }
                    }
                    filePath = outputFile.absolutePath
                }

                // 6. Update manual record in DB
                manualDao.updateDownloadInfo(
                    code, changeNum, format, filePath, System.currentTimeMillis()
                )

                Result.success(
                    workDataOf(
                        KEY_MANUAL_PROGRESS to code,
                        KEY_PROGRESS_CURRENT to total,
                        KEY_PROGRESS_TOTAL to total,
                        KEY_ETA_SECONDS to 0L
                    )
                )
            } catch (e: Exception) {
                Result.failure(workDataOf(KEY_ERROR_REASON to (e.message ?: "Unknown error")))
            }
        }
    }

    /**
     * Builds a [ForegroundInfo] for the persistent download notification.
     * Safe to call multiple times — the notification channel is created on first call.
     */
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

        // Create the notification channel (idempotent on Android 8+)
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

        // On Android 10+ declare the foreground service type so the OS
        // knows this is a data-sync operation and grants it the correct privileges.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
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
