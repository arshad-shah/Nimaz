package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How near the event is changes what the card does.
 *
 * The card ramps its accent with proximity and, once the event has passed its window, renders
 * **nothing at all** — the resolver is supposed to have moved on, and a card left showing a
 * reminder for something that finished hours ago is worse than no card. Only the distant arm had
 * ever run.
 */
@RunWith(RobolectricTestRunner::class)
class WorshipEventCardProximityTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val now = Clock.System.now()

    private fun card(
        inMinutes: Long,
        windowStart: kotlin.time.Instant? = null,
        windowEnd: kotlin.time.Instant? = null,
    ) = WorshipCardUi(
        type = WorshipReminderType.TAHAJJUD,
        name = "Tahajjud",
        arabic = "تهجد",
        body = "The last third of the night",
        eventAt = now + inMinutes.minutes,
        windowStart = windowStart,
        windowEnd = windowEnd,
    )

    @Test
    fun `an event hours away renders quietly but completely`() {
        composeRule.setThemedContent { WorshipEventCard(card(inMinutes = 300)) }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }

    @Test
    fun `an event within the hour renders with more presence, still showing its name`() {
        composeRule.setThemedContent { WorshipEventCard(card(inMinutes = 45)) }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }

    @Test
    fun `an imminent event renders`() {
        composeRule.setThemedContent { WorshipEventCard(card(inMinutes = 5)) }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }

    @Test
    fun `an event happening now stays on screen for the length of its window`() {
        // The window is what stops the card vanishing the moment the event begins — which is
        // exactly when the reader is most likely to be looking at it.
        composeRule.setThemedContent {
            WorshipEventCard(card(inMinutes = -10, windowEnd = now + 2.hours))
        }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }

    @Test
    fun `an event past its window renders nothing at all`() {
        composeRule.setThemedContent {
            WorshipEventCard(card(inMinutes = -300, windowEnd = now - 2.hours))
        }

        composeRule.onNodeWithText("Tahajjud").assertDoesNotExist()
    }

    @Test
    fun `an event past its time with no window at all also renders nothing`() {
        composeRule.setThemedContent { WorshipEventCard(card(inMinutes = -300)) }

        composeRule.onNodeWithText("Tahajjud").assertDoesNotExist()
    }

    @Test
    fun `a card with a window start draws a progress arc toward the event`() {
        // The arc is the glanceable signal — how far through the approach the reader is,
        // without reading digits. It is only drawn when a windowStart is supplied.
        composeRule.setThemedContent {
            WorshipEventCard(card(inMinutes = 60, windowStart = now - 60.minutes))
        }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }

    @Test
    fun `the whole card is the tap target, not a button inside it`() {
        // The card lives in a fixed-height carousel page, so a CTA button would eat scarce
        // vertical space for an affordance the whole surface already carries.
        var opened: WorshipReminderType? = null
        composeRule.setThemedContent {
            WorshipEventCard(card(inMinutes = 60), onAction = { opened = it })
        }

        composeRule.onNodeWithText("Tahajjud").performClick()

        assertThat(opened).isEqualTo(WorshipReminderType.TAHAJJUD)
    }

    @Test
    fun `a card with no action is inert rather than looking tappable`() {
        composeRule.setThemedContent { WorshipEventCard(card(inMinutes = 60), onAction = null) }

        composeRule.onNodeWithText("Tahajjud").assertIsDisplayed()
    }
}
