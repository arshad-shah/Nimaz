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
}
