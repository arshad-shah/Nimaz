package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaLetterDetailSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun letter() = QaidaLetter(
        id = 2,
        letterArabic = "ب",
        nameArabic = "باء",
        nameTransliteration = "Ba",
        isolatedForm = "ب",
        initialForm = "بـ",
        medialForm = "ـبـ",
        finalForm = "ـب",
        isConnecting = true,
        makhrajArea = MakhrajArea.SHAFATAIN,
        makhrajDetail = "Pressing the lips together gently.",
        phoneticHint = "like 'b' in 'book'",
        audioKey = "ba",
        audioPath = "",
        displayOrder = 2,
    )

    @Test
    fun rendersTransliterationName() {
        composeRule.setThemedContent {
            QaidaLetterDetailSheet(
                letter = letter(),
                onPlay = {},
            )
        }

        composeRule.onNodeWithText("Ba").assertExists()
    }

    @Test
    fun rendersPhoneticHint() {
        composeRule.setThemedContent {
            QaidaLetterDetailSheet(
                letter = letter(),
                onPlay = {},
            )
        }

        composeRule.onNodeWithText("like 'b' in 'book'").assertExists()
    }

    @Test
    fun rendersSectionLabels() {
        composeRule.setThemedContent {
            QaidaLetterDetailSheet(
                letter = letter(),
                onPlay = {},
            )
        }

        // SectionLabel uppercases the string resources ("Its shapes",
        // "Where it's made").
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ITS SHAPES").assertExists()
        composeRule.onNodeWithText("WHERE IT'S MADE").assertExists()
    }

    @Test
    fun rendersWithoutPhoneticHintSmoke() {
        composeRule.setThemedContent {
            QaidaLetterDetailSheet(
                letter = letter().copy(phoneticHint = null),
                onPlay = {},
            )
        }

        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
