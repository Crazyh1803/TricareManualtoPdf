package com.tricare.manuals.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tricare.manuals.data.model.Bookmark
import com.tricare.manuals.data.model.Highlight
import com.tricare.manuals.data.model.Manual
import com.tricare.manuals.data.model.Section

@Database(
    entities = [Manual::class, Section::class, Bookmark::class, Highlight::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun manualDao(): ManualDao
    abstract fun sectionDao(): SectionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
}
