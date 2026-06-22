package com.arshadshah.nimaz.support

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import org.junit.rules.ExternalResource

/**
 * JUnit rule that stands up a fresh **in-memory** [NimazDatabase] for each test and
 * tears it down afterwards.
 *
 * In-memory (rather than the shipped `createFromAsset` DB) is deliberate for DAO
 * round-trip tests: it is fast, hermetic, starts empty, and exercises the exact
 * Room schema generated from the `@Entity` classes — no device services, no
 * migrations, no shared state leaking between tests. Asset/migration coverage lives
 * separately in `DatabaseAssetTest` and `MigrationTest`.
 *
 * Usage:
 * ```
 * @get:Rule val dbRule = NimazDbRule()
 * private val db get() = dbRule.db
 * ```
 */
class NimazDbRule : ExternalResource() {

    lateinit var db: NimazDatabase
        private set

    override fun before() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java,
        )
            // Queries in these tests run on a background dispatcher via runTest's
            // coroutine, but allowing main-thread queries keeps simple assertions
            // ergonomic and never deadlocks an in-memory DB.
            .allowMainThreadQueries()
            .build()
    }

    override fun after() {
        if (::db.isInitialized && db.isOpen) {
            db.close()
        }
    }
}
