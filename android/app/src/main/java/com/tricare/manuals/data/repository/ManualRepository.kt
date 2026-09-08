package com.tricare.manuals.data.repository

import com.tricare.manuals.data.db.BookmarkDao
import com.tricare.manuals.data.db.ManualDao
import com.tricare.manuals.data.db.SectionDao
import com.tricare.manuals.data.model.Bookmark
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.data.model.Section
import com.tricare.manuals.data.network.MirrorClient
import com.tricare.manuals.data.network.VersionChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualRepository @Inject constructor(
    private val manualDao: ManualDao,
    private val sectionDao: SectionDao,
    private val bookmarkDao: BookmarkDao,
    private val mirror: MirrorClient,
    private val versionChecker: VersionChecker
) {

    companion object {
        // Fallback list, used only when the mirror's manifest cannot be read
        // (first run while offline). The manifest is the real source: it also
        // carries FR16, which was added to the scraper and the website months
        // ago but never to this list — seeding from the manifest is what stops
        // the two drifting apart again.
        val KNOWN_MANUALS = listOf(
            Manual(code = "TOT5", name = "TRICARE Operations Manual"),
            Manual(code = "TPT5", name = "TRICARE Policy Manual"),
            Manual(code = "TRT5", name = "TRICARE Reimbursement Manual"),
            Manual(code = "TST5", name = "TRICARE Systems Manual"),
            Manual(code = "FR16", name = "32 CFR Part 199")
        )
    }

    fun getAllManuals(): Flow<List<Manual>> = manualDao.getAllManuals()

    suspend fun ensureDefaultManualsExist() {
        withContext(Dispatchers.IO) {
            // Prefer the published manifest so a manual added to the scraper
            // shows up without an app release; fall back to the built-in list
            // when it cannot be reached.
            val fromMirror = mirror.fetchManuals()
                ?.map { Manual(code = it.code, name = it.name) }
            for (manual in fromMirror ?: KNOWN_MANUALS) {
                val existing = manualDao.getManual(manual.code)
                if (existing == null) {
                    manualDao.upsertManual(manual)
                }
            }
        }
    }

    fun getNetworkError(): String = mirror.lastError

    suspend fun checkLatestVersion(code: String): Int? {
        return withContext(Dispatchers.IO) {
            val latest = versionChecker.checkLatestVersion(code)
            if (latest != null) {
                manualDao.updateLatestChange(code, latest)
            }
            latest
        }
    }

    suspend fun discoverAvailableChanges(code: String, latestChange: Int): List<Int> {
        return withContext(Dispatchers.IO) {
            versionChecker.discoverAvailableChanges(code, latestChange)
        }
    }

    fun getBookmarks(code: String): Flow<List<Bookmark>> = bookmarkDao.getBookmarks(code)

    suspend fun addBookmark(bookmark: Bookmark) {
        withContext(Dispatchers.IO) {
            bookmarkDao.insertBookmark(bookmark)
        }
    }

    suspend fun removeBookmark(id: Long) {
        withContext(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(id)
        }
    }

    fun getSections(code: String, change: Int): Flow<List<Section>> =
        sectionDao.getSections(code, change)

    suspend fun getSectionsList(code: String, change: Int): List<Section> {
        return withContext(Dispatchers.IO) {
            sectionDao.getSectionsList(code, change)
        }
    }

    suspend fun getManual(code: String): Manual? {
        return withContext(Dispatchers.IO) {
            manualDao.getManual(code)
        }
    }

    suspend fun updateDownloadInfo(code: String, change: Int, format: String, path: String) {
        withContext(Dispatchers.IO) {
            manualDao.updateDownloadInfo(code, change, format, path, System.currentTimeMillis())
        }
    }

    suspend fun clearDownloadInfo(code: String) {
        withContext(Dispatchers.IO) {
            manualDao.clearDownloadInfo(code)
            sectionDao.clearAllSectionsForManual(code)
        }
    }

    suspend fun clearAllDownloads() {
        withContext(Dispatchers.IO) {
            manualDao.clearAllDownloadInfo()
        }
    }
}
