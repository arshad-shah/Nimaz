package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TopicCitation
import com.arshadshah.nimaz.domain.model.TopicDetail
import com.arshadshah.nimaz.domain.model.TopicTree
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.CitationGroup
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranTopicsViewModel
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicDetailState
import com.arshadshah.nimaz.presentation.viewmodel.quran.TopicSurahContext
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
 * One subject, its neighbours, and every verse cited under it.
 *
 * The citations are grouped by surah and deliberately **not** re-sorted: they arrive in ayah-id
 * order, which is the mushaf's own sequence, and re-ordering them would be substituting ours.
 * The one exception is the surah a reader arrived from, which is pinned to the top — and only
 * when they arrived from one.
 *
 * The previews matter for the same reason the rolled-up counts do on the browser: the list used
 * to read "2:153 — Open in reader", 153 times, which is a row that says nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranTopicDetailScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val detailState = MutableStateFlow(TopicDetailState())
    private val events = mutableListOf<QuranTopicsEvent>()
    private var openedAyah: Pair<Int, Int>? = null
    private var openedTopic: Pair<Int, TopicTree>? = null

    private val viewModel: QuranTopicsViewModel = mockk(relaxed = true) {
        every { detailState } returns this@QuranTopicDetailScreenTest.detailState
        every { onEvent(any()) } answers { events += firstArg<QuranTopicsEvent>() }
    }

    private fun render(topicId: Int = 7, fromSurah: Int? = null) {
        composeRule.setThemedContent {
            QuranTopicDetailScreen(
                topicId = topicId,
                tree = TopicTree.THEMATIC,
                onNavigateBack = {},
                onOpenAyah = { s, a -> openedAyah = s to a },
                onOpenTopic = { id, tree -> openedTopic = id to tree },
                fromSurah = fromSurah,
                viewModel = viewModel,
            )
        }
    }

    private fun topic(id: Int, name: String, description: String = "") = QuranTopic(
        id = id,
        name = name,
        arabicName = "موضوع",
        description = description,
        wikiLink = "",
        ayahCount = 3,
        parentId = null,
        thematicParentId = null,
        ontologyParentId = null,
        isThematic = true,
        isOntology = false,
        relatedTopicIds = emptyList(),
    )

    private val patience = topic(7, "Patience", description = "Bearing what is difficult.")

    private fun detail(
        children: List<QuranTopic> = emptyList(),
        related: List<QuranTopic> = emptyList(),
    ) = TopicDetail(
        topic = patience,
        tree = TopicTree.THEMATIC,
        breadcrumb = listOf(topic(1, "Character")),
        children = children,
        related = related,
        citations = listOf(TopicCitation(ayahId = 160, surahNumber = 2, ayahNumber = 153)),
    )

    private val group = CitationGroup(
        surahNumber = 2,
        surahName = "The Cow",
        citations = listOf(TopicCitation(ayahId = 160, surahNumber = 2, ayahNumber = 153)),
    )

    @Test
    fun `arriving asks for this subject, carrying the surah it was opened from`() {
        render(topicId = 7, fromSurah = 18)

        assertThat(events).contains(
            QuranTopicsEvent.LoadDetail(topicId = 7, tree = TopicTree.THEMATIC, fromSurah = 18)
        )
    }

    @Test
    fun `a subject that is not in the index says so rather than showing a blank page`() {
        detailState.value = TopicDetailState(isLoading = false, detail = null)

        render()

        composeRule.onNodeWithText(str(R.string.quran_topic_not_found)).assertIsDisplayed()
    }

    @Test
    fun `the subject's own description is shown`() {
        detailState.value = TopicDetailState(isLoading = false, detail = detail())

        render()

        composeRule.onNodeWithText("Bearing what is difficult.").assertIsDisplayed()
    }

    @Test
    fun `subtopics and related subjects are offered as their own sections`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(
                children = listOf(topic(8, "Perseverance")),
                related = listOf(topic(9, "Gratitude")),
            ),
        )

        render()

        composeRule.onNodeWithText(str(R.string.quran_topic_subtopics)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.quran_topic_related)).assertIsDisplayed()
        composeRule.onNodeWithText("Perseverance").assertIsDisplayed()
        composeRule.onNodeWithText("Gratitude").assertIsDisplayed()
    }

    @Test
    fun `walking to a neighbouring subject is the caller's business`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(related = listOf(topic(9, "Gratitude"))),
        )
        render()

        composeRule.onNodeWithText("Gratitude").performClick()

        assertThat(openedTopic).isEqualTo(9 to TopicTree.THEMATIC)
    }

    @Test
    fun `a cited verse shows its first line rather than only its reference`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(),
            citationGroups = listOf(group),
            previews = mapOf(160 to "O you who believe, seek help through patience and prayer"),
        )

        render()

        composeRule.onNodeWithText("seek help through patience", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `a citation with no preview still shows its reference`() {
        // Empty when the reader's chosen translation has no text for these verses — the row
        // shows the reference alone rather than a gap where a sentence should be.
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(),
            citationGroups = listOf(group),
            previews = emptyMap(),
        )

        render()

        composeRule.onNodeWithText("2:153", substring = true).assertIsDisplayed()
    }

    @Test
    fun `opening a citation opens that verse`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(),
            citationGroups = listOf(group),
        )
        render()

        composeRule.onNodeWithText("2:153", substring = true).performClick()

        assertThat(openedAyah).isEqualTo(2 to 153)
    }

    @Test
    fun `the surah the reader came from is called out`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(),
            citationGroups = listOf(group.copy(isFromSurah = true)),
            surahContext = TopicSurahContext(surahNumber = 2, surahName = "The Cow", verseCount = 1),
        )

        render(fromSurah = 2)

        composeRule.onNodeWithText(str(R.string.quran_topic_surah_you_came_from)).assertIsDisplayed()
    }

    @Test
    fun `arriving from nowhere singles out no surah`() {
        detailState.value = TopicDetailState(
            isLoading = false,
            detail = detail(),
            citationGroups = listOf(group),
            surahContext = null,
        )

        render()

        composeRule.onNodeWithText(str(R.string.quran_topic_surah_you_came_from))
            .assertDoesNotExist()
    }

    @Test
    fun `a first load shows neither the subject nor a not-found`() {
        detailState.value = TopicDetailState(isLoading = true)

        render()

        composeRule.onNodeWithText(str(R.string.quran_topic_not_found)).assertDoesNotExist()
    }
}
