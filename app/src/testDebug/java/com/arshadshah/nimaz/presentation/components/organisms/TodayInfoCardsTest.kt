package com.arshadshah.nimaz.presentation.components.organisms

import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent

import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayInfoCardsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders fasting status card`() {
        composeRule.setThemedContent {
            TodayInfoCards(
                fastingToday = true,
                dailyHadith = null
            )
        }

        // FastingStatusCard header label (R.string.fasting)
        composeRule.onNodeWithText("Fasting").assertExists()
    }

    @Test
    fun `fasting today shows the today fasting substatus`() {
        composeRule.setThemedContent {
            TodayInfoCards(
                fastingToday = true,
                dailyHadith = null
            )
        }

        // R.string.today_fasting = "Fasting today"
        composeRule.onNodeWithText("Fasting today").assertExists()
    }

    @Test
    fun `not fasting shows the no fast substatus`() {
        composeRule.setThemedContent {
            TodayInfoCards(
                fastingToday = false,
                dailyHadith = null
            )
        }

        // R.string.no_fast_today = "No fast today"
        composeRule.onNodeWithText("No fast today").assertExists()
    }

    @Test
    fun `shows hadith card when dailyHadith is present`() {
        composeRule.setThemedContent {
            TodayInfoCards(
                fastingToday = false,
                dailyHadith = "A sample hadith for today"
            )
        }

        // R.string.hadith_of_the_day = "Hadith of the Day"
        composeRule.onNodeWithText("Hadith of the Day").assertExists()
        composeRule.onNodeWithText("A sample hadith for today").assertExists()
    }

    @Test
    fun `hides hadith card when dailyHadith is null`() {
        composeRule.setThemedContent {
            TodayInfoCards(
                fastingToday = false,
                dailyHadith = null
            )
        }

        composeRule.onNodeWithText("Hadith of the Day").assertDoesNotExist()
    }
}
