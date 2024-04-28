package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Hadiths",
    foreignKeys = [
        ForeignKey(
            entity = HadithChapter::class,
            parentColumns = ["chapterId", "bookId"],
            childColumns = ["chapterId", "bookId"]
        ),
        ForeignKey(
            entity = HadithMetadata::class,
            parentColumns = ["id"],
            childColumns = ["bookId"]
        )
    ]
)
data class HadithEntity(
    @PrimaryKey val id: Int,
    val arabic: String,
    val narrator_english: String,
    val text_english: String,
    val chapterId: Int,
    val bookId: Int,
    val idInBook: Int,
    val favourite: Boolean,
)