package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
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
class PrayerTimesSectionHeaderTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders title`() {
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 2,
                upcomingCount = 3,
                onSettingsClick = {}
            )
        }
        composeRule.onNodeWithText("Prayer Times").assertExists()
    }

    @Test
    fun `renders mixed subtitle when both counts positive`() {
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 2,
                upcomingCount = 3,
                onSettingsClick = {}
            )
        }
        composeRule.onNodeWithText("2 passed · 3 to go").assertExists()
    }

    @Test
    fun `renders to-go subtitle when none passed`() {
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 0,
                upcomingCount = 5,
                onSettingsClick = {}
            )
        }
        composeRule.onNodeWithText("5 to go").assertExists()
    }

    @Test
    fun `renders all-done subtitle when none upcoming`() {
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 5,
                upcomingCount = 0,
                onSettingsClick = {}
            )
        }
        composeRule.onNodeWithText("all done for today").assertExists()
    }

    @Test
    fun `omits subtitle when both counts zero`() {
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 0,
                upcomingCount = 0,
                onSettingsClick = {}
            )
        }
        composeRule.onNodeWithText("Prayer Times").assertExists()
        composeRule.onNodeWithText("all done for today").assertDoesNotExist()
        composeRule.onNodeWithText("0 to go").assertDoesNotExist()
    }

    @Test
    fun `settings button invokes callback`() {
        var clicked = false
        composeRule.setThemedContent {
            PrayerTimesSectionHeader(
                passedCount = 2,
                upcomingCount = 3,
                onSettingsClick = { clicked = true }
            )
        }
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertThat(clicked).isTrue()
    }
}
