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
/**
 * A verse's *place* in the mushaf. Not its text.
 *
 * Until schemaVersion 22 this row carried four renderings of the verse, a boolean and a
 * nullable string for prostration, and a nullable transliteration — of which the two script
 * columns were byte-identical in all 6,236 rows, `text_indopak` was NULL in all of them, and
 * `sajda_type` was NULL in 6,221. A table where most columns are absent for most rows is a
 * table modelling several things at once.
 *
 * Text now lives in [MushafAyahTextEntity], one row per (source, ayah), so a rendering is data
 * rather than a column: `UTHMANI`, `SIMPLE`, `INDOPAK`, `INDOPAK_13`. Prostration lives in
 * [SajdaEntity], fifteen rows instead of two columns on six thousand. `transliteration` and
 * `text_tajweed` stay for now — the first has no second source to sit beside, and the second is
 * JSON spans rather than a script, so filing it under a text *source* would conflate two things.
 */
data class AyahEntity(
    @PrimaryKey
    val id: Int, // Unique ayah id (1-6236)
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    @ColumnInfo(name = "number_in_surah")
    val numberInSurah: Int, // Ayah number within surah
    @ColumnInfo(name = "number_global")
    val numberGlobal: Int, // Global ayah number
    val juz: Int,
    val hizb: Int,
    val page: Int,
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

/**
 * The divisions of the mushaf, as ranges rather than columns on every verse.
 *
 * `ayahs.juz`, `.hizb` and `.page` answer "which juz is this verse in" and no question a
 * reader asks: where juz 18 begins, how many pages it runs, which ruku this is, what the next
 * quarter is. Those needed a scan over 6,236 rows and a MIN/MAX, which is why nothing did them.
 * Each of these tables answers them with one row. Ranges are inclusive global ayah ids and are
 * asserted by the data console to tile 1..6236 exactly once.
 *
 * `rukus` and `manzils` are content the corpus never carried at all.
 */
@Entity(tableName = "juzs")
data class JuzEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
)

@Entity(tableName = "hizb_quarters", indices = [Index(value = ["juz_number"])])
data class HizbQuarterEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "juz_number") val juzNumber: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
)

@Entity(tableName = "manzils")
data class ManzilEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
)

@Entity(tableName = "rukus", indices = [Index(value = ["surah_id"])])
data class RukuEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
)

@Entity(tableName = "pages")
data class PageEntity(
    @PrimaryKey val number: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
)

/**
 * A prostration verse. Fifteen rows, replacing `ayahs.sajda` and `ayahs.sajda_type`.
 *
 * `kind` is the corpus's classification. `upstreamKind` records where Tanzil's metadata
 * disagrees — 7:206 and 84:21 (corpus obligatory, Tanzil recommended) and 41:38 (the reverse) —
 * because obligation is a question of fiqh and discarding one of two answers would hide that
 * the question is open. The reader shows `kind`.
 */
@Entity(tableName = "sajdas", indices = [Index(value = ["sequence"])])
data class SajdaEntity(
    @PrimaryKey @ColumnInfo(name = "ayah_id") val ayahId: Int,
    val sequence: Int,
    val kind: String,
    @ColumnInfo(name = "upstream_kind") val upstreamKind: String? = null,
)

/** Per-surah spans and counts, so a surah header is one row instead of an aggregate query. */
@Entity(tableName = "surah_structure")
data class SurahStructureEntity(
    @PrimaryKey @ColumnInfo(name = "surah_id") val surahId: Int,
    @ColumnInfo(name = "ruku_count") val rukuCount: Int,
    @ColumnInfo(name = "start_ayah_id") val startAyahId: Int,
    @ColumnInfo(name = "end_ayah_id") val endAyahId: Int,
    @ColumnInfo(name = "start_page") val startPage: Int,
    @ColumnInfo(name = "end_page") val endPage: Int,
    @ColumnInfo(name = "has_basmalah") val hasBasmalah: Int,
    @ColumnInfo(name = "revelation_order") val revelationOrder: Int,
)
