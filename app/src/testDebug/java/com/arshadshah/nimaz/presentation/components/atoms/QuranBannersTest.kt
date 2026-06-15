package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranBannersTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `juz banner renders title and subtitle`() {
        composeRule.setThemedContent {
            JuzPageBanner(title = "Juz 1", subtitle = "Al-Fatihah - Al-Baqarah")
        }
        composeRule.onNodeWithText("Juz 1").assertExists()
        composeRule.onNodeWithText("Al-Fatihah - Al-Baqarah").assertExists()
    }

    @Test
    fun `juz banner renders without subtitle`() {
        composeRule.setThemedContent {
            JuzPageBanner(title = "Juz 30", subtitle = "")
        }
        composeRule.onNodeWithText("Juz 30").assertExists()
    }

    @Test
    fun `page surah separator renders full content with bismillah`() {
        composeRule.setThemedContent {
            PageSurahSeparator(
                surahNumber = 2,
                surahNameArabic = "البقرة",
                surahNameEnglish = "Al-Baqarah",
                showBismillah = true
            )
        }
        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        composeRule.onNodeWithText("البقرة").assertExists()
    }

    @Test
    fun `page surah separator renders without arabic name or bismillah`() {
        composeRule.setThemedContent {
            PageSurahSeparator(
                surahNumber = 9,
                surahNameArabic = "",
                surahNameEnglish = "At-Tawbah",
                showBismillah = false
            )
        }
        composeRule.onNodeWithText("At-Tawbah").assertExists()
        composeRule.onNodeWithText("9").assertExists()
    }
}
