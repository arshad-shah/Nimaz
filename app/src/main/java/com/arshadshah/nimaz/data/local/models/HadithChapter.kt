package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "HadithChapters",
    primaryKeys = ["chapterId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = HadithMetadata::class,
            parentColumns = ["id"],
            childColumns = ["bookId"]
        )
    ],
)
data class HadithChapter(
    val chapterId: Int,
    val bookId: Int,
    val title_arabic: String,
    val title_english: String
)
