package com.arshadshah.nimaz.data.local.user

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity

const val NIMAZ_USER_DATABASE_VERSION = 1

/**
 * Everything the user made. Created by Room on the device, never shipped, never
 * overwritten by a content release.
 *
 * The content database arrives as a 128 MB asset and is replaced wholesale by every
 * release. Until now it also *contained* the user's data — twenty-two tables created
 * empty inside that asset — which meant a person's bookmarks, prayer records and khatam
 * progress lived in the same file as the corpus, their schema was part of every content
 * schema version, and the only thing standing between a content release and someone's
 * data was that `createFromAsset` happens to not re-copy on upgrade.
 *
 * Two databases makes that structural instead of incidental. Content is read-only and
 * disposable: if it is wrong, ship another one. This is neither.
 *
 * ## Nothing is lost on the way here
 *
 * An existing install already has rows in the old file. [LegacyUserDataImport] copies
 * them across the first time this database is opened, and the old tables are **left in
 * place** rather than dropped: Room ignores tables it does not declare, so there is no
 * destructive step anywhere in the transition. If the copy is interrupted it runs again;
 * if it has already happened it does nothing.
 */
@Database(
    entities = [
        BookmarkEntity::class,
        ProgressEntity::class,
        ReadingProgressEntity::class,
        PrayerRecordEntity::class,
        FastRecordEntity::class,
        MakeupFastEntity::class,
        KhatamEntity::class,
        KhatamAyahEntity::class,
        KhatamDailyLogEntity::class,
        TasbihSessionEntity::class,
        ZakatHistoryEntity::class,
        TafseerHighlightEntity::class,
        TafseerNoteEntity::class,
        LocationEntity::class,
    ],
    version = NIMAZ_USER_DATABASE_VERSION,
    exportSchema = true,
)
abstract class NimazUserDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    abstract fun progressDao(): ProgressDao

    companion object {
        const val DATABASE_NAME = "nimaz_user_database"

        /**
         * Tables that carried user data in the content database, in the order they must be
         * read: parents before children, so a foreign key never fails mid-copy.
         *
         * `tasbih_presets` is absent on purpose. It holds both the presets we ship and the
         * ones a user creates, so it is the one table that is genuinely both content and
         * user data, and splitting it needs a decision about what happens to a shipped
         * preset a user has edited. It stays in the content database for now.
         */
        val LEGACY_TABLES = listOf(
            "reading_progress",
            "quran_bookmarks",
            "quran_favorites",
            "hadith_bookmarks",
            "dua_bookmarks",
            "dua_progress",
            "prayer_records",
            "fast_records",
            "makeup_fasts",
            "khatams",
            "khatam_ayahs",
            "khatam_daily_log",
            "tasbih_sessions",
            "zakat_history",
            "tafseer_highlights",
            "tafseer_notes",
            "locations",
            "asma_ul_husna_bookmarks",
            "asma_un_nabi_bookmarks",
            "prophet_bookmarks",
            "qaida_lesson_progress",
            "qaida_cell_progress",
        )
    }
}

/**
 * Copies a user's data out of the content database the first time the user database is
 * opened, mapping the seven bookmark tables and three progress tables into the two that
 * replace them.
 *
 * Written as one transaction over an `ATTACH`ed legacy file rather than as a Kotlin loop,
 * so an interrupted copy leaves nothing half-written and a second attempt is a no-op:
 * every statement is `INSERT OR IGNORE` keyed on what the new tables key on.
 *
 * The legacy tables are never modified. Dropping them would be the tidy thing and the
 * wrong thing — a bug here must be survivable, and it is only survivable while the
 * original rows are still on disk.
 */
object LegacyUserDataImport {

