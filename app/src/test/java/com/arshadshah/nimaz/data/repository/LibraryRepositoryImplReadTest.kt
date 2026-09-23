package com.arshadshah.nimaz.data.repository

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The real [LibraryRepositoryImpl], reading the licence list the About screen shows from the
 * catalogue the AboutLibraries plugin generates into `:app`. Under Robolectric the app `Context`
 * it needs is available, so this reads the actual `R.raw.aboutlibraries` — the contract tests in
 * [LibraryRepositoryImplTest] use a fake for the shape. What matters here is that it reads at
 * all, sorts the way a reader scans, and finds a library by the id its row carries.
 */
@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryImplReadTest {

    private val repository = LibraryRepositoryImpl(ApplicationProvider.getApplicationContext())

    @Test
    fun `the catalogue reads, alphabetically`() = runTest {
        val libraries = repository.getLibraries()

        assertThat(libraries).isNotEmpty()
        assertThat(libraries.map { it.name.lowercase() }).isInOrder()
    }

    @Test
    fun `a library is found by its id, and an unknown id finds nothing`() = runTest {
        val first = repository.getLibraries().first()

        assertThat(repository.getLibrary(first.id)).isEqualTo(first)
        assertThat(repository.getLibrary(Int.MIN_VALUE)).isNull()
    }
}
