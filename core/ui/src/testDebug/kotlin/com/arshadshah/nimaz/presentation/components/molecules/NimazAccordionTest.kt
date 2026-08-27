package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun `styles are complete`() {
        assertThat(NimazAccordionStyle.entries).hasSize(2)
    }

    @Test
    fun `the hoisted overload reports a toggle instead of expanding itself`() {
        val toggles = mutableListOf<Boolean>()
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Fajr",
                expanded = false,
                onExpandedChange = { toggles += it },
            ) { Text("body") }
        }

        composeRule.onNodeWithText("Fajr").performClick()

        // The caller owns the state, so the body must stay closed until the caller says otherwise.
        assertThat(toggles).containsExactly(true)
        composeRule.onNodeWithText("body").assertDoesNotExist()
    }

    @Test
    fun `the hoisted overload shows the body when the caller says it is expanded`() {
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Fajr",
                expanded = true,
                onExpandedChange = {},
            ) { Text("body") }
        }
        composeRule.onNodeWithText("body").assertExists()
    }

    @Test
    fun `the flat style still renders its header and body`() {
        composeRule.setThemedContent {
            NimazAccordion(
                title = "Asr",
                expanded = true,
                onExpandedChange = {},
                style = NimazAccordionStyle.FLAT,
            ) { Text("picker") }
        }
        composeRule.onNodeWithText("Asr").assertExists()
        composeRule.onNodeWithText("picker").assertExists()
    }
}
