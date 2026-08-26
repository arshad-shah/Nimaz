package com.arshadshah.nimaz.presentation.screens.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.model.SearchPreferences
import com.arshadshah.nimaz.presentation.viewmodel.settings.SearchSettingsEvent
import com.arshadshah.nimaz.presentation.viewmodel.settings.SearchSettingsUiState
import com.arshadshah.nimaz.presentation.viewmodel.settings.SearchSettingsViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.arshadshah.nimaz.testing.settingsRow
import com.arshadshah.nimaz.testing.tappableAncestorCount
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
 * Search and AI settings — the four local-search knobs, and the opt-in that lets a question leave
 * the device.
 *
 * The consent flow is the part that matters most and the part most easily broken into something
 * that still looks right. "Enable AI answers" must **not** be a plain toggle: switching it on has
 * to open the disclosure sheet and wait for an explicit acceptance, because the whole feature is
 * one where the user's question is sent to a Worker. A toggle that wrote the preference directly
 * would look identical on screen and would opt someone in without ever showing them what is
 * shared.
 *
 * The failure arm is the second half of that, and it is the shipped bug the state field exists
 * for: when the consent write does not commit, the sheet stays up and says so. The alternative —
 * closing over a switch that has quietly stayed off — leaves the user believing they enabled a
 * feature that is not on.
 *
 * The source list has a rule of its own: **the last source on cannot be switched off.** An empty
 * set is a search that returns nothing for every query, and `SearchPreferences.sanitised` reads it
 * straight back as "everything" — so the switch would flip itself back on, which reads as the app
 * ignoring the user.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class SearchSettingsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = MutableStateFlow(SearchSettingsUiState())
    private val events = mutableListOf<SearchSettingsEvent>()
    private val viewModel: SearchSettingsViewModel = mockk(relaxed = true) {
        every { uiState } returns this@SearchSettingsScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<SearchSettingsEvent>() }
    }
    private var backs = 0

    private fun setContent(uiState: SearchSettingsUiState = SearchSettingsUiState()) {
        state.value = uiState
        composeRule.setThemedContent {
            SearchSettingsScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private inline fun <reified T : SearchSettingsEvent> only(): T = events.filterIsInstance<T>()
        .also { check(it.size == 1) { "expected one ${T::class.simpleName}, got $events" } }
        .single()

    // ── Local search ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `the four sections render`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.search_results_section)).assertExists()
        composeRule.onNodeWithText(string(R.string.search_sources_section)).assertExists()
        composeRule.onNodeWithText(string(R.string.ai_answers)).assertExists()
        composeRule.onNodeWithText(string(R.string.ai_privacy)).assertExists()
    }

    @Test
    fun `the results stepper reports the total across the sources that are on`() {
        // The number people actually notice is the total: a search for الله returning exactly
        // 180 results — three sources at a hidden 60 — read as a defect, and the subtitle exists
        // so the cap is legible before it surprises anyone.
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(
                    resultsPerSource = 20,
                    sources = setOf(LibrarySource.QURAN, LibrarySource.HADITH),
                )
            )
        )

        composeRule.onNodeWithText(string(R.string.search_results_per_source_subtitle, 40))
            .assertExists()
    }

    @Test
    fun `the results stepper moves in tens, within its own bounds`() {
        setContent(SearchSettingsUiState(search = SearchPreferences(resultsPerSource = 20)))

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_increase)).onFirst()
            .performClick()

        assertThat(only<SearchSettingsEvent.SetResultsPerSource>().count).isEqualTo(30)
    }

    @Test
    fun `the results stepper will not go below its floor`() {
        // A floor of zero is a search that returns nothing, reached by holding a button.
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(
                    resultsPerSource = SearchPreferences.MIN_RESULTS_PER_SOURCE
                )
            )
        )

        composeRule.onAllNodesWithContentDescription(string(R.string.cd_decrease)).onFirst()
            .performClick()

        assertThat(events).isEmpty()
    }

    @Test
    fun `the strictness row reports the level and its explanation`() {
        setContent(
            SearchSettingsUiState(search = SearchPreferences(strictness = MatchStrictness.EXACT))
        )

        composeRule.onAllNodesWithText(string(R.string.search_strictness_exact)).onFirst()
            .assertExists()
    }

    @Test
    fun `picking a strictness dispatches that level`() {
        setContent(
            SearchSettingsUiState(search = SearchPreferences(strictness = MatchStrictness.BALANCED))
        )

        composeRule.settingsRow(string(R.string.search_strictness)).performClick()
        composeRule.onNodeWithText(string(R.string.search_strictness_broad_desc)).performClick()

        assertThat(only<SearchSettingsEvent.SetStrictness>().strictness)
            .isEqualTo(MatchStrictness.BROAD)
    }

    @Test
    fun `the default scope row says Everything when there is no scope`() {
        setContent(SearchSettingsUiState(search = SearchPreferences(defaultScope = null)))

        composeRule.onAllNodesWithText(string(R.string.search_scope_everything)).onFirst()
            .assertExists()
    }

    @Test
    fun `the scope picker offers only the sources that are actually searched`() {
        // Opening the search screen filtered to a switched-off source shows an empty list that
        // reads as "nothing matched" rather than "you turned this off".
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(sources = setOf(LibrarySource.QURAN))
            )
        )

        // Counted rather than asserted absent: the hadith row below still renders its own
        // description, so the question is whether the *picker* added a second one.
        val hadithBefore = composeRule
            .onAllNodesWithText(string(R.string.search_source_hadith_desc))
            .fetchSemanticsNodes().size

        composeRule.settingsRow(string(R.string.search_default_scope)).performClick()

        composeRule.onNodeWithText(string(R.string.search_scope_everything_desc)).assertExists()
        assertThat(
            composeRule.onAllNodesWithText(string(R.string.search_source_hadith_desc))
                .fetchSemanticsNodes().size
        ).isEqualTo(hadithBefore)
    }

    @Test
    fun `picking Everything clears the scope rather than choosing a source`() {
        // "Everything" is the *absence* of a scope, so it cannot come from `LibrarySource
        // .entries` — the picker models it as a nullable value, and a picker that mapped it to
        // a source would silently filter every search.
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(defaultScope = LibrarySource.QURAN)
            )
        )

        composeRule.settingsRow(string(R.string.search_default_scope)).performClick()
        composeRule.onNodeWithText(string(R.string.search_scope_everything_desc)).performClick()

        assertThat(only<SearchSettingsEvent.SetDefaultScope>().source).isNull()
    }

    // ── Sources ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `every library source is offered`() {
        setContent()

        LibrarySource.entries.forEach { source ->
            val label = when (source) {
                LibrarySource.QURAN -> R.string.quran
                LibrarySource.HADITH -> R.string.hadith
                LibrarySource.DUAS -> R.string.duas
                LibrarySource.NAMES -> R.string.names_title
            }
            composeRule.onAllNodesWithText(string(label)).onFirst().assertExists()
        }
    }

    @Test
    fun `switching a source off dispatches that source`() {
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(sources = LibrarySource.entries.toSet())
            )
        )

        composeRule.settingsRow(string(R.string.search_source_hadith_desc)).performClick()

        assertThat(only<SearchSettingsEvent.ToggleSource>().source)
            .isEqualTo(LibrarySource.HADITH)
    }

    @Test
    fun `the last source left on cannot be switched off, and says why`() {
        // An empty set is a search that returns nothing for every query, and the sanitiser reads
        // it straight back as "everything" — so obeying the tap produces a switch that flips
        // itself back on, which reads as the app ignoring the user.
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(sources = setOf(LibrarySource.QURAN))
            )
        )

        composeRule.onNodeWithText(string(R.string.search_source_last_one)).assertExists()
        assertThat(composeRule.tappableAncestorCount(string(R.string.search_source_last_one)))
            .isEqualTo(0)
    }

    @Test
    fun `a source that is not the last one keeps its ordinary description`() {
        setContent(
            SearchSettingsUiState(
                search = SearchPreferences(
                    sources = setOf(LibrarySource.QURAN, LibrarySource.HADITH)
                )
            )
        )

        composeRule.onNodeWithText(string(R.string.search_source_last_one)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.search_source_quran_desc)).assertExists()
    }

    // ── The AI opt-in ────────────────────────────────────────────────────────────────────────

    @Test
    fun `enabling AI asks rather than writing the preference straight away`() {
        // A plain toggle here would opt someone into sending their question to a Worker without
        // ever showing them the disclosure. The screen must ask, and the ask is a separate event.
        setContent(SearchSettingsUiState(aiEnabled = false))

        composeRule.settingsRow(string(R.string.ai_answers_enable)).performClick()

        assertThat(events).containsExactly(SearchSettingsEvent.ToggleAiRequested)
    }

    @Test
    fun `the consent sheet states what leaves the device before offering to enable`() {
        setContent(SearchSettingsUiState(showConsentSheet = true))

        composeRule.onAllNodesWithText(string(R.string.ai_consent_title)).onFirst().assertExists()
        composeRule.onAllNodesWithText(string(R.string.ai_disclosure_full)).onFirst()
            .assertExists()
    }

    @Test
    fun `accepting the consent is an explicit act`() {
        setContent(SearchSettingsUiState(showConsentSheet = true))

        composeRule.onNodeWithText(string(R.string.ai_consent_enable)).performClick()

        assertThat(events).containsExactly(SearchSettingsEvent.ConsentAccepted)
    }

    @Test
    fun `cancelling the consent sheet does not enable anything`() {
        setContent(SearchSettingsUiState(showConsentSheet = true))

        composeRule.onAllNodesWithText(string(R.string.cancel)).onFirst().performClick()

        assertThat(events).containsExactly(SearchSettingsEvent.ConsentDismissed)
    }

    @Test
    fun `a consent write that failed says so instead of closing over a switch that stayed off`() {
        // The shipped bug this field exists for: the sheet closed, the switch went back to off,
        // and nothing said why.
        setContent(SearchSettingsUiState(showConsentSheet = true, consentFailed = true))

        composeRule.onAllNodesWithText(string(R.string.ai_consent_save_failed)).onFirst()
            .assertExists()
        composeRule.onNodeWithText(string(R.string.ai_consent_enable)).assertExists()
    }

    @Test
    fun `no failure message is shown on a first, clean consent`() {
        setContent(SearchSettingsUiState(showConsentSheet = true, consentFailed = false))

        composeRule.onNodeWithText(string(R.string.ai_consent_save_failed)).assertDoesNotExist()
    }

    // ── Privacy and history ──────────────────────────────────────────────────────────────────

    @Test
    fun `the history toggle passes what the switch reports`() {
        setContent(SearchSettingsUiState(historyEnabled = false))

        composeRule.settingsRow(string(R.string.ai_history)).performClick()

        assertThat(only<SearchSettingsEvent.SetHistoryEnabled>().enabled).isTrue()
    }

    @Test
    fun `clear history is off-limits while there is nothing to clear`() {
        // Asserted by what the tap does rather than by the row's semantics: a confirmation
        // listing nothing is a dialog with no purpose, and the row is disabled to avoid it.
        setContent(SearchSettingsUiState(savedQuestions = emptyList()))

        composeRule.settingsRow(string(R.string.ai_clear_history)).performClick()

        composeRule.onNodeWithText(string(R.string.ai_clear_history_dialog_title))
            .assertDoesNotExist()
        assertThat(events).isEmpty()
    }

    @Test
    fun `clearing history lists what is about to go, and asks first`() {
        // A destructive action whose scope is invisible is one people avoid; naming the
        // questions is what makes the confirmation meaningful.
        setContent(SearchSettingsUiState(savedQuestions = listOf("What is zakat?")))

        composeRule.settingsRow(string(R.string.ai_clear_history)).performClick()

        composeRule.onNodeWithText(string(R.string.ai_clear_history_dialog_title)).assertExists()
        composeRule.onNodeWithText("•  What is zakat?").assertExists()
        assertThat(events).isEmpty()
    }

    @Test
    fun `confirming the clear dispatches it`() {
        setContent(SearchSettingsUiState(savedQuestions = listOf("What is zakat?")))

        composeRule.settingsRow(string(R.string.ai_clear_history)).performClick()
        composeRule.onNodeWithText(string(R.string.delete)).performClick()

        assertThat(events).containsExactly(SearchSettingsEvent.ClearHistory)
    }

    @Test
    fun `cancelling the clear dispatches nothing`() {
        setContent(SearchSettingsUiState(savedQuestions = listOf("What is zakat?")))

        composeRule.settingsRow(string(R.string.ai_clear_history)).performClick()
        composeRule.onAllNodesWithText(string(R.string.cancel)).onFirst().performClick()

        assertThat(events).isEmpty()
    }

    @Test
    fun `the disclosure is readable from the screen without enabling anything`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.ai_what_gets_shared)).performClick()

        composeRule.onAllNodesWithText(string(R.string.ai_disclosure_full)).onFirst()
            .assertExists()
        assertThat(events).isEmpty()
    }

    @Test
    fun `the title is Search and AI, and the back button navigates back`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.search_settings)).onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
