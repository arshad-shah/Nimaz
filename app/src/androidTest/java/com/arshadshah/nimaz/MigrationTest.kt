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
}
