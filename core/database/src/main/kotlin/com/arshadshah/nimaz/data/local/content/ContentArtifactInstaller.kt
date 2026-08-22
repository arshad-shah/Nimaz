package com.arshadshah.nimaz.data.local.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import java.io.File

/**
 * Puts the content artifact this APK ships with onto the device, replacing whatever is there.
 *
 * ## Why this exists
 *
 * `createFromAsset` copies the prepackaged database **only when the file is absent** — so on an
 * app update it does nothing, and a content release reaches nobody who already has the app.
 * That rule was load-bearing for years for one reason: the content database also held the user's
 * bookmarks, progress and khatams, and re-copying would have destroyed them.
 *
 * That stopped being true at schemaVersion 23, when all 22 user tables moved to
 * `NimazUserDatabase`. `DatabaseModule.provideNimazUserDatabase` says why the split was made:
 * *"The only thing that stopped a content release from taking someone's bookmarks with it was
 * that `createFromAsset` happens not to re-copy on upgrade. Two files makes that structural."*
 * This is the other half of that change — the content database is
 * [disposable][com.arshadshah.nimaz.data.local.database.NimazDatabase], and a release replaces
 * it wholesale, which is what `docs/SUBSYSTEMS.md` §5 has claimed all along.
 *
 * It also **replaced** a whole class of problem rather than working around it. `ContentPatchSeeder`
 * existed because content had to reach existing installs *without* replacing the file, and it
 * could not carry a table the baseline lacked at all (`nz patch emit` filed those under
 * `out_of_scope` and emitted nothing) — so a newly added table reached a fresh install and no one
 * else. Replacing the file has no such limit: new tables, changed rows and dropped rows arrive
 * together, already built and already verified by the data build. The seeder, its patch asset,
 * its version preference and its DI wiring were deleted with this change.
 *
 * ## What makes it safe
 *
 * Three independent things, and the third is the one that matters on a real device:
 *
 * 1. The artifact carries no user tables. `artifact.content-only` fails the data build if one
 *    reappears, and `DeviceStateCorpusTest` asserts each is absent.
 * 2. Nothing writes to this database at runtime. The `@Insert`/`DELETE FROM` methods still on
 *    its DAOs are leftovers of the six seeders retired at versionCode 385.
 * 3. **A device that upgraded from schemaVersion ≤22 still physically holds the old user tables
 *    in this file**, kept deliberately so `LegacyUserDataImport` can copy them out and so a bug
 *    in that copy stays survivable. Deleting the file before that copy has run would destroy the
 *    only copy of somebody's bookmarks, so the replace waits for it.
 *
 * ## Why waiting needs a flag and not a look at the file
 *
 * The obvious rule — "defer while legacy user tables still hold rows" — is wrong, and wrong in
 * the direction that matters. `LegacyUserDataImport` copies with `INSERT OR IGNORE` and **leaves
 * the source rows exactly where they are**, on purpose. So those tables hold rows forever, and
 * that rule would defer every content release for the rest of the install's life: the
 * longest-standing users would silently stop receiving content altogether.
 *
 * So the condition is the conjunction: legacy rows are present **and**
 * [ContentArtifactStore.legacyImportComplete] is not yet set. `UserDataMigrator` sets it after a
 * successful copy, on a launch that is awaited before the splash lifts — so the deferral lasts
 * exactly one launch.
 *
 * **This does consume the recovery copy.** Once the replace happens those legacy rows are gone,
 * and the user database becomes the only copy. That is the trade `docs/SUBSYSTEMS.md` §5 already
 * anticipated — *"A later version can drop them once there is nothing left to recover"* — taken
 * deliberately here, gated on the copy having succeeded.
 */
