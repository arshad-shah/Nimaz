package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The timer now counts down to an [kotlin.time.Instant] and derives its digits numerically, so the
 * old string-parsing assertions are gone. The seconds box is wall-clock racy (it may tick between
 * composition and assertion), so only the stable hour/minute boxes and the unit labels are pinned.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownTimerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `shows padded hour and minute boxes for the remaining time`() {
        // Disable the infinite pulse animation clock so waitForIdle does not hang.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(target = Clock.System.now() + 2.hours + 15.minutes + 30.seconds)
        }

        composeRule.onNodeWithText("02").assertExists()
        composeRule.onNodeWithText("15").assertExists()
    }

    @Test
    fun `absent hours and minutes render as two zero-padded boxes`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(target = Clock.System.now() + 45.seconds)
        }

        // Hours and minutes both absent -> two "00" boxes rendered (seconds box is non-zero).
        composeRule.onAllNodesWithText("00").assertCountEquals(2)
    }

    @Test
    fun `renders unit labels in uppercase`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(target = Clock.System.now() + 1.hours + 5.minutes + 9.seconds)
        }

        composeRule.onNodeWithText("HOURS").assertExists()
        composeRule.onNodeWithText("MINUTES").assertExists()
        composeRule.onNodeWithText("SECONDS").assertExists()
    }

    @Test
    fun `null target renders all zero boxes`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            CountdownTimer(target = null)
        }

        composeRule.onAllNodesWithText("00").assertCountEquals(3)
    }
}
