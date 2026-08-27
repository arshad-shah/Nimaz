package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranSurahListItemTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun surah(
        number: Int = 1,
        nameEnglish: String = "Al-Fatihah",
        revelationType: RevelationType = RevelationType.MECCAN,
        ayahCount: Int = 7
    ) = Surah(
        number = number,
        nameArabic = "الفاتحة",
        nameEnglish = nameEnglish,
        nameTransliteration = "The Opening",
        revelationType = revelationType,
        ayahCount = ayahCount,
        orderInMushaf = 5,
        startPage = 1
    )

    @Test
    fun meccanSurah_showsNumberNameAndMakkahBadge() {
        composeRule.setThemedContent {
            SurahListItem(surah = surah(), onClick = {})
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("Makkah").assertExists()
        composeRule.onNodeWithText("7 Verses").assertExists()
        composeRule.onNodeWithContentDescription("Surah Info").assertExists()
    }

    @Test
    fun medinanSurah_showsMadinahBadge() {
        composeRule.setThemedContent {
            SurahListItem(
                surah = surah(
                    number = 2,
                    nameEnglish = "Al-Baqarah",
                    revelationType = RevelationType.MEDINAN,
                    ayahCount = 286
                ),
                onClick = {}
            )
        }

        composeRule.onNodeWithText("Madinah").assertExists()
        composeRule.onNodeWithText("286 Verses").assertExists()
    }

    @Test
    fun withStartPage_showsItOnTheMetaLine() {
        composeRule.setThemedContent {
            // Resolved by the caller from the active edition's pagination (#325).
            SurahListItem(surah = surah(), onClick = {}, startPage = 22)
        }

        // One meta line: verse count, then the opening page. The page *range* and the juz
        // are gone — the range answered one question with two numbers, and the juz is the
        // section header this row now sits under.
        composeRule.onNodeWithText("7 Verses · p. 22").assertExists()
    }

    @Test
    fun withoutStartPage_showsOnlyTheVerseCount() {
        composeRule.setThemedContent {
            SurahListItem(surah = surah(), onClick = {}, startPage = 0)
        }

        composeRule.onNodeWithText("7 Verses").assertExists()
    }

    @Test
    fun completeKhatam_showsCompletedIcon_notNumber() {
        composeRule.setThemedContent {
            SurahListItem(
                surah = surah(),
                onClick = {},
                isKhatamActive = true,
                khatamReadCount = 7,
                khatamTotalAyahs = 7
            )
        }

        composeRule.onNodeWithContentDescription("Completed").assertExists()
    }

    @Test
    fun partialKhatam_showsNumber_andProgress() {
        composeRule.setThemedContent {
            SurahListItem(
                surah = surah(),
                onClick = {},
                isKhatamActive = true,
                khatamReadCount = 3,
                khatamTotalAyahs = 7
            )
        }

        // Not complete -> number shown, no completed icon
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithContentDescription("Completed").assertDoesNotExist()
    }

    @Test
    fun selected_rendersWithoutError() {
        composeRule.setThemedContent {
            SurahListItem(surah = surah(), onClick = {}, isSelected = true)
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
    }

    @Test
    fun click_invokesOnClick() {
        var clicked = false
        composeRule.setThemedContent {
            SurahListItem(surah = surah(), onClick = { clicked = true })
        }

        composeRule.onNodeWithText("Al-Fatihah").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun infoClick_invokesOnInfoClick() {
        var info = false
        composeRule.setThemedContent {
            SurahListItem(surah = surah(), onClick = {}, onInfoClick = { info = true })
        }

        composeRule.onNodeWithContentDescription("Surah Info").performClick()
        assertThat(info).isTrue()
    }
}
