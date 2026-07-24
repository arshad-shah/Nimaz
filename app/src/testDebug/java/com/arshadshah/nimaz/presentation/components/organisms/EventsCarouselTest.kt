package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventsCarouselTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders the first page eyebrow and body`() {
        composeRule.setThemedContent {
            EventsCarousel(
                events = listOf(
                    EventCardUi(
                        occasion = EventOccasion.GENERIC,
                        eyebrow = "Occasion",
                        body = "A warm line.",
                    )
                )
            )
        }
        composeRule.onNodeWithText("Occasion").assertExists()
        composeRule.onNodeWithText("A warm line.").assertExists()
    }

    @Test
    fun `renders nothing when the list is empty`() {
        composeRule.setThemedContent {
            EventsCarousel(events = emptyList())
        }
        composeRule.onNodeWithText("Occasion").assertDoesNotExist()
    }
}
