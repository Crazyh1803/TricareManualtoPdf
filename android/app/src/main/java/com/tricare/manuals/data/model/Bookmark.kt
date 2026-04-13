package com.tricare.manuals.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manualCode: String,
    val sectionFilename: String,
    val sectionTitle: String,
    val scrollY: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
