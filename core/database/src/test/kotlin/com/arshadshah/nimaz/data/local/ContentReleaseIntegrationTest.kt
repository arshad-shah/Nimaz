package com.arshadshah.nimaz.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * A content release reaching an install made before the databases were split — the one sequence
 * in this module where getting it wrong destroys somebody's data.
 *
 * Each piece is unit-tested on its own. This is the only thing that exercises them in the order
 * and with the shared state they actually have, over more than one launch, and the ordering is
 * the whole point:
 *
 * - `ContentArtifactInstaller` runs first, from `DatabaseModule.provideNimazDatabase`, **before**
 *   Room opens the file. Its job is to delete that file.
 * - `UserDataMigrator` runs later, from `AppInitializer`, and is awaited before the splash lifts.
 *   Its job is to copy the user's rows out of that same file.
 * - The only thing sequencing them is a boolean in SharedPreferences, written by the second and
 *   read by the first.
 *
 * Run them in that order on one launch and the installer deletes the rows before the migrator has
 * copied them. So the first launch after an update must **defer**, and the second must replace —
 * and nothing except this test says so end to end. `SharedPreferencesContentArtifactStore` is the
 * real one here rather than a fake, because the flag surviving between the two launches is
 * precisely the mechanism under test.
 */
@RunWith(RobolectricTestRunner::class)
class ContentReleaseIntegrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: SharedPreferencesContentArtifactStore

    /** The sha the APK on the device was built against, and the one this update ships. */
    private val installed = "sha-data-v8"
    private val shipped = "sha-data-v9"

    @Before
    fun setUp() {
        context.getSharedPreferences("nimaz_content_artifact", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
        store = SharedPreferencesContentArtifactStore(context)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
        context.deleteDatabase(NimazUserDatabase.DATABASE_NAME)
    }

    @Test
    fun `an install that predates the split keeps its data and gets the new content`() = runTest {
        legacyInstall(bookmarkedAyahs = listOf(262, 2262))

        // ---- Launch 1: the update lands. ----
        val firstLaunch = install()
        assertThat(firstLaunch).isInstanceOf(
            ContentArtifactInstaller.Outcome.DeferredForLegacyData::class.java
        )
        // The file the rows are in is still there, which is the only reason the copy below works.
        assertThat(contentDatabase().exists()).isTrue()

        val copied = withUserDatabase { migrator(it).migrateIfNeeded() }
        assertThat(copied).isGreaterThan(0)
        assertThat(store.legacyImportComplete()).isTrue()

        // ---- Launch 2: the flag is set, so the replace goes ahead. ----
        val secondLaunch = install()
        assertThat(secondLaunch).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        // Deleted, not emptied: Room copies the new artifact out of the APK on the next open.
        assertThat(contentDatabase().exists()).isFalse()
        assertThat(store.installedArtifact()).isEqualTo(shipped)
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)

        // ---- What the person still has. ----
        val bookmarks = withUserDatabase { it.bookmarkDao().all() }
        assertThat(bookmarks.map { it.targetId }).containsExactly(262, 2262)
    }

    @Test
    fun `the deferral lasts exactly one launch, not one per session`() = runTest {
        legacyInstall(bookmarkedAyahs = listOf(262))

        install()
        assertThat(store.consecutiveDeferrals()).isEqualTo(1)

        withUserDatabase { migrator(it).migrateIfNeeded() }
        install()

        // A deferral that repeats is the failure mode #473 exists for: such a device stops
        // receiving content and a search index altogether, and Arabic search on it returns
        // nothing against a corpus where الله alone appears in 1,746 verses.
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)
    }

    @Test
    fun `a launch where the copy never runs holds the content back rather than losing the rows`() =
        runTest {
            legacyInstall(bookmarkedAyahs = listOf(262))

            // Three launches where the installer runs and the migrator never gets to — the app
            // killed before `AppInitializer` finishes, over and over. Deferring is the only
            // answer that does not delete the sole copy of this person's bookmarks.
            repeat(3) { install() }

            assertThat(store.legacyImportComplete()).isFalse()
            assertThat(store.consecutiveDeferrals()).isEqualTo(3)
            assertThat(contentDatabase().exists()).isTrue()
            assertThat(legacyBookmarkCount()).isEqualTo(1)
        }

    @Test
    fun `an install made after the split takes the release on its first launch`() = runTest {
        // No legacy tables at all: everything from schemaVersion 23 onward.
        SQLiteDatabase.openOrCreateDatabase(
            contentDatabase().also { it.parentFile?.mkdirs() }, null
        ).use { it.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, name TEXT)") }
        store.setInstalledArtifact(installed)

        // The migrator still runs on every launch; with no legacy file to read it says so at
        // once, which is what unblocks the installer without a deferral.
        withUserDatabase { migrator(it).migrateIfNeeded() }

        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)
    }

    @Test
    fun `a launch with nothing new to install does not touch the file`() = runTest {
        SQLiteDatabase.openOrCreateDatabase(
            contentDatabase().also { it.parentFile?.mkdirs() }, null
        ).use { it.execSQL("CREATE TABLE surahs (id INTEGER PRIMARY KEY, name TEXT)") }
        store.setInstalledArtifact(shipped)

        assertThat(install()).isEqualTo(ContentArtifactInstaller.Outcome.AlreadyCurrent)
        assertThat(contentDatabase().exists()).isTrue()
    }

    // ---- The device, as it is at the moment of the update ----

    /** A content database in the shape an install from before schemaVersion 23 has one. */
    private fun legacyInstall(bookmarkedAyahs: List<Int>) {
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
        }
        store.setInstalledArtifact(installed)
    }

    private fun legacyBookmarkCount(): Int =
        SQLiteDatabase.openDatabase(
            contentDatabase().absolutePath, null, SQLiteDatabase.OPEN_READONLY
        ).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM quran_bookmarks", null).use {
                it.moveToFirst()
                it.getInt(0)
            }
        }

    private fun contentDatabase(): File = context.getDatabasePath(NimazDatabase.DATABASE_NAME)

    /** What `DatabaseModule.provideNimazDatabase` does before Room opens the file. */
    private fun install(): ContentArtifactInstaller.Outcome =
        ContentArtifactInstaller(context, store, installedArtifact = shipped).installIfChanged()

    private fun migrator(userDatabase: NimazUserDatabase) =
        UserDataMigrator(context, userDatabase, store)

    private fun openUserDatabase(): NimazUserDatabase = Room.databaseBuilder(
        context,
        NimazUserDatabase::class.java,
        NimazUserDatabase.DATABASE_NAME,
    ).allowMainThreadQueries().build()

    /** One launch's worth of the user database, closed again at the end of it. */
    private suspend fun <T> withUserDatabase(block: suspend (NimazUserDatabase) -> T): T {
        val db = openUserDatabase()
        return try {
            block(db)
        } finally {
            db.close()
        }
    }
}
