package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameDetailHeaderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders number, arabic, primary and secondary`() {
        composeRule.setThemedContent {
            NameDetailHeader(
                arabicName = "ArabicRahman",
                accent = NamesAccents.allah(),
                number = 52,
                primaryLabel = "Ar-Rahman",
                secondaryLabel = "The Most Compassionate",
            )
        }
        composeRule.onNodeWithText("52").assertExists()
        composeRule.onNodeWithText("ArabicRahman").assertExists()
        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        composeRule.onNodeWithText("The Most Compassionate").assertExists()
    }

    @Test
    fun `omits number when null`() {
        composeRule.setThemedContent {
            NameDetailHeader(
                arabicName = "ArabicMuhammad",
                accent = NamesAccents.prophets(),
                number = null,
                primaryLabel = "Muhammad",
                secondaryLabel = "Seal of the Prophets",
            )
        }
        composeRule.onNodeWithText("Muhammad").assertExists()
        composeRule.onNodeWithText("Seal of the Prophets").assertExists()
    }
}
