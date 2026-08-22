package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranBannersTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

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
