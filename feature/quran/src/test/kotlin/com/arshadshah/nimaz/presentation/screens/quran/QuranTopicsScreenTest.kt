package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicBrowseState
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
 * The subject browser: one tree that expands in place.
 *
 * Descending used to replace the list with the children, so walking from "Stories" to "Musa" to
 * "the parting of the sea" discarded every sibling on the way. What is asserted here is the
 * shape that replaced it — a node's children appear beneath it, the row knows it is a branch
 * *before* it is tapped (a leaf offering a disclosure control is a dead tap), and past the indent
 * cap the offer changes from "open" to "re-root here" because a 390dp screen has no text column
 * left after four levels.
 *
 * And the two empty states that are not the same sentence: "your content predates the subject
 * index" and "nothing matches that".
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranTopicsScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val browseState = MutableStateFlow(TopicBrowseState())
    private val events = mutableListOf<QuranTopicsEvent>()
    private var openedTopic: Pair<Int, TopicTree>? = null

    private val viewModel: QuranTopicsViewModel = mockk(relaxed = true) {
        every { browseState } returns this@QuranTopicsScreenTest.browseState
        every { onEvent(any()) } answers { events += firstArg<QuranTopicsEvent>() }
    }

    private fun render() {
        composeRule.setThemedContent {
            QuranTopicsScreen(
                onNavigateBack = {},
                onOpenTopic = { id, tree -> openedTopic = id to tree },
                viewModel = viewModel,
            )
        }
    }

    private fun topic(id: Int, name: String, ayahs: Int = 12, parent: Int? = null) = QuranTopic(
        id = id,
        name = name,
        arabicName = "موضوع",
        description = "",
        wikiLink = "",
        ayahCount = ayahs,
        parentId = parent,
        thematicParentId = parent,
        ontologyParentId = parent,
        isThematic = true,
        isOntology = true,
        relatedTopicIds = emptyList(),
    )

    private val stories = topic(1, "Stories")
    private val musa = topic(2, "Musa", parent = 1)
    private val loaded = TopicBrowseState(
        isLoading = false,
        level = listOf(stories, topic(9, "Prayer")),
        branchIds = setOf(1),
        children = mapOf(1 to listOf(musa)),
        rolledUpCounts = mapOf(1 to 340, 9 to 88),
    )

    // ---- Asking for the level ----

    @Test
    fun `arriving asks for the current level`() {
        render()

        // Idempotent by design, so the screen can send it on every composition.
        assertThat(events).contains(QuranTopicsEvent.OpenBrowser)
    }

    // ---- The tree ----

    @Test
    fun `the top level is listed`() {
        browseState.value = loaded

        render()

        composeRule.onNodeWithText("Stories").assertIsDisplayed()
        composeRule.onNodeWithText("Prayer").assertIsDisplayed()
    }

    @Test
    fun `a branch's children appear beneath it, with its siblings still there`() {
        browseState.value = loaded.copy(expanded = setOf(1))

        render()

        composeRule.onNodeWithText("Musa").assertIsDisplayed()
        // The whole point of expanding in place: the context on the way down is not discarded.
        composeRule.onNodeWithText("Prayer").assertIsDisplayed()
    }

    @Test
    fun `a row carries the count of everything beneath it, not its own citations`() {
        browseState.value = loaded

        render()

        // A branch's own citation count is usually zero, so the browser used to open on three
        // roots each reading "0 verses".
        composeRule.onNodeWithText("340", substring = true).assertIsDisplayed()
    }

    @Test
    fun `opening a subject is the caller's business, and carries the tree it came from`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText("Prayer").performClick()

        assertThat(openedTopic).isEqualTo(9 to TopicTree.THEMATIC)
    }

    // ---- Focus and crumbs ----

    @Test
    fun `a focused branch is shown as crumbs above the list`() {
        browseState.value = loaded.copy(focus = listOf(stories, musa))

        render()

        composeRule.onNodeWithText("Musa").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_topics_crumb_home)).assertIsDisplayed()
    }

    @Test
    fun `tapping the home crumb re-roots on the whole tree`() {
        browseState.value = loaded.copy(focus = listOf(stories))
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_crumb_home)).performClick()

        assertThat(events).contains(QuranTopicsEvent.RebaseTo(QuranTopicsEvent.RebaseTo.ROOT))
    }

    // ---- Search ----

    @Test
    fun `typing hands the query to the view model`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_search_hint)).performTextInput("musa")

        assertThat(events.filterIsInstance<QuranTopicsEvent.Search>().map { it.query })
            .contains("musa")
    }

    @Test
    fun `a search that matches nothing says so, naming what was searched for`() {
        browseState.value = loaded.copy(
            searchQuery = "zzz",
            searchResults = emptyList(),
            isSearching = false,
        )

        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_no_results_title)).assertIsDisplayed()
    }

    @Test
    fun `results replace the tree while a query stands`() {
        browseState.value = loaded.copy(
            searchQuery = "musa",
            searchResults = listOf(musa),
            searchPaths = mapOf(2 to listOf(stories)),
        )

        render()

        composeRule.onNodeWithText("Musa").assertIsDisplayed()
    }

    // ---- The state that is not an error ----

    @Test
    fun `an install whose content predates the index is told so, not shown an empty tree`() {
        browseState.value = TopicBrowseState(isLoading = false, isAvailable = false)

        render()

        // The migration runs before the artifact that fills the tables arrives. That is an
        // explainable state, and "no subjects" is the wrong sentence for it.
        composeRule.onNodeWithText(str(R.string.quran_topics_unavailable_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_topics_search_hint)).assertDoesNotExist()
    }

    @Test
    fun `a first load shows neither the tree nor an empty state`() {
        browseState.value = TopicBrowseState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_no_results_title)).assertDoesNotExist()
    }

    @Test
    fun `going back is the caller's business`() {
        var back = false
        browseState.value = loaded
        composeRule.setThemedContent {
            QuranTopicsScreen(
                onNavigateBack = { back = true },
                onOpenTopic = { _, _ -> },
                viewModel = viewModel,
            )
        }

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(back).isTrue()
    }
}
