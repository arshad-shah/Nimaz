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
