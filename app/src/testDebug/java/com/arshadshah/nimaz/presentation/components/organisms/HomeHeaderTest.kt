package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeHeaderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders location name`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        composeRule.onNodeWithText("Dublin, Ireland").assertExists()
    }

    @Test
    fun `renders gregorian and hijri dates`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        // Default LocalUseHijriPrimary is false: gregorian is primary, hijri secondary;
        // both are rendered as Text nodes.
        composeRule.onNodeWithText("Friday, January 31, 2026").assertExists()
        composeRule.onNodeWithText("7 Rajab 1446").assertExists()
    }

    @Test
    fun `renders the NEXT PRAYER label`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        // R.string.next_prayer = "NEXT PRAYER"
        composeRule.onNodeWithText("NEXT PRAYER").assertExists()
    }

    @Test
    fun `renders next prayer display name`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        // PrayerType.ASR.displayName == "Asr"
        composeRule.onNodeWithText("Asr").assertExists()
    }

    @Test
    fun `renders next prayer time using the at format`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        // R.string.at_time = "at %s" -> "at 4:30 PM"
        composeRule.onNodeWithText("at 4:30 PM").assertExists()
    }

    @Test
    fun `renders placeholder dash when nextPrayer is null`() {
        composeRule.setThemedContent {
            HomeHeader(
                locationName = "Dublin, Ireland",
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = null,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s",
                onSettingsClick = {}
            )
        }

        // Falls back to "—" when no next prayer
        composeRule.onNodeWithText("—").assertExists()
    }
}
