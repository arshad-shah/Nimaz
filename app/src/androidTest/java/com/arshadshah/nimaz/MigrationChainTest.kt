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

/**
 * Complements the per-step assertions in [MigrationTest] by running the *entire*
 * migration chain end-to-end: create the schema at the oldest supported version (7)
 * and migrate all the way to the current version, validating against the exported
 * schema at each hop. This catches ordering bugs and cumulative drift that
 * single-step tests can miss — the exact path a long-time user's database takes when
 * they finally update.
 *
 * It replays [NimazDatabase.ALL_MIGRATIONS] — the same array the real database registers —
 * rather than a copy of the list. A copy is what previously let this test drift: a migration
 * added to the app but not to the test's own array failed here with "a migration from 7 to N
 * was required but not found", which reads like a production bug but was only the test being
 * out of date.
 */
@RunWith(AndroidJUnit4::class)
class MigrationChainTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NimazDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesCleanlyFromV7ToCurrent() {
        val dbName = "migration-chain-test"
        helper.createDatabase(dbName, 7).close()

        val db = helper.runMigrationsAndValidate(
            dbName,
            NimazDatabase.SCHEMA_VERSION,
            true,
            *NimazDatabase.ALL_MIGRATIONS,
        )

        // A representative table from the latest schema must exist and be queryable.
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='qaida_lessons'"
        )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()
    }
}
