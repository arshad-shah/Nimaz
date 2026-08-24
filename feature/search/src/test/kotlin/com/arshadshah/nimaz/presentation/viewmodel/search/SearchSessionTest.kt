package com.arshadshah.nimaz.presentation.viewmodel.search

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LibrarySearchResults
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.repository.settings.FakeSearchSettings
import com.arshadshah.nimaz.domain.usecase.ObserveSearchPreferencesUseCase
import com.arshadshah.nimaz.domain.usecase.SearchLibraryUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * A search session over time: a failure, a correction, the history it leaves behind.
 *
 * `SearchViewModelTest` covers a search that works. This covers the rest of a real session, where
 * the interesting states are the ones that persist *between* searches. A failed lookup that leaves
 * `error` set is the sharpest of them: the next search must clear it, or the screen renders an
 * error banner over a list of perfectly good results — the reader is told the search failed while
 * looking at what it found.
 *
 * The recent-searches list is the other one. It is the only thing here that survives a query being
 * cleared, and it is deliberately *not* written on every keystroke — search-as-you-type would
 * otherwise fill it with "n", "no", "noo" and bury what was actually searched for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchSessionTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var searchLibrary: SearchLibraryUseCase
    private val telemetry = RecordingTelemetry()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        searchLibrary = mockk()
        coEvery { searchLibrary.invoke(any(), any()) } returns LibrarySearchResults.EMPTY
        coEvery { searchLibrary.byTerms(any(), any()) } returns LibrarySearchResults.EMPTY
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(settings: FakeSearchSettings = FakeSearchSettings()) =
        SearchViewModel(
            searchLibrary,
            ObserveSearchPreferencesUseCase(settings),
            telemetry,
        )

    // ── a lookup that fails ──────────────────────────────────────────────────

    /**
     * The failure has to be *reported*, not swallowed into an empty list — an empty list is
     * indistinguishable from "your library has nothing on this", which is the wrong answer.
     */
    @Test
    fun `a failed lookup surfaces as an error rather than as no matches`() = runTest {
        coEvery { searchLibrary.invoke(any(), any()) } throws
            IllegalStateException("no such table: quran_fts")

        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()

        val error = vm.searchState.value.error
        assertThat(error).isNotNull()
        assertThat(error!!.message).isEqualTo(R.string.search_failed)
        // The exception's own text is kept for a bug report, never used as the message.
        assertThat(error.details).isEqualTo("no such table: quran_fts")
        assertThat(vm.searchState.value.isSearching).isFalse()
    }

    /** An error banner left standing over fresh results tells the reader the opposite of the truth. */
    @Test
    fun `the next search clears the previous failure`() = runTest {
        coEvery { searchLibrary.invoke(any(), any()) } throws IllegalStateException("boom")
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()
        assertThat(vm.searchState.value.error).isNotNull()

        coEvery { searchLibrary.invoke(any(), any()) } returns LibrarySearchResults.EMPTY
        vm.onEvent(SearchEvent.UpdateQuery("noor light"))
        advanceUntilIdle()

        assertThat(vm.searchState.value.error).isNull()
    }

    @Test
    fun `a failure is reported to telemetry without the query text`() = runTest {
        coEvery { searchLibrary.invoke(any(), any()) } throws IllegalStateException("boom")
        val vm = viewModel()

        vm.onEvent(SearchEvent.UpdateQuery("my private question"))
        advanceUntilIdle()

        // What is recorded is the length and the filter — never the words.
        val search = telemetry.searches.single()
        assertThat(search.queryLength).isEqualTo("my private question".length)
        assertThat(telemetry.calls.toString()).doesNotContain("my private question")
    }

    // ── superseded searches ──────────────────────────────────────────────────

    /**
     * A slow first lookup must not land on top of a faster second one. The list would then show
     * results for a query the box no longer contains, and nothing on screen would say so.
     */
    @Test
    fun `a slow result is discarded when a newer search has already been submitted`() = runTest {
        val slow = CompletableDeferred<LibrarySearchResults>()
        coEvery { searchLibrary.invoke("noor", any()) } coAnswers { slow.await() }
        coEvery { searchLibrary.invoke("sabr", any()) } returns LibrarySearchResults(
            surahs = listOf(mockk(relaxed = true)),
        )

        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()

        vm.onEvent(SearchEvent.UpdateQuery("sabr"))
        advanceUntilIdle()

        // The first lookup completes late — the job it belonged to was already cancelled.
        slow.complete(LibrarySearchResults(quran = List(9) { mockk(relaxed = true) }))
        advanceUntilIdle()

        assertThat(vm.searchState.value.allResults).hasSize(1)
        assertThat(vm.statsState.value.totalResults).isEqualTo(1)
    }

    // ── the recent list ──────────────────────────────────────────────────────

    /** Typing is not submitting: search-as-you-type would otherwise record every prefix. */
    @Test
    fun `typing does not write to the recent list`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        advanceUntilIdle()

        assertThat(vm.searchState.value.recentSearches).isEmpty()
    }

    @Test
    fun `submitting records the query and searches immediately`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        vm.onEvent(SearchEvent.ExecuteSearch)
        advanceUntilIdle()

        assertThat(vm.searchState.value.recentSearches).containsExactly("noor")
    }

    @Test
    fun `submitting an empty box searches nothing and records nothing`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("   "))
        vm.onEvent(SearchEvent.ExecuteSearch)
        advanceUntilIdle()

        coVerify(exactly = 0) { searchLibrary.invoke(any(), any()) }
        assertThat(vm.searchState.value.recentSearches).isEmpty()
        // Nothing ran, so nothing may be counted as a search performed.
        assertThat(telemetry.searches).isEmpty()
    }

    /** Searching the same words again moves that entry up rather than adding a duplicate. */
    @Test
    fun `re-running a search moves it to the top instead of repeating it`() = runTest {
        val vm = viewModel()
        listOf("noor", "sabr", "noor").forEach {
            vm.onEvent(SearchEvent.UpdateQuery(it))
            vm.onEvent(SearchEvent.ExecuteSearch)
            advanceUntilIdle()
        }

        assertThat(vm.searchState.value.recentSearches).containsExactly("noor", "sabr").inOrder()
    }

    /** The list is a convenience, not an archive — it stops at ten so the resting screen stays usable. */
    @Test
    fun `the recent list keeps the last ten searches`() = runTest {
        val vm = viewModel()
        (1..12).forEach {
            vm.onEvent(SearchEvent.UpdateQuery("query $it"))
            vm.onEvent(SearchEvent.ExecuteSearch)
            advanceUntilIdle()
        }

        val recents = vm.searchState.value.recentSearches
        assertThat(recents).hasSize(10)
        assertThat(recents.first()).isEqualTo("query 12")
        assertThat(recents).doesNotContain("query 1")
        assertThat(recents).doesNotContain("query 2")
    }

    @Test
    fun `a recent search can be removed without touching the others`() = runTest {
        val vm = viewModel()
        listOf("noor", "sabr").forEach {
            vm.onEvent(SearchEvent.UpdateQuery(it))
            vm.onEvent(SearchEvent.ExecuteSearch)
            advanceUntilIdle()
        }

        vm.onEvent(SearchEvent.RemoveRecentSearch("noor"))

        assertThat(vm.searchState.value.recentSearches).containsExactly("sabr")
    }

    @Test
    fun `clearing the recent list empties it`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        vm.onEvent(SearchEvent.ExecuteSearch)
        advanceUntilIdle()

        vm.onEvent(SearchEvent.ClearRecentSearches)

        assertThat(vm.searchState.value.recentSearches).isEmpty()
    }

    /**
     * Clearing the box is not clearing the history: the recent list is what makes the resting
     * screen useful, and the clear button is next to the query, not next to the history.
     */
    @Test
    fun `clearing the search keeps the history it produced`() = runTest {
        coEvery { searchLibrary.invoke(any(), any()) } returns
            LibrarySearchResults(surahs = listOf(mockk(relaxed = true)))
        val vm = viewModel()
        vm.onEvent(SearchEvent.UpdateQuery("noor"))
        vm.onEvent(SearchEvent.ExecuteSearch)
        advanceUntilIdle()

        vm.onEvent(SearchEvent.ClearSearch)

        assertThat(vm.searchState.value.query).isEmpty()
        assertThat(vm.searchState.value.allResults).isEmpty()
        assertThat(vm.statsState.value.totalResults).isEqualTo(0)
        assertThat(vm.searchState.value.recentSearches).containsExactly("noor")
    }

    @Test
    fun `tapping a recent search runs it again and keeps it at the top`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.SelectRecentSearch("sabr"))
        advanceUntilIdle()

        coVerify { searchLibrary.invoke("sabr", any()) }
        assertThat(vm.searchState.value.query).isEqualTo("sabr")
        assertThat(vm.searchState.value.recentSearches).containsExactly("sabr")
    }

    // ── AI terms ─────────────────────────────────────────────────────────────

    /** Nothing usable in the terms means nothing happens — not a search for the empty string. */
    @Test
    fun `blank AI terms leave the existing list alone`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.ApplyAiTerms(listOf("", "   ")))
        advanceUntilIdle()

        coVerify(exactly = 0) { searchLibrary.byTerms(any(), any()) }
    }

    @Test
    fun `repeated AI terms are asked for once`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchEvent.ApplyAiTerms(listOf("sabr", "sabr", " sabr ")))
        advanceUntilIdle()

        coVerify { searchLibrary.byTerms(listOf("sabr"), any()) }
    }

    // ── the stored default scope, for every source ───────────────────────────

    /**
     * Every [LibrarySource] has to map to a chip. The mapping is exhaustive in the ViewModel so a
     * new source will not compile without one — this is the other half: that each existing source
     * maps to the *right* chip, which the compiler cannot check.
     */
    @Test
    fun `each stored source opens search on its own filter`() = runTest {
        mapOf(
            LibrarySource.QURAN to SearchFilter.QURAN,
            LibrarySource.HADITH to SearchFilter.HADITH,
            LibrarySource.DUAS to SearchFilter.DUA,
            LibrarySource.NAMES to SearchFilter.NAMES,
        ).forEach { (source, filter) ->
            val vm = viewModel(FakeSearchSettings(defaultScope = source.name))
            advanceUntilIdle()

            assertThat(vm.searchState.value.selectedFilter).isEqualTo(filter)
        }
    }

    @Test
    fun `an unrecognised stored scope leaves search on everything`() = runTest {
        // A scope written by a newer build, or a corrupted value: fall back rather than crash.
        val vm = viewModel(FakeSearchSettings(defaultScope = "PODCASTS"))
        advanceUntilIdle()

        assertThat(vm.searchState.value.selectedFilter).isEqualTo(SearchFilter.ALL)
    }

    // ── the filter predicate ─────────────────────────────────────────────────

    /**
     * One predicate decides both what the list shows and what the chips count, so every branch of
     * it has to agree with the corpus it names. `SurahResult` under QURAN is the one that is not
     * obvious — and it is exactly the case the deleted per-corpus counters got wrong.
     */
    @Test
    fun `each filter accepts its own corpus and nothing else`() {
        val quran = com.arshadshah.nimaz.domain.model.UnifiedSearchResult.QuranResult(mockk())
        val surah = com.arshadshah.nimaz.domain.model.UnifiedSearchResult.SurahResult(mockk())
        val hadith = com.arshadshah.nimaz.domain.model.UnifiedSearchResult.HadithResult(mockk())
        val dua = com.arshadshah.nimaz.domain.model.UnifiedSearchResult.DuaResult(mockk())
        val name = com.arshadshah.nimaz.domain.model.UnifiedSearchResult.NameResult(mockk())
        val all = listOf(quran, surah, hadith, dua, name)

        assertThat(all.filter { SearchFilter.ALL.accepts(it) }).hasSize(5)
        // A surah is a Qur'an hit for the purpose of filtering — it is a place in the Qur'an.
        assertThat(all.filter { SearchFilter.QURAN.accepts(it) })
            .containsExactly(quran, surah)
        assertThat(all.filter { SearchFilter.HADITH.accepts(it) }).containsExactly(hadith)
        assertThat(all.filter { SearchFilter.DUA.accepts(it) }).containsExactly(dua)
        assertThat(all.filter { SearchFilter.NAMES.accepts(it) }).containsExactly(name)
    }
}
