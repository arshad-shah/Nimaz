package com.arshadshah.nimaz.presentation.screens.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskUiState
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskViewModel
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchEvent
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchFilter
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchUiState
import com.arshadshah.nimaz.presentation.viewmodel.search.SearchViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The keyword half of Search: what the list shows, and what a tap on it does.
 *
 * Three of these states are only distinguishable on screen. `isSearching`, "no results" and a
 * failed lookup all render a list with nothing in it, and the ViewModel cannot tell them apart —
 * the difference is entirely in which branch the screen renders and in what order. Getting that
 * order wrong is not cosmetic: showing "No results for X" while the lookup is still running tells
 * someone their library has nothing, half a second before it fills with matches; showing it in
 * place of an error tells them the same thing when in fact the search never ran.
 *
 * The rest is the row-to-callback wiring. A result row is the only way into the reader from here,
 * and a row that dispatches the wrong identifiers opens *something* — the wrong verse, the wrong
 * hadith — which reads as content corruption rather than as a bug in a tap handler. Each of the
 * five row kinds carries its own text so an assertion cannot pass on another row's rendering.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SearchScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(SearchUiState())
    private val stats = MutableStateFlow(SearchStatsUiState())
    private val askState = MutableStateFlow(AskUiState())

    private val events = mutableListOf<SearchEvent>()

    private val viewModel: SearchViewModel = mockk(relaxed = true) {
        every { searchState } returns this@SearchScreenTest.state
        every { statsState } returns this@SearchScreenTest.stats
        every { onEvent(any()) } answers { events += firstArg<SearchEvent>() }
    }

    private val askViewModel: AskViewModel = mockk(relaxed = true) {
        every { uiState } returns this@SearchScreenTest.askState
    }

    private var backs = 0
    private var settingsOpens = 0
    private val quranAyahOpens = mutableListOf<Pair<Int, Int>>()
    private val surahOpens = mutableListOf<Int>()
    private val hadithOpens = mutableListOf<Pair<String, String>>()
    private val duaOpens = mutableListOf<String>()
    private val nameOpens = mutableListOf<Pair<NameCatalog, Int>>()

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun render(
        initialFilter: SearchFilter? = null,
        enableAsk: Boolean = false,
    ) {
        composeRule.setThemedContent {
            SearchScreen(
                onNavigateBack = { backs++ },
                onNavigateToQuranAyah = { surah, ayah -> quranAyahOpens += surah to ayah },
                onNavigateToSurah = { surahOpens += it },
                onNavigateToHadith = { book, hadith -> hadithOpens += book to hadith },
                onNavigateToDua = { duaOpens += it },
                onNavigateToName = { catalog, id -> nameOpens += catalog to id },
                initialFilter = initialFilter,
                enableAsk = enableAsk,
                onNavigateToSearchSettings = { settingsOpens++ },
                viewModel = viewModel,
                askViewModel = askViewModel,
            )
        }
        composeRule.waitForIdle()
    }

    // ── the three empty lists ────────────────────────────────────────────────

    /**
     * A search that is still running must not render as a library with nothing in it. The
     * ViewModel flips `isSearching` on synchronously for exactly this reason, and this is the
     * half of that contract that lives in the screen.
     */
    @Test
    fun `a search in flight shows progress and never the no-results sentence`() {
        // The progress spinner animates forever, so the clock is driven by hand — with
        // `autoAdvance` on, `waitForIdle()` would wait for an animation that never ends.
        composeRule.mainClock.autoAdvance = false
        state.value = SearchUiState(query = "noor", isSearching = true)
        render()

        composeRule.onNodeWithText(str(R.string.no_results_format, "noor")).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.no_results_hint)).assertDoesNotExist()
    }

    @Test
    fun `a finished search with nothing in it names the query it found nothing for`() {
        state.value = SearchUiState(query = "qwertyuiop", isSearching = false)
        render()

        // Naming the query is the point: "No results" alone reads as the library being empty.
        composeRule.onNodeWithText(str(R.string.no_results_format, "qwertyuiop")).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.no_results_hint)).assertIsDisplayed()
    }

    /**
     * A failed lookup renders as a section with a retry, *before* the no-results branch. Without
     * that ordering a search that never ran is reported as a search that found nothing, which is
     * the one wrong answer that reads as fact rather than as failure.
     */
    @Test
    fun `a failed search reports the failure instead of claiming there were no matches`() {
        state.value = SearchUiState(
            query = "noor",
            isSearching = false,
            error = UiError(
                message = R.string.search_failed,
                kind = NimazErrorKind.GENERIC,
                details = "SQLITE_ERROR: no such table",
            ),
        )
        render()

        composeRule.onNodeWithText(str(R.string.search_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.search_failed_body)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.no_results_format, "noor")).assertDoesNotExist()
    }

    @Test
    fun `retrying a failed search runs it again`() {
        state.value = SearchUiState(
            query = "noor",
            error = UiError(message = R.string.search_failed),
        )
        render()

        composeRule.onNodeWithText(str(R.string.try_again)).performClick()

        assertThat(events).contains(SearchEvent.ExecuteSearch)
    }

    /** The query box has to survive a failure — different words are the other thing to try. */
    @Test
    fun `the query stays in the bar after a failure`() {
        state.value = SearchUiState(query = "noor", error = UiError(message = R.string.search_failed))
        render()

        composeRule.onAllNodesWithText("noor").onFirst().assertIsDisplayed()
    }

    // ── the rows, and where each one goes ────────────────────────────────────

    @Test
    fun `a verse row opens that verse`() {
        val result = quranResult(surah = 25, ayah = 63, surahName = "Al-Furqan")
        state.value = SearchUiState(
            query = "humbly",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText(str(R.string.surah_result_format, 25, 63)).performClick()

        assertThat(quranAyahOpens).containsExactly(25 to 63)
    }

    @Test
    fun `a surah row opens the surah, not a verse inside it`() {
        val result = surahResult(number = 18, nameEnglish = "The Cave")
        state.value = SearchUiState(
            query = "cave",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText("The Cave").performClick()

        assertThat(surahOpens).containsExactly(18)
        assertThat(quranAyahOpens).isEmpty()
    }

    /**
     * The hadith row carries two identifiers and they are not interchangeable — the book slug and
     * the record id. Passing the record id as the book (or the number as the id) opens a reader
     * pointed at nothing.
     */
    @Test
    fun `a hadith row passes the book and the record id in that order`() {
        val result = hadithResult(id = "bukhari-1", bookId = "bukhari", hadithNumber = 1)
        state.value = SearchUiState(
            query = "intentions",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText(str(R.string.hadith_result_format, 1)).performClick()

        assertThat(hadithOpens).containsExactly("bukhari" to "bukhari-1")
    }

    @Test
    fun `a dua row opens that dua`() {
        val result = duaResult(id = "dua-anxiety", title = "Dua for anxiety")
        state.value = SearchUiState(
            query = "anxiety",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText("Dua for anxiety").performClick()

        assertThat(duaOpens).containsExactly("dua-anxiety")
    }

    /**
     * Three catalogues share one row type and one id space each, so the catalogue has to travel
     * with the tap. Name 1 is Ar-Rahman in one catalogue and Adam in another; dropping the
     * catalogue opens the wrong record with no sign anything went wrong.
     */
    @Test
    fun `a name row carries its catalogue`() {
        val result = nameResult(
            catalog = NameCatalog.PROPHETS,
            id = 1,
            transliteration = "Adam",
            english = "Adam",
        )
        state.value = SearchUiState(
            query = "adam",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText("Adam").performClick()

        assertThat(nameOpens).containsExactly(NameCatalog.PROPHETS to 1)
    }

    /** "Names" alone would not say whether the row is a Name of Allah or a prophet. */
    @Test
    fun `each name catalogue is tagged with its own label`() {
        state.value = SearchUiState(
            query = "a",
            allResults = listOf(
                nameResult(NameCatalog.ASMA_UL_HUSNA, 1, "Ar-Rahman", "The Most Merciful"),
                nameResult(NameCatalog.ASMA_UN_NABI, 2, "Al-Mustafa", "The Chosen"),
                nameResult(NameCatalog.PROPHETS, 3, "Ibrahim", "Abraham"),
            ),
        ).let { it.copy(filteredResults = it.allResults) }
        render()

        composeRule.onNodeWithText(str(R.string.names_tab_allah)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.names_tab_prophet)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.names_tab_prophets)).assertIsDisplayed()
    }

    /** A name with no transliteration still needs a title — the English name stands in. */
    @Test
    fun `a name with no transliteration falls back to its english name`() {
        val result = nameResult(
            catalog = NameCatalog.ASMA_UN_NABI,
            id = 7,
            transliteration = "",
            english = "The Chosen One",
        )
        state.value = SearchUiState(
            query = "chosen",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText("The Chosen One").assertIsDisplayed()
    }

    // ── the filter row ───────────────────────────────────────────────────────

    /** Nothing to scope while the box is empty, so the chips stay out of the way. */
    @Test
    fun `the filter row appears only once there is something to scope`() {
        state.value = SearchUiState(query = "")
        render()
        composeRule.onNodeWithText(str(R.string.all)).assertDoesNotExist()

        state.value = SearchUiState(query = "noor")
        composeRule.waitForIdle()
        composeRule.onNodeWithText(str(R.string.all)).assertIsDisplayed()
    }

    /**
     * The count on a chip says where the matches are *before* you narrow to it — so narrowing to
     * Hadith and finding nothing is never a decision anyone has to make blind. It is counted from
     * `allResults` with the same predicate the list filters by, so the number and the rows agree.
     */
    @Test
    fun `each chip counts the unfiltered matches for its own source`() {
        val all = listOf(
            quranResult(surah = 2, ayah = 153),
            surahResult(number = 18),
            hadithResult(id = "h1", hadithNumber = 1),
            duaResult(id = "d1"),
        )
        state.value = SearchUiState(
            query = "patience",
            selectedFilter = SearchFilter.HADITH,
            allResults = all,
            filteredResults = all.filter { it is com.arshadshah.nimaz.domain.model.UnifiedSearchResult.HadithResult },
        )
        render()

        // A surah hit counts under Qur'an alongside the verse — two, not one.
        composeRule.onNodeWithText("${str(R.string.quran)}  2").assertIsDisplayed()
        composeRule.onNodeWithText("${str(R.string.hadith)}  1").assertIsDisplayed()
        composeRule.onNodeWithText("${str(R.string.duas)}  1").assertIsDisplayed()
        // Zero matches show the bare label rather than a "0".
        composeRule.onNodeWithText(str(R.string.names_title)).assertIsDisplayed()
    }

    @Test
    fun `tapping a chip asks for that filter`() {
        state.value = SearchUiState(query = "noor", allResults = listOf(quranResult()))
        render()

        composeRule.onNodeWithText("${str(R.string.quran)}  1").performClick()

        assertThat(events).contains(SearchEvent.SetFilter(SearchFilter.QURAN))
    }

    /** Opening search *from* duas scopes it to duas — the screen says so on first composition. */
    @Test
    fun `an initial filter scopes the screen as it opens`() {
        render(initialFilter = SearchFilter.DUA)

        assertThat(events).contains(SearchEvent.SetFilter(SearchFilter.DUA))
    }

    @Test
    fun `search opens unscoped when it was not launched from a section`() {
        render()

        assertThat(events.filterIsInstance<SearchEvent.SetFilter>()).isEmpty()
    }

    // ── the resting state ────────────────────────────────────────────────────

    @Test
    fun `recent searches are offered while the box is empty`() {
        state.value = SearchUiState(query = "", recentSearches = listOf("patience", "zakat"))
        render()

        composeRule.onNodeWithText(str(R.string.search_recent)).assertIsDisplayed()
        composeRule.onNodeWithText("patience").assertIsDisplayed()
        composeRule.onNodeWithText("zakat").assertIsDisplayed()
    }

    @Test
    fun `tapping a recent search runs it`() {
        state.value = SearchUiState(query = "", recentSearches = listOf("patience"))
        render()

        composeRule.onNodeWithText("patience").performClick()

        assertThat(events).contains(SearchEvent.SelectRecentSearch("patience"))
    }

    @Test
    fun `a recent search can be removed on its own`() {
        state.value = SearchUiState(query = "", recentSearches = listOf("patience"))
        render()

        composeRule.onNodeWithContentDescription(str(R.string.remove)).performClick()

        assertThat(events).contains(SearchEvent.RemoveRecentSearch("patience"))
        // Removing one row must not be wired to the clear-everything action.
        assertThat(events).doesNotContain(SearchEvent.ClearRecentSearches)
    }

    @Test
    fun `the whole recent list can be cleared`() {
        state.value = SearchUiState(query = "", recentSearches = listOf("patience", "zakat"))
        render()

        composeRule.onNodeWithText(str(R.string.cd_clear)).performClick()

        assertThat(events).contains(SearchEvent.ClearRecentSearches)
    }

    @Test
    fun `no recent header is drawn when there is no history`() {
        state.value = SearchUiState(query = "", recentSearches = emptyList())
        render()

        composeRule.onNodeWithText(str(R.string.search_recent)).assertDoesNotExist()
    }

    // ── the bar itself ───────────────────────────────────────────────────────

    @Test
    fun `typing in the bar updates the query`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.search_placeholder))
            .performTextInput("noor")

        assertThat(events).contains(SearchEvent.UpdateQuery("noor"))
    }

    @Test
    fun `clearing the bar clears the search`() {
        state.value = SearchUiState(query = "noor")
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_clear_search)).performClick()

        assertThat(events).contains(SearchEvent.ClearSearch)
    }

    @Test
    fun `the match count is the count of what is on screen`() {
        val results = listOf(quranResult(), hadithResult())
        state.value = SearchUiState(
            query = "patience",
            allResults = results,
            filteredResults = results,
        )
        stats.value = SearchStatsUiState(totalResults = 2)
        render()

        composeRule.onNodeWithText("2 matches").assertIsDisplayed()
    }

    @Test
    fun `the top bar goes back and opens search settings`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.search_settings)).performClick()

        assertThat(backs).isEqualTo(1)
        assertThat(settingsOpens).isEqualTo(1)
    }

    /**
     * The AI surface is opt-in *and* per-screen: Qur'an, Hadith and Dua search pass
     * `enableAsk = false`, and no amount of "AI is on in settings" may put an Ask affordance on
     * those screens. This is the screen-level half of the consent boundary that
     * `AskWithProofConsentTest` pins in the use case.
     */
    @Test
    fun `a scoped search offers nothing to ask even with the feature switched on`() {
        askState.value = AskUiState(aiEnabled = true)
        state.value = SearchUiState(query = "patience")
        render(enableAsk = false)

        composeRule.onNodeWithText(str(R.string.search_ask_button)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.search_typing_hint)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(str(R.string.search_placeholder)).assertExists()
    }
}
