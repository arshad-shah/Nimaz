package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@RunWith(RobolectricTestRunner::class)
class JumuahCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders the jumuah header and khutbah time`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahAt = testInstant(13, 30),
            )
        }

        // R.string.jumuah_mubarak == "Jumu'ah Mubarak"
        composeRule.onNodeWithText("Jumu'ah Mubarak").assertExists()
        // Time is formatted at the leaf (12h default in tests).
        composeRule.onNodeWithText("1:30 PM").assertExists()
        // R.string.khutbah_time == "Khutbah time"
        composeRule.onNodeWithText("Khutbah time").assertExists()
    }

    @Test
    fun `upcoming jumuah shows the countdown row`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahAt = Clock.System.now() + 3.hours,
            )
        }

        // R.string.time_until_jumuah == "Time until Jumu'ah"
        composeRule.onNodeWithText("Time until Jumu'ah").assertExists()
        // The countdown ticks off the real clock; assert the row, not the digits.
        // The passed acknowledgement should not appear.
        composeRule.onNodeWithText("Jumu'ah prayer time has passed").assertDoesNotExist()
    }

    @Test
    fun `passed jumuah shows the passed acknowledgement`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahAt = Clock.System.now() - 1.hours,
            )
        }

        // R.string.jumuah_passed == "Jumu'ah prayer time has passed"
        composeRule.onNodeWithText("Jumu'ah prayer time has passed").assertExists()
        // The countdown label should not appear when passed.
        composeRule.onNodeWithText("Time until Jumu'ah").assertDoesNotExist()
    }

    @Test
    fun `empty jumuah time hides the time column`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahAt = null,
            )
        }

        // jumuahTime is empty -> the time/Khutbah column is not rendered.
        composeRule.onNodeWithText("Khutbah time").assertDoesNotExist()
        // Header still renders.
        composeRule.onNodeWithText("Jumu'ah Mubarak").assertExists()
    }

    @Test
    fun `renders the hadith quote`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahAt = Clock.System.now() + 3.hours,
            )
        }

        // R.string.jumuah_hadith_quote. The unescaped double quotes in the
        // resource are whitespace-preservation markers and are stripped by the
        // Android resource parser, so the rendered text carries no quote chars.
        composeRule.onNodeWithText(
            "The best day on which the sun rises is Friday. — Sahih Muslim"
        ).assertExists()
    }
}

/** A fixed wall-clock instant today, so tests read like a real day. */
private fun testInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
