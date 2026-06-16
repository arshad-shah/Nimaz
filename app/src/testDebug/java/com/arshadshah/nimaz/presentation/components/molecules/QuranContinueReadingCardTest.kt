package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranContinueReadingCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleSurah() = Surah(
        number = 1,
        nameArabic = "ArabicFatihah",
        nameEnglish = "Al-Fatihah",
        nameTransliteration = "The Opening",
        revelationType = RevelationType.MECCAN,
        ayahCount = 7,
        juzStart = 1,
        orderInMushaf = 5,
        startPage = 1
    )

    @Test
    fun `renders with surah model and progress percent`() {
        composeRule.setThemedContent {
            ContinueReadingCard(
                surahNumber = 1,
                ayahNumber = 5,
                juzNumber = 1,
                pageNumber = 1,
                totalAyahsRead = 150,
                surahName = sampleSurah(),
                onClick = {}
            )
        }
        composeRule.onNodeWithText("CONTINUE READING").assertExists()
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("ArabicFatihah").assertExists()
        composeRule.onNodeWithText("Verse 5").assertExists()
        composeRule.onNodeWithText("Juz 1").assertExists()
        composeRule.onNodeWithText("Page 1").assertExists()
        // 150/6236 * 100 = 2
        composeRule.onNodeWithText("2%").assertExists()
    }

    @Test
    fun `renders fallback surah name when model is null`() {
        composeRule.setThemedContent {
            ContinueReadingCard(
                surahNumber = 36,
                ayahNumber = 10,
                juzNumber = 22,
                pageNumber = 440,
                totalAyahsRead = 0,
                surahName = null,
                onClick = {}
            )
        }
        // R.string.quran_home_surah_fallback -> "Surah %1$d"
        composeRule.onNodeWithText("Surah 36").assertExists()
        composeRule.onNodeWithText("Verse 10").assertExists()
        composeRule.onNodeWithText("0%").assertExists()
        // Arabic name only rendered when surahName != null; the fallback path
        // omits it. (No specific arabic assertion needed; existence of fallback
        // english name confirms the null branch.)
    }

    @Test
    fun `progress is clamped to 100 percent when read exceeds total`() {
        composeRule.setThemedContent {
            ContinueReadingCard(
                surahNumber = 1,
                ayahNumber = 1,
                juzNumber = 1,
                pageNumber = 1,
                totalAyahsRead = 99999,
                surahName = null,
                onClick = {}
            )
        }
        composeRule.onNodeWithText("100%").assertExists()
    }

    @Test
    fun `click invokes callback`() {
        var clicked = false
        composeRule.setThemedContent {
            ContinueReadingCard(
                surahNumber = 1,
                ayahNumber = 5,
                juzNumber = 1,
                pageNumber = 1,
                totalAyahsRead = 150,
                surahName = sampleSurah(),
                onClick = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Al-Fatihah").performClick()
        assertThat(clicked).isTrue()
    }
}
