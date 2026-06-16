package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazPillTabsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders all tab labels`() {
        composeRule.setThemedContent {
            NimazPillTabs(
                tabs = listOf("Daily", "Weekly", "Monthly"),
                selectedIndex = 0,
                onTabSelect = {}
            )
        }

        composeRule.onNodeWithText("Daily").assertExists()
        composeRule.onNodeWithText("Weekly").assertExists()
        composeRule.onNodeWithText("Monthly").assertExists()
    }

    @Test
    fun `clicking a tab reports its index`() {
        var selected = -1
        composeRule.setThemedContent {
            NimazPillTabs(
                tabs = listOf("Daily", "Weekly", "Monthly"),
                selectedIndex = 0,
                onTabSelect = { selected = it }
            )
        }

        composeRule.onNodeWithText("Weekly").performClick()
        assertThat(selected).isEqualTo(1)
    }

    @Test
    fun `clicking the last tab reports the last index`() {
        var selected = -1
        composeRule.setThemedContent {
            NimazPillTabs(
                tabs = listOf("Daily", "Weekly", "Monthly"),
                selectedIndex = 0,
                onTabSelect = { selected = it }
            )
        }

        composeRule.onNodeWithText("Monthly").performClick()
        assertThat(selected).isEqualTo(2)
    }

    @Test
    fun `renders with a non-zero selected index`() {
        composeRule.setThemedContent {
            NimazPillTabs(
                tabs = listOf("Daily", "Weekly", "Monthly"),
                selectedIndex = 1,
                onTabSelect = {}
            )
        }

        composeRule.onNodeWithText("Weekly").assertExists()
    }
}
