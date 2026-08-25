package com.arshadshah.nimaz.presentation.screens.qaida

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.QaidaReaderViewModel
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
 * The lesson reader — tappable cells, a transliteration toggle, the course-walk bar, and the
 * celebration that must fire exactly once.
 *
 * **The celebration is the subtle part.** It must fire when a lesson is completed *during this
 * visit*, and must not fire when a child re-opens a lesson they already finished — that visit
 * is practice, and a confetti overlay in front of it is a screen they have to dismiss to read.
 * The screen holds `openedComplete` for this: captured on the first non-null status and reset
 * per lesson. Both halves are silent failures — one loses the reward, the other makes finished
 * lessons unusable.
 *
 * **The navigation bar's three buttons are each conditional.** Previous is disabled at the
 * start, Next is disabled at the end *and* on a locked lesson, and Continue appears only when
 * the open lesson is not the one the course points at — which is precisely when browsing
 * backwards has left somewhere to return to. The `LOCKED` guard is duplicated from the
 * ViewModel deliberately, so a locked lesson reads as unavailable rather than as a button that
 * does nothing.
 *
 * The bar renders nothing at all when the open lesson is not in the course list, which is the
 * state every navigation starts in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QaidaReaderScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val lessonContent = MutableStateFlow<QaidaLessonContent?>(null)
    private val playingCell = MutableStateFlow<com.arshadshah.nimaz.domain.model.QaidaCell?>(null)
    private val lessonProgress = MutableStateFlow<QaidaLessonState?>(null)
    private val courseProgress = MutableStateFlow<QaidaCourseProgress?>(null)
    private val completedCellIds = MutableStateFlow<Set<Int>>(emptySet())
    private val selectedLessonId = MutableStateFlow<Int?>(null)
    private val events = mutableListOf<QaidaReaderEvent>()

    private val viewModel: QaidaReaderViewModel = mockk(relaxed = true) {
        every { this@mockk.lessonContent } returns this@QaidaReaderScreenTest.lessonContent
        every { this@mockk.playingCell } returns this@QaidaReaderScreenTest.playingCell
        every { this@mockk.lessonProgress } returns this@QaidaReaderScreenTest.lessonProgress
        every { this@mockk.courseProgress } returns this@QaidaReaderScreenTest.courseProgress
        every { this@mockk.completedCellIds } returns this@QaidaReaderScreenTest.completedCellIds
        every { this@mockk.selectedLessonId } returns this@QaidaReaderScreenTest.selectedLessonId
        every { onEvent(any()) } answers { events += firstArg<QaidaReaderEvent>() }
    }

    private var backs = 0

    private fun setContent(lessonId: Int = 1) {
        composeRule.setThemedContent {
            QaidaReaderScreen(
                lessonId = lessonId,
                onNavigateBack = { backs++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun threeLessonCourse(open: Int, nextLessonId: Int? = 2) {
        selectedLessonId.value = open
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters", status = LessonStatus.COMPLETED),
                qaidaLessonState(2, "Fatha"),
                qaidaLessonState(3, "Kasra", status = LessonStatus.LOCKED),
            ),
            completedLessons = 1,
            nextLessonId = nextLessonId,
        )
    }

    @Test
    fun `opening the reader selects the lesson named in the route`() {
        setContent(lessonId = 7)

        assertThat(events).contains(QaidaReaderEvent.SelectLesson(7))
    }

    @Test
    fun `the lesson's cells and its instruction are on the page`() {
        lessonContent.value = qaidaLessonContent(
            lessonId = 1,
            titleEnglish = "The Arabic Letters",
            cells = listOf(
                qaidaCell(1, textArabic = "ا", transliteration = "alif"),
                qaidaCell(2, textArabic = "ب", transliteration = "ba"),
            ),
            instructionEnglish = "Tap each letter to hear it",
        )

        setContent()

        composeRule.onNodeWithText("The Arabic Letters").assertExists()
        composeRule.onNodeWithText("ا").assertIsDisplayed()
        composeRule.onNodeWithText("ب").assertIsDisplayed()
        composeRule.onNodeWithText("Tap each letter to hear it").assertExists()
    }

    @Test
    fun `the transliteration toggle removes the transliteration`() {
        // A learner reading the Arabic wants the crutch gone; the toggle is the only way, and
        // the two icon states are the only signal of which way it is set.
        lessonContent.value = qaidaLessonContent(
            cells = listOf(qaidaCell(1, textArabic = "ا", transliteration = "alif")),
        )

        setContent()
        composeRule.onNodeWithText("alif").assertExists()

        composeRule.onNodeWithContentDescription(string(R.string.qaida_toggle_transliteration))
            .performClick()

        composeRule.onNodeWithText("alif").assertDoesNotExist()
        composeRule.onNodeWithText("ا").assertExists()
    }

    @Test
    fun `a cell the artifact records no transliteration for shows only the Arabic`() {
        // The tile's guard is `showTransliteration && transliteration.isNotBlank()`. With only
        // the first half, a cell the content artifact carries no transliteration for renders an
        // empty line under the glyph — which shifts every tile in the row and reads as a
        // layout bug rather than as missing data.
        lessonContent.value = qaidaLessonContent(
            cells = listOf(
                qaidaCell(1, textArabic = "ا", transliteration = "alif"),
                qaidaCell(2, textArabic = "ب", transliteration = ""),
            ),
        )

        setContent()

        composeRule.onNodeWithText("ا").assertIsDisplayed()
        composeRule.onNodeWithText("ب").assertIsDisplayed()
        composeRule.onNodeWithText("alif").assertExists()
    }

    @Test
    fun `tapping a cell asks for that cell`() {
        val alif = qaidaCell(1, textArabic = "ا", transliteration = "alif")
        val ba = qaidaCell(2, textArabic = "ب", transliteration = "ba")
        lessonContent.value = qaidaLessonContent(cells = listOf(alif, ba))

        setContent()
        composeRule.onNodeWithText("ب").performClick()

        assertThat(events).contains(QaidaReaderEvent.CellTapped(ba))
        assertThat(events).doesNotContain(QaidaReaderEvent.CellTapped(alif))
    }

    @Test
    fun `the bar shows nothing while the open lesson is not yet in the course`() {
        // `indexOfFirst` returns -1 and the whole bar returns early — the state every
        // navigation spends its first frames in.
        lessonContent.value = qaidaLessonContent()

        setContent()

        composeRule.onNodeWithText(string(R.string.qaida_previous_lesson)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.qaida_next_lesson)).assertDoesNotExist()
    }

    @Test
    fun `the first lesson cannot go back`() {
        lessonContent.value = qaidaLessonContent(lessonId = 1)
        threeLessonCourse(open = 1)

        setContent(lessonId = 1)

        composeRule.onNodeWithText(string(R.string.qaida_previous_lesson)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.qaida_next_lesson)).assertIsEnabled()
    }

    @Test
    fun `a locked next lesson is offered as unavailable rather than as a dead button`() {
        // Lesson 3 is LOCKED, so from lesson 2 the forward button must be disabled — the
        // ViewModel would refuse the move anyway, and a button that silently does nothing is
        // worse than one that says it cannot.
        lessonContent.value = qaidaLessonContent(lessonId = 2, titleEnglish = "Fatha")
        threeLessonCourse(open = 2)

        setContent(lessonId = 2)

        composeRule.onNodeWithText(string(R.string.qaida_next_lesson)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.qaida_previous_lesson)).assertIsEnabled()
    }

    @Test
    fun `walking the course dispatches the move`() {
        lessonContent.value = qaidaLessonContent(lessonId = 2, titleEnglish = "Fatha")
        threeLessonCourse(open = 2)

        setContent(lessonId = 2)
        events.clear()
        composeRule.onNodeWithText(string(R.string.qaida_previous_lesson)).performClick()

        assertThat(events).containsExactly(QaidaReaderEvent.PreviousLesson)
    }

    @Test
    fun `continue appears only once browsing has left somewhere to return to`() {
        // Standing on the lesson the course points at, there is nothing to resume; step back
        // to lesson 1 and there is.
        lessonContent.value = qaidaLessonContent(lessonId = 2, titleEnglish = "Fatha")
        threeLessonCourse(open = 2, nextLessonId = 2)

        setContent(lessonId = 2)

        composeRule.onNodeWithText(string(R.string.qaida_resume_lesson)).assertDoesNotExist()
    }

    @Test
    fun `browsing backwards offers the way back to where the learner was`() {
        lessonContent.value = qaidaLessonContent(lessonId = 1)
        threeLessonCourse(open = 1, nextLessonId = 2)

        setContent(lessonId = 1)
        events.clear()
        composeRule.onNodeWithText(string(R.string.qaida_resume_lesson)).performClick()

        assertThat(events).containsExactly(QaidaReaderEvent.Resume)
    }

    @Test
    fun `a finished course still offers a way back to the first lesson`() {
        // `nextLessonId` is null once everything is complete, and the bar falls back to the
        // first lesson rather than hiding the control — otherwise a learner who has finished
        // and stepped back to lesson 1 is offered nothing at all, on a screen whose other two
        // buttons are both disabled at the ends.
        lessonContent.value = qaidaLessonContent(lessonId = 3, titleEnglish = "Kasra")
        selectedLessonId.value = 3
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters", status = LessonStatus.COMPLETED),
                qaidaLessonState(2, "Fatha", status = LessonStatus.COMPLETED),
                qaidaLessonState(3, "Kasra", status = LessonStatus.COMPLETED),
            ),
            completedLessons = 3,
            nextLessonId = null,
        )

        setContent(lessonId = 3)
        events.clear()
        composeRule.onNodeWithText(string(R.string.qaida_resume_lesson)).performClick()

        assertThat(events).containsExactly(QaidaReaderEvent.Resume)
    }

    @Test
    fun `a finished course standing on lesson one offers no resume`() {
        // The fallback resolves to lesson 1, which is where the learner already is —
        // `resumeId != openLessonId` is what keeps the control from pointing at itself.
        lessonContent.value = qaidaLessonContent(lessonId = 1)
        selectedLessonId.value = 1
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters", status = LessonStatus.COMPLETED),
                qaidaLessonState(2, "Fatha", status = LessonStatus.COMPLETED),
            ),
            completedLessons = 2,
            nextLessonId = null,
        )

        setContent(lessonId = 1)

        composeRule.onNodeWithText(string(R.string.qaida_resume_lesson)).assertDoesNotExist()
    }

    @Test
    fun `finishing a lesson during this visit celebrates it`() {
        lessonContent.value = qaidaLessonContent(lessonId = 1, titleEnglish = "The Arabic Letters")
        threeLessonCourse(open = 1)
        lessonProgress.value = qaidaLessonState(1, status = LessonStatus.IN_PROGRESS)

        setContent(lessonId = 1)

        // The completion arrives while the reader is open — the only case that celebrates.
        lessonProgress.value = qaidaLessonState(
            1,
            status = LessonStatus.COMPLETED,
            stars = 3,
            completedCells = 10,
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.qaida_mashaallah)).assertExists()
    }

    @Test
    fun `re-opening a finished lesson to practise does not celebrate it again`() {
        // `openedComplete` is captured from the first non-null status. Without it, every visit
        // to a completed lesson opens behind an overlay the child has to dismiss.
        lessonContent.value = qaidaLessonContent(lessonId = 1, titleEnglish = "The Arabic Letters")
        threeLessonCourse(open = 1)
        lessonProgress.value = qaidaLessonState(
            1,
            status = LessonStatus.COMPLETED,
            stars = 3,
            completedCells = 10,
        )

        setContent(lessonId = 1)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.qaida_mashaallah)).assertDoesNotExist()
        composeRule.onNodeWithText("ا").assertIsDisplayed()
    }

    @Test
    fun `the bar carries a generic title until the lesson arrives`() {
        threeLessonCourse(open = 1)

        setContent()

        composeRule.onNodeWithText(string(R.string.qaida_lesson)).assertExists()
    }

    @Test
    fun `back is reachable from the reader`() {
        lessonContent.value = qaidaLessonContent()

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
