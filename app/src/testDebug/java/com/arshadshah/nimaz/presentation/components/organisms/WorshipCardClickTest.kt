package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * The worship card's click wiring.
 *
 * This is the test that would have caught the card shipping inert. `WorshipEventCard` accepted an
 * `onAction` that `HomeScreen` never passed, so the card rendered a countdown and did nothing when
 * tapped — and an unwired callback fails no test. Rendering assertions all still passed.
 *
 * The card is also the whole tap target rather than a CTA button, so the assertion is that the
 * *card* carries the click action, not some child.
 */
@RunWith(RobolectricTestRunner::class)
class WorshipCardClickTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun card(type: WorshipReminderType) = WorshipCardUi(
        type = type,
        name = "Tahajjud",
        arabic = "تهجد",
        body = "The last third of the night",
        eventAt = Clock.System.now() + 3.hours,
    )

    @Test
    fun `tapping the card reports its reminder type`() {
        val tapped = mutableListOf<WorshipReminderType>()
        composeRule.setThemedContent {
            WorshipEventCard(
                card = card(WorshipReminderType.TAHAJJUD),
                onAction = { tapped += it },
            )
        }

        composeRule.onNodeWithTag(WorshipCardTestTag).performClick()

        assertEquals(
            "The handler must receive the type so navigation can route by it",
            listOf(WorshipReminderType.TAHAJJUD),
            tapped,
        )
    }

    @Test
    fun `the whole card carries the click action`() {
        composeRule.setThemedContent {
            WorshipEventCard(card = card(WorshipReminderType.ADHKAR_EVENING), onAction = {})
        }

        composeRule.onNodeWithTag(WorshipCardTestTag).assertHasClickAction()
    }

    /**
     * With no handler the card must not advertise a tap it cannot honour — a ripple that leads
     * nowhere is worse than a plainly static card.
     */
    @Test
    fun `without a handler the card is not clickable`() {
        composeRule.setThemedContent {
            WorshipEventCard(card = card(WorshipReminderType.WITR), onAction = null)
        }

        val clickable = composeRule.onAllNodesWithTag(WorshipCardTestTag).fetchSemanticsNodes()
        assertTrue("Card should carry no click affordance without a handler", clickable.isEmpty())
    }

    /**
     * Screen readers announce the destination, not just "button". The card has no visible CTA, so
     * without an onClickLabel a non-sighted user gets a tappable card with no idea where it leads.
     */
    @Test
    fun `the click action is labelled for screen readers`() {
        composeRule.setThemedContent {
            WorshipEventCard(card = card(WorshipReminderType.ADHKAR_EVENING), onAction = {})
        }

        val label = composeRule.onNodeWithTag(WorshipCardTestTag)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.OnClick)
            ?.label

        assertNotNull("Worship card click has no accessibility label", label)
        assertTrue("Label should describe the destination", !label.isNullOrBlank())
    }
}
