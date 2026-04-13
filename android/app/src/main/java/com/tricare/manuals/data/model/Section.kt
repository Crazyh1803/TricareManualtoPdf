package com.tricare.manuals.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val manualCode: String,
    val change: Int,
    val filename: String,
    val title: String,
    val sortOrder: Int,
    val contentMd: String? = null
)
