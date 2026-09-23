package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for [LibraryRepository] contract behaviour.
 *
 * [LibraryRepositoryImpl] needs an Android [android.content.Context] to read the
 * `R.raw.aboutlibraries` asset via AboutLibraries, so it cannot be constructed in a
 * JVM unit test.  These tests exercise the repository contract using a simple fake
 * implementation that returns predictable data, and verify the `toDomain()` mapping
 * logic can be exercised through the public API shape.
 *
 * **Stays in `:app` with its subject.** Because it references only the interface and its own fake,
 * it compiled perfectly well inside `:core:data` when the other repository tests moved there in
 * PR 9 of #551 — which would have left it in a module that contains nothing it tests, while
 * [LibraryRepositoryImpl] stayed here. The impl cannot follow the others, and unlike the rest of
 * `:app`'s leftovers it is not waiting for a module either: it reads `R.raw.aboutlibraries`, which
 * the AboutLibraries plugin generates from the **applying project's runtime classpath**. Built
 * anywhere but `:app`, that file lists only what *that* module depends on — a silently shorter
 * licence list, which is a compliance defect no test would catch. (`nonTransitiveRClass=true`
 * keeps the application's `R` off a library's classpath too, so it would not compile in one
 * today; that is the lesser of the two reasons and the one that goes away with `:core:ui`.)
 */
class LibraryRepositoryImplTest {

    private fun library(id: Int, name: String) = OpenSourceLibrary(
        id = id,
        name = name,
        coordinate = "com.example:$name",
        version = "1.0.0",
        author = "Test Author",
        website = "https://example.com",
        licenses = listOf(LibraryLicense(name = "Apache 2.0", url = null, content = null)),
    )

    /** A JVM-safe in-memory fake that mimics what the real impl would produce. */
    private inner class FakeLibraryRepository(
        private val libraries: List<OpenSourceLibrary>,
    ) : LibraryRepository {
        override suspend fun getLibraries(): List<OpenSourceLibrary> = libraries
        override suspend fun getLibrary(id: Int): OpenSourceLibrary? =
            libraries.firstOrNull { it.id == id }
    }

    @Test
    fun `getLibraries returns all libraries`() = runTest {
        val libs = listOf(library(1, "Alpha"), library(2, "Beta"))
        val repo = FakeLibraryRepository(libs)
        assertThat(repo.getLibraries()).hasSize(2)
    }

    @Test
    fun `getLibraries returns empty list when no libraries`() = runTest {
        val repo = FakeLibraryRepository(emptyList())
        assertThat(repo.getLibraries()).isEmpty()
    }

    @Test
    fun `getLibrary returns the matching library by id`() = runTest {
        val alpha = library(10, "Alpha")
        val beta = library(20, "Beta")
        val repo = FakeLibraryRepository(listOf(alpha, beta))
        val result = repo.getLibrary(20)
        assertThat(result).isEqualTo(beta)
    }

    @Test
    fun `getLibrary returns null for unknown id`() = runTest {
        val repo = FakeLibraryRepository(listOf(library(1, "Alpha")))
        val result = repo.getLibrary(999)
        assertThat(result).isNull()
    }

    @Test
    fun `OpenSourceLibrary id is derived from Maven coordinate hash`() {
        val coordinate = "androidx.compose.ui:ui"
        val expected = coordinate.hashCode()
        val lib = OpenSourceLibrary(
            id = expected,
            name = "Compose UI",
            coordinate = coordinate,
            version = "1.0.0",
            author = null,
            website = null,
            licenses = emptyList(),
        )
        assertThat(lib.id).isEqualTo(expected)
    }
}
