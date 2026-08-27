package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders eyebrow and body`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "Eid al-Fitr",
                arabic = "عيد مبارك",
                body = "Thirty days behind you.",
            )
        }
        composeRule.onNodeWithText("Eid al-Fitr").assertExists()
        composeRule.onNodeWithText("Thirty days behind you.").assertExists()
    }

    @Test
    fun `primary action fires onClick`() {
        var clicked = false
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, body = "b",
                primaryAction = EventAction("Go") { clicked = true },
            )
        }
        composeRule.onNodeWithText("Go").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `dismiss button fires onDismiss and is hidden when null`() {
        var dismissed = false
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, body = "b",
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        assertThat(dismissed).isTrue()
    }
}
