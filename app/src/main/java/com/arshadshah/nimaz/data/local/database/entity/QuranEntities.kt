package com.arshadshah.nimaz.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey
    val id: Int,
    val number: Int,
    @ColumnInfo(name = "name_arabic")
    val nameArabic: String,
    @ColumnInfo(name = "name_english")
    val nameEnglish: String,
    @ColumnInfo(name = "name_transliteration")
    val nameTransliteration: String,
    @ColumnInfo(name = "revelation_type")
    val revelationType: String, // "meccan" or "medinan"
    @ColumnInfo(name = "verses_count")
    val versesCount: Int,
    @ColumnInfo(name = "order_revealed")
    val orderRevealed: Int,
    @ColumnInfo(name = "start_page")
    val startPage: Int
)

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["id"],
            childColumns = ["surah_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["surah_id"]),
        Index(value = ["juz"]),
        Index(value = ["page"])
    ]
)
data class AyahEntity(
    @PrimaryKey
    val id: Int, // Unique ayah id (1-6236)
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "number_in_surah")
    val numberInSurah: Int, // Ayah number within surah
    @ColumnInfo(name = "number_global")
    val numberGlobal: Int, // Global ayah number
    @ColumnInfo(name = "text_arabic")
    val textArabic: String,
    @ColumnInfo(name = "text_uthmani")
    val textUthmani: String, // Uthmani script
    val juz: Int,
    val hizb: Int,
    val page: Int,
    val sajda: Int, // 0 = no sajda, 1 = sajda
    @ColumnInfo(name = "sajda_type")
    val sajdaType: String?, // "obligatory", "recommended", or null
    val transliteration: String? = null
)

@Entity(
    tableName = "translations",
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = ["id"],
            childColumns = ["ayah_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ayah_id"])]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    val text: String,
    @ColumnInfo(name = "translator_id")
    val translatorId: String // e.g., "en.sahih", "en.pickthall"
)

@Entity(
    tableName = "quran_bookmarks",
    indices = [Index(value = ["ayahId"])]
)
data class QuranBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val note: String?,
    val color: String?, // Hex color for bookmark categorization
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_favorites")
data class QuranFavoriteEntity(
    @PrimaryKey val ayahId: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "surah_info")
data class SurahInfoEntity(
    @PrimaryKey val surahNumber: Int,
    val description: String,
    val themes: String // comma-separated
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey
    val id: Int = 1, // Single row for current progress
    val lastReadSurah: Int,
    val lastReadAyah: Int,
    val lastReadPage: Int,
    val lastReadJuz: Int,
    val totalAyahsRead: Int,
    val currentKhatmaCount: Int, // Number of complete Quran readings
    val updatedAt: Long = System.currentTimeMillis()
)
