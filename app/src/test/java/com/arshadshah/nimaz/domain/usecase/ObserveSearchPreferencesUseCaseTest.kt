package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.presentation.viewmodel.FakeSearchSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The boundary between four stored primitives and the typed model search runs on.
 *
 * A preferences file outlives the build that wrote it — a downgrade, a restore from a device on
 * a newer version, a hand-edit — so everything here is about what happens when the stored text
 * is not something this build would have written.
 */
class ObserveSearchPreferencesUseCaseTest {

    private suspend fun preferencesFrom(settings: FakeSearchSettings) =
        ObserveSearchPreferencesUseCase(settings)().first()

    @Test
    fun `an unwritten preferences file is the defaults`() = runTest {
        val prefs = preferencesFrom(FakeSearchSettings())

        assertThat(prefs.resultsPerSource).isEqualTo(60)
        assertThat(prefs.sources).isEqualTo(LibrarySource.entries.toSet())
        assertThat(prefs.strictness).isEqualTo(MatchStrictness.BALANCED)
        assertThat(prefs.defaultScope).isNull()
    }

    @Test
    fun `a chosen subset round-trips through the stored form`() = runTest {
        val chosen = setOf(LibrarySource.QURAN, LibrarySource.DUAS)

        val prefs = preferencesFrom(
            FakeSearchSettings(sources = ObserveSearchPreferencesUseCase.encode(chosen))
        )

        assertThat(prefs.sources).isEqualTo(chosen)
    }

    @Test
    fun `everything-selected is stored as empty, so a later source is included too`() {
        // If the full set were written out by name, a build that adds an eighth source would
        // read back seven and silently stop searching the new one for existing users.
        assertThat(ObserveSearchPreferencesUseCase.encode(LibrarySource.entries.toSet()))
            .isEmpty()
    }

    @Test
    fun `a source name this build does not know is dropped, not fatal`() = runTest {
        val prefs = preferencesFrom(FakeSearchSettings(sources = "QURAN,TAFSIR,DUAS"))

        assertThat(prefs.sources).containsExactly(LibrarySource.QURAN, LibrarySource.DUAS)
    }

    @Test
    fun `a source list that is entirely unknown falls back to everything`() = runTest {
        // Dropping all of them would leave an empty set, which is search returning nothing.
        val prefs = preferencesFrom(FakeSearchSettings(sources = "TAFSIR,SEERAH"))

        assertThat(prefs.sources).isEqualTo(LibrarySource.entries.toSet())
    }

    @Test
    fun `an unrecognised strictness degrades to the default rather than throwing`() = runTest {
        val prefs = preferencesFrom(FakeSearchSettings(strictness = "PEDANTIC"))

        assertThat(prefs.strictness).isEqualTo(MatchStrictness.BALANCED)
    }

    @Test
    fun `a stored value out of range is clamped on the way out`() = runTest {
        val prefs = preferencesFrom(FakeSearchSettings(resultsPerSource = 100_000))

        assertThat(prefs.resultsPerSource).isEqualTo(200)
    }

    @Test
    fun `the default scope is dropped when it points outside the chosen sources`() = runTest {
        val prefs = preferencesFrom(
            FakeSearchSettings(sources = "QURAN", defaultScope = "HADITH")
        )

        assertThat(prefs.defaultScope).isNull()
    }

    @Test
    fun `changing a stored value re-emits`() = runTest {
        // The settings screen writes; search reads on its next query. Both go through here.
        val settings = FakeSearchSettings()
        val preferences = ObserveSearchPreferencesUseCase(settings)

        assertThat(preferences().first().resultsPerSource).isEqualTo(60)
        settings.setSearchResultsPerSource(25)
        assertThat(preferences().first().resultsPerSource).isEqualTo(25)
    }
}
