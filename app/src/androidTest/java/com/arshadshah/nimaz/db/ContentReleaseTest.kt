package com.arshadshah.nimaz.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.data.local.content.ContentArtifactInstaller
import com.arshadshah.nimaz.data.local.content.SharedPreferencesContentArtifactStore
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.data.local.user.UserDataMigrator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * How a content release reaches an install made before the databases were split — on a device.
 *
 * The same sequence is covered off-device in `:core:database`'s `ContentReleaseIntegrationTest`,
 * and it is worth having in both places because the two prove different things. That one proves
 * the *logic*: which component runs when, and what the flag between them means. This one proves
 * it against the parts Robolectric substitutes, and every one of them is load-bearing here:
 *
 * - **Real SQLite.** `ATTACH DATABASE` on a live connection is what makes the framework close and
 *   reopen the primary connection, taking Room's invalidation triggers with it — the crash
 *   `LegacyUserDataImport`'s comment is mostly about. It is a property of the platform's SQLite,
 *   not of the SQL.
 * - **Real `deleteDatabase`.** The installer deletes a file with `-wal` and `-shm` siblings, and
 *   whether those go with it is the framework's business.
 * - **Real SharedPreferences with a real `commit`.** The flag has to survive the process, which
 *   is the entire reason it is not DataStore.
 * - **A real app-private files directory**, so the paths are the ones production uses.
 *
 * Deliberately not on the Hilt graph: `DatabaseModule` builds the content database eagerly and
 * the point here is what happens *before* Room opens it.
 */
