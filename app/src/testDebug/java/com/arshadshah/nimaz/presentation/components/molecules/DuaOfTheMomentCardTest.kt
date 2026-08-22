package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DuaOfTheMomentCardTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders title category arabic and translation`() {
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "اللَّهُمَّ",
                translation = "O Allah, protect us.",
                categoryLabel = "Morning Adhkar",
            )
        }
        composeRule.onNodeWithText("Dua of the Moment").assertExists()
        composeRule.onNodeWithText("Morning Adhkar").assertExists()
        composeRule.onNodeWithText("اللَّهُمَّ").assertExists()
        composeRule.onNodeWithText("O Allah, protect us.").assertExists()
    }

    @Test
    fun `renders source footer when provided`() {
        composeRule.setThemedContent {
            DuaOfTheMomentCard(
                arabic = "بِسْمِ اللَّهِ",
                translation = "In the name of Allah.",
                categoryLabel = "Evening Adhkar",
                source = "Sahih Muslim 2723",
            )
        }
        composeRule.onNodeWithText("Sahih Muslim 2723").assertExists()
    }
}
