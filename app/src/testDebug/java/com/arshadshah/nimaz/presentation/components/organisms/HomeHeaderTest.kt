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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
                onSettingsClick = {}
            )
        }

        // Falls back to "—" when no next prayer
        composeRule.onNodeWithText("—").assertExists()
    }
}

/** A fixed wall-clock instant today, so tests read like a real day. */
private fun testInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
