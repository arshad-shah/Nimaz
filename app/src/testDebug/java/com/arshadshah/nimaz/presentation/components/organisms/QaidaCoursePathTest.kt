package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaCoursePathTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun lessonState(
        id: Int,
        status: LessonStatus,
        stars: Int,
    ): QaidaLessonState {
        val lesson = QaidaLesson(
            id = id,
            lessonNumber = id,
            titleEnglish = "Lesson $id",
            titleArabic = "الدرس",
            titleTransliteration = "Dars $id",
            description = "",
            conceptTags = emptyList(),
            icon = "",
            displayOrder = id,
        )
        return QaidaLessonState(
            lesson = lesson,
            status = status,
            stars = stars,
            completedCells = if (status == LessonStatus.COMPLETED) 10 else 0,
            totalCells = 10,
            completionFraction = if (status == LessonStatus.COMPLETED) 1f else 0f,
            lastCellId = null,
        )
    }

    private val lessons = listOf(
        lessonState(1, LessonStatus.COMPLETED, 3),
        lessonState(2, LessonStatus.COMPLETED, 2),
        lessonState(3, LessonStatus.IN_PROGRESS, 0),
        lessonState(4, LessonStatus.LOCKED, 0),
        lessonState(5, LessonStatus.LOCKED, 0),
    )

    @Test
    fun `renders the course path without crashing`() {
        composeRule.setThemedContent {
            QaidaCoursePath(
                lessons = lessons,
                currentLessonId = 3,
                onLessonClick = {},
                modifier = Modifier.height(600.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders empty list without crashing`() {
        composeRule.setThemedContent {
            QaidaCoursePath(
                lessons = emptyList(),
                currentLessonId = null,
                onLessonClick = {},
                modifier = Modifier.height(200.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders with null current lesson without crashing`() {
        composeRule.setThemedContent {
            QaidaCoursePath(
                lessons = lessons,
                currentLessonId = null,
                onLessonClick = {},
                modifier = Modifier.height(600.dp),
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