@RunWith(AndroidJUnit4::class)
class ContentReleaseTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: SharedPreferencesContentArtifactStore

    private val installed = "sha-data-v8"
    private val shipped = "sha-data-v9"

    @Before
    fun setUp() {
        // This test owns the real files the app uses, so it clears them either side.
        context.getSharedPreferences(ARTIFACT_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
        store = SharedPreferencesContentArtifactStore(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(ARTIFACT_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
    }

    @Test
    fun legacyInstall_keepsItsDataAndTakesTheRelease() = runTest {
        legacyInstall(bookmarkedAyahs = listOf(262, 2262), fastedDays = listOf(1_000L, 2_000L))

        // Launch 1 — the installer runs first, from DatabaseModule, and must not delete a file
        // whose rows have not been copied yet.
        assertThat(install())
            .isInstanceOf(ContentArtifactInstaller.Outcome.DeferredForLegacyData::class.java)
        assertThat(contentDatabase().exists()).isTrue()

        // …then AppInitializer awaits the copy, before the splash lifts.
        val userDb = openUserDatabase()
        val copied = UserDataMigrator(context, userDb, store).migrateIfNeeded()
        assertThat(copied).isGreaterThan(0)
        assertThat(store.legacyImportComplete()).isTrue()

        // Launch 2 — the flag is set, so the file goes and Room copies the new artifact.
        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(contentDatabase().exists()).isFalse()
        assertThat(store.installedArtifact()).isEqualTo(shipped)

        // And the person still has everything they had.
        assertThat(userDb.bookmarkDao().all().map { it.targetId }).containsExactly(262, 2262)
        assertThat(userDb.fastingDao().getAllFastRecords().map { it.date })
            .containsExactly(1_000L, 2_000L)
        userDb.close()
    }

    @Test
    fun deferral_survivesTheProcessThatWroteIt() {
        legacyInstall(bookmarkedAyahs = listOf(262), fastedDays = emptyList())

        install()

        // A fresh store over the same file, standing in for the next launch. `commit` rather
        // than `apply` is what makes this hold when the process is killed straight after.
        val nextLaunch = SharedPreferencesContentArtifactStore(context)
        assertThat(nextLaunch.consecutiveDeferrals()).isEqualTo(1)
        assertThat(nextLaunch.installedArtifact()).isEqualTo(installed)
        assertThat(nextLaunch.legacyImportComplete()).isFalse()
    }

    @Test
    fun replace_takesTheWalAndShmFilesWithIt() {
        legacyInstall(bookmarkedAyahs = emptyList(), fastedDays = emptyList())
        store.setLegacyImportComplete()
        // Write-ahead logging leaves two siblings beside the database. `deleteDatabase` is used
        // rather than `File.delete` precisely so they go together — a half-deleted family is
        // worse than either whole state, and it is the framework that knows what the family is.
        SQLiteDatabase.openOrCreateDatabase(contentDatabase(), null).use { db ->
            db.enableWriteAheadLogging()
            db.execSQL("INSERT INTO surahs (id, name) VALUES (2, 'Al-Baqarah')")
        }

        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)

        assertThat(contentDatabase().exists()).isFalse()
        assertThat(File(contentDatabase().absolutePath + "-wal").exists()).isFalse()
        assertThat(File(contentDatabase().absolutePath + "-shm").exists()).isFalse()
    }

    @Test
    fun freshInstall_recordsTheArtifactSoTheNextReleaseIsAComparison() {
        // Nothing on disk: `createFromAsset` is about to do the right thing on its own.
        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.FreshInstall)
        assertThat(store.installedArtifact()).isEqualTo(shipped)

        // …and then it does, which is the step that turns the *next* launch into a comparison.
        // Without it the file is still absent and the installer is entitled to say `FreshInstall`
        // again — it is answering "there is nothing here", which would still be true.
        createFromAssetHappens()

        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.AlreadyCurrent)
    }

    @Test
    fun migrator_onAnInstallWithNoLegacyFile_unblocksTheInstallerAtOnce() = runTest {
        val userDb = openUserDatabase()

        assertThat(UserDataMigrator(context, userDb, store).migrateIfNeeded()).isEqualTo(0)

        // Everything installed at schemaVersion 23 or later. Saying "done" here is what stops the
        // installer reading the content database on every launch to learn the same thing.
        assertThat(store.legacyImportComplete()).isTrue()
        userDb.close()
    }

    /** A content database in the shape an install from before schemaVersion 23 has one. */
    private fun legacyInstall(bookmarkedAyahs: List<Int>, fastedDays: List<Long>) {
        SQLiteDatabase.openOrCreateDatabase(
            contentDatabase().also { it.parentFile?.mkdirs() }, null
        ).use { db ->
            db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL("INSERT INTO surahs (id, name) VALUES (1, 'Al-Fatihah')")
            db.execSQL(
                "CREATE TABLE quran_bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ayahId INTEGER NOT NULL, surahNumber INTEGER NOT NULL, " +
                    "ayahNumber INTEGER NOT NULL, note TEXT, color TEXT, " +
                    "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
            )
            bookmarkedAyahs.forEach { ayah ->
                db.execSQL(
                    "INSERT INTO quran_bookmarks (ayahId, surahNumber, ayahNumber, createdAt, " +
                        "updatedAt) VALUES (?, 2, 255, 1, 1)",
                    arrayOf<Any>(ayah),
                )
            }
            db.execSQL(
                "CREATE TABLE fast_records (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "date INTEGER NOT NULL, hijriDate TEXT, hijriMonth INTEGER, hijriYear INTEGER, " +
                    "fastType TEXT NOT NULL, status TEXT NOT NULL, exemptionReason TEXT, " +
                    "suhoorTime INTEGER, iftarTime INTEGER, note TEXT, " +
                    "createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
            )
            fastedDays.forEach { day ->
                db.execSQL(
                    "INSERT INTO fast_records (date, fastType, status, createdAt, updatedAt) " +
                        "VALUES (?, 'voluntary', 'fasted', 1, 1)",
                    arrayOf<Any>(day),
                )
            }
        }
        store.setInstalledArtifact(installed)
    }

    /**
     * Stands in for Room's `createFromAsset`, which runs *after* the installer and is what
     * actually puts the file on disk. The installer only ever looks at whether it exists.
     */
    private fun createFromAssetHappens() {
        SQLiteDatabase.openOrCreateDatabase(
            contentDatabase().also { it.parentFile?.mkdirs() }, null
        ).use { db ->
            db.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, name TEXT)")
        }
    }

    private fun contentDatabase(): File = context.getDatabasePath(NimazDatabase.DATABASE_NAME)

    /** What `DatabaseModule.provideNimazDatabase` does before Room opens the file. */
    private fun install(): ContentArtifactInstaller.Outcome =
        ContentArtifactInstaller(context, store, installedArtifact = shipped).installIfChanged()

    private fun openUserDatabase(): NimazUserDatabase = Room.databaseBuilder(
        context,
        NimazUserDatabase::class.java,
        NimazUserDatabase.DATABASE_NAME,
    ).allowMainThreadQueries().build()

    private companion object {
        const val ARTIFACT_PREFS = "nimaz_content_artifact"
    }
}
