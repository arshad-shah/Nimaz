package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazLegendItemTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `legend item renders dot and label`() {
        composeRule.setThemedContent {
            NimazLegendItem(color = Color(0xFF22C55E), label = "Fasted")
        }
        composeRule.onNodeWithText("Fasted").assertExists()
    }

    @Test
    fun `legend item renders with custom dot size`() {
        composeRule.setThemedContent {
            NimazLegendItem(
                color = Color.Red,
                label = "Missed",
                dotSize = 12.dp
            )
        }
        composeRule.onNodeWithText("Missed").assertExists()
    }
}
