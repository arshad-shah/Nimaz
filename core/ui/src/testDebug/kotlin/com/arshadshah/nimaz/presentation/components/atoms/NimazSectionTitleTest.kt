package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.material3.Text
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSectionTitleTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `section title uppercases text by default`() {
        composeRule.setThemedContent {
            NimazSectionTitle(text = "Display Options")
        }
        composeRule.onNodeWithText("DISPLAY OPTIONS").assertExists()
    }

    @Test
    fun `section title preserves case and shows trailing content`() {
        composeRule.setThemedContent {
            NimazSectionTitle(
                text = "Links",
                uppercase = false,
                trailingContent = { Text("More") }
            )
        }
        composeRule.onNodeWithText("Links").assertExists()
        composeRule.onNodeWithText("More").assertExists()
    }
}
