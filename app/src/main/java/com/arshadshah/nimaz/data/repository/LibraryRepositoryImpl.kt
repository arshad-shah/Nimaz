package com.arshadshah.nimaz.data.repository

import android.content.Context
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext
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
 */
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LibraryRepository {

    override suspend fun getLibraries(): List<OpenSourceLibrary> = onDispatcher(Dispatchers.IO) {
        Libs.Builder().withContext(context).build()
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
    version = artifactVersion,
    author = developers.firstOrNull()?.name,
    website = website,
    licenses = licenses.map { LibraryLicense(name = it.name, url = it.url, content = it.licenseContent) },
)
