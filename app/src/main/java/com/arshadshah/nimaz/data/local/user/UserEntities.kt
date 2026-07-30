package com.arshadshah.nimaz.data.local.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * What the user made, as opposed to what we shipped them.
 *
 * These tables used to live in the content database — created empty inside a 128 MB
 * asset that cannot hold a single row of them, so a person's bookmarks shared a file
 * with the corpus and their schema rode along in every content release. They are here
 * now, in a database Room creates on the device and nothing else ever writes.
 *
 * Two of them are consolidations, because seven bookmark tables and three progress
 * tables were seven and three shapes of the same idea.
 */

/** What a bookmark can point at. Stored as text so the table is readable in a dump. */
object BookmarkKind {
    const val AYAH = "AYAH"
    const val HADITH = "HADITH"
    const val DUA = "DUA"
    const val ASMA_UL_HUSNA = "ASMA_UL_HUSNA"
    const val ASMA_UN_NABI = "ASMA_UN_NABI"
    const val PROPHET = "PROPHET"
}

/**
 * One row per thing the user marked, whatever kind of thing it is.
 *
 * Replaces `quran_bookmarks`, `quran_favorites`, `hadith_bookmarks`, `dua_bookmarks`,
 * `asma_ul_husna_bookmarks`, `asma_un_nabi_bookmarks` and `prophet_bookmarks` — seven
 * tables with the same three questions (what did you mark, when, and did you write a
 * note) and seven different spellings of them, three of which called the same column
 * `is_favorite`, `isFavorite` and `color`.
 *
 * `bookmarked` and `favourite` are separate flags rather than separate tables, because
 * a verse can be both and used to be a row in each. Nothing is lost in the merge:
 * `context_id` keeps the surah, book or category the target belongs to, and `ordinal`
 * keeps its number within that context — the columns the old tables carried so a
 * bookmark could be rendered without touching the content database.
 */
@Entity(
    tableName = "bookmarks",
    primaryKeys = ["kind", "target_id"],
    indices = [Index(value = ["kind", "created_at"]), Index(value = ["kind", "favourite"])],
)
data class BookmarkEntity(
    val kind: String,
    @ColumnInfo(name = "target_id") val targetId: Int,
    val bookmarked: Boolean = true,
    val favourite: Boolean = false,
    val note: String? = null,
    val colour: String? = null,
    /** Surah for an ayah, book for a hadith, category for a dua. Null where there is none. */
    @ColumnInfo(name = "context_id") val contextId: Int? = null,
    /** The target's number within its context: ayah in surah, hadith in book. */
    val ordinal: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** What a progress row is about. */
object ProgressKind {
    const val DUA = "DUA"
    const val QAIDA_LESSON = "QAIDA_LESSON"
    const val QAIDA_CELL = "QAIDA_CELL"
}

/**
 * Per-item progress: a dua repeated so many times today, a qaida lesson at three stars,
 * a qaida cell heard four times.
 *
 * Replaces `dua_progress`, `qaida_lesson_progress` and `qaida_cell_progress`. All three
 * counted something towards something, recorded whether it was finished, and stamped
 * when — in three vocabularies (`completedCount`/`targetCount`/`isCompleted`,
 * `completed_cells`/`total_cells`/`status`/`stars`, `heard_count`/`is_completed`).
 *
 * `date` is part of the key because dua progress is per-day and the others are not;
 * they use 0, which keeps one row per target rather than one per day.
 *
 * `reading_progress` is deliberately **not** folded in here. It is a single row with six
 * typed fields, read on every app open to answer "where was I" — a different shape and a
 * different access pattern from per-item counting, and flattening it into a generic
 * counter or a JSON blob would make the hottest read in the app the least legible.
 */
@Entity(
    tableName = "progress",
    primaryKeys = ["kind", "target_id", "date"],
    indices = [Index(value = ["kind", "is_completed"]), Index(value = ["kind", "context_id"])],
)
data class ProgressEntity(
    val kind: String,
    @ColumnInfo(name = "target_id") val targetId: Int,
    /** Day for dua progress, 0 for anything counted once per target. */
    val date: Long = 0,
    /** The lesson a qaida cell belongs to. Null where there is no parent. */
    @ColumnInfo(name = "context_id") val contextId: Int? = null,
    /** Repetitions done, cells completed, times heard. */
    val completed: Int = 0,
    /** Repetitions or cells aimed at, where the target declares one. */
    val total: Int? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    /** A qaida lesson's status string; null for kinds that have no state machine. */
    val state: String? = null,
    /** Stars for a qaida lesson. */
    val score: Int? = null,
    /** A qaida lesson's last cell, so "resume" does not need a second table. */
    @ColumnInfo(name = "resume_id") val resumeId: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * A counting preset the user made.
 *
 * `tasbih_presets` in the content database shipped the defaults *and* accepted the ones a
 * person created, distinguished by an `is_custom` flag. That made it the one table that was
 * both content and user data, and it made "delete all my data" reach into the content
 * database to find rows that belonged to the user — which is exactly the confusion this
 * split exists to end. Content is not user data: the defaults are shipped and read-only,
 * and these are the user's, here, where nothing but the user writes.
 *
 * The shape mirrors the shipped preset so the repository can present one list.
 */
@Entity(tableName = "custom_tasbih_presets")
data class CustomTasbihPresetEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    @ColumnInfo(name = "target_count") val targetCount: Int,
    @ColumnInfo(name = "display_order") val displayOrder: Int,
    val category: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
