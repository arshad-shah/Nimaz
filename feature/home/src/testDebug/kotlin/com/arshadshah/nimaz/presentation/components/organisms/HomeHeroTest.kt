package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.PrayerType
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.onAllNodesWithText

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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
            )
        }

        // LocalUseHijriPrimary defaults to false, so the sky shows the Gregorian
        // date as its single date line.
        composeRule.onNodeWithText("Friday, January 31, 2026").assertExists()
    }

    @Test
    fun `renders the prayer clock time`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.MAGHRIB,
                nextPrayerAt = testInstant(18, 12),
            )
        }

        // R.string.at_time == "at %s"
        composeRule.onNodeWithText("at 6:12 PM").assertExists()
    }

    @Test
    fun `renders the countdown`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = PrayerType.ASR,
                // Relative to *now*, not a fixed wall-clock time. `testInstant(16, 30)` meant
                // "today at 16:30", so this test passed every morning and failed every afternoon:
                // once that moment is past the countdown correctly renders "Now" and there is no
                // " m" to find. The countdown is derived from the real clock at the leaf, so a
                // target must be expressed relative to it.
                nextPrayerAt = Clock.System.now() + 2.hours,
            )
        }

        // Assert a countdown rendered, not its digits — they move while the test runs.
        composeRule.onAllNodesWithText(" m", substring = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) }
    }

    @Test
    fun `null next prayer shows the em dash placeholder`() {
        composeRule.setThemedContent {
            HomeHero(
                hijriDate = "7 Rajab 1446",
                gregorianDate = "Friday, January 31, 2026",
                nextPrayer = null,
                nextPrayerAt = testInstant(16, 30),
            )
        }

        // nextPrayer?.displayName ?: "—"
        composeRule.onNodeWithText("—").assertExists()
    }
}

/** A fixed wall-clock instant today, so tests read like a real day. */
private fun testInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
