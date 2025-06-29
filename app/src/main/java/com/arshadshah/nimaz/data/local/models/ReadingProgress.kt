package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ReadingProgress")
data class ReadingProgress(
    @PrimaryKey
    val surahNumber: Int,
    val lastReadAyaNumber: Int,
    val completionPercentage: Float,
    val lastReadDate: String,
    val totalReadingTimeMinutes: Int = 0
)