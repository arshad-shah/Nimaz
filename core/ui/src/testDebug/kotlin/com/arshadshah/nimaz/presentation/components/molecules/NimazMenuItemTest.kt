package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
class NimazMenuItemTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders title only and triggers click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazMenuItem(
                title = "Prayer Tracker",
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Prayer Tracker").assertExists()
        composeRule.onNodeWithText("Prayer Tracker").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `renders subtitle and icon when provided`() {
        composeRule.setThemedContent {
            NimazMenuItem(
                title = "Prayer Tracker",
                subtitle = "Track your daily prayers",
                icon = Icons.Default.Schedule,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Prayer Tracker").assertExists()
        composeRule.onNodeWithText("Track your daily prayers").assertExists()
    }

    @Test
    fun `does not render subtitle when null`() {
        composeRule.setThemedContent {
            NimazMenuItem(
                title = "Settings",
                subtitle = null,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun `disabled item does not invoke click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazMenuItem(
                title = "Disabled Item",
                onClick = { clicked = true },
                enabled = false
            )
        }
        composeRule.onNodeWithText("Disabled Item").assertExists()
        composeRule.onNodeWithText("Disabled Item").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `menu group renders its content`() {
        composeRule.setThemedContent {
            NimazMenuGroup {
                NimazMenuItem(
                    title = "Grouped Item",
                    onClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Grouped Item").assertExists()
    }
}
