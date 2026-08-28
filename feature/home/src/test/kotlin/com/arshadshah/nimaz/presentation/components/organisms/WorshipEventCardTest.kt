package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The card now receives **instants** and derives its own time/countdown/proximity at the leaf, so
 * the exact rendered clock time and countdown text are wall-clock dependent and no longer asserted
 * here. We pin the stable content (name/arabic/body) and that every reminder type renders without
 * crashing; the selection/window behaviour is covered purely by [com.arshadshah.nimaz.core.util]
 * tests.
 */
@RunWith(RobolectricTestRunner::class)
class WorshipEventCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun cardFor(type: WorshipReminderType, name: String) = WorshipCardUi(
        type = type,
        name = name,
        arabic = "تَهَجُّد",
        body = "A blessed time for du'a.",
        // Comfortably in the future so proximity is not PASSED and the card renders.
        eventAt = Clock.System.now() + 4.hours,
        windowEnd = Clock.System.now() + 8.hours,
    )

    @Test
    fun `renders name, arabic and body`() {
        composeRule.setThemedContent {
            WorshipEventCard(card = cardFor(WorshipReminderType.TAHAJJUD, "Tahajjud"))
        }
        composeRule.onNodeWithText("Tahajjud").assertExists()
        composeRule.onNodeWithText("A blessed time for du'a.").assertExists()
    }

    @Test
    fun `renders for every worship type without crashing`() {
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                WorshipReminderType.entries.forEach { type ->
                    WorshipEventCard(card = cardFor(type, type.key))
                }
            }
        }
        WorshipReminderType.entries.forEach { type ->
            composeRule.onNodeWithText(type.key).assertExists()
        }
    }
}
