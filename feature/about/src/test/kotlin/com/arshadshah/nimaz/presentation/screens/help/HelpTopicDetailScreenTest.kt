package com.arshadshah.nimaz.presentation.screens.help

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.HelpItem
import com.arshadshah.nimaz.domain.model.HelpTopic
import com.arshadshah.nimaz.domain.model.HelpTopicDetail
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpEvent
import com.arshadshah.nimaz.presentation.viewmodel.help.HelpTopicUiState
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
 * One help topic: its questions, and the guides that go with it.
 *
 * The branch order is the behaviour. A failed load and a topic that genuinely is not in the
 * catalogue both leave `detail` null, and the screen distinguishes them **only** by testing the
 * error first — get that backwards and a transient failure tells the reader "this topic is
 * unavailable", which is a statement about the content rather than about the load, and which no
 * retry button then appears to contradict.
 *
 * The rest is that help content ships as **data**, written by a release that may be newer than
 * the build rendering it. A topic with no questions, or no guides, or neither, is a shape the
 * renderer will meet, and the sections must simply not appear rather than render empty headings.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class HelpTopicDetailScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val topicState = MutableStateFlow(HelpTopicUiState())
    private val events = mutableListOf<HelpEvent>()

    private val viewModel: HelpViewModel = mockk(relaxed = true) {
        every { this@mockk.topicState } returns this@HelpTopicDetailScreenTest.topicState
        every { onEvent(any()) } answers { events += firstArg<HelpEvent>() }
    }

    private val guidesOpened = mutableListOf<String>()

    private fun setContent(topicId: String = "prayer") {
        composeRule.setThemedContent {
            HelpTopicDetailScreen(
                topicId = topicId,
                onNavigateBack = {},
                onOpenGuide = { guidesOpened += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** `NimazSectionTitle` uppercases by default, so a heading is matched as it renders. */
    private fun sectionTitle(@StringRes res: Int): String = string(res).uppercase()

    private fun detail(
        questions: List<HelpItem.HelpQuestion> = emptyList(),
        guides: List<HelpItem.HelpGuide> = emptyList(),
        subtitle: String = "Times, alerts and adhan",
    ) = HelpTopicDetail(
        topic = HelpTopic(
            id = "prayer",
            iconKey = "schedule",
            colorKey = "indigo",
            title = "Prayer times",
            subtitle = subtitle,
            order = 0,
            itemCount = questions.size + guides.size,
        ),
        questions = questions,
        guides = guides,
    )

    private fun question(id: String, q: String, a: String) =
        HelpItem.HelpQuestion(id = id, order = 0, question = q, answer = a)

    private fun guide(id: String, title: String, minutes: Int? = 3) =
        HelpItem.HelpGuide(
            id = id,
            order = 0,
            iconKey = "tune",
            title = title,
            estimatedMinutes = minutes,
            stepCount = 4,
        )

    @Test
    fun `opening the screen asks for the topic it was routed to`() {
        topicState.value = HelpTopicUiState(detail = detail(), isLoading = false)
        setContent(topicId = "qibla")

        // The route argument, not a default: the screen is one destination reused for every
        // topic, so the id is the whole of what distinguishes one visit from another.
        assertThat(events).contains(HelpEvent.LoadTopic("qibla"))
    }

    @Test
    fun `a loaded topic shows its hero and both sections`() {
        topicState.value = HelpTopicUiState(
            detail = detail(
                questions = listOf(question("q1", "When is Fajr?", "At dawn.")),
                guides = listOf(guide("g1", "Set a prayer alert")),
            ),
            isLoading = false,
        )
        setContent()

        // The title is on the app bar as well as on the hero, so both nodes carry it.
        composeRule.onAllNodesWithText("Prayer times").onFirst().assertExists()
        composeRule.onNodeWithText("Times, alerts and adhan").assertExists()
        composeRule.onNodeWithText(sectionTitle(R.string.help_common_questions)).assertExists()
        composeRule.onNodeWithText(sectionTitle(R.string.help_step_by_step)).assertExists()
    }

    @Test
    fun `a topic with no guides renders no step-by-step heading`() {
        // Content this build did not write: a topic that is all questions is a shape the
        // renderer meets, and an empty heading reads as content that failed to load.
        topicState.value = HelpTopicUiState(
            detail = detail(questions = listOf(question("q1", "When is Fajr?", "At dawn."))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(sectionTitle(R.string.help_common_questions)).assertExists()
        composeRule.onNodeWithText(sectionTitle(R.string.help_step_by_step)).assertDoesNotExist()
    }

    @Test
    fun `a topic with neither questions nor guides still renders its hero`() {
        topicState.value = HelpTopicUiState(detail = detail(subtitle = ""), isLoading = false)
        setContent()

        // The title is on the app bar as well as on the hero, so both nodes carry it.
        composeRule.onAllNodesWithText("Prayer times").onFirst().assertExists()
        composeRule.onNodeWithText(sectionTitle(R.string.help_common_questions)).assertDoesNotExist()
        composeRule.onNodeWithText(sectionTitle(R.string.help_step_by_step)).assertDoesNotExist()
    }

    @Test
    fun `an answer is hidden until its question is tapped`() {
        topicState.value = HelpTopicUiState(
            detail = detail(questions = listOf(question("q1", "When is Fajr?", "At dawn."))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("At dawn.").assertDoesNotExist()

        composeRule.onNodeWithText("When is Fajr?").performClick()

        composeRule.onNodeWithText("At dawn.").assertExists()
    }

    @Test
    fun `several questions each expand on their own`() {
        topicState.value = HelpTopicUiState(
            detail = detail(
                questions = listOf(
                    question("q1", "When is Fajr?", "At dawn."),
                    question("q2", "Why did the adhan not play?", "Check the alert style."),
                ),
            ),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("When is Fajr?").performClick()

        composeRule.onNodeWithText("At dawn.").assertExists()
        // Opening one does not open the other: each row owns its own expansion.
        composeRule.onNodeWithText("Check the alert style.").assertDoesNotExist()
    }

    @Test
    fun `a guide row opens that guide`() {
        topicState.value = HelpTopicUiState(
            detail = detail(guides = listOf(guide("g1", "Set a prayer alert"), guide("g2", "Fix qibla"))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText("Fix qibla").performClick()

        assertThat(guidesOpened).containsExactly("g2")
    }

    @Test
    fun `a guide with no estimate says step-by-step instead of a time`() {
        topicState.value = HelpTopicUiState(
            detail = detail(guides = listOf(guide("g1", "Set a prayer alert", minutes = null))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_guide_step_by_step)).assertExists()
    }

    @Test
    fun `a guide with an estimate reports it`() {
        topicState.value = HelpTopicUiState(
            detail = detail(guides = listOf(guide("g1", "Set a prayer alert", minutes = 5))),
            isLoading = false,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_guide_about_minutes_format, 5)).assertExists()
    }

    @Test
    fun `a failed load says the load failed, not that the topic is missing`() {
        // Both leave `detail` null. Only the branch order tells them apart, and getting it
        // wrong blames the catalogue for a transient failure.
        topicState.value = HelpTopicUiState(
            detail = null,
            isLoading = false,
            error = UiError(message = R.string.help_topic_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.help_topic_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.help_topic_unavailable)).assertDoesNotExist()
    }

    @Test
    fun `the failed load can be retried`() {
        topicState.value = HelpTopicUiState(
            isLoading = false,
            error = UiError(message = R.string.help_topic_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).contains(HelpEvent.Retry)
    }

    @Test
    fun `a topic still loading shows neither content nor an absence`() {
        // The spinner animates forever, so the clock is pinned before `setContent` — otherwise
        // `waitForIdle` never returns. Only this test renders a loading state.
        composeRule.mainClock.autoAdvance = false
        topicState.value = HelpTopicUiState(detail = null, isLoading = true)
        setContent()

        // "This topic is unavailable" during the load would be a verdict delivered before the
        // question was asked.
        composeRule.onNodeWithText(string(R.string.help_topic_unavailable)).assertDoesNotExist()
    }

    @Test
    fun `a topic that is genuinely absent says so`() {
        topicState.value = HelpTopicUiState(detail = null, isLoading = false, error = null)
        setContent()

        composeRule.onNodeWithText(string(R.string.help_topic_unavailable)).assertExists()
    }
}
