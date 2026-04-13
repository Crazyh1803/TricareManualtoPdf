package com.tricare.manuals.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tricare.manuals.data.model.Highlight
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Query("SELECT * FROM highlights WHERE manualCode = :manualCode AND sectionFilename = :sectionFilename ORDER BY startOffset ASC")
    fun getHighlights(manualCode: String, sectionFilename: String): Flow<List<Highlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("DELETE FROM highlights WHERE manualCode = :manualCode")
    suspend fun clearHighlightsForManual(manualCode: String)
}