class ContentArtifactInstaller(
    private val context: Context,
    private val store: ContentArtifactStore,
    /**
     * The sha256 of the artifact **this build ships**, from `BuildConfig.CONTENT_ARTIFACT_SHA256`.
     *
     * Passed in rather than read here. A library module's `BuildConfig` carries only its own
     * fields, never the application's, so reading `com.arshadshah.nimaz.BuildConfig` from
     * `:core:database` does not compile — and defaulting it to something wrong would be worse
     * than not compiling, since the installer's whole job is comparing this against what is on
     * disk. `DatabaseModule` in `:app` supplies it, which is the module that has it.
     */
    private val installedArtifact: String,
    /**
     * Called once when a device looks stuck rather than merely deferred.
     *
     * A lambda rather than a direct `CrashReporter` call so a test can assert it fired —
     * `CrashReporter` is an `object` with a static `Context` and no seam, which is why no test
     * in this repo can currently assert that anything was reported. The general fix for that is
     * the injectable `Telemetry` seam in #359; this is the local version of it.
     */
    private val reportStuck: (String) -> Unit = { CrashReporter.log(it) },
) {

    /**
     * Replace the on-disk content database when this build ships a different artifact.
     *
     * Called from `DatabaseModule.provideNimazDatabase` **before** Room opens the file, because
     * once Room has it open the delete would be a live rug-pull. Returns what it did, which the
     * caller logs and the tests assert on.
     */
    fun installIfChanged(): Outcome {
        val database = context.getDatabasePath(NimazDatabase.DATABASE_NAME)

        // Nothing on disk: `createFromAsset` is about to do the right thing by itself, and the
        // artifact it copies is this one. Recording it now is what makes the *next* release a
        // comparison rather than a guess.
        if (!database.exists()) {
            store.clearDeferrals()
            store.setInstalledArtifact(installedArtifact)
            return Outcome.FreshInstall
        }

        if (store.installedArtifact() == installedArtifact) {
            store.clearDeferrals()
            return Outcome.AlreadyCurrent
        }

        // The order matters: ask the cheap flag first. Once the copy has run, the rows in the
        // content database are a spare copy and reading the file to find them is wasted work on
        // every launch that follows.
        if (!store.legacyImportComplete()) {
            val blocker = legacyDataBlocking(database)
            if (blocker != null) {
                // Not an error, and not permanent: `UserDataMigrator` runs on every launch and is
                // awaited before the splash lifts, so the copy completes during this session and
                // the next launch replaces. One launch of delay, for the shrinking set of installs
                // that predate schemaVersion 23, against destroying the only copy of their data.
                Log.i(TAG, "holding off the content replace: $blocker still has un-copied rows")
                noteDeferral(blocker)
                return Outcome.DeferredForLegacyData(blocker)
            }
        }

        return try {
            // deleteDatabase, not File.delete: the journal, -wal and -shm files are part of the
            // database and a half-deleted family is worse than either whole state.
            val deleted = context.deleteDatabase(NimazDatabase.DATABASE_NAME)
            if (!deleted && database.exists()) {
                Outcome.Failed("deleteDatabase returned false and the file is still there")
            } else {
                store.clearDeferrals()
                store.setInstalledArtifact(installedArtifact)
                Outcome.Replaced
            }
        } catch (e: Exception) {
            // The old database is still openable if the delete failed part-way, and the app is
            // strictly better off starting with stale content than not starting. The stored
            // artifact is deliberately *not* updated, so the next launch tries again.
            CrashReporter.recordException(e)
            Outcome.Failed(e.message ?: e::class.java.simpleName)
        }
    }

    /**
     * Count this deferral, and report once when the count says the device is stuck.
     *
     * One deferral is by design. Repeated ones are not: they mean this install has stopped
     * receiving content releases entirely — no new content, and no FTS index, so every Arabic
     * search on it returns zero rows against a corpus where الله alone appears in 1,746 verses.
     * Reported exactly once, at the threshold, so a stuck device does not become a repeating
     * report for the rest of its life.
     */
    private fun noteDeferral(blocker: String) {
        store.recordDeferral()
        val deferrals = store.consecutiveDeferrals()
        if (deferrals == STUCK_AFTER_DEFERRALS) {
            reportStuck(
                "content artifact deferred $deferrals launches running; " +
                    "\"$blocker\" still holds un-copied rows and the legacy import has not " +
                    "completed — this install is no longer receiving content or a search index"
            )
        }
    }

    /**
     * The name of a legacy user table that still holds rows, or null when there is nothing left
     * to lose.
     *
     * Opened read-only and directly, not through Room: this runs before Room has the file and
     * must not be the thing that creates or migrates it. Any failure to read is treated as
     * "something is there" — refusing to replace is always the recoverable answer.
     */
    private fun legacyDataBlocking(database: File): String? = try {
        SQLiteDatabase.openDatabase(
            database.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { db ->
            val present = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN (${
                    USER_TABLES.joinToString(",") { "?" }
                })",
                USER_TABLES.toTypedArray(),
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            present.firstOrNull { table ->
                db.rawQuery("SELECT EXISTS(SELECT 1 FROM `$table`)", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) == 1
                }
            }
        }
    } catch (e: SQLiteException) {
        Log.w(TAG, "could not inspect the content database for legacy rows", e)
        "unreadable"
    }

    sealed interface Outcome {
        /** No database yet — `createFromAsset` will copy this artifact. */
        data object FreshInstall : Outcome

        /** The file on disk was built from this artifact already. The common case. */
        data object AlreadyCurrent : Outcome

        /** The old file is gone; Room is about to copy the new one. */
        data object Replaced : Outcome

        /** Held off because [table] still holds rows `LegacyUserDataImport` has not copied. */
        data class DeferredForLegacyData(val table: String) : Outcome

        /** The delete did not happen. The app opens the old content and retries next launch. */
        data class Failed(val reason: String) : Outcome
    }

    companion object {
        /**
         * Consecutive deferrals before this install is treated as stuck rather than waiting.
         *
         * Three, not one: a single deferral is the designed path, and two can legitimately
         * happen if the first launch was killed before `UserDataMigrator` finished. Three in a
         * row is not a slow migration, it is a migration that is not completing.
         */
        const val STUCK_AFTER_DEFERRALS = 3

        private const val TAG = "ContentArtifactInstaller"

        /**
         * Tables the app writes to at runtime. Mirrors `user_tables` in the data console's
         * `console.yaml`.
         *
         * Since schemaVersion 23 these are not in the content database at all — they live in
         * `NimazUserDatabase`, and `artifact.content-only` fails the data build if one reappears
         * in the artifact. The list survives here for the one case where they *are* still on
         * disk: an install that predates that split and has not yet had its rows copied out.
         */
        private val USER_TABLES = setOf(
            "reading_progress", "quran_bookmarks", "quran_favorites", "hadith_bookmarks",
            "dua_bookmarks", "dua_progress", "prayer_records", "fast_records", "makeup_fasts",
            "khatams", "khatam_ayahs", "khatam_daily_log", "tasbih_sessions", "zakat_history",
            "tafseer_highlights", "tafseer_notes", "locations", "asma_ul_husna_bookmarks",
            "asma_un_nabi_bookmarks", "prophet_bookmarks", "qaida_lesson_progress",
            "qaida_cell_progress",
        )
    }
}
