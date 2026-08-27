package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HarakatArabicTextTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders arabic text`() {
        composeRule.setThemedContent {
            HarakatArabicText(text = "بَ", highlightGroup = "fatha")
        }
        composeRule.onNodeWithText("بَ").assertExists()
    }

    @Test
    fun `renders with null highlight group`() {
        composeRule.setThemedContent {
            HarakatArabicText(text = "بَا", highlightGroup = null)
        }
        composeRule.onNodeWithText("بَا").assertExists()
    }

    @Test
    fun `renders kasra group while playing`() {
        composeRule.setThemedContent {
            HarakatArabicText(text = "بِ", highlightGroup = "kasra", playing = true)
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
