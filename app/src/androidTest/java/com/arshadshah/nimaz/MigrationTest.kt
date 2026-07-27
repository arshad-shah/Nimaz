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
     * ADR-001's migration is the one in this chain that moves *user-visible data* rather than
     * just adding an empty table: an existing install already holds ~13,970 seeded IndoPak
     * layout rows, and losing them empties the 16-line reader until the next re-seed. These
     * three tests cover exactly the ways that can go wrong — rows dropped, rows duplicated by
     * a re-run, and the old table left behind to drift.
     */
    @Test
    fun migrate18To19_carriesLayoutRowsIntoTheGenericTable() {
        val db18 = helper.createDatabase(dbName, 18)
        db18.execSQL(
            "INSERT INTO `mushaf_layout_indopak16` " +
                "(page, line, line_type, surah_id, ayah_id, first_word_position, last_word_position) " +
                "VALUES (1, 1, 'surah_header', 1, NULL, NULL, NULL), " +
                "(1, 2, 'ayah', 1, 1, 1, 4), " +
                "(2, 3, 'ayah', 2, 8, 2, 9)"
        )
        db18.close()

        val db = helper.runMigrationsAndValidate(
            dbName, 19, true, NimazDatabase.MIGRATION_18_19
        )

        // Every row survives, stamped with the edition it came from.
        val rows = db.query(
            "SELECT layout_id, page, line, line_type, surah_id, ayah_id, " +
                "first_word_position, last_word_position FROM `mushaf_layouts` ORDER BY page, line"
        )
        assertThat(rows.count).isEqualTo(3)

        rows.moveToFirst()
        assertThat(rows.getString(0)).isEqualTo("indopak16")
        assertThat(rows.getString(3)).isEqualTo("surah_header")
        assertThat(rows.isNull(5)).isTrue()

        rows.moveToNext()
        assertThat(rows.getString(0)).isEqualTo("indopak16")
        assertThat(rows.getInt(1)).isEqualTo(1)
        assertThat(rows.getInt(2)).isEqualTo(2)
        assertThat(rows.getInt(5)).isEqualTo(1)
        assertThat(rows.getInt(6)).isEqualTo(1)
        assertThat(rows.getInt(7)).isEqualTo(4)

        rows.moveToNext()
        assertThat(rows.getInt(1)).isEqualTo(2)
        assertThat(rows.getInt(5)).isEqualTo(8)
        rows.close()

        // The per-edition table is gone, so nothing can read stale rows from it.
        val oldTable = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='mushaf_layout_indopak16'"
        )
        assertThat(oldTable.count).isEqualTo(0)
        oldTable.close()

        // Indexed with the discriminator leading — every read is scoped to one edition.
        val index = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' " +
                "AND name='index_mushaf_layouts_layout_id_page_line'"
        )
        assertThat(index.count).isEqualTo(1)
        index.close()
    }

    @Test
    fun migrate18To19_isIdempotentWhenRunTwice() {
        val db18 = helper.createDatabase(dbName, 18)
        db18.execSQL(
            "INSERT INTO `mushaf_layout_indopak16` " +
                "(page, line, line_type, surah_id, ayah_id, first_word_position, last_word_position) " +
                "VALUES (1, 1, 'ayah', 1, 1, 1, 4)"
        )
        db18.close()

        val db = helper.runMigrationsAndValidate(
            dbName, 19, true, NimazDatabase.MIGRATION_18_19
        )
        // Re-running must not double the rows: Room re-runs a migration if an upgrade is
        // interrupted, and a duplicated layout renders every word twice.
        NimazDatabase.MIGRATION_18_19.migrate(db)

        val count = db.query("SELECT COUNT(*) FROM `mushaf_layouts`")
        count.moveToFirst()
        assertThat(count.getInt(0)).isEqualTo(1)
        count.close()
    }

    @Test
    fun migrate18To19_createsAnEmptyTableWhenTheOldOneWasNeverSeeded() {
        // A fresh install runs this straight after createFromAsset, whose ~147 MB prepackaged
        // asset predates both tables — so the copy must be skipped, not fail.
        helper.createDatabase(dbName, 18).close()

        val db = helper.runMigrationsAndValidate(
            dbName, 19, true, NimazDatabase.MIGRATION_18_19
        )

        val count = db.query("SELECT COUNT(*) FROM `mushaf_layouts`")
        count.moveToFirst()
        assertThat(count.getInt(0)).isEqualTo(0)
        count.close()
    }
}
