package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSettingsItemTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders title and subtitle with icon`() {
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Calculation Method",
                subtitle = "Prayer settings",
                icon = Icons.Default.Notifications,
                tintIcon = true
            )
        }

        composeRule.onNodeWithText("Calculation Method").assertExists()
        composeRule.onNodeWithText("Prayer settings").assertExists()
    }

    @Test
    fun `falls back to value when subtitle is null`() {
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "High Latitude Method",
                value = "Middle of the Night"
            )
        }

        composeRule.onNodeWithText("Middle of the Night").assertExists()
    }

    @Test
    fun `onClick fires when clicked`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Navigate",
                onClick = { clicked = true },
                showArrow = true
            )
        }

        composeRule.onNodeWithText("Navigate").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `toggle row invokes onCheckedChange with inverted value`() {
        var newValue: Boolean? = null
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Haptic Feedback",
                checked = true,
                onCheckedChange = { newValue = it }
            )
        }

        composeRule.onNodeWithText("Haptic Feedback").performClick()
        assertThat(newValue).isFalse()
    }

    @Test
    fun `trailing content is rendered`() {
        composeRule.setThemedContent {
            NimazSettingsItem(
                title = "Custom",
                trailingContent = { Text("TrailingMarker") }
            )
        }

        composeRule.onNodeWithText("TrailingMarker").assertIsDisplayed()
    }
}
