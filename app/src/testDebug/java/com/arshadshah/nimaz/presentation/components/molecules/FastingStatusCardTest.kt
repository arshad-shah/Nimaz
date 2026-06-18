package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FastingStatusCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `shows fasting substatus when fasting today`() {
        composeRule.setThemedContent {
            FastingStatusCard(fastingToday = true)
        }

        composeRule.onNodeWithText("Fasting").assertExists()
        composeRule.onNodeWithText("Fasting today").assertExists()
    }

    @Test
    fun `shows no-fast substatus when not fasting`() {
        composeRule.setThemedContent {
            FastingStatusCard(fastingToday = false)
        }

        composeRule.onNodeWithText("No fast today").assertExists()
    }

    @Test
    fun `renders with fillHeight enabled`() {
        composeRule.setThemedContent {
            FastingStatusCard(fastingToday = true, fillHeight = true)
        }

        composeRule.onNodeWithText("Fasting").assertExists()
    }
}
