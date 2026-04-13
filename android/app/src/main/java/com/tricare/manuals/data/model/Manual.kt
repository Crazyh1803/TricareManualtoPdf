package com.tricare.manuals.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manuals")
data class Manual(
    @PrimaryKey val code: String,
    val name: String,
    val latestChange: Int = 0,
    val downloadedChange: Int? = null,
    val downloadedFormat: String? = null,
    val filePath: String? = null,
    val downloadedAt: Long? = null
)
