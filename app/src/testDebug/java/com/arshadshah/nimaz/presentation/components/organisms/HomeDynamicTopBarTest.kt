package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeDynamicTopBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders location name at rest`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 0f,
                locationName = "Dublin, Ireland",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m",
                onSettingsClick = {}
            )
        }

        composeRule.onNodeWithText("Dublin, Ireland").assertExists()
    }

    @Test
    fun `empty location shows placeholder`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 0f,
                locationName = "",
                nextPrayer = null,
                nextPrayerTime = "",
                timeUntilNextPrayer = "—",
                onSettingsClick = {}
            )
        }

        composeRule.onNodeWithText("Set location").assertExists()
    }

    @Test
    fun `compact title shows next prayer name and countdown`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 1f,
                locationName = "Dublin, Ireland",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m",
                onSettingsClick = {}
            )
        }

        // PrayerType.ASR.displayName == "Asr"
        composeRule.onNodeWithText("Asr").assertExists()
        // Countdown rendered as "in 2h 15m"
        composeRule.onNodeWithText("in 2h 15m").assertExists()
    }

    @Test
    fun `compact title shows next prayer time with separator`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 1f,
                locationName = "",
                nextPrayer = PrayerType.MAGHRIB,
                nextPrayerTime = "6:12 PM",
                timeUntilNextPrayer = "1h 04m",
                onSettingsClick = {}
            )
        }

        composeRule.onNodeWithText("  ·  6:12 PM").assertExists()
    }

    @Test
    fun `null next prayer shows em dash placeholder`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 1f,
                locationName = "",
                nextPrayer = null,
                nextPrayerTime = "",
                timeUntilNextPrayer = "—",
                onSettingsClick = {}
            )
        }

        // CompactPrayerTitle renders "—" when nextPrayer is null
        composeRule.onNodeWithText("—").assertExists()
    }

    @Test
    fun `settings icon renders and click fires`() {
        var fired = false
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 0f,
                locationName = "Dublin, Ireland",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m",
                onSettingsClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Settings").assertExists()
        composeRule.onNodeWithContentDescription("Settings").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `progress beyond range is coerced and still renders`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 2f,
                locationName = "Dublin, Ireland",
                nextPrayer = PrayerType.FAJR,
                nextPrayerTime = "5:23 AM",
                timeUntilNextPrayer = "30m",
                onSettingsClick = {}
            )
        }

        // PrayerType.FAJR.displayName == "Fajr"
        composeRule.onNodeWithText("Fajr").assertExists()
    }
}
