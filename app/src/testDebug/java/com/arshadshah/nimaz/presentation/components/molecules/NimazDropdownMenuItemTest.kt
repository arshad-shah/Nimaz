package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDropdownMenuItemTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders text and triggers click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownMenuItem(
                text = "Amiri",
                selected = false,
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Amiri").assertExists()
        composeRule.onNodeWithText("Amiri").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `renders description when provided`() {
        composeRule.setThemedContent {
            NimazDropdownMenuItem(
                text = "Scheherazade New",
                selected = true,
                description = "Classic naskh typeface",
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Scheherazade New").assertExists()
        composeRule.onNodeWithText("Classic naskh typeface").assertExists()
    }

    @Test
    fun `disabled item does not invoke click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownMenuItem(
                text = "Disabled",
                selected = false,
                onClick = { clicked = true },
                enabled = false
            )
        }
        composeRule.onNodeWithText("Disabled").assertExists()
        composeRule.onNodeWithText("Disabled").performClick()
        assertThat(clicked).isFalse()
    }
}
