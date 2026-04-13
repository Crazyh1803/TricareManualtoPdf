package com.tricare.manuals.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tricare.manuals.data.model.Manual
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualDao {

    @Query("SELECT * FROM manuals ORDER BY name ASC")
    fun getAllManuals(): Flow<List<Manual>>

    @Query("SELECT * FROM manuals WHERE code = :code LIMIT 1")
    suspend fun getManual(code: String): Manual?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManual(manual: Manual)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManuals(manuals: List<Manual>)

    @Update
    suspend fun updateManual(manual: Manual)

    @Delete
    suspend fun deleteManual(manual: Manual)

    @Query("""
        UPDATE manuals
        SET downloadedChange = :change,
            downloadedFormat = :format,
            filePath = :path,
            downloadedAt = :timestamp
        WHERE code = :code
    """)
    suspend fun updateDownloadInfo(code: String, change: Int, format: String, path: String, timestamp: Long)

    @Query("UPDATE manuals SET latestChange = :latestChange WHERE code = :code")
    suspend fun updateLatestChange(code: String, latestChange: Int)

    @Query("""
        UPDATE manuals
        SET downloadedChange = NULL,
            downloadedFormat = NULL,
            filePath = NULL,
            downloadedAt = NULL
        WHERE code = :code
    """)
    suspend fun clearDownloadInfo(code: String)

    @Query("UPDATE manuals SET downloadedChange = NULL, downloadedFormat = NULL, filePath = NULL, downloadedAt = NULL")
    suspend fun clearAllDownloadInfo()
}
