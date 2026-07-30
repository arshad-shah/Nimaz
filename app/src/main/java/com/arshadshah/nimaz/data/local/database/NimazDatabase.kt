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
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.HelpDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.ProphetDao
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TafseerDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.entity.AsmaUlHusnaEntity
import com.arshadshah.nimaz.data.local.database.entity.AsmaUnNabiEntity
import com.arshadshah.nimaz.data.local.database.entity.AyahEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaCategoryEntity
import com.arshadshah.nimaz.data.local.database.entity.DuaEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithBookEntity
import com.arshadshah.nimaz.data.local.database.entity.HadithEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpItemEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStepEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpStringEntity
import com.arshadshah.nimaz.data.local.database.entity.HelpTopicEntity
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafAyahTextEntity
import com.arshadshah.nimaz.data.local.database.entity.MushafLayoutLineEntity
import com.arshadshah.nimaz.data.local.database.entity.ProphetEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahInfoEntity
import com.arshadshah.nimaz.data.local.database.entity.HizbQuarterEntity
import com.arshadshah.nimaz.data.local.database.entity.JuzEntity
import com.arshadshah.nimaz.data.local.database.entity.ManzilEntity
import com.arshadshah.nimaz.data.local.database.entity.PageEntity
import com.arshadshah.nimaz.data.local.database.entity.RukuEntity
import com.arshadshah.nimaz.data.local.database.entity.SajdaEntity
import com.arshadshah.nimaz.data.local.database.entity.SurahStructureEntity
import com.arshadshah.nimaz.data.local.database.entity.TafseerBlockEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TranslationEntity

/**
 * Single source of truth for the database schema version. Bump this (and add a
 * migration) for any schema change — it drives both the Room `@Database(version = …)`
 * annotation below and `NimazDatabase.SCHEMA_VERSION` (used to tag crash reports).
 */
const val NIMAZ_DATABASE_VERSION = 23

