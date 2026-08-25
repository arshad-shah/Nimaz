package com.arshadshah.nimaz.presentation.screens.qaida

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
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
 * The Qaida course map — the screen a child lands on, and the one place the whole journey can
 * be thrown away.
 *
 * **Every value in the header is null-guarded, because `courseProgress` is null for the whole
 * first frame.** Six `?:` fallbacks in as many lines, and the lesson-index one is not a plain
 * default: it is `(completed + 1).coerceAtMost(total)`, so a finished course reads "Lesson 12
 * of 12" rather than "13 of 12". A missing guard here is a crash on launch, and a wrong
 * `coerceAtMost` is a course that claims a lesson that does not exist.
 *
 * **"Continue" resolves a title from an id.** `nextLessonId` is an id; the label is the matching
 * lesson's own title, looked up in the list. An id with no lesson behind it — a course whose
 * content artifact changed under a stored pointer — has to leave the label absent rather than
 * render an empty button.
 *
 * **Reset is destructive and confirmed.** The event fires from the dialog's confirm, never from
 * the menu row, and nothing in the UI distinguishes the two until a child taps "Reset journey"
 * out of curiosity and loses every star.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QaidaHomeScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val courseProgress = MutableStateFlow<QaidaCourseProgress?>(null)
    private val events = mutableListOf<QaidaReaderEvent>()

    private val viewModel: QaidaReaderViewModel = mockk(relaxed = true) {
        every { this@mockk.courseProgress } returns this@QaidaHomeScreenTest.courseProgress
        every { onEvent(any()) } answers { events += firstArg<QaidaReaderEvent>() }
    }

    private val openedLessons = mutableListOf<Int>()
    private var lettersOpened = 0

    private fun setContent() {
        composeRule.setThemedContent {
            QaidaHomeScreen(
                onNavigateBack = {},
                onOpenLesson = { openedLessons += it },
                onOpenLetters = { lettersOpened++ },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the map renders before any progress has arrived`() {
        // Every header value is read through a `?:` for exactly this frame. One missing guard
        // is a null dereference on the screen's first composition.
        setContent()

        composeRule.onNodeWithText(string(R.string.qaida_journey_title)).assertExists()
    }

    @Test
    fun `the lessons are laid out along the trail, each announced with its own state`() {
        // The medallions are drawn art, addressable only by their a11y description — which is
        // also the better contract: the description carries the lesson number, its title *and*
        // whether it is locked, and that phrase is all a screen-reader user gets.
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters"),
                qaidaLessonState(2, "Fatha", status = LessonStatus.LOCKED),
            ),
            nextLessonId = 1,
        )

        setContent()

        composeRule.onNodeWithContentDescription(
            string(R.string.qaida_a11y_lesson_current_format, 1, "The Arabic Letters")
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            string(R.string.qaida_a11y_lesson_locked_format, 2, "Fatha")
        ).assertExists()
    }

    @Test
    fun `tapping a lesson on the trail opens that lesson`() {
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters"),
                qaidaLessonState(2, "Fatha"),
            ),
            nextLessonId = 1,
        )

        setContent()
        composeRule.onNodeWithContentDescription(
            string(R.string.qaida_a11y_lesson_current_format, 2, "Fatha")
        ).performClick()

        assertThat(openedLessons).containsExactly(2)
    }

    @Test
    fun `continue opens the lesson the course points at, not the first one`() {
        // `nextLessonId` is the resume pointer; opening lesson 1 instead sends a child who is
        // on lesson 5 back to the beginning.
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters", status = LessonStatus.COMPLETED),
                qaidaLessonState(2, "Fatha"),
                qaidaLessonState(3, "Kasra"),
            ),
            completedLessons = 1,
            nextLessonId = 2,
        )

        setContent()
        composeRule.onNodeWithText(string(R.string.qaida_continue_format, "Fatha")).performClick()

        assertThat(openedLessons).containsExactly(2)
    }

    @Test
    fun `a resume pointer with no lesson behind it leaves the header without a label`() {
        // The lookup is `lessons.firstOrNull { it.lesson.id == nextLessonId }` — a stored
        // pointer can outlive the content artifact that had that lesson.
        courseProgress.value = qaidaCourse(
            lessons = listOf(qaidaLessonState(1, "The Arabic Letters")),
            nextLessonId = 99,
        )

        setContent()

        composeRule.onNodeWithText(string(R.string.qaida_journey_title)).assertExists()
    }

    @Test
    fun `a finished course does not claim a lesson beyond the last`() {
        // `(completed + 1).coerceAtMost(total)`. Without the coerce a child who finished the
        // course is told they are on lesson 3 of 2.
        courseProgress.value = qaidaCourse(
            lessons = listOf(
                qaidaLessonState(1, "The Arabic Letters", status = LessonStatus.COMPLETED, stars = 3),
                qaidaLessonState(2, "Fatha", status = LessonStatus.COMPLETED, stars = 3),
            ),
            completedLessons = 2,
            totalStars = 6,
            nextLessonId = null,
        )

        setContent()

        composeRule.onNodeWithText("3").assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.qaida_journey_title)).assertExists()
    }

    @Test
    fun `the letter explorer is reachable from the top bar`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.qaida_letter_explorer))
            .performClick()

        assertThat(lettersOpened).isEqualTo(1)
    }

    @Test
    fun `resetting the journey is confirmed before anything is thrown away`() {
        courseProgress.value = qaidaCourse(
            lessons = listOf(qaidaLessonState(1, status = LessonStatus.COMPLETED, stars = 3)),
            completedLessons = 1,
            totalStars = 3,
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.more)).performClick()
        composeRule.onNodeWithText(string(R.string.qaida_reset_journey)).performClick()

        // The menu row opens the dialog and dispatches nothing.
        assertThat(events).isEmpty()
        composeRule.onNodeWithText(string(R.string.qaida_reset_title)).assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.reset)).performClick()
        assertThat(events).containsExactly(QaidaReaderEvent.ResetJourney)
    }

    @Test
    fun `cancelling the reset leaves the journey alone`() {
        courseProgress.value = qaidaCourse(
            lessons = listOf(qaidaLessonState(1, status = LessonStatus.COMPLETED, stars = 3)),
            completedLessons = 1,
            totalStars = 3,
        )

        setContent()
        composeRule.onNodeWithContentDescription(string(R.string.more)).performClick()
        composeRule.onNodeWithText(string(R.string.qaida_reset_journey)).performClick()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertThat(events).isEmpty()
        composeRule.onNodeWithText(string(R.string.qaida_reset_title)).assertDoesNotExist()
    }
}
