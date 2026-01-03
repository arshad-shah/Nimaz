package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "khatam_sessions")
data class KhatamSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: String, // ISO date string
    val targetCompletionDate: String? = null,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val completionDate: String? = null,
    val currentSurah: Int = 1,
    val currentAya: Int = 1,
    val totalAyasRead: Int = 0,
    val dailyTarget: Int? = null,
    val notes: String = "",
    val createdAt: String = ""
)