package com.tricare.manuals.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tricare.manuals.data.model.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Query("SELECT * FROM sections WHERE manualCode = :manualCode AND `change` = :change ORDER BY sortOrder ASC")
    fun getSections(manualCode: String, change: Int): Flow<List<Section>>

    @Query("SELECT * FROM sections WHERE manualCode = :manualCode AND `change` = :change ORDER BY sortOrder ASC")
    suspend fun getSectionsList(manualCode: String, change: Int): List<Section>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<Section>)

    @Query("DELETE FROM sections WHERE manualCode = :manualCode AND `change` = :change")
    suspend fun clearSections(manualCode: String, change: Int)

    @Query("DELETE FROM sections WHERE manualCode = :manualCode")
    suspend fun clearAllSectionsForManual(manualCode: String)
}
