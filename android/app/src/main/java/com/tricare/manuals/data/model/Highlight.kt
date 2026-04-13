package com.tricare.manuals.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manualCode: String,
    val sectionFilename: String,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val color: String = "#FFFF00",
    val createdAt: Long = System.currentTimeMillis()
)
