package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDropdownMenuTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `action invokes its click handler`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownAction(text = "Share", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Share").assertHasClickAction().performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `disabled action does not invoke its handler`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownAction(text = "Share", enabled = false, onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Share").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `destructive action renders with a leading icon and still emits`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownAction(
                text = "Reset Journey",
                leadingIcon = Icons.Filled.RestartAlt,
                destructive = true,
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Reset Journey").assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `collapsed menu shows none of its actions`() {
        composeRule.setThemedContent {
            NimazDropdownMenu(expanded = false, onDismissRequest = {}) {
                NimazDropdownAction(text = "Reset Journey", onClick = {})
            }
        }
        composeRule.onNodeWithText("Reset Journey").assertDoesNotExist()
    }

    @Test
    fun `expanded menu shows its actions and forwards clicks`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDropdownMenu(expanded = true, onDismissRequest = {}) {
                NimazDropdownAction(text = "Reset Journey", onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithText("Reset Journey").assertIsDisplayed().performClick()
        assertThat(clicked).isTrue()
    }
}
