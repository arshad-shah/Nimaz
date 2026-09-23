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
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicIndexSort
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicSearchHit
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicTally
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicThemeCard
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
 * The Topics front page: each hierarchy in its own shape, and every tap opening a subject.
 *
 * What is asserted is what the redesign changed. Themes are chapter cards whose chips open
 * their branches and whose header opens the chapter; Kinds are tiles; Index is a concordance
 * with a letter rail and a most-cited view. Every tap hands the caller a subject *and the
 * hierarchy it was reached through*, because the same subject has a different place in each.
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

    private fun render(onBack: () -> Unit = {}) {
        composeRule.setThemedContent {
            QuranTopicsScreen(
                onNavigateBack = onBack,
                onOpenTopic = { id, tree -> openedTopic = id to tree },
                viewModel = viewModel,
            )
        }
    }

    private fun topic(id: Int, name: String, arabic: String = "") = QuranTopic(
        id = id,
        name = name,
        arabicName = arabic,
        description = "",
        wikiLink = "",
        ayahCount = 0,
        parentId = null,
        thematicParentId = null,
        ontologyParentId = null,
        isThematic = true,
        isOntology = true,
        relatedTopicIds = emptyList(),
    )

    private val stories = topic(1883, "Stories")
    private val prophets = topic(20, "Prophets")
    private val nations = topic(21, "Nations")
    private val living = topic(257, "Living Creation")
    private val patience = topic(30, "Patience", arabic = "الصبر")
    private val moses = topic(40, "Moses")

    private val loaded = TopicBrowseState(
        isLoading = false,
        themes = listOf(
            TopicThemeCard(
                root = TopicTally(stories, verseCount = 1310),
                branches = listOf(TopicTally(prophets, 1042), TopicTally(nations, 311)),
            )
        ),
        kinds = listOf(TopicTally(living, verseCount = 812)),
        index = listOf(TopicTally(moses, 431, childCount = 13), TopicTally(patience, 87)),
    )

    @Test
    fun `arriving asks for the catalogue`() {
        render()

        // Idempotent by design, so the screen can send it on every composition.
        assertThat(events).contains(QuranTopicsEvent.OpenBrowser)
    }

    // ---- Themes ----

    @Test
    fun `a theme card shows its size and its branches with theirs`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText("Stories").assertIsDisplayed()
        composeRule.onNodeWithText("1,310", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Prophets").assertIsDisplayed()
        composeRule.onNodeWithText("1,042").assertIsDisplayed()
    }

    @Test
    fun `a branch chip opens that branch in the outline`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText("Prophets").performClick()

        assertThat(openedTopic).isEqualTo(20 to TopicTree.THEMATIC)
    }

    @Test
    fun `the card's header opens the chapter itself`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText("Stories").performClick()

        assertThat(openedTopic).isEqualTo(1883 to TopicTree.THEMATIC)
    }

    // ---- Kinds ----

    @Test
    fun `a kind tile opens in the ontology`() {
        browseState.value = loaded.copy(tree = TopicTree.ONTOLOGY)
        render()

        composeRule.onNodeWithText("812", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Living Creation").performClick()

        assertThat(openedTopic).isEqualTo(257 to TopicTree.ONTOLOGY)
    }

    @Test
    fun `choosing a tab is handed to the view model`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_tree_ontology)).performClick()

        assertThat(events).contains(QuranTopicsEvent.SelectTree(TopicTree.ONTOLOGY))
    }

    // ---- Index ----

    @Test
    fun `the index lists entries under their letters, with sub-entries and Arabic`() {
        browseState.value = loaded.copy(tree = TopicTree.INDEX)
        render()

        composeRule.onNodeWithText("Moses").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_topics_sub_entries, 13), substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("الصبر").assertIsDisplayed()
    }

    @Test
    fun `the letter rail offers each letter to TalkBack by name`() {
        browseState.value = loaded.copy(tree = TopicTree.INDEX)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_topics_jump_to_letter, "M"))
            .assertIsDisplayed()
            .performClick()
    }

    @Test
    fun `most cited is a sort the view model is asked for`() {
        browseState.value = loaded.copy(tree = TopicTree.INDEX)
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_sort_most_cited)).performClick()

        assertThat(events).contains(QuranTopicsEvent.SetIndexSort(TopicIndexSort.MOST_CITED))
    }

    @Test
    fun `most cited says how many of the index it is showing`() {
        browseState.value = loaded.copy(tree = TopicTree.INDEX, indexSort = TopicIndexSort.MOST_CITED)
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_most_cited_footer, 2, "2"))
            .assertIsDisplayed()
    }

    @Test
    fun `an index entry opens in the index`() {
        browseState.value = loaded.copy(tree = TopicTree.INDEX)
        render()

        composeRule.onNodeWithText("Patience").performClick()

        assertThat(openedTopic).isEqualTo(30 to TopicTree.INDEX)
    }

    // ---- Search ----

    @Test
    fun `typing hands the query to the view model`() {
        browseState.value = loaded
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_search_hint)).performTextInput("pat")

        assertThat(events).contains(QuranTopicsEvent.Search("pat"))
    }

    @Test
    fun `a result shows where it sits and opens in its own hierarchy`() {
        browseState.value = loaded.copy(
            searchQuery = "moses",
            searchResults = listOf(
                TopicSearchHit(moses, TopicTree.THEMATIC, path = listOf(stories, prophets), verseCount = 431)
            ),
        )
        render()

        composeRule.onNodeWithText("Stories › Prophets", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("431").assertIsDisplayed()
        composeRule.onNodeWithText("Moses").performClick()

        assertThat(openedTopic).isEqualTo(40 to TopicTree.THEMATIC)
    }

    @Test
    fun `a search that matches nothing says so, naming what was searched for`() {
        browseState.value = loaded.copy(searchQuery = "zzz", searchResults = emptyList())
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_no_match, "zzz")).assertIsDisplayed()
    }

    // ---- States ----

    @Test
    fun `an install whose content predates the index is told so, not shown empty cards`() {
        browseState.value = TopicBrowseState(isLoading = false, isAvailable = false)
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_unavailable_title)).assertIsDisplayed()
    }

    @Test
    fun `a first load shows neither cards nor an empty state`() {
        browseState.value = TopicBrowseState(isLoading = true)
        render()

        composeRule.onNodeWithText(str(R.string.quran_topics_unavailable_title)).assertDoesNotExist()
        composeRule.onNodeWithText("Stories").assertDoesNotExist()
    }

    @Test
    fun `going back is the caller's business`() {
        var back = false
        browseState.value = loaded
        render(onBack = { back = true })

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(back).isTrue()
    }
}
