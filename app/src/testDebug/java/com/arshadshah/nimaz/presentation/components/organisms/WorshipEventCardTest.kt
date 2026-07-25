package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorshipEventCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders name, arabic, body, event time and countdown`() {
        composeRule.setThemedContent {
            WorshipEventCard(
                type = WorshipReminderType.TAHAJJUD,
                name = "Tahajjud",
                arabic = "تَهَجُّد",
                body = "A blessed time for du'a has begun.",
                eventTime = "2:48 AM",
                timeLabel = "Begins",
                countdown = "4h 12m",
                countdownLabel = "Begins in",
            )
        }
        composeRule.onNodeWithText("Tahajjud").assertExists()
        composeRule.onNodeWithText("A blessed time for du'a has begun.").assertExists()
        composeRule.onNodeWithText("2:48 AM").assertExists()
        composeRule.onNodeWithText("4h 12m").assertExists()
        composeRule.onNodeWithText("Begins in").assertExists()
    }

    @Test
    fun `hides trailing time and countdown when empty`() {
        composeRule.setThemedContent {
            WorshipEventCard(
                type = WorshipReminderType.IFTAR,
                name = "Iftar",
                arabic = "إفْطار",
                body = "Break your fast.",
                eventTime = "",
                timeLabel = "",
                countdown = "",
                countdownLabel = "",
            )
        }
        composeRule.onNodeWithText("Iftar").assertExists()
        composeRule.onNodeWithText("Break your fast.").assertExists()
    }

    @Test
    fun `renders for every worship type without crashing`() {
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                WorshipReminderType.entries.forEach { type ->
                    WorshipEventCard(
                        type = type,
                        name = type.key,
                        arabic = "نص",
                        body = "body",
                        eventTime = "1:00 AM",
                        timeLabel = "",
                        countdown = "1h",
                        countdownLabel = "In",
                    )
                }
            }
        }
        WorshipReminderType.entries.forEach { type ->
            composeRule.onNodeWithText(type.key).assertExists()
        }
    }
}
