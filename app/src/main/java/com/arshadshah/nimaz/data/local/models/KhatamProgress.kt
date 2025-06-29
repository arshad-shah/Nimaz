package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "khatam_progress",
    foreignKeys = [
        ForeignKey(
            entity = KhatamSession::class,
            parentColumns = ["id"],
            childColumns = ["khatamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["khatamId"])]
)
data class KhatamProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val khatamId: Long,
    val surahNumber: Int,
    val ayaNumber: Int,
    val dateRead: String,
    val sessionDuration: Int = 0, // minutes spent reading
    val timestamp: String = ""
)