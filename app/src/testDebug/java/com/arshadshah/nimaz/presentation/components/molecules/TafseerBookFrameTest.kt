package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerBookFrameTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `frame renders provided content`() {
        composeRule.setThemedContent {
            TafseerBookFrame {
                Text("FramedContent")
            }
        }

        composeRule.onNodeWithText("FramedContent").assertIsDisplayed()
    }

    @Test
    fun `ornamental divider composes standalone`() {
        composeRule.setThemedContent {
            TafseerOrnamentalDivider()
            Text("DividerSibling")
        }

        composeRule.onNodeWithText("DividerSibling").assertExists()
    }
}
