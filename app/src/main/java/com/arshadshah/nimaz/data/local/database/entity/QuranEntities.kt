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
    @ColumnInfo(name = "text_indopak")
    val textIndopak: String? = null, // Full IndoPak-script ayah text (16-line Mushaf); null until seeded
    val juz: Int,
    val hizb: Int,
    val page: Int,
    val sajda: Int, // 0 = no sajda, 1 = sajda
    @ColumnInfo(name = "sajda_type")
    val sajdaType: String?, // "obligatory", "recommended", or null
    val transliteration: String? = null,
    @ColumnInfo(name = "text_tajweed")
    val textTajweed: String? = null
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "surah_info")
data class SurahInfoEntity(
    @PrimaryKey val surahNumber: Int,
    val description: String,
    val themes: String // comma-separated
)

/**
 * One line-segment of a line-accurate Mushaf layout, for any edition.
 *
 * The layout is stored as line *segments* rather than one row per word: each row is a
 * contiguous run of one ayah's words that falls on a single printed line. This mirrors the
 * source data 1:1 and keeps the table compact (~13,970 rows for the 16-line IndoPak
 * edition). The actual glyph text is not duplicated here — it is reconstructed by slicing
 * the ayah text (split on space) with the inclusive
 * [firstWordPosition]..[lastWordPosition] range, which is verified lossless against the
 * source (`' '.join(words) == text_<source>`, no intra-word spaces).
 *
 * [layoutId] is the discriminator: one row set per edition, all in this table. It matches a
 * `QuranEditions.mushafLayouts` id, and *which ayah text column* the positions index into is
 * that edition's `textSource` — the ranges are only valid against the exact text the layout
 * was tokenised from. Before ADR-001 this was a table per edition
 * (`mushaf_layout_indopak16`), which meant a new layout duplicated six DAO methods, a seeder
 * and a migration; now a layout is data only.
 *
 * `line_type` is one of `ayah`, `surah_header`, or `basmalah`. Header and basmalah lines
 * carry only [surahId] for context; their [ayahId] and word positions are null.
 */
@Entity(
    tableName = "mushaf_layouts",
    // The discriminator leads: every read is scoped to a single edition.
    indices = [Index(value = ["layout_id", "page", "line"])]
)
data class MushafLayoutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "layout_id")
    val layoutId: String, // a QuranEditions.mushafLayouts id, e.g. "indopak16"
    val page: Int,
    val line: Int,
    @ColumnInfo(name = "line_type")
    val lineType: String, // "ayah" | "surah_header" | "basmalah"
    @ColumnInfo(name = "surah_id")
    val surahId: Int, // 1-114
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int?, // Global ayah id (1-6236); joins AyahEntity.id. Null for header/basmalah.
    @ColumnInfo(name = "first_word_position")
    val firstWordPosition: Int?, // 1-based index into the ayah's words; null for header/basmalah
    @ColumnInfo(name = "last_word_position")
    val lastWordPosition: Int? // inclusive; null for header/basmalah
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
