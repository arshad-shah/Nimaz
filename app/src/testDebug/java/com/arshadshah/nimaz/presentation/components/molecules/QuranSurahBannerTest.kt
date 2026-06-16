package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.RevelationType
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
                surahNameEnglish = "Al-Fatihah",
                surahMeaning = "The Opening",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                showBismillah = true
            )
        }
        composeRule.onNodeWithText("ArabicName").assertExists()
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("The Opening").assertExists()
        composeRule.onNodeWithText("Meccan").assertExists()
        composeRule.onNodeWithText("7 Ayahs").assertExists()
    }

    @Test
    fun `renders medinan surah without bismillah`() {
        composeRule.setThemedContent {
            SurahBanner(
                surahNameArabic = "ArabicName2",
                surahNameEnglish = "Al-Baqarah",
                surahMeaning = "The Cow",
                revelationType = RevelationType.MEDINAN,
                ayahCount = 286,
                showBismillah = false
            )
        }
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        composeRule.onNodeWithText("Medinan").assertExists()
        composeRule.onNodeWithText("286 Ayahs").assertExists()
        // Meccan label must not be present for a medinan surah
        composeRule.onNodeWithText("Meccan").assertDoesNotExist()
    }
}
