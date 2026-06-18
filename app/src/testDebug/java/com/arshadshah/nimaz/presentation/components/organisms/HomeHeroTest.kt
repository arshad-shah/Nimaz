package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeHeroTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders the next prayer name and label`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s"
            )
        }

        // R.string.next_prayer == "NEXT PRAYER"
        composeRule.onNodeWithText("NEXT PRAYER").assertExists()
        // PrayerType.ASR.displayName == "Asr"
        composeRule.onNodeWithText("Asr").assertExists()
    }

    @Test
    fun `renders the gregorian date when hijri is not primary`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s"
            )
        }

        // LocalUseHijriPrimary defaults to false, so gregorian is the primary
        // line and hijri the secondary line; both render.
        composeRule.onNodeWithText("Friday, January 31, 2026").assertExists()
        composeRule.onNodeWithText("7 Rajab 1446").assertExists()
    }

    @Test
    fun `renders the prayer clock time`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.MAGHRIB,
                nextPrayerTime = "6:12 PM",
                timeUntilNextPrayer = "0h 45m 0s"
            )
        }

        // R.string.at_time == "at %s"
        composeRule.onNodeWithText("at 6:12 PM").assertExists()
    }

    @Test
    fun `renders the countdown digits`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                nextPrayerTime = "4:30 PM",
                timeUntilNextPrayer = "2h 15m 30s"
            )
        }

        // CountdownTimer pads each unit to two digits: 2h -> "02", 15m -> "15".
        composeRule.onNodeWithText("02").assertExists()
        composeRule.onNodeWithText("15").assertExists()
    }

    @Test
    fun `null next prayer shows the em dash placeholder`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = null,
                nextPrayerTime = "",
                timeUntilNextPrayer = ""
            )
        }

        // nextPrayer?.displayName ?: "—"
        composeRule.onNodeWithText("—").assertExists()
    }
}
