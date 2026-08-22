package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranFrameTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `study frame renders provided content`() {
        composeRule.setThemedContent {
            QuranFrame(variant = QuranFrameVariant.STUDY) {
                Text("FramedContent")
            }
        }

        composeRule.onNodeWithText("FramedContent").assertIsDisplayed()
    }

    @Test
    fun `reader frame renders provided content`() {
        composeRule.setThemedContent {
            QuranFrame(variant = QuranFrameVariant.READER) {
                Text("ReaderContent")
            }
        }

        composeRule.onNodeWithText("ReaderContent").assertIsDisplayed()
    }

    @Test
    fun `medallion footer shows the supplied number`() {
        composeRule.setThemedContent {
            QuranFrame(variant = QuranFrameVariant.STUDY, number = 604) {
                Text("PagedContent")
            }
        }

        composeRule.onNodeWithText("604").assertIsDisplayed()
    }

    @Test
    fun `ornamental divider composes standalone`() {
        composeRule.setThemedContent {
            QuranOrnamentalDivider()
            Text("DividerSibling")
        }

        composeRule.onNodeWithText("DividerSibling").assertExists()
    }
}
