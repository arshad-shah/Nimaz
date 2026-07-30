package com.arshadshah.nimaz

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NimazDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Tafseer moves from one row per ayah (`tafseer_texts`) to one row per
     * commentary block (`tafseer_blocks`, carrying its own `ayah_start`/`ayah_end`
     * range) — see issue #329.
     *
     * The rows are **folded, not dropped**. `createFromAsset` re-copies the
     * artifact only on a fresh install and a content patch cannot carry a table
     * absent from its baseline, so an upgrading device has no other source for its
     * commentary: emptying the table here would empty the reader for every
     * existing user.
     */
    @Test
    fun migrate20To21_foldsTafseerTextsIntoBlocks() {
        helper.createDatabase(dbName, 20).use { db ->
            // 43:81-83 share one passage — one block covering the range.
            (81..83).forEach { ayah ->
                db.execSQL(
                    "INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text) " +
                        "VALUES (${4300 + ayah}, 43, $ayah, 'ibn_kathir_en', 'on 81 through 83')"
                )
            }
            // 43:84 is its own passage, so the run above has to end at 83.
            db.execSQL(
                "INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text) " +
                    "VALUES (4384, 43, 84, 'ibn_kathir_en', 'on 84 alone')"
            )
            // 43:86 repeats the *same* text as 81-83 after a gap at 85. Grouping by
            // text alone would merge these into one impossible 81-86 block.
            db.execSQL(
                "INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text) " +
                    "VALUES (4386, 43, 86, 'ibn_kathir_en', 'on 81 through 83')"
            )
            // A second commentator on the same ayah stays a separate block.
            db.execSQL(
                "INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text) " +
                    "VALUES (4381, 43, 81, 'maariful_quran_en', 'maariful on 81')"
            )
        }

        val db = helper.runMigrationsAndValidate(
            dbName, 21, true, NimazDatabase.MIGRATION_20_21
        )

        val droppedCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='tafseer_texts'"
        )
        assertThat(droppedCursor.count).isEqualTo(0)
        droppedCursor.close()

        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND " +
                "name='index_tafseer_blocks_tafseer_id_surah_number_ayah_start_ayah_end'"
        )
        assertThat(indexCursor.count).isEqualTo(1)
        indexCursor.close()

        val blocks = mutableListOf<String>()
        db.query(
            "SELECT tafseer_id, surah_number, ayah_start, ayah_end, text FROM tafseer_blocks " +
                "ORDER BY tafseer_id, surah_number, ayah_start"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                blocks += "${cursor.getString(0)} ${cursor.getInt(1)}:" +
                    "${cursor.getInt(2)}-${cursor.getInt(3)} ${cursor.getString(4)}"
            }
        }

        assertThat(blocks).containsExactly(
            "ibn_kathir_en 43:81-83 on 81 through 83",
            "ibn_kathir_en 43:84-84 on 84 alone",
            "ibn_kathir_en 43:86-86 on 81 through 83",
            "maariful_quran_en 43:81-81 maariful on 81",
        ).inOrder()
    }

    /**
     * The legacy-asset repair runs on every freshly copied artifact, before Room
     * validates it. It used to index `tafseer_texts` unconditionally, and
     * `CREATE INDEX IF NOT EXISTS` guards the index name, not the table — so
     * against a schemaVersion 21 artifact, which has no such table, the first
     * launch of a fresh install threw "no such table: tafseer_texts".
     */
    @Test
    fun prepackagedRepair_survivesAnArtifactWithoutTafseerTexts() {
        helper.createDatabase(dbName, 20).use { db ->
            db.execSQL("DROP TABLE tafseer_texts")

            NimazDatabase.PREPACKAGED_CALLBACK.onOpenPrepackagedDatabase(db)

            // The repairs that *do* apply still happened.
            db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND " +
                    "name='index_tafseer_highlights_ayah_id_tafseer_id'"
            ).use { assertThat(it.count).isEqualTo(1) }
        }
    }

    /**
     * The fresh-install shape: the schemaVersion 21 artifact already carries
     * `tafseer_blocks` and has no `tafseer_texts` at all. The migration must be a
     * no-op there rather than throwing on a table that is not present.
     */
    @Test
    fun migrate20To21_toleratesAnArtifactThatAlreadyHasBlocks() {
        helper.createDatabase(dbName, 20).use { db ->
            db.execSQL("DROP TABLE tafseer_texts")
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
                "INSERT INTO tafseer_blocks (tafseer_id, surah_number, ayah_start, ayah_end, text) " +
                    "VALUES ('ibn_kathir_en', 43, 81, 89, 'already a block')"
            )
        }

        val db = helper.runMigrationsAndValidate(
            dbName, 21, true, NimazDatabase.MIGRATION_20_21
        )

        db.query("SELECT COUNT(*) FROM tafseer_blocks").use { cursor ->
            cursor.moveToFirst()
            assertThat(cursor.getInt(0)).isEqualTo(1)
        }
    }

    @Test
    fun migrate13To14_createsHelpTables() {
        helper.createDatabase(dbName, 13).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 14, true, NimazDatabase.MIGRATION_13_14
        )
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN " +
                "('help_topic','help_item','help_step','help_string')"
        )
        assertThat(cursor.count).isEqualTo(4)
        cursor.close()
    }

    @Test
    fun migrate14To15_createsQaidaTables() {
        helper.createDatabase(dbName, 14).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 15, true, NimazDatabase.MIGRATION_14_15
        )
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name IN " +
                "('qaida_lessons','qaida_letters','qaida_lines','qaida_cells'," +
                "'qaida_lesson_progress','qaida_cell_progress')"
        )
        assertThat(cursor.count).isEqualTo(6)
        cursor.close()
    }

    @Test
    fun migrate15To16_addsCategoryColumn() {
        helper.createDatabase(dbName, 15).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 16, true, NimazDatabase.MIGRATION_15_16
        )
        val cursor = db.query("PRAGMA table_info(`tasbih_presets`)")
        val nameIndex = cursor.getColumnIndex("name")
        val columns = generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }.toList()
        cursor.close()
        assertThat(columns).contains("category")
    }

    @Test
    fun migrate16To17_addsNarratorChainColumn() {
        helper.createDatabase(dbName, 16).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 17, true, NimazDatabase.MIGRATION_16_17
        )
        val cursor = db.query("PRAGMA table_info(`hadiths`)")
        val nameIndex = cursor.getColumnIndex("name")
        val columns = generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }.toList()
        cursor.close()
        assertThat(columns).contains("narrator_chain")
    }

    @Test
    fun migrate17To18_addsIndopakColumnAndLayoutTable() {
        helper.createDatabase(dbName, 17).close()
        val db = helper.runMigrationsAndValidate(
            dbName, 18, true, NimazDatabase.MIGRATION_17_18
        )

        // ayahs gained the nullable text_indopak column
        val ayahCols = db.query("PRAGMA table_info(`ayahs`)").let { c ->
            val nameIndex = c.getColumnIndex("name")
            generateSequence { if (c.moveToNext()) c.getString(nameIndex) else null }.toList().also { c.close() }
        }
        assertThat(ayahCols).contains("text_indopak")

        // the mushaf_layout_indopak16 table exists
        val tableCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='mushaf_layout_indopak16'"
        )
        assertThat(tableCursor.count).isEqualTo(1)
        tableCursor.close()

        // and it is indexed on (page, line)
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_mushaf_layout_indopak16_page_line'"
        )
        assertThat(indexCursor.count).isEqualTo(1)
        indexCursor.close()
    }

    /**
     * The real upgrade path for the translation catalogue + generalised mushaf layouts: a
     * device sitting on 18 runs both migrations in one go. `runMigrationsAndValidate` also
     * checks the resulting schema against the exported v20 definition, which is what proves
     * the hand-written SQL matches the Room entities exactly.
     */
    @Test
    fun migrate18To20_dedupesTranslationsAndGeneralisesMushafTables() {
        helper.createDatabase(dbName, 18).use { db ->
            db.execSQL(
                "INSERT INTO ayahs (id, surah_id, number_in_surah, number_global, text_arabic, " +
                    "text_uthmani, text_indopak, juz, hizb, page, sajda, sajda_type, " +
                    "transliteration, text_tajweed) VALUES " +
                    "(1, 1, 1, 1, 'a', 'a', 'indopak text', 1, 1, 1, 0, NULL, NULL, NULL)"
            )
            // Two rows for the same (ayah, translator) — exactly what an un-scoped re-seed
            // used to produce, and what the unique index must not allow to survive.
            db.execSQL(
                "INSERT INTO translations (ayah_id, text, translator_id) VALUES " +
                    "(1, 'first', 'sahih_international'), " +
                    "(1, 'duplicate', 'sahih_international'), " +
                    "(1, 'other translation', 'en_pickthall')"
            )
        }

        val db = helper.runMigrationsAndValidate(
            dbName, 20, true,
            NimazDatabase.MIGRATION_18_19,
            NimazDatabase.MIGRATION_19_20
        )

        // The duplicate is gone and the surviving row is the earliest one, not an arbitrary pick.
        db.query(
            "SELECT text FROM translations WHERE ayah_id = 1 AND translator_id = 'sahih_international'"
        ).use { c ->
            assertThat(c.count).isEqualTo(1)
            c.moveToFirst()
            assertThat(c.getString(0)).isEqualTo("first")
        }
        // Other translators are untouched by the dedupe.
        db.query("SELECT COUNT(*) FROM translations WHERE translator_id = 'en_pickthall'").use { c ->
            c.moveToFirst()
            assertThat(c.getInt(0)).isEqualTo(1)
        }
        // The unique index now makes a duplicate impossible rather than merely absent.
        db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND " +
                "name='index_translations_ayah_id_translator_id'"
        ).use { assertThat(it.count).isEqualTo(1) }

        // Script-keyed mushaf tables replaced the bespoke 16-line one.
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND " +
                "name IN ('mushaf_layout_lines','mushaf_ayah_texts')"
        ).use { assertThat(it.count).isEqualTo(2) }
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='mushaf_layout_indopak16'"
        ).use { assertThat(it.count).isEqualTo(0) }

        // The superseded column is emptied so its ~4 MB of glyphs are not left behind.
        db.query("SELECT COUNT(*) FROM ayahs WHERE text_indopak IS NOT NULL").use { c ->
            c.moveToFirst()
            assertThat(c.getInt(0)).isEqualTo(0)
        }
    }
}
