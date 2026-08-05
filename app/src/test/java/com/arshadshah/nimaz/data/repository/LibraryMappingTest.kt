package com.arshadshah.nimaz.data.repository

import com.google.common.truth.Truth.assertThat
import com.mikepenz.aboutlibraries.entity.Developer
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import org.junit.Test

/**
 * The identity the licence detail route travels on.
 *
 * It used to be `library.hashCode()` — the hash of the whole object — so a version bump
 * or a re-ordered developer list silently changed the id of the same library, and a
 * saved detail route pointed at nothing. The domain id comes from `uniqueId`, the Maven
 * coordinate, which is what actually identifies a dependency.
 */
class LibraryMappingTest {

    private fun library(
        uniqueId: String = "androidx.compose.ui:ui",
        version: String? = "1.7.0",
        developers: List<Developer> = listOf(Developer("Google", null)),
        licenses: Set<License> = setOf(
            License(
                name = "Apache-2.0",
                url = "https://apache.org/licenses/LICENSE-2.0",
                licenseContent = "Apache text",
                hash = "apache-2.0",
            ),
        ),
    ) = Library(
        uniqueId = uniqueId,
        artifactVersion = version,
        name = "Compose UI",
        description = null,
        website = "https://developer.android.com",
        developers = developers,
        organization = null,
        scm = null,
        licenses = licenses,
    )

    @Test
    fun `identity comes from the coordinate, not the whole object`() {
        val current = library(version = "1.7.0").toDomain()
        val bumped = library(version = "1.8.0").toDomain()

        assertThat(current.id).isEqualTo(bumped.id)
        assertThat(current.id).isEqualTo("androidx.compose.ui:ui".hashCode())
    }

    @Test
    fun `the fields the screens render survive the mapping`() {
        val mapped = library().toDomain()

        assertThat(mapped.name).isEqualTo("Compose UI")
        assertThat(mapped.version).isEqualTo("1.7.0")
        assertThat(mapped.author).isEqualTo("Google")
        assertThat(mapped.website).isEqualTo("https://developer.android.com")
        assertThat(mapped.licenses).hasSize(1)
        assertThat(mapped.licenses.first().name).isEqualTo("Apache-2.0")
        assertThat(mapped.licenses.first().content).isEqualTo("Apache text")
    }

    @Test
    fun `a library with no developer and no licence still maps`() {
        // Both are optional in a pom, and the old screens reached for
        // `developers.firstOrNull()?.name` at the call site every time they rendered.
        val mapped = library(developers = emptyList(), licenses = emptySet()).toDomain()

        assertThat(mapped.author).isNull()
        assertThat(mapped.licenses).isEmpty()
    }
}
