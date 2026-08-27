package com.arshadshah.nimaz.presentation.screens.help

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HelpSearchResult
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpViewModel
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
 * Help's front door: browse, search, or write to a person.
 *
 * The state machine here is the part worth pinning, because three of its states render a list
 * with nothing useful in it and the ViewModel cannot tell them apart. Searching-with-no-hits,
 * a failed topic load and an empty catalogue are the same `HelpHomeUiState` shape with different
 * flags, and which one the reader sees is decided entirely by the order of the branches in this
 * screen. Two of the orderings are wrong in a way nothing else catches: showing the topic grid
 * while a search is running makes the search look broken, and showing "no results" in place of a
 * failed load tells someone their help catalogue is empty when in fact it never loaded.
 *
 * The failure is rendered as a **section** inside the list rather than over it, and that is
 * deliberate: search reads a different code path from the topic load, so a reader whose grid
 * failed can still find a topic by name. A full-screen error would take the search bar with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HelpScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val homeState = MutableStateFlow(HelpHomeUiState())
    private val events = mutableListOf<HelpEvent>()

    private val viewModel: HelpViewModel = mockk(relaxed = true) {
        every { this@mockk.homeState } returns this@HelpScreenTest.homeState
        every { onEvent(any()) } answers { events += firstArg<HelpEvent>() }
    }

    private val topicsOpened = mutableListOf<String>()
    private var contacts = 0

    private fun setContent() {
        composeRule.setThemedContent {
            HelpScreen(
                onNavigateBack = {},
                onNavigateToTopic = { topicsOpened += it },
                onContact = { contacts++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** `NimazSectionTitle` uppercases by default, so a heading is matched as it renders. */
    private fun sectionTitle(@StringRes res: Int): String = string(res).uppercase()

    private fun topic(id: String, title: String, subtitle: String = "") = HelpTopic(
        id = id,
        iconKey = "schedule",
        colorKey = "indigo",
        title = title,
        subtitle = subtitle,
        order = 0,
        itemCount = 3,
    )

    @Test
    fun `the topic grid is what an idle Help screen shows`() {
        homeState.value = HelpHomeUiState(
            topics = listOf(
                topic("prayer", "Prayer times", "When and why"),
                topic("quran", "Reading the Qur'an"),
                topic("widgets", "Widgets"),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(sectionTitle(R.string.help_browse_topics)).assertExists()
        composeRule.onNodeWithText("Prayer times").assertExists()
        composeRule.onNodeWithText("When and why").assertExists()
        // An odd count still lays out — the grid chunks by two and pads the last row.
        composeRule.onNodeWithText("Widgets").assertExists()
        composeRule.onNodeWithText(string(R.string.help_still_need)).assertExists()
    }

    @Test
    fun `a topic tile opens that topic`() {
        homeState.value = HelpHomeUiState(
            topics = listOf(topic("prayer", "Prayer times"), topic("quran", "Qur'an")),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Qur'an").performClick()

        // The id, not the title: a tile handing over its label opens nothing.
        assertThat(topicsOpened).containsExactly("quran")
    }

    @Test
    fun `typing asks the ViewModel to search`() {
        homeState.value = HelpHomeUiState(topics = listOf(topic("p", "Prayer")), isLoading = false)
        setContent()

        // The bar carries its placeholder as a content description — the field itself has no
        // text until something is typed into it.
        composeRule.onNodeWithContentDescription(string(R.string.help_search_hint))
            .performTextInput("qibla")

        assertThat(events).contains(HelpEvent.Search("qibla"))
    }

    @Test
    fun `results replace the grid while a search is running`() {
        homeState.value = HelpHomeUiState(
            topics = listOf(topic("prayer", "Prayer times")),
            query = "qibla",
            isSearching = true,
            results = listOf(
                HelpSearchResult(
                    topicId = "qibla",
                    itemId = "q1",
                    isGuide = false,
                    title = "Why is the qibla arrow spinning?",
                    snippet = "Calibrate the compass",
                ),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Why is the qibla arrow spinning?").assertExists()
        // The grid is gone: leaving it under the results makes the search look like it did
        // nothing at all.
        composeRule.onNodeWithText(sectionTitle(R.string.help_browse_topics)).assertDoesNotExist()
        composeRule.onNodeWithText("Prayer times").assertDoesNotExist()
    }

    @Test
    fun `a result opens the topic it belongs to`() {
        homeState.value = HelpHomeUiState(
            query = "qibla",
            isSearching = true,
            results = listOf(
                HelpSearchResult("qibla", "q1", isGuide = false, title = "Spinning arrow", snippet = ""),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Spinning arrow").performClick()

        assertThat(topicsOpened).containsExactly("qibla")
    }

    @Test
    fun `a search with no hits says so rather than showing an empty screen`() {
        homeState.value = HelpHomeUiState(
            topics = listOf(topic("prayer", "Prayer times")),
            query = "xyzzy",
            isSearching = true,
            results = emptyList(),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_no_results)).assertExists()
    }

    @Test
    fun `a failed topic load keeps the search bar reachable`() {
        homeState.value = HelpHomeUiState(
            isLoading = false,
            error = UiError(message = R.string.help_topics_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_topics_load_failed)).assertExists()
        // SECTION, not FULLSCREEN: the search path is a different query and may well work.
        composeRule.onNodeWithContentDescription(string(R.string.help_search_hint)).assertExists()
    }

    @Test
    fun `the failed load offers a retry that reaches the ViewModel`() {
        homeState.value = HelpHomeUiState(
            isLoading = false,
            error = UiError(message = R.string.help_topics_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).contains(HelpEvent.Retry)
    }

    @Test
    fun `an error is preferred over the topic grid but not over search results`() {
        // Both flags set at once is a real state — a search running while the grid's own load
        // failed — and the results are what the reader asked for.
        homeState.value = HelpHomeUiState(
            query = "qibla",
            isSearching = true,
            results = listOf(
                HelpSearchResult("qibla", null, isGuide = false, title = "Qibla", snippet = ""),
            ),
            isLoading = false,
            error = UiError(message = R.string.help_topics_load_failed),
        )
        setContent()

        composeRule.onNodeWithText("Qibla").assertExists()
        composeRule.onNodeWithText(string(R.string.help_topics_load_failed)).assertDoesNotExist()
    }

    @Test
    fun `the contact card writes to support`() {
        homeState.value = HelpHomeUiState(topics = listOf(topic("p", "Prayer")), isLoading = false)
        setContent()

        composeRule.onNodeWithText(string(R.string.help_still_need)).performClick()

        assertThat(contacts).isEqualTo(1)
    }
}
