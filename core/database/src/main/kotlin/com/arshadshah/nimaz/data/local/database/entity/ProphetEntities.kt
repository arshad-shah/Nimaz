package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "prophets")
data class ProphetEntity(
    @PrimaryKey val id: Int,
    val number: Int,
    @ColumnInfo(name = "name_arabic") val nameArabic: String,
    @ColumnInfo(name = "name_english") val nameEnglish: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "title_arabic") val titleArabic: String,
    @ColumnInfo(name = "title_english") val titleEnglish: String,
    @ColumnInfo(name = "story_summary") val storySummary: String,
    @ColumnInfo(name = "key_lessons") val keyLessons: String, // JSON array stored as string
    @ColumnInfo(name = "quran_mentions") val quranMentions: String, // JSON array stored as string
    val era: String,
    val lineage: String,
    @ColumnInfo(name = "years_lived") val yearsLived: String,
    @ColumnInfo(name = "place_of_preaching") val placeOfPreaching: String,
    val miracles: String, // JSON array stored as string
    @ColumnInfo(name = "display_order") val displayOrder: Int
)

@Entity(
    tableName = "prophet_bookmarks",
    indices = [Index(value = ["prophet_id"], unique = true)]
)
data class ProphetBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "prophet_id") val prophetId: Int,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
