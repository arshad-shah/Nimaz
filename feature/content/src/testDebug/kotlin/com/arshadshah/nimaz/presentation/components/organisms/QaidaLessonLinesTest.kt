package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLessonLinesTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun cell(id: Int, arabic: String, translit: String) = QaidaCell(
        id = id,
        lineId = 1,
        lessonId = 1,
        position = id,
        textArabic = arabic,
        transliteration = translit,
        tokenType = TokenType.SYLLABLE,
        audioKey = "k$id",
        audioPath = "",
        highlightGroup = null,
        letterId = null,
        notes = null,
    )

    private fun content(instruction: String?) = QaidaLessonContent(
        lesson = QaidaLesson(
            id = 1,
            lessonNumber = 1,
            titleEnglish = "Lesson",
            titleArabic = "",
            titleTransliteration = "",
            description = "",
            conceptTags = emptyList(),
            icon = "",
            displayOrder = 1,
        ),
        lines = listOf(
            QaidaLineContent(
                line = QaidaLine(
                    id = 1,
                    lessonId = 1,
                    lineNumber = 1,
                    lineType = LineType.EXERCISE,
                    instructionEnglish = instruction,
                    instructionArabic = null,
                    displayOrder = 1,
                ),
                cells = listOf(
                    cell(1, "مَدّ", "madd"),
                    cell(2, "شَدّ", "shadd"),
                ),
            ),
        ),
    )

    @Test
    fun rendersInstructionText() {
        composeRule.setThemedContent {
            QaidaLessonLines(
                content = content("Read these slowly"),
                playingCellId = null,
                showTransliteration = true,
                onCellTap = {},
                onPlayLine = {},
            )
        }

        composeRule.onNodeWithText("Read these slowly").assertExists()
    }

    @Test
    fun rendersWithoutInstruction() {
        composeRule.setThemedContent {
            QaidaLessonLines(
                content = content(null),
                playingCellId = null,
                showTransliteration = false,
                onCellTap = {},
                onPlayLine = {},
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }

    @Test
    fun rendersWithCompletedAndPlayingCells() {
        composeRule.setThemedContent {
            QaidaLessonLines(
                content = content("Integrated words"),
                playingCellId = 1,
                showTransliteration = true,
                onCellTap = {},
                onPlayLine = {},
                completedCellIds = setOf(2),
            )
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Integrated words").assertExists()
    }
}
