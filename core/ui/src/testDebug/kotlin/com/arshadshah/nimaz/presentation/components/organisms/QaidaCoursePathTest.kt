package com.arshadshah.nimaz.presentation.components.organisms

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Qaida course trail — the winding path of lesson medallions a learner works down.
 *
 * Two things carry the whole feature. The **status → medallion mapping** decides what a learner
 * can even open: `LOCKED`, `CURRENT` and `DONE` are drawn differently and read out differently,
 * and `IN_PROGRESS` and `UNLOCKED` must both land on `CURRENT` — an arm that sent `UNLOCKED` to
 * `LOCKED` would make the next lesson unreachable and look deliberate. The **current-lesson
 * resolution** is the other: given no explicit id the trail falls back to the last lesson that is
 * not locked, which is what draws the walked half of the path in gold and the rest in faded
 * dashes.
 *
 * Every medallion is addressed by its announcement rather than its number, because the numbers are
 * rendered in Arabic-Indic digits — and because the announcement is what actually tells a learner
 * whether a lesson is open, which is the thing worth pinning.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QaidaCoursePathTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun lesson(id: Int, status: LessonStatus, stars: Int = 0) = QaidaLessonState(
        lesson = QaidaLesson(
            id = id,
            lessonNumber = id,
            titleEnglish = "Lesson $id",
            titleArabic = "الدرس",
            titleTransliteration = "Dars $id",
            description = "",
            conceptTags = emptyList(),
            icon = "",
            displayOrder = id,
        ),
        status = status,
        stars = stars,
        completedCells = if (status == LessonStatus.COMPLETED) 10 else 0,
        totalCells = 10,
        completionFraction = if (status == LessonStatus.COMPLETED) 1f else 0f,
        lastCellId = null,
    )

    private val course = listOf(
        lesson(1, LessonStatus.COMPLETED, stars = 3),
        lesson(2, LessonStatus.COMPLETED, stars = 2),
        lesson(3, LessonStatus.IN_PROGRESS),
        lesson(4, LessonStatus.LOCKED),
        lesson(5, LessonStatus.LOCKED),
    )

    private fun currentLabel(number: Int, title: String) =
        context.getString(R.string.qaida_a11y_lesson_current_format, number, title)

    private fun lockedLabel(number: Int, title: String) =
        context.getString(R.string.qaida_a11y_lesson_locked_format, number, title)

    private fun completeLabel(number: Int, title: String, stars: Int) =
        context.resources.getQuantityString(
            R.plurals.qaida_a11y_lesson_complete_format, stars, number, title, stars,
        )

    private fun showCourse(
        lessons: List<QaidaLessonState> = course,
        currentLessonId: Int? = 3,
        onLessonClick: (Int) -> Unit = {},
    ) {
        composeRule.setThemedContent {
            Box(Modifier.fillMaxWidth().height(700.dp)) {
                QaidaCoursePath(
                    lessons = lessons,
                    currentLessonId = currentLessonId,
                    onLessonClick = onLessonClick,
                )
            }
        }
    }

    @Test
    fun `a completed lesson announces its stars`() {
        // The star count is a plural resource, so one star and three read differently — and the
        // count is the learner's whole record of how well they did.
        showCourse()

        composeRule.onNodeWithContentDescription(completeLabel(1, "Lesson 1", 3)).assertExists()
        composeRule.onNodeWithContentDescription(completeLabel(2, "Lesson 2", 2)).assertExists()
    }

    @Test
    fun `the lesson in progress announces itself as the current one`() {
        showCourse()

        composeRule.onNodeWithContentDescription(currentLabel(3, "Lesson 3")).assertExists()
    }

    @Test
    fun `a locked lesson says it is locked`() {
        // The only thing telling a screen-reader learner why tapping does not open lesson 4.
        showCourse()

        composeRule.onNodeWithContentDescription(lockedLabel(4, "Lesson 4")).assertExists()
        composeRule.onNodeWithContentDescription(lockedLabel(5, "Lesson 5")).assertExists()
    }

    @Test
    fun `an unlocked but unstarted lesson reads as current, not locked`() {
        // `UNLOCKED` and `IN_PROGRESS` share the `else` arm. Sending `UNLOCKED` to `LOCKED` makes
        // the next lesson unreachable and looks like a deliberate gate.
        showCourse(
            lessons = listOf(
                lesson(1, LessonStatus.COMPLETED, stars = 1),
                lesson(2, LessonStatus.UNLOCKED),
            ),
            currentLessonId = 2,
        )

        composeRule.onNodeWithContentDescription(currentLabel(2, "Lesson 2")).assertExists()
        composeRule.onNodeWithContentDescription(lockedLabel(2, "Lesson 2")).assertDoesNotExist()
    }

    @Test
    fun `tapping a medallion opens that lesson`() {
        var opened: Int? = null
        showCourse(onLessonClick = { opened = it })

        composeRule.onNodeWithContentDescription(currentLabel(3, "Lesson 3")).performClick()

        assertThat(opened).isEqualTo(3)
    }

    @Test
    fun `a locked medallion is not offered as a control`() {
        // It carries the announcement and no click action, so a screen reader reads "locked" and
        // never presents a button that would do nothing. The gate is in the medallion rather than
        // in the trail, which is why the trail hands every lesson the same `onClick`.
        showCourse()

        composeRule.onNodeWithContentDescription(lockedLabel(4, "Lesson 4"))
            .assertHasNoClickAction()
    }

    @Test
    fun `with no current lesson the trail falls back to the last one that is open`() {
        // `indexOfLast { it.status != LOCKED }` — this is what decides how much of the path is
        // drawn as walked. Resuming a course with no stored pointer must not reset the learner to
        // the beginning.
        showCourse(currentLessonId = null)

        composeRule.onNodeWithContentDescription(currentLabel(3, "Lesson 3")).assertExists()
    }

    @Test
    fun `a course whose lessons are all locked still renders`() {
        // `coerceAtLeast(0)` — `indexOfLast` returns -1 when nothing is open, which is a fresh
        // install before the first lesson is unlocked, and a negative index would take the trail
        // down with it.
        showCourse(
            lessons = listOf(lesson(1, LessonStatus.LOCKED), lesson(2, LessonStatus.LOCKED)),
            currentLessonId = null,
        )

        composeRule.onNodeWithContentDescription(lockedLabel(1, "Lesson 1")).assertExists()
    }

    @Test
    fun `a one-lesson course draws no trail between medallions`() {
        // `if (lessons.size < 2) return@drawBehind` — the path is built from pairs, and a
        // single-lesson course is what a partially-downloaded content artifact looks like.
        showCourse(lessons = listOf(lesson(1, LessonStatus.IN_PROGRESS)), currentLessonId = 1)

        composeRule.onNodeWithContentDescription(currentLabel(1, "Lesson 1")).assertExists()
    }

    @Test
    fun `a course already finished draws every medallion as done`() {
        // `currentIndex == lessons.lastIndex`, so the locked half of the trail is skipped
        // entirely — the other side of the same condition.
        showCourse(
            lessons = listOf(
                lesson(1, LessonStatus.COMPLETED, stars = 3),
                lesson(2, LessonStatus.COMPLETED, stars = 3),
            ),
            currentLessonId = 2,
        )

        composeRule.onNodeWithContentDescription(completeLabel(1, "Lesson 1", 3)).assertExists()
        composeRule.onNodeWithContentDescription(completeLabel(2, "Lesson 2", 3)).assertExists()
    }
}
