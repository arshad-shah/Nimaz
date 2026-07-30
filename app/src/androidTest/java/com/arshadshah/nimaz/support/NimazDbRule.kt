package com.arshadshah.nimaz.support

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
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

    /** The corpus: read-only in production, and shipped as an asset. */
    lateinit var db: NimazDatabase
        private set

    /**
     * The user's own database, since schemaVersion 23 a separate file.
     *
     * Both are stood up here because most DAO tests need one or the other and a few need both
     * — a bookmark on a verse, say, whose ids come from the content side. Keeping them in one
     * rule is what stops a test quietly asserting against the wrong one.
     */
    lateinit var userDb: NimazUserDatabase
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, NimazDatabase::class.java)
            // Queries in these tests run on a background dispatcher via runTest's
            // coroutine, but allowing main-thread queries keeps simple assertions
            // ergonomic and never deadlocks an in-memory DB.
            .allowMainThreadQueries()
            .build()
        userDb = Room.inMemoryDatabaseBuilder(context, NimazUserDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    override fun after() {
        if (::db.isInitialized && db.isOpen) db.close()
        if (::userDb.isInitialized && userDb.isOpen) userDb.close()
    }
}
