package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurahBannerTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders meccan surah with bismillah shown`() {
        composeRule.setThemedContent {
            SurahBanner(
                surahNameArabic = "ArabicName",
                surahMeaning = "The Opening",
                revelationType = RevelationType.MECCAN,
                showBismillah = true
            )
        }
        composeRule.onNodeWithText("ArabicName").assertExists()
        composeRule.onNodeWithText("The Opening").assertExists()
        composeRule.onNodeWithText("Meccan").assertExists()
    }

    @Test
    fun `renders medinan surah without bismillah`() {
        composeRule.setThemedContent {
            SurahBanner(
                surahNameArabic = "ArabicName2",
                surahMeaning = "The Cow",
                revelationType = RevelationType.MEDINAN,
                showBismillah = false
            )
        }
        composeRule.onNodeWithText("The Cow").assertExists()
        composeRule.onNodeWithText("Medinan").assertExists()
        // Meccan label must not be present for a medinan surah
        composeRule.onNodeWithText("Meccan").assertDoesNotExist()
    }
}
