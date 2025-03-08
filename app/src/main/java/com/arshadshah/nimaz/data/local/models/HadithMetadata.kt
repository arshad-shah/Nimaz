package com.arshadshah.nimaz.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Metadata")
data class HadithMetadata(
    @PrimaryKey val id: Int,
    val length: Int,
    val title_arabic: String,
    val author_arabic: String,
    val introduction_arabic: String,
    val title_english: String,
    val author_english: String,
    val introduction_english: String
)
