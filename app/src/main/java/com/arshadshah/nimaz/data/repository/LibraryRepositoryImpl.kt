package com.arshadshah.nimaz.data.repository

import android.content.Context
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext as onDispatcher

/**
 * The only file in the app that imports `aboutlibraries`.
 *
 * Building [Libs] parses a generated asset, which is why both About screens used to do it
 * inside a `LaunchedEffect` on an IO dispatcher — presentation reaching straight into a
 * data source, and holding the library entity in `remember`. It happens here instead, and
 * what leaves is a domain model.
 *
 * Deliberately uncached: this is a bundled asset parse behind a `suspend` on IO, reached
 * at most twice per visit to the About area. A `by lazy` over a suspend-produced list
 * cannot be written correctly, and a `Mutex`-guarded cache is more machinery than two
 * screens justify.
 *
 * ## Why the resource id, and not `withContext(context)`
 *
 * `withContext(context)` looks the JSON up **by name** — `resources.getIdentifier("aboutlibraries",
 * "raw", packageName)` — and nothing in this app references `R.raw.aboutlibraries` in code. With
 * `isShrinkResources = true` on release, the shrinker therefore reads the resource as unused and
 * strips it from the APK. `getIdentifier` then returns 0, `openRawResource(0)` throws, and
 * AboutLibraries **catches that and returns the builder unchanged**, logging to logcat and
 * carrying on with no data. The failure only surfaces one call later, out of
 * `Libs.Builder.build()`, as "Please provide the required library data via the available APIs" —
 * which describes a caller mistake rather than a missing file, and sent the Licenses screen's
 * error state to say the list could not be loaded when what had happened was that the list had
 * been deleted from the build.
 *
 * Passing `R.raw.aboutlibraries` fixes it at the root: a compile-time `R` reference is exactly
 * what the resource shrinker looks for, so the JSON is kept. It also means that if the Gradle
 * plugin ever stops generating the file, this stops **compiling** rather than failing silently in
 * a release build that debug cannot reproduce.
 */
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LibraryRepository {

    override suspend fun getLibraries(): List<OpenSourceLibrary> = onDispatcher(Dispatchers.IO) {
        Libs.Builder().withJson(context, R.raw.aboutlibraries).build()
            .libraries
            .map { it.toDomain() }
            .sortedBy { it.name.lowercase() }
    }

    override suspend fun getLibrary(id: Int): OpenSourceLibrary? =
        getLibraries().firstOrNull { it.id == id }
}

/**
 * `uniqueId` is the Maven coordinate — `androidx.compose.ui:ui` — so it identifies the
 * dependency across version bumps, which `Library.hashCode()` did not.
 */
internal fun Library.toDomain(): OpenSourceLibrary = OpenSourceLibrary(
    id = uniqueId.hashCode(),
    name = name,
    coordinate = uniqueId,
    version = artifactVersion,
    author = developers.firstOrNull()?.name,
    website = website,
    licenses = licenses.map { LibraryLicense(name = it.name, url = it.url, content = it.licenseContent) },
)
