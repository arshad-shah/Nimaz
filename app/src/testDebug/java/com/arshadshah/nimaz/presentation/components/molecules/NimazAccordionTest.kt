package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazAccordionTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `title always visible body hidden when collapsed`() {
        composeRule.setThemedContent {
            NimazAccordion(title = "Question?") {
                Text("Answer body")
            }
        }
        composeRule.onNodeWithText("Question?").assertExists()
        composeRule.onNodeWithText("Answer body").assertDoesNotExist()
    }

    @Test
    fun `body visible when initially expanded`() {
        composeRule.setThemedContent {
            NimazAccordion(title = "Question?", initiallyExpanded = true) {
                Text("Answer body")
            }
        }
        composeRule.onNodeWithText("Answer body").assertExists()
    }

    @Test
    fun `tapping header expands the body`() {
        composeRule.setThemedContent {
            NimazAccordion(title = "Question?") {
                Text("Answer body")
            }
        }
        composeRule.onNodeWithText("Question?").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Answer body").assertExists()
    }
}