@Database(
    entities = [
        // Quran
        SurahEntity::class,
        AyahEntity::class,
        TranslationEntity::class,
        SurahInfoEntity::class,
        MushafAyahTextEntity::class,
        JuzEntity::class,
        HizbQuarterEntity::class,
        ManzilEntity::class,
        RukuEntity::class,
        PageEntity::class,
        SajdaEntity::class,
        SurahStructureEntity::class,
        MushafLayoutLineEntity::class,
        // Hadith
        HadithBookEntity::class,
        HadithEntity::class,
        // Dua
        DuaCategoryEntity::class,
        DuaEntity::class,
        // Prayer & Fasting
        // Tasbih
        TasbihPresetEntity::class,
        // Zakat
        // Tafseer
        TafseerBlockEntity::class,
        // Khatam
        // Asma ul Husna
        AsmaUlHusnaEntity::class,
        // Asma un Nabi
        AsmaUnNabiEntity::class,
        // Prophets
        ProphetEntity::class,
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
        // Other
        IslamicEventEntity::class
    ],
    version = NIMAZ_DATABASE_VERSION,
    exportSchema = true
)
abstract class NimazDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao
    abstract fun tasbihDao(): TasbihDao
    abstract fun islamicEventDao(): IslamicEventDao
    abstract fun tafseerDao(): TafseerDao
    abstract fun asmaUlHusnaDao(): AsmaUlHusnaDao
    abstract fun asmaUnNabiDao(): AsmaUnNabiDao
    abstract fun prophetDao(): ProphetDao
    abstract fun helpDao(): HelpDao
    abstract fun qaidaDao(): QaidaDao

    companion object {
        const val DATABASE_NAME = "nimaz_database"

        // Exposed so crash reports can be tagged with the schema version, which
        // makes migration-related crashes far easier to diagnose. Derived from the
        // single source of truth so it can never drift from @Database(version = …).
        const val SCHEMA_VERSION = NIMAZ_DATABASE_VERSION

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
            // Every repair below is guarded on the table existing, because this runs against
            // whatever shape the fetched artifact happens to have — and the artifact keeps
            // getting smaller. Five of these `updatedAt` columns and both tafseer annotation
            // tables belong to the user now (schemaVersion 23) and are not in the artifact at
            // all, so an unguarded `ALTER TABLE` throws "no such table: quran_favorites"
            // before Room has validated anything: a crash on first launch of a fresh install.
            UPDATED_AT_TABLES.filter { db.hasTable(it) }.forEach { table ->
                db.addColumnIfMissing(table, "updatedAt", "INTEGER NOT NULL DEFAULT 0")
            }

            // Only for a database that still has the per-ayah table. Since
            // schemaVersion 21 the artifact ships `tafseer_blocks` and no
            // `tafseer_texts` at all, and `CREATE INDEX IF NOT EXISTS` does not
            // guard against a missing *table* — it throws "no such table", which
            // on this path is a crash on first launch of a fresh install.
            if (db.hasTable("tafseer_texts")) {
                db.execSQL("DROP INDEX IF EXISTS `index_tafseer_texts_ayah_tafseer`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tafseer_texts_ayah_id_tafseer_id` ON `tafseer_texts` (`ayah_id`, `tafseer_id`)")
            }

            // The reader's own annotations moved to the user database at schemaVersion 23, so
            // on a current artifact these tables are absent and there is nothing to repair.
            // They are still guarded rather than deleted: this function's whole job is fixing
            // up databases that arrived in an older shape.
            if (db.hasTable("tafseer_highlights")) {
                db.execSQL("DROP INDEX IF EXISTS `index_tafseer_highlights_ayah_tafseer`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tafseer_highlights_ayah_id_tafseer_id` ON `tafseer_highlights` (`ayah_id`, `tafseer_id`)")
            }

            if (db.hasTable("tafseer_notes")) {
                db.execSQL("DROP INDEX IF EXISTS `index_tafseer_notes_ayah_tafseer`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tafseer_notes_ayah_id_tafseer_id` ON `tafseer_notes` (`ayah_id`, `tafseer_id`)")
            }
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

        // 16-line IndoPak Quran (sub-task 2/7 of #263). Adds the nullable
        // `text_indopak` column to `ayahs` and creates the `mushaf_layout_indopak16`
        // table that holds the line-accurate 548-page layout. Both are created empty
        // here for BOTH fresh installs and upgrades — the prepackaged DB asset is not
        // regenerated (it is a ~147 MB Git-LFS blob that `createFromAsset` never
        // re-copies on upgrade), so the IndoPak text + layout are shipped as bundled
        // JSON assets and populated at runtime by QuranIndopakSeeder, exactly like the
        // Dua/Help/Qaida content. This keeps the APK impact to a few MB of compressible
        // JSON instead of adding tens of MB to the LFS asset. Every statement is
        // idempotent so running it after createFromAsset (fresh install) is safe.
        // Translations became a catalogue (15 shipped editions, seeded lazily per
        // translation) rather than the single Saheeh International set the prepackaged DB
        // carried. `translations.id` is auto-generated and there was no uniqueness
        // constraint, so a re-seed that inserted without deleting first would silently
        // double every verse. Collapse any existing duplicates — keeping the lowest id per
        // (ayah, translator) — and add the unique index that makes the class of bug
        // impossible from here on.
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM translations
                    WHERE id NOT IN (
                        SELECT MIN(id) FROM translations GROUP BY ayah_id, translator_id
                    )
                """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_translations_ayah_id_translator_id` " +
                            "ON `translations` (`ayah_id`, `translator_id`)"
                )
            }
        }

        // Generalises the line-accurate mushaf storage so more than one edition can exist.
        //
        // Before: one bespoke table (`mushaf_layout_indopak16`) plus a single
        // `ayahs.text_indopak` column — a shape that could only ever hold the 16-line
        // IndoPak edition. After: `mushaf_layout_lines` keyed by script and
        // `mushaf_ayah_texts` keyed by text source, so an edition is data rather than
        // schema.
        //
        // Both tables are created empty and repopulated by MushafLayoutSeeder from the
        // bundled assets (the version key is new, so every install re-seeds on first use of
        // an edition). Nothing is lost: the dropped table held only derived content shipped
        // in those same assets — no user data. `ayahs.text_indopak` is nulled rather than
        // dropped, because dropping a column in SQLite means rebuilding a 6,236-row table
        // for no functional gain; nulling reclaims the space and leaves the column inert.
        // Every statement is idempotent so running it after createFromAsset is safe.
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mushaf_ayah_texts` (
                        `text_source` TEXT NOT NULL,
                        `ayah_id` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        PRIMARY KEY(`text_source`, `ayah_id`)
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushaf_ayah_texts_ayah_id` ON `mushaf_ayah_texts` (`ayah_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mushaf_layout_lines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `script` TEXT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `line` INTEGER NOT NULL,
                        `line_type` TEXT NOT NULL,
                        `surah_id` INTEGER NOT NULL,
                        `ayah_id` INTEGER,
                        `first_word_position` INTEGER,
                        `last_word_position` INTEGER
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushaf_layout_lines_script_page_line` ON `mushaf_layout_lines` (`script`, `page`, `line`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushaf_layout_lines_script` ON `mushaf_layout_lines` (`script`)")

                db.execSQL("DROP TABLE IF EXISTS `mushaf_layout_indopak16`")
                db.execSQL("UPDATE ayahs SET text_indopak = NULL WHERE text_indopak IS NOT NULL")
            }
        }

        // Tafseer is range-based, not ayah-based: a single commentary passage (e.g.
        // Ibn Kathir on 43:81-89) used to be duplicated into `tafseer_texts` under
        // every ayah id it covers — 4,340 redundant rows for ibn_kathir_en alone.
        // `tafseer_blocks` stores the block once with its own range
        // (`ayah_start`/`ayah_end`), so the reader can render "Commentary on
        // 43:81-89" instead of showing the same text nine times with no indication
        // it's one passage.
        //
        // The old rows are folded into the new table before being dropped, and
        // that is not a nicety. `createFromAsset` copies the artifact **only on a
        // fresh install**, so an upgrading device never receives the reshaped data;
        // the only mechanism that reaches an existing install is a content patch,
        // and a patch cannot express a table that did not exist in its baseline
        // (`nz patch emit` refuses). Dropping `tafseer_texts` and creating an empty
        // `tafseer_blocks` would therefore have emptied the Tafseer reader for
        // every existing user until they reinstalled.
        //
        // Nothing has to be fetched to do it: the old table *is* the source. A
        // block is a maximal run of consecutive ayahs in one surah sharing one
        // commentary text, which is exactly how the importer derives blocks
        // upstream. Run against the published data-v2 artifact, the fold below
        // reproduces the artifact's own 1,896 + 3,037 blocks with zero rows
        // differing in either direction.
        //
        // `tafseer_highlights`/`tafseer_notes` (user data, keyed by `ayah_id`) are
        // untouched: their `start_offset`/`end_offset` index into the commentary
        // text, which is unchanged for the ayah they were made on.
// schemaVersion 22 — a verse's row stops being four renderings, a boolean and a
        // nullable string, and becomes its place in the mushaf.
        //
        // Everything moved here is *derivable on the device*, which is the only reason this can
        // be a migration rather than a reinstall. `createFromAsset` re-copies the artifact only
        // on a fresh install, and a content patch cannot create a table that its baseline never
        // had, so anything not derivable would simply be absent for existing users — the trap
        // MIGRATION_20_21 fell into with tafseer.
        //
        //   text_uthmani  -> mushaf_ayah_texts as source UTHMANI  (the row already holds it)
        //   sajda columns -> sajdas                               (the 15 marked rows)
        //   juz/hizb/page -> juzs, hizb_quarters, pages           (MIN/MAX over the columns)
        //
        // `rukus`, `manzils` and `surah_structure` are content that has never been on a device:
        // they are created empty and filled from the artifact on a fresh install, or by
        // QuranStructureSeeder from the bundled seed on an upgrade. `text_arabic` was
        // byte-identical to `text_uthmani` in all 6,236 rows, so nothing is lost by dropping it;
        // `text_indopak` was NULL in all of them.
        // schemaVersion 23 — the content database stops declaring the twenty-two tables the
        // user writes to. They are in the user's own database now (NimazUserDatabase), copied
        // across on its first open.
        //
        // Deliberately empty. The tables are **not dropped**: Room ignores tables it does not
        // declare, so leaving them costs a few kilobytes on devices that already have them and
        // keeps the original rows on disk while the copy is young. A bug in that copy has to be
        // survivable, and it is only survivable while the source still exists. A later version
        // can drop them once there is nothing left to recover.
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `juzs` (
                        `number` INTEGER NOT NULL, `start_ayah_id` INTEGER NOT NULL,
                        `end_ayah_id` INTEGER NOT NULL, PRIMARY KEY(`number`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hizb_quarters` (
                        `number` INTEGER NOT NULL, `juz_number` INTEGER NOT NULL,
                        `start_ayah_id` INTEGER NOT NULL, `end_ayah_id` INTEGER NOT NULL,
                        PRIMARY KEY(`number`))
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hizb_quarters_juz_number` ON `hizb_quarters` (`juz_number`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `manzils` (
                        `number` INTEGER NOT NULL, `start_ayah_id` INTEGER NOT NULL,
                        `end_ayah_id` INTEGER NOT NULL, PRIMARY KEY(`number`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rukus` (
                        `number` INTEGER NOT NULL, `surah_id` INTEGER NOT NULL,
                        `start_ayah_id` INTEGER NOT NULL, `end_ayah_id` INTEGER NOT NULL,
                        PRIMARY KEY(`number`))
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rukus_surah_id` ON `rukus` (`surah_id`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pages` (
                        `number` INTEGER NOT NULL, `start_ayah_id` INTEGER NOT NULL,
                        `end_ayah_id` INTEGER NOT NULL, PRIMARY KEY(`number`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sajdas` (
                        `ayah_id` INTEGER NOT NULL, `sequence` INTEGER NOT NULL,
                        `kind` TEXT NOT NULL, `upstream_kind` TEXT, PRIMARY KEY(`ayah_id`))
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sajdas_sequence` ON `sajdas` (`sequence`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `surah_structure` (
                        `surah_id` INTEGER NOT NULL, `ruku_count` INTEGER NOT NULL,
                        `start_ayah_id` INTEGER NOT NULL, `end_ayah_id` INTEGER NOT NULL,
                        `start_page` INTEGER NOT NULL, `end_page` INTEGER NOT NULL,
                        `has_basmalah` INTEGER NOT NULL, `revelation_order` INTEGER NOT NULL,
                        PRIMARY KEY(`surah_id`))
                    """.trimIndent()
                )

                // The Uthmani text the row already carries becomes a text source. INSERT OR
                // IGNORE, not REPLACE: a device that already has the artifact's UTHMANI rows
                // must keep them rather than have them overwritten from a column.
                if (db.hasColumn("ayahs", "text_uthmani")) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `mushaf_ayah_texts` (`text_source`, `ayah_id`, `text`)
                        SELECT 'UTHMANI', `id`, `text_uthmani` FROM `ayahs`
                        WHERE `text_uthmani` IS NOT NULL AND `text_uthmani` != ''
                        """.trimIndent()
                    )
                }

                if (db.hasColumn("ayahs", "sajda")) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `sajdas` (`ayah_id`, `sequence`, `kind`, `upstream_kind`)
                        SELECT `id`,
                               (SELECT COUNT(*) FROM `ayahs` b WHERE b.`sajda` <> 0 AND b.`id` <= a.`id`),
                               COALESCE(`sajda_type`, 'recommended'),
                               NULL
                        FROM `ayahs` a WHERE a.`sajda` <> 0
                        """.trimIndent()
                    )
                }

                // The divisions, from the columns that already describe them per verse.
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `juzs` (`number`, `start_ayah_id`, `end_ayah_id`)
                    SELECT `juz`, MIN(`id`), MAX(`id`) FROM `ayahs` GROUP BY `juz`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `hizb_quarters` (`number`, `juz_number`, `start_ayah_id`, `end_ayah_id`)
                    SELECT `hizb`, MIN(`juz`), MIN(`id`), MAX(`id`) FROM `ayahs` GROUP BY `hizb`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `pages` (`number`, `start_ayah_id`, `end_ayah_id`)
                    SELECT `page`, MIN(`id`), MAX(`id`) FROM `ayahs` GROUP BY `page`
                    """.trimIndent()
                )

                // Rebuild `ayahs` without the columns that moved. SQLite cannot drop a column
                // in a table this old, so it is the documented twelve-step dance, minus the
                // steps that do not apply.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ayahs_new` (
                        `id` INTEGER NOT NULL,
                        `surah_id` INTEGER NOT NULL,
                        `number_in_surah` INTEGER NOT NULL,
                        `number_global` INTEGER NOT NULL,
                        `juz` INTEGER NOT NULL,
                        `hizb` INTEGER NOT NULL,
                        `page` INTEGER NOT NULL,
                        `transliteration` TEXT,
                        `text_tajweed` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`surah_id`) REFERENCES `surahs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `ayahs_new`
                        (`id`, `surah_id`, `number_in_surah`, `number_global`, `juz`, `hizb`,
                         `page`, `transliteration`, `text_tajweed`)
                    SELECT `id`, `surah_id`, `number_in_surah`, `number_global`, `juz`, `hizb`,
                           `page`, `transliteration`, `text_tajweed`
                    FROM `ayahs`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `ayahs`")
                db.execSQL("ALTER TABLE `ayahs_new` RENAME TO `ayahs`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ayahs_surah_id` ON `ayahs` (`surah_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ayahs_juz` ON `ayahs` (`juz`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ayahs_page` ON `ayahs` (`page`)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tafseer_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tafseer_id` TEXT NOT NULL,
                        `surah_number` INTEGER NOT NULL,
                        `ayah_start` INTEGER NOT NULL,
                        `ayah_end` INTEGER NOT NULL,
                        `text` TEXT NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS " +
                            "`index_tafseer_blocks_tafseer_id_surah_number_ayah_start_ayah_end` " +
                            "ON `tafseer_blocks` (`tafseer_id`, `surah_number`, `ayah_start`, `ayah_end`)"
                )

                // Belt and braces on the ordering: fold only into an empty table, so
                // a database that somehow holds both shapes cannot end up with the
                // same commentary twice.
                if (db.hasTable("tafseer_texts") && db.isEmpty("tafseer_blocks")) {
                    // Gaps-and-islands: subtracting a per-text row number from the
                    // ayah number is constant exactly while ayahs are consecutive
                    // *and* the text is unchanged, so it groups a run without
                    // merging two separate runs that happen to share the same text
                    // — or bridging a missing ayah. Window functions need SQLite
                    // 3.25; minSdk 29 ships 3.28.
                    db.execSQL(
                        """
                        INSERT INTO `tafseer_blocks`
                            (`tafseer_id`, `surah_number`, `ayah_start`, `ayah_end`, `text`)
                        SELECT tafseer_id, surah_number,
                               MIN(ayah_number), MAX(ayah_number), text
                        FROM (
                            SELECT tafseer_id, surah_number, ayah_number, text,
                                   ayah_number - ROW_NUMBER() OVER (
                                       PARTITION BY tafseer_id, surah_number, text
                                       ORDER BY ayah_number
                                   ) AS run
                            FROM `tafseer_texts`
                        )
                        GROUP BY tafseer_id, surah_number, text, run
                        ORDER BY tafseer_id, surah_number, MIN(ayah_number)
                    """.trimIndent()
                    )
                    db.execSQL("DROP TABLE `tafseer_texts`")
                }
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.addColumnIfMissing("ayahs", "text_indopak", "TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mushaf_layout_indopak16` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `page` INTEGER NOT NULL,
                        `line` INTEGER NOT NULL,
                        `line_type` TEXT NOT NULL,
                        `surah_id` INTEGER NOT NULL,
                        `ayah_id` INTEGER,
                        `first_word_position` INTEGER,
                        `last_word_position` INTEGER
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mushaf_layout_indopak16_page_line` ON `mushaf_layout_indopak16` (`page`, `line`)")
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

        /**
         * Every migration, in one place — the single source of truth for the chain, in the
         * same spirit as [NIMAZ_DATABASE_VERSION] for the version itself.
         *
         * `DatabaseModule` registers this on the real database and `MigrationChainTest`
         * replays it from v7, so **adding a migration here is the only step required**.
         * Both previously kept their own hand-maintained copies, which is precisely how a
         * new migration ended up registered in production but missing from the chain test:
         * the test failed with "A migration from 7 to 20 was required but not found" while
         * the app itself was fine. One list means that can't happen again.
         *
         * Order is irrelevant — Room indexes migrations by their start/end versions — but it
         * is kept ascending for readability.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
        )
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

/**
 * Whether [table] exists in this database.
 *
 * `CREATE INDEX IF NOT EXISTS … ON missing_table` is not idempotent: the guard
 * covers the index name, not the table, so the statement throws "no such table".
 * Any repair that names a table which has since been dropped from the artifact
 * has to ask first — see [NimazDatabase.PREPACKAGED_CALLBACK], which runs against
 * whatever shape the fetched artifact happens to have.
 */
private fun SupportSQLiteDatabase.hasTable(table: String): Boolean =
    query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table))
        .use { it.moveToFirst() }

/**
 * Whether [table] has [column].
 *
 * A migration that moves a column's contents has to run against a database that may or may not
 * still have it — a device upgrading through the chain, or one that arrived already reshaped in
 * the artifact. Asking is what makes the move idempotent.
 */
private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
            .any { it == column }
    }

/** Whether [table] holds no rows. Used to keep a data-carrying migration re-runnable. */
private fun SupportSQLiteDatabase.isEmpty(table: String): Boolean =
    query("SELECT 1 FROM `$table` LIMIT 1").use { !it.moveToFirst() }
