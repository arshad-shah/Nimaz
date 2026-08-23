package com.arshadshah.nimaz.data.local.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The installer is what makes a content release reach an existing install, and the thing it does
 * is delete a file. So the tests that matter are the ones about *not* deleting it.
 */
@RunWith(RobolectricTestRunner::class)
class ContentArtifactInstallerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: FakeStore

    @Before
    fun setUp() {
        store = FakeStore()
        context.deleteDatabase(NimazDatabase.DATABASE_NAME)
    }

    private fun installer(artifact: String) =
        ContentArtifactInstaller(context, store, installedArtifact = artifact)

    private fun databaseFile(): File = context.getDatabasePath(NimazDatabase.DATABASE_NAME)

    /** A content database in the shape a device actually has one: content, no user tables. */
    private fun writeContentDatabase(marker: String) {
        SQLiteDatabase.openOrCreateDatabase(databaseFile().also { it.parentFile?.mkdirs() }, null)
            .use { db ->
                db.execSQL("CREATE TABLE IF NOT EXISTS surahs (id INTEGER PRIMARY KEY, name TEXT)")
                db.execSQL("INSERT INTO surahs (id, name) VALUES (1, ?)", arrayOf(marker))
            }
    }

    private fun addLegacyUserTable(table: String, rows: Int) {
        SQLiteDatabase.openOrCreateDatabase(databaseFile(), null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `$table` (id INTEGER PRIMARY KEY)")
            repeat(rows) { db.execSQL("INSERT INTO `$table` (id) VALUES (${it + 1})") }
        }
    }

    @Test
    fun `a fresh install records the artifact and leaves the copy to Room`() {
        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.FreshInstall)
        assertThat(store.installedArtifact()).isEqualTo("sha-v8")
    }

    @Test
    fun `an unchanged artifact is not re-copied`() {
        writeContentDatabase("v7 content")
        store.setInstalledArtifact("sha-v7")

        val outcome = installer("sha-v7").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.AlreadyCurrent)
        assertThat(databaseFile().exists()).isTrue()
    }

    /**
     * The whole point: an update that ships a different artifact removes the old database so
     * `createFromAsset` copies the new one. Without this, a content release reaches nobody who
     * already has the app.
     */
    @Test
    fun `a changed artifact deletes the database so the new one is copied`() {
        writeContentDatabase("v7 content")
        store.setInstalledArtifact("sha-v7")

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(databaseFile().exists()).isFalse()
        assertThat(store.installedArtifact()).isEqualTo("sha-v8")
    }

    /**
     * An install that predates schemaVersion 23 still holds the user's own rows in this file,
     * kept so `LegacyUserDataImport` can copy them out. Deleting it first would be the worst bug
     * in the app: someone's bookmarks, gone, to deliver a topic index.
     */
    @Test
    fun `legacy user rows hold the replace off`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome)
            .isEqualTo(ContentArtifactInstaller.Outcome.DeferredForLegacyData("quran_bookmarks"))
        assertThat(databaseFile().exists()).isTrue()
        // Deliberately not recorded: the next launch must try again, after the copy has run.
        assertThat(store.installedArtifact()).isEqualTo("sha-v7")
    }

    /**
     * The deferral is temporary, not a permanent block. Once the legacy tables are empty — or
     * `MIGRATION_22_23`-era rows were never there — the replace proceeds.
     */
    @Test
    fun `an empty legacy table does not hold the replace off`() {
        writeContentDatabase("v23 content")
        addLegacyUserTable("quran_bookmarks", rows = 0)
        store.setInstalledArtifact("sha-v7")

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(databaseFile().exists()).isFalse()
    }

    /**
     * The regression that matters most in this file.
     *
     * `LegacyUserDataImport` leaves the source rows in place, so "are there legacy rows" answers
     * yes forever. A deferral keyed on that alone would stop these installs — the oldest and
     * most-invested users — from ever receiving another content release. The flag is what bounds
     * the wait to one launch.
     */
    @Test
    fun `once the legacy import has run, the replace proceeds despite the rows remaining`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")
        store.setLegacyImportComplete()

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(databaseFile().exists()).isFalse()
    }

    @Test
    fun `every user table is treated as a blocker, not just bookmarks`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("khatam_ayahs", rows = 1)
        store.setInstalledArtifact("sha-v7")

        assertThat(installer("sha-v8").installIfChanged())
            .isEqualTo(ContentArtifactInstaller.Outcome.DeferredForLegacyData("khatam_ayahs"))
    }

    /**
     * A device that has never recorded an artifact — every install made before this code shipped
     * — must still get the new content. `null != sha` is the replace path, which is what makes
     * this change reach the people who are waiting for it.
     */
    @Test
    fun `an install that predates the store is replaced`() {
        writeContentDatabase("content from before the installer existed")

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(store.installedArtifact()).isEqualTo("sha-v8")
    }

    /**
     * A file that cannot be read is not a file to delete. Refusing is always recoverable; a
     * wrong delete is not.
     */
    @Test
    fun `an unreadable database is not deleted`() {
        databaseFile().parentFile?.mkdirs()
        databaseFile().writeText("this is not a database")
        store.setInstalledArtifact("sha-v7")

        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome)
            .isEqualTo(ContentArtifactInstaller.Outcome.DeferredForLegacyData("unreadable"))
        assertThat(databaseFile().exists()).isTrue()
    }

    // ── Stuck-deferral detection (#472, #473) ────────────────────────────────────
    //
    // A deferral is meant to last exactly one launch: `UserDataMigrator` runs on every
    // launch and is awaited before the splash lifts, so the copy completes during that
    // session and the next launch replaces. The failure that is *not* designed for is a
    // deferral that repeats — a migrator that keeps failing, or a database that cannot be
    // read (`legacyDataBlocking` treats any read failure as "something is there"). Such a
    // device silently stops receiving content releases altogether, which also means it
    // never receives an FTS index, which is why Arabic search returns nothing on it.
    //
    // Nothing reported that. These tests are what make it reportable.

    @Test
    fun `a deferral is counted`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")

        installer("sha-v8").installIfChanged()

        assertThat(store.consecutiveDeferrals()).isEqualTo(1)
    }

    @Test
    fun `a successful replace clears the deferral count`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")
        installer("sha-v8").installIfChanged()
        assertThat(store.consecutiveDeferrals()).isEqualTo(1)

        // The migrator has now run, which is what unblocks it.
        store.setLegacyImportComplete()
        val outcome = installer("sha-v8").installIfChanged()

        assertThat(outcome).isEqualTo(ContentArtifactInstaller.Outcome.Replaced)
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)
    }

    @Test
    fun `a deferral that repeats past the threshold is reported`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")

        val reported = mutableListOf<String>()
        repeat(ContentArtifactInstaller.STUCK_AFTER_DEFERRALS) {
            ContentArtifactInstaller(
                context, store, installedArtifact = "sha-v8", reportStuck = reported::add,
            ).installIfChanged()
        }

        assertThat(reported).hasSize(1)
        assertThat(reported.single()).contains("quran_bookmarks")
    }

    @Test
    fun `a single deferral is not reported`() {
        writeContentDatabase("v22 content")
        addLegacyUserTable("quran_bookmarks", rows = 3)
        store.setInstalledArtifact("sha-v7")

        val reported = mutableListOf<String>()
        ContentArtifactInstaller(
            context, store, installedArtifact = "sha-v8", reportStuck = reported::add,
        ).installIfChanged()

        assertThat(reported).isEmpty()
    }

    @Test
    fun `the default report path is the one production uses`() {
        // Every other test here injects its own `reportStuck` so it can assert the message. That
        // leaves the lambda the app actually runs with — the one that reaches CrashReporter —
        // never executed, which is how a crash in the reporting of a stuck device would ship.
        writeContentDatabase("v7 content")
        addLegacyUserTable("quran_bookmarks", rows = 1)
        store.setInstalledArtifact("sha-v7")
        val installer = ContentArtifactInstaller(context, store, installedArtifact = "sha-v8")

        repeat(5) { installer.installIfChanged() }

        assertThat(store.consecutiveDeferrals()).isEqualTo(5)
    }

    private class FakeStore : ContentArtifactStore {

        private var value: String? = null
        private var legacyDone = false
        override fun installedArtifact(): String? = value
        override fun setInstalledArtifact(sha256: String) {
            value = sha256
        }

        override fun legacyImportComplete(): Boolean = legacyDone
        override fun setLegacyImportComplete() {
            legacyDone = true
        }

        private var deferrals = 0
        override fun consecutiveDeferrals(): Int = deferrals
        override fun recordDeferral() {
            deferrals++
        }

        override fun clearDeferrals() {
            deferrals = 0
        }
    }
}