    /** True when the legacy file exists and has at least one of the tables we read. */
    fun isNeeded(db: SupportSQLiteDatabase, legacyPath: String): Boolean {
        if (!java.io.File(legacyPath).exists()) return false
        return db.query("SELECT COUNT(*) FROM bookmarks").use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) == 0
        } && db.query("SELECT COUNT(*) FROM reading_progress").use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) == 0
        }
    }

    fun run(db: SupportSQLiteDatabase, legacyPath: String): Int {
        db.execSQL("ATTACH DATABASE ? AS legacy", arrayOf(legacyPath))
        var copied = 0
        try {
            db.beginTransaction()
            try {
                copied += bookmarks(db)
                copied += progress(db)
                copied += straightCopies(db)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.execSQL("DETACH DATABASE legacy")
        }
        return copied
    }

    private fun has(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query(
            "SELECT 1 FROM legacy.sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun exec(db: SupportSQLiteDatabase, table: String, sql: String): Int {
        if (!has(db, table)) return 0
        db.execSQL(sql)
        return 1
    }

    /**
     * Seven tables into one.
     *
     * A verse that was both bookmarked and favourited was a row in each, and must end up
     * as one row with both flags — hence the favourites pass is an `UPDATE` of anything the
     * bookmark pass already inserted, then an `INSERT` for the rest.
     */
    private fun bookmarks(db: SupportSQLiteDatabase): Int {
        var n = 0
        n += exec(
            db, "quran_bookmarks",
            """
            INSERT OR IGNORE INTO bookmarks
                (kind, target_id, bookmarked, favourite, note, colour, context_id, ordinal,
                 created_at, updated_at)
            SELECT '${BookmarkKind.AYAH}', ayahId, 1, 0, note, color, surahNumber, ayahNumber,
                   createdAt, updatedAt
            FROM legacy.quran_bookmarks
            """.trimIndent(),
        )
        if (has(db, "quran_favorites")) {
            db.execSQL(
                """
                UPDATE bookmarks SET favourite = 1
                WHERE kind = '${BookmarkKind.AYAH}'
                  AND target_id IN (SELECT ayahId FROM legacy.quran_favorites)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO bookmarks
                    (kind, target_id, bookmarked, favourite, note, colour, context_id, ordinal,
                     created_at, updated_at)
                SELECT '${BookmarkKind.AYAH}', ayahId, 0, 1, NULL, NULL, surahNumber, ayahNumber,
                       createdAt, updatedAt
                FROM legacy.quran_favorites
                """.trimIndent()
            )
            n++
        }
        n += exec(
            db, "hadith_bookmarks",
            """
            INSERT OR IGNORE INTO bookmarks
                (kind, target_id, bookmarked, favourite, note, colour, context_id, ordinal,
                 created_at, updated_at)
            SELECT '${BookmarkKind.HADITH}', hadithId, 1, 0, note, color, bookId, hadithNumber,
                   createdAt, updatedAt
            FROM legacy.hadith_bookmarks
            """.trimIndent(),
        )
        n += exec(
            db, "dua_bookmarks",
            """
            INSERT OR IGNORE INTO bookmarks
                (kind, target_id, bookmarked, favourite, note, colour, context_id, ordinal,
                 created_at, updated_at)
            SELECT '${BookmarkKind.DUA}', duaId, 1, isFavorite, note, NULL, categoryId, NULL,
                   createdAt, updatedAt
            FROM legacy.dua_bookmarks
            """.trimIndent(),
        )
        for ((table, kind, column) in listOf(
            Triple("asma_ul_husna_bookmarks", BookmarkKind.ASMA_UL_HUSNA, "name_id"),
            Triple("asma_un_nabi_bookmarks", BookmarkKind.ASMA_UN_NABI, "name_id"),
            Triple("prophet_bookmarks", BookmarkKind.PROPHET, "prophet_id"),
        )) {
            n += exec(
                db, table,
                """
                INSERT OR IGNORE INTO bookmarks
                    (kind, target_id, bookmarked, favourite, note, colour, context_id, ordinal,
                     created_at, updated_at)
                SELECT '$kind', $column, 1, is_favorite, NULL, NULL, NULL, NULL,
                       created_at, created_at
                FROM legacy.$table
                """.trimIndent(),
            )
        }
        return n
    }

    /** Three progress tables into one. `reading_progress` copies across unchanged. */
    private fun progress(db: SupportSQLiteDatabase): Int {
        var n = 0
        n += exec(
            db, "dua_progress",
            """
            INSERT OR IGNORE INTO progress
                (kind, target_id, date, context_id, completed, total, is_completed, state,
                 score, resume_id, created_at, updated_at)
            SELECT '${ProgressKind.DUA}', duaId, date, NULL, completedCount, targetCount,
                   isCompleted, NULL, NULL, NULL, createdAt, createdAt
            FROM legacy.dua_progress
            """.trimIndent(),
        )
        n += exec(
            db, "qaida_lesson_progress",
            """
            INSERT OR IGNORE INTO progress
                (kind, target_id, date, context_id, completed, total, is_completed, state,
                 score, resume_id, created_at, updated_at)
            SELECT '${ProgressKind.QAIDA_LESSON}', lesson_id, 0, NULL, completed_cells,
                   total_cells, CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END, status,
                   stars, last_cell_id, updated_at, updated_at
            FROM legacy.qaida_lesson_progress
            """.trimIndent(),
        )
        n += exec(
            db, "qaida_cell_progress",
            """
            INSERT OR IGNORE INTO progress
                (kind, target_id, date, context_id, completed, total, is_completed, state,
                 score, resume_id, created_at, updated_at)
            SELECT '${ProgressKind.QAIDA_CELL}', cell_id, 0, lesson_id, heard_count, NULL,
                   is_completed, NULL, NULL, NULL, last_practiced_at, last_practiced_at
            FROM legacy.qaida_cell_progress
            """.trimIndent(),
        )
        return n
    }

    /**
     * The tables that move unchanged. Column lists are spelled out rather than
     * `SELECT *`: a `SELECT *` copy silently depends on column order matching, which is
     * exactly the kind of assumption that survives review and fails on one device.
     */
    private fun straightCopies(db: SupportSQLiteDatabase): Int {
        val copies = listOf(
            "reading_progress" to
                "id, lastReadSurah, lastReadAyah, lastReadPage, lastReadJuz, totalAyahsRead, " +
                "currentKhatmaCount, updatedAt",
            "prayer_records" to null,
            "fast_records" to null,
            "makeup_fasts" to null,
            "khatams" to null,
            "khatam_ayahs" to null,
            "khatam_daily_log" to null,
            "tasbih_sessions" to null,
            "zakat_history" to null,
            "tafseer_highlights" to null,
            "tafseer_notes" to null,
            "locations" to null,
        )
        var n = 0
        for ((table, columns) in copies) {
            if (!has(db, table)) continue
            val list = columns ?: columnsOf(db, table).joinToString(", ") { "`$it`" }
            if (list.isBlank()) continue
            db.execSQL("INSERT OR IGNORE INTO `$table` ($list) SELECT $list FROM legacy.`$table`")
            n++
        }
        return n
    }

    /** The columns the *new* database declares, so a legacy extra column is left behind. */
    private fun columnsOf(db: SupportSQLiteDatabase, table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val index = cursor.getColumnIndex("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(index)) }
        }
}
