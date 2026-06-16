package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HadithOfTheDayCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders header and hadith body in default mode`() {
        val hadith = "The best of you are those who learn the Quran."
        composeRule.setThemedContent {
            HadithOfTheDayCard(hadith = hadith)
        }
        composeRule.onNodeWithText("Hadith of the Day").assertExists()
        composeRule.onNodeWithText(hadith).assertExists()
    }

    @Test
    fun `renders in fill height capped mode`() {
        val hadith = "A capped hadith body that should ellipsize in carousel mode."
        composeRule.setThemedContent {
            HadithOfTheDayCard(
                hadith = hadith,
                fillHeight = true,
                maxLines = 2
            )
        }
        composeRule.onNodeWithText("Hadith of the Day").assertExists()
        composeRule.onNodeWithText(hadith).assertExists()
    }
}
