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

        // `validateDroppedTables = false`, deliberately. Every declared entity is still
        // validated — columns, types, indices — but extra tables no longer fail the run,
        // because since schemaVersion 23 there are supposed to be some: the twenty-two tables
        // the user writes to moved to NimazUserDatabase and MIGRATION_22_23 leaves them where
        // they are rather than dropping them. A bug in that copy is only survivable while the
        // original rows are still on disk, so the tables outliving their declaration is the
        // design, and this test asserts it below instead of failing on it.
        val db = helper.runMigrationsAndValidate(
            dbName,
            NimazDatabase.SCHEMA_VERSION,
            false,
            *NimazDatabase.ALL_MIGRATIONS,
        )

        // A representative table from the latest schema must exist and be queryable.
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='qaida_lessons'"
        )
        assertThat(cursor.count).isEqualTo(1)
        cursor.close()

        // The user's rows survive the chain. If this ever goes to 0, an upgrade has thrown
        // away the only copy of somebody's bookmarks before UserDataMigrator could read it.
        db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name = 'quran_bookmarks'"
        ).use { legacy ->
            assertThat(legacy.count).isEqualTo(1)
        }
    }
}
