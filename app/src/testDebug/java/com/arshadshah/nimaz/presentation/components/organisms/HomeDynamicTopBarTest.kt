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
import androidx.compose.ui.test.onAllNodesWithText

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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
                onSettingsClick = {}
            )
        }

        // PrayerType.ASR.displayName == "Asr"
        composeRule.onNodeWithText("Asr").assertExists()
        // Countdown rendered as "in 2h 15m"
        // The countdown is derived from the real clock at the leaf, so its exact digits are not
        // assertable; pin that the "in <countdown>" line rendered at all.
        composeRule.onAllNodesWithText("in ", substring = true)
            .fetchSemanticsNodes().isNotEmpty().let { assert(it) }
    }

    @Test
    fun `compact title shows next prayer time with separator`() {
        composeRule.setThemedContent {
            HomeDynamicTopBar(
                transitionProgress = 1f,
                locationName = "",
                nextPrayer = PrayerType.MAGHRIB,
                nextPrayerAt = testInstant(18, 12),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
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
                nextPrayerAt = testInstant(16, 30),
                onSettingsClick = {}
            )
        }

        // PrayerType.FAJR.displayName == "Fajr"
        composeRule.onNodeWithText("Fajr").assertExists()
    }
}

/** A fixed wall-clock instant today, so tests read like a real day. */
private fun testInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
