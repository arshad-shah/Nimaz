package com.arshadshah.nimaz.data.local.user

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.content.ContentArtifactStore
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * When the copy out of the old database is allowed to be declared finished.
 *
 * That flag is not bookkeeping: `ContentArtifactInstaller` reads it to decide whether it may
 * delete the content database, and deleting it discards the very rows this copy exists to
 * rescue. So the two directions matter equally — it has to be set on an install that has nothing
 * to copy (or that device defers every content release for the rest of its life), and it must
 * never be set on a run that failed.
 */
@RunWith(RobolectricTestRunner::class)
class UserDataMigratorTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: RecordingStore
    private lateinit var db: NimazUserDatabase

    @Before
    fun setUp() {
        store = RecordingStore()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
        // At the real path, not in memory: the copy attaches both files by path.
        db = Room.databaseBuilder(
            context,
            NimazUserDatabase::class.java,
            NimazUserDatabase.DATABASE_NAME,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        if (db.isOpen) db.close()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
    }

    private fun migrator() = UserDataMigrator(context, db, store)

    @Test
    fun `an install with no old database has nothing to copy and says so`() = runTest {
        assertThat(migrator().migrateIfNeeded()).isEqualTo(0)

        // Anything installed at schemaVersion 23 or later. Saying "done" here is what stops the
        // installer reading the database on every launch to learn the same thing.
        assertThat(store.legacyImportComplete()).isTrue()
    }

    @Test
    fun `an old database is copied across and then declared done`() = runTest {
        legacyDatabase {
            it.execSQL(
                "CREATE TABLE zakat_history (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "calculatedAt INTEGER NOT NULL, totalAssets REAL NOT NULL, " +
                    "totalLiabilities REAL NOT NULL, netWorth REAL NOT NULL, " +
                    "zakatDue REAL NOT NULL, nisabType TEXT NOT NULL, nisabValue REAL NOT NULL, " +
                    "isPaid INTEGER NOT NULL, paidAt INTEGER, notes TEXT, updatedAt INTEGER NOT NULL)"
            )
            it.execSQL(
                "INSERT INTO zakat_history (calculatedAt, totalAssets, totalLiabilities, " +
                    "netWorth, zakatDue, nisabType, nisabValue, isPaid, updatedAt) " +
                    "VALUES (100, 4000.0, 0.0, 4000.0, 100.0, 'silver', 500.0, 0, 100)"
            )
        }

        val copied = migrator().migrateIfNeeded()

        assertThat(copied).isGreaterThan(0)
        assertThat(store.legacyImportComplete()).isTrue()
        assertThat(db.zakatDao().getAllHistorySync().map { it.zakatDue }).containsExactly(100.0)
    }

    @Test
    fun `the old rows are left where they were`() = runTest {
        legacyDatabase {
            it.execSQL("CREATE TABLE zakat_history (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "calculatedAt INTEGER NOT NULL, totalAssets REAL NOT NULL, " +
                "totalLiabilities REAL NOT NULL, netWorth REAL NOT NULL, zakatDue REAL NOT NULL, " +
                "nisabType TEXT NOT NULL, nisabValue REAL NOT NULL, isPaid INTEGER NOT NULL, " +
                "paidAt INTEGER, notes TEXT, updatedAt INTEGER NOT NULL)")
            it.execSQL("INSERT INTO zakat_history (calculatedAt, totalAssets, totalLiabilities, " +
                "netWorth, zakatDue, nisabType, nisabValue, isPaid, updatedAt) " +
                "VALUES (100, 4000.0, 0.0, 4000.0, 100.0, 'silver', 500.0, 0, 100)")
        }

        migrator().migrateIfNeeded()

        // A bug in the copy has to stay survivable, which it only is while the originals are
        // still on disk.
        SQLiteDatabase.openDatabase(legacyFile().absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            .use { legacy ->
                legacy.rawQuery("SELECT COUNT(*) FROM zakat_history", null).use { cursor ->
                    cursor.moveToFirst()
                    assertThat(cursor.getInt(0)).isEqualTo(1)
                }
            }
    }

    @Test
    fun `running a second time copies nothing more`() = runTest {
        legacyDatabase {
            it.execSQL("CREATE TABLE zakat_history (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "calculatedAt INTEGER NOT NULL, totalAssets REAL NOT NULL, " +
                "totalLiabilities REAL NOT NULL, netWorth REAL NOT NULL, zakatDue REAL NOT NULL, " +
                "nisabType TEXT NOT NULL, nisabValue REAL NOT NULL, isPaid INTEGER NOT NULL, " +
                "paidAt INTEGER, notes TEXT, updatedAt INTEGER NOT NULL)")
            it.execSQL("INSERT INTO zakat_history (calculatedAt, totalAssets, totalLiabilities, " +
                "netWorth, zakatDue, nisabType, nisabValue, isPaid, updatedAt) " +
                "VALUES (100, 4000.0, 0.0, 4000.0, 100.0, 'silver', 500.0, 0, 100)")
        }
        migrator().migrateIfNeeded()

        migrator().migrateIfNeeded()

        // It runs on every launch, so "already done" has to be free and has to not duplicate.
        assertThat(db.zakatDao().getAllHistorySync()).hasSize(1)
    }

    @Test
    fun `a copy that fails is never recorded as done`() = runTest {
        legacyDatabase { it.execSQL("CREATE TABLE locations (id INTEGER PRIMARY KEY)") }
        // Standing in for any failure to open or write the user database.
        db.close()

        assertThat(migrator().migrateIfNeeded()).isEqualTo(0)

        // The installer reads this to decide whether it may delete the file the un-copied rows
        // are still in. Setting it on a failed run is how they would be lost for good.
        assertThat(store.legacyImportComplete()).isFalse()
    }

    private fun legacyFile(): File = context.getDatabasePath(NimazDatabase.DATABASE_NAME)

    private fun legacyDatabase(build: (SQLiteDatabase) -> Unit) {
        val file = legacyFile().also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use(build)
    }

    private class RecordingStore : ContentArtifactStore {
        private var artifact: String? = null
        private var legacyDone = false
        private var deferrals = 0

        override fun installedArtifact(): String? = artifact
        override fun setInstalledArtifact(sha256: String) {
            artifact = sha256
        }

        override fun legacyImportComplete(): Boolean = legacyDone
        override fun setLegacyImportComplete() {
            legacyDone = true
        }

        override fun consecutiveDeferrals(): Int = deferrals
        override fun recordDeferral() {
            deferrals++
        }

        override fun clearDeferrals() {
            deferrals = 0
        }
    }
}
