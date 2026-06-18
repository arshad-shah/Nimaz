package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MushafPageTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        id: Int,
        surahNumber: Int,
        ayahNumber: Int,
        textArabic: String = "نَصٌّ عَرَبِيّ",
        translation: String? = null
    ) = Ayah(
        id = id,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textSimple = "نص عربي",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        isBookmarked = false
    )

    private val fatihah = Surah(
        number = 1,
        nameArabic = "الفاتحة",
        nameEnglish = "Al-Fatihah",
        nameTransliteration = "The Opening",
        revelationType = RevelationType.MECCAN,
        ayahCount = 7,
        juzStart = 1,
        orderInMushaf = 1,
        startPage = 1
    )

    private val baqarah = Surah(
        number = 2,
        nameArabic = "البقرة",
        nameEnglish = "Al-Baqarah",
        nameTransliteration = "The Cow",
        revelationType = RevelationType.MEDINAN,
        ayahCount = 286,
        juzStart = 1,
        orderInMushaf = 2,
        startPage = 2
    )

    @Test
    fun `renders the page number footer`() {
        composeRule.setThemedContent {
            // Use a distinctive page number so the footer does not collide with
            // the surah/ayah number "1" rendered in the page header.
            MushafPage(
                pageNumber = 99,
                ayahs = listOf(ayah(id = 1, surahNumber = 1, ayahNumber = 1)),
                surahMap = mapOf(1 to fatihah)
            )
        }

        composeRule.onNodeWithText("99").assertExists()
    }

    @Test
    fun `shows surah header when page starts a new surah`() {
        composeRule.setThemedContent {
            MushafPage(
                pageNumber = 1,
                ayahs = listOf(
                    ayah(id = 1, surahNumber = 1, ayahNumber = 1),
                    ayah(id = 2, surahNumber = 1, ayahNumber = 2)
                ),
                surahMap = mapOf(1 to fatihah)
            )
        }

        // MushafSurahHeader renders the English name and ayah count
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("7 Ayahs").assertExists()
    }

    @Test
    fun `does not show surah header when first ayah is not ayah one`() {
        composeRule.setThemedContent {
            MushafPage(
                pageNumber = 5,
                ayahs = listOf(
                    ayah(id = 3, surahNumber = 1, ayahNumber = 3),
                    ayah(id = 4, surahNumber = 1, ayahNumber = 4)
                ),
                surahMap = mapOf(1 to fatihah)
            )
        }

        // The header (and therefore the English name) is not rendered mid-surah
        composeRule.onNodeWithText("Al-Fatihah").assertDoesNotExist()
        composeRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun `renders headers for multiple surahs starting on the page`() {
        composeRule.setThemedContent {
            MushafPage(
                pageNumber = 2,
                ayahs = listOf(
                    ayah(id = 1, surahNumber = 1, ayahNumber = 1),
                    ayah(id = 2, surahNumber = 2, ayahNumber = 1)
                ),
                surahMap = mapOf(1 to fatihah, 2 to baqarah)
            )
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
    }

    @Test
    fun `renders without crashing when surah is missing from map`() {
        composeRule.setThemedContent {
            MushafPage(
                pageNumber = 3,
                ayahs = listOf(ayah(id = 1, surahNumber = 1, ayahNumber = 1)),
                surahMap = emptyMap()
            )
        }

        // Header is skipped (surah == null) but the page frame still renders
        composeRule.onNodeWithText("3").assertExists()
    }
}
