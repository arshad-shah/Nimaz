package com.arshadshah.nimaz.data.local.user

import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao

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
        CustomTasbihPresetEntity::class,
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

    abstract fun customPresetDao(): CustomPresetDao

    abstract fun readingProgressDao(): ReadingProgressDao

    abstract fun tasbihSessionDao(): TasbihSessionDao

    abstract fun tafseerUserDao(): TafseerUserDao

    // Wholly the user's: every method on these reads or writes only their own rows, so they
    // move here as they are rather than being split.
    abstract fun prayerDao(): PrayerDao

    abstract fun fastingDao(): FastingDao

    abstract fun locationDao(): LocationDao

    abstract fun zakatDao(): ZakatDao

    abstract fun khatamDao(): KhatamDao

    companion object {
        const val DATABASE_NAME = "nimaz_user_database"

        /**
         * Tables that carried user data in the content database, in the order they must be
         * read: parents before children, so a foreign key never fails mid-copy.
         *
         * `tasbih_presets` is read but not moved wholesale: only the rows with
         * `is_custom = 1` are the user's, and they become `custom_tasbih_presets` here. The
         * shipped defaults stay in the content database, read-only, where they belong.
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
            // read for its `is_custom = 1` rows only
            "tasbih_presets",
        )
    }
}

/**
 * Copies a user's data out of the content database the first time the user database is
 * opened, mapping the seven bookmark tables and three progress tables into the two that
 * replace them.
 *
 * Written as one transaction over two `ATTACH`ed files rather than as a Kotlin loop, so an
 * interrupted copy leaves nothing half-written and a second attempt is a no-op: every
 * statement is `INSERT OR IGNORE` keyed on what the new tables key on.
 *
 * The legacy tables are never modified. Dropping them would be the tidy thing and the
 * wrong thing — a bug here must be survivable, and it is only survivable while the
 * original rows are still on disk.
 *
 * ## Why this opens a connection of its own
 *
 * Neither database is opened as `main`: both are attached to a throwaway in-memory database
 * that this object owns and closes. That is not tidiness, it is the fix for a crash.
 *
 * `SQLiteDatabase.execSQL` inspects every statement, and on the first `ATTACH` a connection
 * ever sees it clears `ENABLE_WRITE_AHEAD_LOGGING` and reconfigures the pool. Changing the
 * open flags that way makes the framework **close the primary connection and open a new
 * one**. Room keeps its entire invalidation tracker in that connection's temporary schema —
 * `room_table_modification_log` is a `CREATE TEMP TABLE` and the per-table triggers are
 * `CREATE TEMP TRIGGER` — and it only builds them when *it* opens a connection. After the
 * swap they are gone for the life of the process, and the next Flow to start observing a
 * table dies in `syncTriggers` with
 *
 *     android.database.sqlite.SQLiteException: no such table: room_table_modification_log,
 *     while compiling: INSERT OR IGNORE INTO room_table_modification_log VALUES(4, 0)
 *
 * which is a crash on launch for anyone the copy runs for. So the `ATTACH` has to happen
 * somewhere Room is not looking. An in-memory `main` is the one place that costs nothing:
 * it has no journal mode to be taken out of and its pool is capped at a single connection
 * either way, so the reconfigure is a no-op and both real files keep the journal mode Room
 * gave them.
 *
 * The flip side is that Room does not see these writes — the triggers that would have
 * noticed them belong to a different connection — so this must finish before anything
 * starts observing. `AppInitializer` awaits it and the splash screen holds until it does.
 */
object LegacyUserDataImport {

    /**
     * Copies [legacyPath]'s user rows into [userPath], and returns how many statements
     * moved anything.
     *
     * Both are file paths rather than an open database on purpose — see the note above on
     * why this cannot borrow Room's connection. [userPath] must already hold the schema;
     * Room creates it when it opens the database, which [UserDataMigrator] does first.
     */
    fun run(userPath: String, legacyPath: String): Int {
        if (!java.io.File(legacyPath).exists()) return 0
        val db = SQLiteDatabase.create(null)
        return try {
            db.execSQL("ATTACH DATABASE ? AS user", arrayOf<Any?>(userPath))
            db.execSQL("ATTACH DATABASE ? AS legacy", arrayOf<Any?>(legacyPath))
            if (isNeeded(db)) copy(db) else 0
        } finally {
            // Closing detaches both; the in-memory database itself never held anything.
            db.close()
        }
    }

    /** Nothing to do once the user database has rows of its own. */
    private fun isNeeded(db: SQLiteDatabase): Boolean =
        isEmpty(db, "bookmarks") && isEmpty(db, "reading_progress")

    private fun isEmpty(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery("SELECT 1 FROM user.`$table` LIMIT 1", null).use { !it.moveToFirst() }

    private fun copy(db: SQLiteDatabase): Int {
        var copied = 0
        // Non-exclusive (`BEGIN IMMEDIATE`): both attached files are open in WAL on Room's own
        // connections at this point, and an exclusive transaction would take locks that lock
        // Room's readers out for the length of the copy.
        db.beginTransactionNonExclusive()
        try {
            copied += bookmarks(db)
            copied += progress(db)
            copied += customPresets(db)
            copied += straightCopies(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return copied
    }

    private fun has(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM legacy.sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun exec(db: SQLiteDatabase, table: String, sql: String): Int {
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
    private fun bookmarks(db: SQLiteDatabase): Int {
        var n = 0
        n += exec(
            db, "quran_bookmarks",
            """
            INSERT OR IGNORE INTO user.bookmarks
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
                UPDATE user.bookmarks SET favourite = 1
                WHERE kind = '${BookmarkKind.AYAH}'
                  AND target_id IN (SELECT ayahId FROM legacy.quran_favorites)
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO user.bookmarks
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
            INSERT OR IGNORE INTO user.bookmarks
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
            INSERT OR IGNORE INTO user.bookmarks
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
                INSERT OR IGNORE INTO user.bookmarks
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

    /**
     * The presets the user made, out of the table that shipped the defaults.
     *
     * `is_custom = 1` is the only thing that ever distinguished them. Taking them here is what
     * lets "delete all my data" stop reaching into the content database: content is not user
     * data, and a preset somebody wrote is not content.
     *
     * The legacy columns are `target_count` and `display_order` — snake_case in the table,
     * camelCase on the entity. Writing the entity's names here compiled fine and failed on a
     * device with "no such column: targetCount", on *every app launch*, because this runs when
     * the user database is first opened. The instrumented suite caught it; the unit test had
     * built its own fixture and did not include this table, so it could not.
     */
    private fun customPresets(db: SQLiteDatabase): Int = exec(
        db, "tasbih_presets",
        """
        INSERT OR IGNORE INTO user.custom_tasbih_presets
            (id, name, arabic, transliteration, translation, target_count, display_order,
             category, created_at, updated_at)
        SELECT id, name, arabic, transliteration, translation, target_count, display_order,
               category, updatedAt, updatedAt
        FROM legacy.tasbih_presets WHERE is_custom = 1
        """.trimIndent(),
    )

    /** Three progress tables into one. `reading_progress` copies across unchanged. */
    private fun progress(db: SQLiteDatabase): Int {
        var n = 0
        n += exec(
            db, "dua_progress",
            """
            INSERT OR IGNORE INTO user.progress
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
            INSERT OR IGNORE INTO user.progress
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
            INSERT OR IGNORE INTO user.progress
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
    private fun straightCopies(db: SQLiteDatabase): Int {
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
            db.execSQL(
                "INSERT OR IGNORE INTO user.`$table` ($list) SELECT $list FROM legacy.`$table`"
            )
            n++
        }
        return n
    }

    /**
     * The columns the *new* database declares, so a legacy extra column is left behind.
     *
     * Also the guard for a table the new database does not have at all: `PRAGMA table_info`
     * on a missing table returns no rows, which [straightCopies] reads as "skip".
     */
    private fun columnsOf(db: SQLiteDatabase, table: String): List<String> =
        db.rawQuery("PRAGMA user.table_info(`$table`)", null).use { cursor ->
            val index = cursor.getColumnIndex("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(index)) }
        }
}
