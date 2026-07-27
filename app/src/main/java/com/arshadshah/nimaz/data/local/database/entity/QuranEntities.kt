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
    /**
     * Superseded by [MushafAyahTextEntity], which holds one row per (text source, ayah) and
     * so can carry more than one script. Kept as a column because dropping one in SQLite
     * means rebuilding a 6,236-row table for no functional gain; the schema v20 migration
     * nulls it out to reclaim the space. Nothing reads it — resolve glyph text through
     * `mushaf_ayah_texts` instead.
     */
    @ColumnInfo(name = "text_indopak")
    val textIndopak: String? = null,
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
    indices = [
        Index(value = ["ayah_id"]),
        // One verse per (ayah, translator). The seeder replaces a translation by deleting
        // then re-inserting, and `id` is auto-generated, so without this a bug in that path
        // would silently double every verse and the reader would pick an arbitrary copy.
        Index(value = ["ayah_id", "translator_id"], unique = true)
    ]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int,
    val text: String,
    /**
     * Stable id of the translation, matching
     * [com.arshadshah.nimaz.domain.model.QuranTranslation.id] — e.g. "sahih_international",
     * "ur_maududi". Also what the user's `quran_translator_id` preference stores.
     */
    @ColumnInfo(name = "translator_id")
    val translatorId: String
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
 * The glyph text of one ayah in one *text source* — the script a printed edition sets its
 * words in.
 *
 * A text source is not the same thing as a layout: editions that set identical glyphs and
 * segment them into identical words share one source and differ only in where their lines
 * break. The 16-line (Taj) and 15-line (Qudratullah) IndoPak mushafs are verified to be
 * exactly that case, so both read `INDOPAK`; the 13-line Taj print differs in the vowel
 * marks of 28 ayahs and therefore carries its own `INDOPAK_13`. See
 * [com.arshadshah.nimaz.domain.model.MushafScript.textSource].
 *
 * Word positions in [MushafLayoutLineEntity] index into `text.split(" ")`, which the
 * generator verifies is lossless (`" ".join(words) == text`, no intra-word spaces).
 */
@Entity(
    tableName = "mushaf_ayah_texts",
    primaryKeys = ["text_source", "ayah_id"],
    indices = [Index(value = ["ayah_id"])]
)
data class MushafAyahTextEntity(
    @ColumnInfo(name = "text_source")
    val textSource: String, // e.g. "INDOPAK", "INDOPAK_13"
    @ColumnInfo(name = "ayah_id")
    val ayahId: Int, // Global ayah id (1-6236); matches AyahEntity.id
    val text: String
)

/**
 * One line-segment of one line-accurate Mushaf layout.
 *
 * A layout is stored as line *segments* rather than one row per word: each row is a
 * contiguous run of one ayah's words that falls on a single printed line. This mirrors the
 * generated source data 1:1 and keeps the table compact (~14k rows per edition). The glyph
 * text is not duplicated here — it is reconstructed by slicing the matching
 * [MushafAyahTextEntity.text] with the inclusive
 * [firstWordPosition]..[lastWordPosition] range.
 *
 * [script] names the edition ([com.arshadshah.nimaz.domain.model.MushafScript] entry name),
 * which is what makes this table hold every line-accurate edition at once rather than one
 * table per edition — adding an edition is data, not schema.
 *
 * `line_type` is one of `ayah`, `surah_header`, or `basmalah`. Header and basmalah lines
 * carry only [surahId] for context; their [ayahId] and word positions are null.
 */
@Entity(
    tableName = "mushaf_layout_lines",
    indices = [
        Index(value = ["script", "page", "line"]),
        Index(value = ["script"])
    ]
)
data class MushafLayoutLineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val script: String, // MushafScript enum name, e.g. "INDOPAK_16"
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
