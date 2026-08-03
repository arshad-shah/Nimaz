package com.arshadshah.nimaz.data.local.content

import android.content.Context
import androidx.core.content.edit

/**
 * Which content artifact the database on disk was created from.
 *
 * **SharedPreferences, not DataStore** — and that is the one interesting thing about this file.
 * Every other preference in the app is DataStore, but this value has to be read at the instant
 * `DatabaseModule.provideNimazDatabase` builds the database, before Room opens the file. DataStore
 * is suspend-only, so reading it there would mean `runBlocking` on whatever thread first injects
 * the database — potentially the main one. A single synchronous `getString` on a one-key file is
 * the honest tool for a value the constructor of another component depends on.
 */
interface ContentArtifactStore {
    /** The artifact sha256 the on-disk database came from, or null before anything recorded one. */
    fun installedArtifact(): String?

    fun setInstalledArtifact(sha256: String)

    /**
     * Whether `LegacyUserDataImport` has finished copying a pre-schemaVersion-23 install's rows
     * into the user database.
     *
     * This exists because the copy **leaves the source rows where they are** — deliberately, so a
     * bug in it stays survivable. That makes "are there still legacy rows in the content
     * database" useless as a completion signal: the answer is yes forever, and an installer that
     * waited on it would defer every content release for the rest of that install's life.
     */
    fun legacyImportComplete(): Boolean

    fun setLegacyImportComplete()
}

class SharedPreferencesContentArtifactStore(
    context: Context,
) : ContentArtifactStore {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun installedArtifact(): String? = prefs.getString(KEY, null)

    override fun setInstalledArtifact(sha256: String) {
        // `commit`, not `apply`: the write records that a database was replaced, and the process
        // can be killed between the delete and the next launch. An async write that loses the
        // race leaves the app replacing the same artifact on every launch — a 176 MB copy each
        // time, forever.
        prefs.edit(commit = true) { putString(KEY, sha256) }
    }

    override fun legacyImportComplete(): Boolean = prefs.getBoolean(LEGACY_KEY, false)

    override fun setLegacyImportComplete() {
        // Also `commit`. Losing this write means the next launch defers a content release it
        // could have taken — recoverable, but it costs the user a release each time it happens.
        prefs.edit(commit = true) { putBoolean(LEGACY_KEY, true) }
    }

    private companion object {
        const val FILE = "nimaz_content_artifact"
        const val KEY = "installed_artifact_sha256"
        const val LEGACY_KEY = "legacy_user_data_import_complete"
    }
}
