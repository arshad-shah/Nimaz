package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazStatCardTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders value and label in default mode`() {
        composeRule.setThemedContent {
            NimazStatCard(value = "15", label = "Fasted")
        }
        composeRule.onNodeWithText("15").assertExists()
        composeRule.onNodeWithText("Fasted").assertExists()
    }

    @Test
    fun `renders value and label in compact mode`() {
        composeRule.setThemedContent {
            NimazStatCard(value = "7", label = "Missed", compact = true)
        }
        composeRule.onNodeWithText("7").assertExists()
        composeRule.onNodeWithText("Missed").assertExists()
    }
}
