package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `renders headline and body`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "Eid al-Fitr",
                arabic = "عيد مبارك",
                headline = "Eid Mubarak",
                body = "Thirty days behind you.",
            )
        }
        composeRule.onNodeWithText("Eid Mubarak").assertExists()
        composeRule.onNodeWithText("Thirty days behind you.").assertExists()
    }

    @Test
    fun `proof chip is hidden when proof is null`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                proof = null,
            )
        }
        composeRule.onNodeWithText("Al-Baqarah 2:185", substring = true).assertDoesNotExist()
    }

    @Test
    fun `proof chip renders ref and text when present`() {
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                proof = "Al-Baqarah 2:185" to "…complete the count.",
            )
        }
        composeRule.onNodeWithText("Al-Baqarah 2:185", substring = true).assertExists()
        composeRule.onNodeWithText("…complete the count.", substring = true).assertExists()
    }

    @Test
    fun `primary action fires onClick`() {
        var clicked = false
        composeRule.setThemedContent {
            EventCard(
                accent = Color(0xFF2E7D32),
                icon = Icons.Filled.Celebration,
                eyebrow = "e", arabic = null, headline = "h", body = "b",
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
                eyebrow = "e", arabic = null, headline = "h", body = "b",
                onDismiss = { dismissed = true },
            )
        }
        composeRule.onNodeWithContentDescription("Dismiss").performClick()
        assertThat(dismissed).isTrue()
    }
}
