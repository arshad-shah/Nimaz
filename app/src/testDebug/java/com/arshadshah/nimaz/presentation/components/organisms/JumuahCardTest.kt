package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JumuahCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders the jumuah header and khutbah time`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahTime = "1:30 PM",
                timeUntilJumuah = "3h 15m",
                isJumuahPassed = false
            )
        }

        // R.string.jumuah_mubarak == "Jumu'ah Mubarak"
        composeRule.onNodeWithText("Jumu'ah Mubarak").assertExists()
        composeRule.onNodeWithText("1:30 PM").assertExists()
        // R.string.khutbah_time == "Khutbah time"
        composeRule.onNodeWithText("Khutbah time").assertExists()
    }

    @Test
    fun `upcoming jumuah shows the countdown row`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahTime = "1:30 PM",
                timeUntilJumuah = "3h 15m",
                isJumuahPassed = false
            )
        }

        // R.string.time_until_jumuah == "Time until Jumu'ah"
        composeRule.onNodeWithText("Time until Jumu'ah").assertExists()
        composeRule.onNodeWithText("3h 15m").assertExists()
        // The passed acknowledgement should not appear.
        composeRule.onNodeWithText("Jumu'ah prayer time has passed").assertDoesNotExist()
    }

    @Test
    fun `passed jumuah shows the passed acknowledgement`() {
        composeRule.setThemedContent {
            JumuahCard(
                jumuahTime = "1:30 PM",
                timeUntilJumuah = "",
                isJumuahPassed = true
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
                jumuahTime = "",
                timeUntilJumuah = "3h 15m",
                isJumuahPassed = false
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
                jumuahTime = "1:30 PM",
                timeUntilJumuah = "3h 15m",
                isJumuahPassed = false
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
