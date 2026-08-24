package com.arshadshah.nimaz.presentation.screens.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.ContentTarget
import com.arshadshah.nimaz.domain.model.NameCatalog
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskEvent
import com.arshadshah.nimaz.presentation.viewmodel.ai.AskPhase
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
 * "Ask with Proof" as the user meets it — the consent boundary first, then the answer.
 *
 * The feature is off until someone turns it on, and until #607 that promise was pinned in two
 * places that a user never sees: `AiOptInDefaultsTest` on the stored default and
 * `AskWithProofConsentTest` on the use case. Neither says anything about the screen, and the
 * screen is where consent is actually offered or bypassed. A question that reaches the Worker is
 * a question that left the device — the one thing this feature promises will not happen without
 * an explicit opt-in — so "the Ask affordance is not rendered while the feature is off" is a
 * privacy assertion, not a layout one.
 *
 * The answer half is about not overstating what the AI produced: the cited proofs are real local
 * records, they carry a "Cited" mark, and a cited record must not also appear as an ordinary
 * keyword hit — the same verse twice reads as two independent sources agreeing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SearchScreenAskTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(SearchUiState())
    private val stats = MutableStateFlow(SearchStatsUiState())
    private val askState = MutableStateFlow(AskUiState())

    private val events = mutableListOf<SearchEvent>()
    private val askEvents = mutableListOf<AskEvent>()
    private val proofOpens = mutableListOf<ContentTarget>()
    private var settingsOpens = 0

    private val viewModel: SearchViewModel = mockk(relaxed = true) {
        every { searchState } returns this@SearchScreenAskTest.state
        every { statsState } returns this@SearchScreenAskTest.stats
        every { onEvent(any()) } answers { events += firstArg<SearchEvent>() }
    }

    private val askViewModel: AskViewModel = mockk(relaxed = true) {
        every { uiState } returns this@SearchScreenAskTest.askState
        every { onEvent(any()) } answers { askEvents += firstArg<AskEvent>() }
    }

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun render(enableAsk: Boolean = true) {
        composeRule.setThemedContent {
            SearchScreen(
                onNavigateBack = {},
                onNavigateToQuranAyah = { _, _ -> },
                onNavigateToSurah = {},
                onNavigateToHadith = { _, _ -> },
                onNavigateToDua = {},
                onNavigateToName = { _: NameCatalog, _: Int -> },
                enableAsk = enableAsk,
                onNavigateToSearchSettings = { settingsOpens++ },
                onNavigateToProof = { proofOpens += it },
                viewModel = viewModel,
                askViewModel = askViewModel,
            )
        }
        composeRule.waitForIdle()
    }

    // ── consent ──────────────────────────────────────────────────────────────

    /**
     * The whole promise, on the surface that could break it: with the feature off there is no
     * control that sends a question anywhere. Not disabled — absent.
     */
    @Test
    fun `there is no way to ask a question until the feature is turned on`() {
        askState.value = AskUiState(aiEnabled = false)
        state.value = SearchUiState(query = "What does the Quran say about patience?")
        render()

        composeRule.onNodeWithText(str(R.string.search_ask_button)).assertDoesNotExist()
        assertThat(askEvents.filterIsInstance<AskEvent.Submit>()).isEmpty()
    }

    /** The bar says what it can do, and while AI is off it can only search. */
    @Test
    fun `the bar promises an answer only once answers are switched on`() {
        askState.value = AskUiState(aiEnabled = false)
        render()
        composeRule.onNodeWithContentDescription(str(R.string.search_placeholder)).assertExists()

        askState.value = AskUiState(aiEnabled = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(str(R.string.search_or_ask_placeholder))
            .assertExists()
    }

    /**
     * The discovery card is the opt-in offer, and what it says is the consent copy: only the
     * question leaves the device, everything else is matched locally. If that sentence ever
     * stops matching what the feature does, this is the test that has to change with it.
     */
    @Test
    fun `the opt-in offer states what leaves the device`() {
        askState.value = AskUiState(aiEnabled = false, hintDismissed = false)
        state.value = SearchUiState(query = "")
        render()

        composeRule.onNodeWithText(str(R.string.ai_discover_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_discover_privacy)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_discover_enable)).assertIsDisplayed()
    }

    /** Turning it on is a trip to settings — the card itself never flips the switch. */
    @Test
    fun `enabling from the offer goes to settings rather than switching it on here`() {
        askState.value = AskUiState(aiEnabled = false)
        render()

        composeRule.onNodeWithText(str(R.string.ai_discover_enable)).performClick()

        assertThat(settingsOpens).isEqualTo(1)
        assertThat(askEvents).isEmpty()
    }

    @Test
    fun `declining the offer remembers the decision`() {
        askState.value = AskUiState(aiEnabled = false)
        render()

        composeRule.onNodeWithText(str(R.string.ai_discover_dismiss)).performClick()

        assertThat(askEvents).contains(AskEvent.DismissHint)
    }

    @Test
    fun `the offer is not made again once it has been declined`() {
        askState.value = AskUiState(aiEnabled = false, hintDismissed = true)
        render()

        composeRule.onNodeWithText(str(R.string.ai_discover_title)).assertDoesNotExist()
    }

    /** It is a resting-state offer: mid-search it would be in the way of the results. */
    @Test
    fun `the offer stays out of the way while a search is being typed`() {
        askState.value = AskUiState(aiEnabled = false)
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText(str(R.string.ai_discover_title)).assertDoesNotExist()
    }

    @Test
    fun `the offer is never made on a scoped search that has no ask affordance`() {
        askState.value = AskUiState(aiEnabled = false)
        render(enableAsk = false)

        composeRule.onNodeWithText(str(R.string.ai_discover_title)).assertDoesNotExist()
    }

    // ── asking ───────────────────────────────────────────────────────────────

    @Test
    fun `with the feature on, the ask pill submits the typed question`() {
        askState.value = AskUiState(aiEnabled = true)
        state.value = SearchUiState(query = "What does the Quran say about patience?")
        render()

        composeRule.onNodeWithText(str(R.string.search_ask_button)).performClick()

        assertThat(askEvents).contains(AskEvent.Submit)
    }

    /**
     * One bar drives two things, so what is typed has to reach the question as well as the
     * search — otherwise Ask sends whatever was last submitted rather than what is on screen.
     */
    @Test
    fun `typing feeds the search and the question from one bar`() {
        askState.value = AskUiState(aiEnabled = true)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.search_or_ask_placeholder))
            .performTextInput("patience")

        assertThat(events).contains(SearchEvent.UpdateQuery("patience"))
        assertThat(askEvents).contains(AskEvent.UpdateQuestion("patience"))
    }

    /** Emptying the bar takes the answer with it — a stale answer under an empty box is a lie. */
    @Test
    fun `clearing the bar clears the answer too`() {
        askState.value = AskUiState(aiEnabled = true)
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_clear_search)).performClick()

        assertThat(events).contains(SearchEvent.ClearSearch)
        assertThat(askEvents).contains(AskEvent.Clear)
    }

    @Test
    fun `an example question is asked, not just typed into the box`() {
        askState.value = AskUiState(aiEnabled = true)
        state.value = SearchUiState(query = "")
        render()

        composeRule.onNodeWithText(str(R.string.ask_example_2)).performClick()

        assertThat(events).contains(SearchEvent.UpdateQuery(str(R.string.ask_example_2)))
        assertThat(askEvents).contains(AskEvent.SelectRecent(str(R.string.ask_example_2)))
    }

    @Test
    fun `examples are only offered where questions can be asked`() {
        askState.value = AskUiState(aiEnabled = false)
        state.value = SearchUiState(query = "")
        render()

        composeRule.onNodeWithText(str(R.string.search_try_asking)).assertDoesNotExist()
    }

    /** Past questions and past searches are one history to the person who typed them. */
    @Test
    fun `recent searches and recent questions are offered as one list`() {
        askState.value = AskUiState(aiEnabled = true, recentQuestions = listOf("Why fast Ashura?"))
        state.value = SearchUiState(query = "", recentSearches = listOf("zakat nisab"))
        render()

        composeRule.onNodeWithText("zakat nisab").assertIsDisplayed()
        composeRule.onNodeWithText("Why fast Ashura?").assertIsDisplayed()
    }

    @Test
    fun `re-asking from history puts the question back in the bar`() {
        askState.value = AskUiState(aiEnabled = true, recentQuestions = listOf("Why fast Ashura?"))
        state.value = SearchUiState(query = "")
        render()

        composeRule.onNodeWithText("Why fast Ashura?").performClick()

        assertThat(events).contains(SearchEvent.SelectRecentSearch("Why fast Ashura?"))
        assertThat(askEvents).contains(AskEvent.UpdateQuestion("Why fast Ashura?"))
    }

    // ── the phases ───────────────────────────────────────────────────────────

    @Test
    fun `a question in flight says so`() {
        // The thinking card pulses and its progress bar is indeterminate — both animate
        // forever, so the clock is manual here or `waitForIdle()` never returns.
        composeRule.mainClock.autoAdvance = false
        askState.value = AskUiState(aiEnabled = true, phase = AskPhase.Loading)
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText(str(R.string.ai_thinking)).assertIsDisplayed()
    }

    /** The trust note is not decoration: it is the only thing on screen saying this is not a fatwa. */
    @Test
    fun `an answer carries its confidence and the note that it is not a ruling`() {
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                answer = "Patience is repeatedly commanded.",
                confidence = AnswerConfidence.MEDIUM,
                proofs = emptyList(),
            ),
        )
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText("Patience is repeatedly commanded.").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_confidence_medium)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_trust_note)).assertIsDisplayed()
    }

    @Test
    fun `cited sources are marked as cited and open the record they cite`() {
        val proof = quranProof(surah = 2, ayah = 153, surahName = "Al-Baqarah")
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Be patient.", AnswerConfidence.HIGH, listOf(proof)),
        )
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText(str(R.string.search_cited_chip)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.surah_result_format, 2, 153)).performClick()

        assertThat(proofOpens).containsExactly(ContentTarget.Ayah(2, 153))
    }

    @Test
    fun `a cited hadith opens the hadith record`() {
        val proof = hadithProof(id = "muslim-2999", hadithNumber = 2999)
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Wondrous.", AnswerConfidence.LOW, listOf(proof)),
        )
        state.value = SearchUiState(query = "believer")
        render()

        composeRule.onNodeWithText(str(R.string.hadith_result_format, 2999)).performClick()

        assertThat(proofOpens).containsExactly(ContentTarget.Hadith("muslim-2999"))
    }

    /**
     * A verse the AI cited and the keyword search also matched is one verse. Rendering it twice
     * would read as two sources agreeing, which is exactly the impression this feature must not
     * manufacture.
     */
    @Test
    fun `a cited verse is not repeated as an ordinary result`() {
        val proof = quranProof(surah = 2, ayah = 153)
        val sameVerse = quranResult(surah = 2, ayah = 153, surahName = "Al-Baqarah")
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Be patient.", AnswerConfidence.HIGH, listOf(proof)),
        )
        state.value = SearchUiState(
            query = "patience",
            allResults = listOf(sameVerse),
            filteredResults = listOf(sameVerse),
        )
        render()

        // One row for Surah 2:153, and it is the cited one.
        composeRule.onAllNodes(hasText(str(R.string.surah_result_format, 2, 153)))
            .assertCountEquals(1)
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 1, 1))
            .assertIsDisplayed()
    }

    /** A surah hit has no citation form, so it can never dedup away against a cited verse. */
    @Test
    fun `a surah result survives beside the cited verses`() {
        val proof = quranProof(surah = 18, ayah = 10)
        val surah = surahResult(number = 18, nameEnglish = "The Cave")
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("The Cave.", AnswerConfidence.HIGH, listOf(proof)),
        )
        state.value = SearchUiState(
            query = "cave",
            allResults = listOf(surah),
            filteredResults = listOf(surah),
        )
        render()

        composeRule.onNodeWithText("The Cave").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 2, 1))
            .assertIsDisplayed()
    }

    /**
     * The AI cites verses, hadiths and duas — never names. Narrowing to Names therefore hides the
     * cited strip rather than filtering it down to a list of nothing.
     */
    @Test
    fun `narrowing to names hides the cited sources rather than emptying them`() {
        val proof = quranProof(surah = 2, ayah = 153)
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Be patient.", AnswerConfidence.HIGH, listOf(proof)),
        )
        state.value = SearchUiState(query = "patience", selectedFilter = SearchFilter.NAMES)
        render()

        composeRule.onNodeWithText(str(R.string.search_cited_chip)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 0, 0))
            .assertIsDisplayed()
    }

    @Test
    fun `narrowing to hadith keeps only the cited hadiths`() {
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                "Both.",
                AnswerConfidence.HIGH,
                listOf(quranProof(surah = 2, ayah = 153), hadithProof(hadithNumber = 2999)),
            ),
        )
        state.value = SearchUiState(query = "patience", selectedFilter = SearchFilter.HADITH)
        render()

        composeRule.onNodeWithText(str(R.string.hadith_result_format, 2999)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.surah_result_format, 2, 153)).assertDoesNotExist()
    }

    @Test
    fun `narrowing to the quran keeps only the cited verses`() {
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                "Both.",
                AnswerConfidence.HIGH,
                listOf(quranProof(surah = 2, ayah = 153), hadithProof(hadithNumber = 2999)),
            ),
        )
        state.value = SearchUiState(query = "patience", selectedFilter = SearchFilter.QURAN)
        render()

        composeRule.onNodeWithText(str(R.string.surah_result_format, 2, 153)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.hadith_result_format, 2999)).assertDoesNotExist()
    }

    /**
     * The AI has no dua citations yet, so narrowing to Duas empties the cited strip the same way
     * Names does — the filter is applied to the proofs rather than ignored for them.
     */
    @Test
    fun `narrowing to duas keeps no verse or hadith citation`() {
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                "Both.",
                AnswerConfidence.HIGH,
                listOf(quranProof(surah = 2, ayah = 153), hadithProof(hadithNumber = 2999)),
            ),
        )
        state.value = SearchUiState(query = "patience", selectedFilter = SearchFilter.DUA)
        render()

        composeRule.onNodeWithText(str(R.string.search_cited_chip)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 0, 0))
            .assertIsDisplayed()
    }

    /** A name has no citation form either, so it stands beside the cited rows rather than merging. */
    @Test
    fun `a name result stands beside the cited sources`() {
        val name = nameResult(id = 1, transliteration = "As-Sabur", english = "The Patient")
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                "Be patient.",
                AnswerConfidence.HIGH,
                listOf(quranProof(surah = 2, ayah = 153)),
            ),
        )
        state.value = SearchUiState(
            query = "patience",
            allResults = listOf(name),
            filteredResults = listOf(name),
        )
        render()

        composeRule.onNodeWithText("As-Sabur").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 2, 1))
            .assertIsDisplayed()
    }

    /**
     * An answer stays on screen when the box is emptied by the AI-terms lookup replacing what was
     * typed. The rows then have nothing to highlight, and highlighting against an empty string is
     * not merely pointless — `indexOf("")` matches at every position, so the loop that builds the
     * highlight never terminates. The guard is the reason this state renders at all.
     */
    @Test
    fun `results under an answer render without a query to highlight`() {
        val proof = quranProof(surah = 2, ayah = 153)
        val related = hadithResult(id = "h1", hadithNumber = 1)
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Be patient.", AnswerConfidence.HIGH, listOf(proof)),
        )
        state.value = SearchUiState(
            query = "",
            allResults = listOf(related),
            filteredResults = listOf(related),
        )
        render()

        composeRule.onNodeWithText(str(R.string.surah_result_format, 2, 153)).assertIsDisplayed()
        composeRule.onNodeWithText("Actions are judged by intentions").assertIsDisplayed()
    }

    /** With the cited rows shown and the related lookup still running, the list says it is working. */
    @Test
    fun `an answer with no related results yet shows the lookup in progress`() {
        // Ends on a spinner, which animates forever — manual clock.
        composeRule.mainClock.autoAdvance = false
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer(
                "Be patient.",
                AnswerConfidence.HIGH,
                listOf(quranProof(surah = 2, ayah = 153)),
            ),
        )
        state.value = SearchUiState(query = "patience", isSearching = true)
        render()

        // The cited row is already readable — the pending lookup is only the related half.
        composeRule.onNodeWithText(str(R.string.surah_result_format, 2, 153)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 1, 1))
            .assertDoesNotExist()
    }

    /** The keyword list the reader was already looking at stays put while the AI list lands. */
    @Test
    fun `the answer's results list does not blank while the related lookup runs`() {
        val existing = hadithResult(id = "h1", hadithNumber = 1)
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Answer("Answer.", AnswerConfidence.HIGH, emptyList()),
        )
        state.value = SearchUiState(
            query = "intentions",
            isSearching = true,
            allResults = listOf(existing),
            filteredResults = listOf(existing),
        )
        render()

        composeRule.onNodeWithText(str(R.string.hadith_result_format, 1)).assertIsDisplayed()
        // The count line is held back while the list is still settling.
        composeRule.onNodeWithText(str(R.string.search_answer_count_format, 1, 0))
            .assertDoesNotExist()
    }

    // ── failures ─────────────────────────────────────────────────────────────

    @Test
    fun `a network failure offers a retry that asks again`() {
        askState.value = AskUiState(aiEnabled = true, phase = AskPhase.Error(AiError.Network))
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText(str(R.string.ai_error_network_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).performClick()

        assertThat(askEvents).contains(AskEvent.Submit)
    }

    /**
     * A daily cap is not a failure to retry into — a retry button there is an invitation to burn
     * taps against a limit that only time lifts.
     */
    @Test
    fun `a usage cap explains the pause and offers no retry`() {
        askState.value = AskUiState(
            aiEnabled = true,
            phase = AskPhase.Error(AiError.RateLimited(retryAfterSeconds = null)),
        )
        state.value = SearchUiState(query = "patience")
        render()

        composeRule.onNodeWithText(str(R.string.ai_error_rate_limited_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.ai_try_again)).assertDoesNotExist()
    }

    /**
     * Keyword results carry on under a failed ask: the library is local and searching it never
     * needed the network in the first place.
     */
    @Test
    fun `keyword results stay on screen under a failed ask`() {
        val result = duaResult(id = "d1", title = "Dua for anxiety")
        askState.value = AskUiState(aiEnabled = true, phase = AskPhase.Error(AiError.Network))
        state.value = SearchUiState(
            query = "anxiety",
            allResults = listOf(result),
            filteredResults = listOf(result),
        )
        render()

        composeRule.onNodeWithText("Dua for anxiety").assertIsDisplayed()
    }

    // ── the AI's related terms drive the list ────────────────────────────────

    @Test
    fun `the answer's related terms re-run the local search`() {
        askState.value = AskUiState(aiEnabled = true)
        render()

        askState.value = askState.value.copy(relatedTerms = listOf("patience", "sabr"))
        composeRule.waitForIdle()

        assertThat(events).contains(SearchEvent.ApplyAiTerms(listOf("patience", "sabr")))
    }

    /** A scoped search never asked, so nothing may drive its list from an AI answer. */
    @Test
    fun `related terms are ignored on a search with no ask affordance`() {
        askState.value = AskUiState(aiEnabled = true, relatedTerms = listOf("patience"))
        render(enableAsk = false)

        assertThat(events.filterIsInstance<SearchEvent.ApplyAiTerms>()).isEmpty()
    }

    @Test
    fun `the typing hint appears only before a question has been asked`() {
        // Ends on the thinking card, which animates forever — manual clock, as above.
        composeRule.mainClock.autoAdvance = false
        askState.value = AskUiState(aiEnabled = true, phase = AskPhase.Idle)
        state.value = SearchUiState(query = "patience")
        render()
        composeRule.onNodeWithText(str(R.string.search_typing_hint)).assertIsDisplayed()

        askState.value = askState.value.copy(phase = AskPhase.Loading)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(str(R.string.search_typing_hint)).assertDoesNotExist()
    }
}
