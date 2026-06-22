package com.arshadshah.nimaz.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.data.local.database.NimazDatabase.Companion.MIGRATION_12_13
import com.arshadshah.nimaz.data.local.database.NimazDatabase.Companion.PREPACKAGED_CALLBACK
import com.arshadshah.nimaz.data.local.database.dao.AsmaUlHusnaDao
import com.arshadshah.nimaz.data.local.database.dao.AsmaUnNabiDao
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranBookmarkEntity
import com.arshadshah.nimaz.data.local.database.entity.QuranFavoriteEntity
import com.arshadshah.nimaz.data.local.database.entity.ReadingProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerHighlightEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerNoteEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerTextEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity

@Database(
    entities = [
        // Quran
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        QuranBookmarkEntity::class,
        QuranFavoriteEntity::class,
        ReadingProgressEntity::class,
        SurahInfoEntity::class,
        // Hadith
        HadithBookEntity::class,
        HadithEntity::class,
        HadithBookmarkEntity::class,
        // Dua
        DuaCategoryEntity::class,
        DuaEntity::class,
        DuaBookmarkEntity::class,
        DuaProgressEntity::class,
        // Prayer & Fasting
        PrayerRecordEntity::class,
        FastRecordEntity::class,
        MakeupFastEntity::class,
        // Tasbih
        TasbihPresetEntity::class,
        TasbihSessionEntity::class,
        // Zakat
        ZakatHistoryEntity::class,
        // Tafseer
        TafseerTextEntity::class,
        TafseerHighlightEntity::class,
        TafseerNoteEntity::class,
        // Khatam
        KhatamEntity::class,
        KhatamAyahEntity::class,
        KhatamDailyLogEntity::class,
        // Asma ul Husna
        AsmaUlHusnaEntity::class,
        AsmaUlHusnaBookmarkEntity::class,
        // Asma un Nabi
        AsmaUnNabiEntity::class,
        AsmaUnNabiBookmarkEntity::class,
        // Prophets
        ProphetEntity::class,
        ProphetBookmarkEntity::class,
        // Help (data-driven, seeded at runtime from help.json)
        HelpTopicEntity::class,
        HelpItemEntity::class,
        HelpStepEntity::class,
        HelpStringEntity::class,
        // Qaida (Noorani Qaida reader; content seeded from prepopulated DB,
        // progress tables created empty and written at runtime)
        QaidaLessonEntity::class,
        QaidaLetterEntity::class,
        QaidaLineEntity::class,
        QaidaCellEntity::class,
        QaidaLessonProgressEntity::class,
        QaidaCellProgressEntity::class,
        // Other
        LocationEntity::class,
        IslamicEventEntity::class
    ],
    version = 17,
    exportSchema = true
)
abstract class NimazDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun prayerDao(): PrayerDao
    abstract fun fastingDao(): FastingDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun locationDao(): LocationDao
    abstract fun islamicEventDao(): IslamicEventDao
    abstract fun zakatDao(): ZakatDao
    abstract fun tafseerDao(): TafseerDao
    abstract fun khatamDao(): KhatamDao
    abstract fun asmaUlHusnaDao(): AsmaUlHusnaDao
    abstract fun asmaUnNabiDao(): AsmaUnNabiDao
    abstract fun prophetDao(): ProphetDao
    abstract fun helpDao(): HelpDao
    abstract fun qaidaDao(): QaidaDao

    companion object {
        const val DATABASE_NAME = "nimaz_database"

        // Current Room schema version. Keep in sync with @Database(version = ...)
        // above. Exposed so crash reports can be tagged with the schema version,
        // which makes migration-related crashes far easier to diagnose.
        const val SCHEMA_VERSION = 17

        // Tables that gained an `updatedAt` column in schema v10/v11.
        private val UPDATED_AT_TABLES = listOf(
            "quran_favorites",
            "tasbih_presets",
            "tasbih_sessions",
            "zakat_history",
            "khatam_ayahs",
            "khatam_daily_log",
        )

        /**
         * Brings a database that was created from the legacy pre-packaged asset
         * in line with the current schema: it adds the `updatedAt` columns
         * introduced in v10/v11 and recreates the tafseer composite indices
         * under the names Room expects (the generator used "*_ayah_tafseer").
         *
         * The shipped asset was stamped at user_version 12 while it still lacked
         * these, so it has to be repaired through two different paths:
         *  - when the asset is freshly copied, via [PREPACKAGED_CALLBACK], and
         *  - when a device already sits at version 12 with the stale schema, via
         *    [MIGRATION_12_13].
         *
         * Every statement is idempotent, so running it through either path — or
         * both, or once the asset is finally regenerated — is always safe.
         */
        private fun repairLegacyAssetSchema(db: SupportSQLiteDatabase) {
            UPDATED_AT_TABLES.forEach { table ->
                db.addColumnIfMissing(table, "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            }

            db.execSQL("DROP INDEX IF EXISTS `index_tafseer_texts_ayah_tafseer`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tafseer_texts_ayah_id_tafseer_id` ON `tafseer_texts` (`ayah_id`, `tafseer_id`)")

            db.execSQL("DROP INDEX IF EXISTS `index_tafseer_highlights_ayah_tafseer`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tafseer_highlights_ayah_id_tafseer_id` ON `tafseer_highlights` (`ayah_id`, `tafseer_id`)")

            db.execSQL("DROP INDEX IF EXISTS `index_tafseer_notes_ayah_tafseer`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tafseer_notes_ayah_id_tafseer_id` ON `tafseer_notes` (`ayah_id`, `tafseer_id`)")
        }

        /**
         * Repairs the bundled, pre-packaged database right after it is copied
         * from assets and *before* Room validates its schema. Only runs on a
         * fresh copy, which is why the same repair is also exposed as
         * [MIGRATION_12_13] for devices that already hold the stale database.
         */
        val PREPACKAGED_CALLBACK = object : PrepackagedDatabaseCallback() {
            override fun onOpenPrepackagedDatabase(db: SupportSQLiteDatabase) {
                repairLegacyAssetSchema(db)
            }
        }

        // Devices that installed an earlier release received the pre-packaged
        // asset stamped at user_version 12 while it was still missing the
        // `updatedAt` columns (v10/v11) and shipped the tafseer composite
        // indices under the wrong names. Because their database already reports
        // version 12, neither the pre-packaged copy callback nor the pre-12
        // migrations ever run again, so Room validates the stale schema and
        // crashes on launch with "Pre-packaged database has an invalid schema"
        // (most visibly on quran_favorites, the first affected table). Re-apply
        // the same idempotent repairs here so those installs heal on upgrade.
        // Adds a `category` column to tasbih_presets and backfills the known
        // default adhkar (the prepackaged DB and any already-seeded extras had no
        // category, so the category tabs showed nothing). Idempotent.
        // Adds the `narrator_chain` column to `hadiths` so a curated chain of
        // narration (isnād) can be stored; when absent the reader derives the
        // chain from the Arabic text. Room runs migrations even after
        // createFromAsset, so this column is added for both fresh installs (whose
        // asset predates it) and existing users. Idempotent.
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("hadiths", "narrator_chain", "TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("tasbih_presets", "category", "TEXT")
                db.execSQL(
                    "UPDATE tasbih_presets SET category = 'after_prayer' WHERE name IN " +
                            "('SubhanAllah','Alhamdulillah','Allahu Akbar'," +
                            "'La ilaha illallahu wahdah','SubhanAllahi wa bihamdih')"
                )
                db.execSQL(
                    "UPDATE tasbih_presets SET category = 'daily' WHERE name IN " +
                            "('La ilaha illallah','Astaghfirullah')"
                )
                db.execSQL(
                    "UPDATE tasbih_presets SET category = 'morning' WHERE name IN " +
                            "('Asbahna wa asbahal-mulku lillah','Bismillahilladhi la yadurr'," +
                            "'Radeetu billahi Rabba')"
                )
                db.execSQL(
                    "UPDATE tasbih_presets SET category = 'evening' WHERE name IN " +
                            "('Amsayna wa amsal-mulku lillah','A''udhu bikalimatillahit-tammat'," +
                            "'Allahumma bika amsayna')"
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                repairLegacyAssetSchema(db)
            }
        }

        // Adds the data-driven Help content tables. Room runs migrations even
        // after createFromAsset, so this creates the (empty) tables for both
        // fresh installs and existing users; HelpContentSeeder fills them from
        // the bundled help.json at runtime.
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `help_topic` (
                        `id` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        `icon_key` TEXT NOT NULL,
                        `color_key` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `help_item` (
                        `id` TEXT NOT NULL,
                        `topic_id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        `icon_key` TEXT,
                        `estimated_minutes` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_item_topic_id` ON `help_item` (`topic_id`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `help_step` (
                        `id` TEXT NOT NULL,
                        `item_id` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        `deeplink_route` TEXT,
                        `path_labels` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_step_item_id` ON `help_step` (`item_id`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `help_string` (
                        `owner_type` TEXT NOT NULL,
                        `owner_id` TEXT NOT NULL,
                        `field_key` TEXT NOT NULL,
                        `lang_code` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        PRIMARY KEY(`owner_type`, `owner_id`, `field_key`, `lang_code`)
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_string_owner_type_owner_id_lang_code` ON `help_string` (`owner_type`, `owner_id`, `lang_code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_help_string_lang_code` ON `help_string` (`lang_code`)")
            }
        }

        // Adds the Qaida (Noorani Qaida reader) tables. Room runs migrations
        // even after createFromAsset, so this runs for both fresh installs and
        // existing users. On a fresh install the four content tables
        // (lessons/letters/lines/cells) already exist with data in the
        // pre-packaged DB, so `CREATE TABLE IF NOT EXISTS` is a no-op there;
        // on an upgrade they are created empty. The two progress tables are
        // user data and are always created empty. Every statement is
        // idempotent so running it through either path is safe.
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Content: lessons
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_lessons` (
                        `id` INTEGER NOT NULL,
                        `lesson_number` INTEGER NOT NULL,
                        `title_english` TEXT NOT NULL,
                        `title_arabic` TEXT NOT NULL,
                        `title_transliteration` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `concept_tags` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )

                // Content: letters (reference table)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_letters` (
                        `id` INTEGER NOT NULL,
                        `letter_arabic` TEXT NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `isolated_form` TEXT NOT NULL,
                        `initial_form` TEXT,
                        `medial_form` TEXT,
                        `final_form` TEXT,
                        `is_connecting` INTEGER NOT NULL,
                        `makhraj_area` TEXT NOT NULL,
                        `makhraj_detail` TEXT NOT NULL,
                        `phonetic_hint` TEXT,
                        `audio_key` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent()
                )

                // Content: lines (FK → lessons)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_lines` (
                        `id` INTEGER NOT NULL,
                        `lesson_id` INTEGER NOT NULL,
                        `line_number` INTEGER NOT NULL,
                        `line_type` TEXT NOT NULL,
                        `instruction_english` TEXT,
                        `instruction_arabic` TEXT,
                        `display_order` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`lesson_id`) REFERENCES `qaida_lessons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qaida_lines_lesson_id` ON `qaida_lines` (`lesson_id`)")

                // Content: cells (FK → lines CASCADE, FK → letters SET NULL)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_cells` (
                        `id` INTEGER NOT NULL,
                        `line_id` INTEGER NOT NULL,
                        `lesson_id` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `text_arabic` TEXT NOT NULL,
                        `transliteration` TEXT NOT NULL,
                        `token_type` TEXT NOT NULL,
                        `audio_key` TEXT NOT NULL,
                        `highlight_group` TEXT,
                        `letter_id` INTEGER,
                        `notes` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`line_id`) REFERENCES `qaida_lines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`letter_id`) REFERENCES `qaida_letters`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qaida_cells_line_id` ON `qaida_cells` (`line_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qaida_cells_lesson_id` ON `qaida_cells` (`lesson_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_qaida_cells_letter_id` ON `qaida_cells` (`letter_id`)")

                // User progress: per-lesson (created empty)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_lesson_progress` (
                        `lesson_id` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `stars` INTEGER NOT NULL,
                        `last_cell_id` INTEGER,
                        `completed_cells` INTEGER NOT NULL,
                        `total_cells` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`lesson_id`)
                    )
                """.trimIndent()
                )

                // User progress: per-cell, optional fine-grained (created empty)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `qaida_cell_progress` (
                        `lesson_id` INTEGER NOT NULL,
                        `cell_id` INTEGER NOT NULL,
                        `heard_count` INTEGER NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        `last_practiced_at` INTEGER NOT NULL,
                        PRIMARY KEY(`lesson_id`, `cell_id`)
                    )
                """.trimIndent()
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix incorrect start_page values in surahs table
                // The start_page column had values from a different Mushaf edition
                // that didn't match the actual ayah page data
                db.execSQL(
                    """
                    UPDATE surahs SET start_page = (
                        SELECT MIN(a.page) FROM ayahs a WHERE a.surah_id = surahs.id
                    )
                """.trimIndent()
                )
            }
        }

        // Schema v10 added `updatedAt` to quran_favorites. The original release
        // bumped the database version to 10 without ever registering this
        // migration, so any device sitting at schema v9 crashes on launch with
        // "A migration from 9 to 10 was required but not found" the moment Room
        // opens the database. Restore the missing step here.
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.addColumnIfMissing(
                    "quran_favorites",
                    "updatedAt",
                    "INTEGER NOT NULL DEFAULT $now"
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                // quran_favorites.updatedAt already exists at schema v10 (added by
                // MIGRATION_9_10). Guard every ALTER so re-running on a database
                // that already has the column is a no-op instead of a
                // "duplicate column name" crash.
                db.addColumnIfMissing(
                    "quran_favorites",
                    "updatedAt",
                    "INTEGER NOT NULL DEFAULT $now"
                )
                db.addColumnIfMissing(
                    "tasbih_presets",
                    "updatedAt",
                    "INTEGER NOT NULL DEFAULT $now"
                )
                db.addColumnIfMissing(
                    "tasbih_sessions",
                    "updatedAt",
                    "INTEGER NOT NULL DEFAULT $now"
                )
                db.addColumnIfMissing("khatam_ayahs", "updatedAt", "INTEGER NOT NULL DEFAULT $now")
                db.addColumnIfMissing(
                    "khatam_daily_log",
                    "updatedAt",
                    "INTEGER NOT NULL DEFAULT $now"
                )
                db.addColumnIfMissing("zakat_history", "updatedAt", "INTEGER NOT NULL DEFAULT $now")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create asma_ul_husna table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `asma_ul_husna` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `benefits` TEXT NOT NULL,
                        `quran_references` TEXT NOT NULL,
                        `usage_in_dua` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create asma_ul_husna_bookmarks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `asma_ul_husna_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_asma_ul_husna_bookmarks_name_id` ON `asma_ul_husna_bookmarks` (`name_id`)")

                // Create asma_un_nabi table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `asma_un_nabi` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `meaning` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create asma_un_nabi_bookmarks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `asma_un_nabi_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_asma_un_nabi_bookmarks_name_id` ON `asma_un_nabi_bookmarks` (`name_id`)")

                // Create prophets table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prophets` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `number` INTEGER NOT NULL,
                        `name_arabic` TEXT NOT NULL,
                        `name_english` TEXT NOT NULL,
                        `name_transliteration` TEXT NOT NULL,
                        `title_arabic` TEXT NOT NULL,
                        `title_english` TEXT NOT NULL,
                        `story_summary` TEXT NOT NULL,
                        `key_lessons` TEXT NOT NULL,
                        `quran_mentions` TEXT NOT NULL,
                        `era` TEXT NOT NULL,
                        `lineage` TEXT NOT NULL,
                        `years_lived` TEXT NOT NULL,
                        `place_of_preaching` TEXT NOT NULL,
                        `miracles` TEXT NOT NULL,
                        `display_order` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create prophet_bookmarks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prophet_bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `prophet_id` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_prophet_bookmarks_prophet_id` ON `prophet_bookmarks` (`prophet_id`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create khatams table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `khatams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'active',
                        `is_active` INTEGER NOT NULL DEFAULT 0,
                        `daily_target` INTEGER NOT NULL DEFAULT 20,
                        `deadline` INTEGER,
                        `reminder_enabled` INTEGER NOT NULL DEFAULT 0,
                        `reminder_time` TEXT,
                        `total_ayahs_read` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        `started_at` INTEGER,
                        `completed_at` INTEGER,
                        `updated_at` INTEGER NOT NULL
                    )
                """.trimIndent()
                )

                // Create khatam_ayahs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `khatam_ayahs` (
                        `khatam_id` INTEGER NOT NULL,
                        `ayah_id` INTEGER NOT NULL,
                        `read_at` INTEGER NOT NULL,
                        PRIMARY KEY(`khatam_id`, `ayah_id`),
                        FOREIGN KEY(`khatam_id`) REFERENCES `khatams`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                // Create indexes for khatam_ayahs
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_khatam_id` ON `khatam_ayahs` (`khatam_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_ayah_id` ON `khatam_ayahs` (`ayah_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_ayahs_read_at` ON `khatam_ayahs` (`read_at`)")

                // Create khatam_daily_log table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `khatam_daily_log` (
                        `khatam_id` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `ayahs_read` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`khatam_id`, `date`),
                        FOREIGN KEY(`khatam_id`) REFERENCES `khatams`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent()
                )

                // Create index for khatam_daily_log
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_khatam_daily_log_khatam_id` ON `khatam_daily_log` (`khatam_id`)")
            }
        }
    }
}

/**
 * Adds [column] to [table] only when it is not already present. SQLite has no
 * "ADD COLUMN IF NOT EXISTS", so a migration that runs against a database where
 * the column already exists would otherwise throw "duplicate column name".
 * Keeping the ALTER idempotent lets migrations stay safe regardless of which
 * exact schema version a device is upgrading from.
 */
private fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definition: String,
) {
    val columnExists = query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
            .any { it == column }
    }
    if (!columnExists) {
        execSQL("ALTER TABLE `$table` ADD COLUMN $column $definition")
    }
}
