package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The states a preferences file can hold that the settings screen cannot produce.
 *
 * Everything here is reachable without a bug in the app: a preferences file survives a
 * downgrade, a restore from a device running a newer build, and a hand-edit. `sanitised` is
 * the boundary where those become something search can run under.
 */
class SearchPreferencesTest {

    @Test
    fun `an empty source set would return nothing, so it means everything`() {
        val stored = SearchPreferences(sources = emptySet())

        assertThat(stored.sanitised.sources).isEqualTo(LibrarySource.entries.toSet())
    }

    @Test
    fun `a result cap outside the allowed range is clamped, not honoured`() {
        // 0 would make every search look broken; 100_000 would make one hang the UI.
        assertThat(SearchPreferences(resultsPerSource = 0).sanitised.resultsPerSource)
            .isEqualTo(SearchPreferences.MIN_RESULTS_PER_SOURCE)
        assertThat(SearchPreferences(resultsPerSource = 100_000).sanitised.resultsPerSource)
            .isEqualTo(SearchPreferences.MAX_RESULTS_PER_SOURCE)
    }

    @Test
    fun `a default scope pointing at a switched-off source is dropped`() {
        // Otherwise the search screen opens focused on a source it will never query, and
        // shows an empty result list that looks like "nothing matched".
        val stored = SearchPreferences(
            sources = setOf(LibrarySource.QURAN),
            defaultScope = LibrarySource.HADITH,
        )

        assertThat(stored.sanitised.defaultScope).isNull()
    }

    @Test
    fun `a default scope inside the chosen sources survives`() {
        val stored = SearchPreferences(
            sources = setOf(LibrarySource.QURAN, LibrarySource.HADITH),
            defaultScope = LibrarySource.HADITH,
        )

        assertThat(stored.sanitised.defaultScope).isEqualTo(LibrarySource.HADITH)
    }

    @Test
    fun `sanitising is idempotent`() {
        val once = SearchPreferences(resultsPerSource = 0, sources = emptySet()).sanitised

        assertThat(once.sanitised).isEqualTo(once)
    }

    @Test
    fun `the defaults are what the app did before any of this was a setting`() {
        val defaults = SearchPreferences()

        assertThat(defaults.resultsPerSource).isEqualTo(60)
        assertThat(defaults.strictness.wordPasses).isEqualTo(8)
        assertThat(defaults.sources).isEqualTo(LibrarySource.entries.toSet())
        assertThat(defaults.defaultScope).isNull()
    }

    @Test
    fun `strictness orders the way its names claim`() {
        assertThat(MatchStrictness.EXACT.wordPasses).isEqualTo(0)
        assertThat(MatchStrictness.EXACT.wordPasses)
            .isLessThan(MatchStrictness.BALANCED.wordPasses)
        assertThat(MatchStrictness.BALANCED.wordPasses)
            .isLessThan(MatchStrictness.BROAD.wordPasses)
    }
}
