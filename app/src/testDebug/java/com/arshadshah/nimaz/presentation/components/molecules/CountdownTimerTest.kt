package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CountdownTimerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `parses full hms string into padded units`() {
        // Disable the infinite pulse animation clock so waitForIdle does not hang.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(timeUntilNextPrayer = "2h 15m 30s")
        }

        composeRule.onNodeWithText("02").assertExists()
        composeRule.onNodeWithText("15").assertExists()
        composeRule.onNodeWithText("30").assertExists()
    }

    @Test
    fun `missing units default to zero-padded value`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(timeUntilNextPrayer = "45s")
        }

        // Hours and minutes both absent -> two "00" boxes rendered.
        composeRule.onAllNodesWithText("00").assertCountEquals(2)
        composeRule.onNodeWithText("45").assertExists()
    }

    @Test
    fun `renders unit labels in uppercase`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(timeUntilNextPrayer = "1h 5m 9s")
        }

        composeRule.onNodeWithText("HOURS").assertExists()
        composeRule.onNodeWithText("MINUTES").assertExists()
        composeRule.onNodeWithText("SECONDS").assertExists()
    }
}
