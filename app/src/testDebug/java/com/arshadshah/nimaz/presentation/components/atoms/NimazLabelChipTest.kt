package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazLabelChipTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders label text`() {
        composeRule.setThemedContent { NimazLabelChip(text = "Sahih al-Bukhari") }
        composeRule.onNodeWithText("Sahih al-Bukhari").assertExists()
    }

    @Test
    fun `renders highlighted chip`() {
        composeRule.setThemedContent {
            NimazLabelChip(text = "Eid al-Fitr", highlighted = true)
        }
        composeRule.onNodeWithText("Eid al-Fitr").assertExists()
    }

    @Test
    fun `renders chip with leading icon`() {
        composeRule.setThemedContent {
            NimazLabelChip(text = "Repeat 3", icon = Icons.Default.Repeat)
        }
        composeRule.onNodeWithText("Repeat 3").assertExists()
    }
}
