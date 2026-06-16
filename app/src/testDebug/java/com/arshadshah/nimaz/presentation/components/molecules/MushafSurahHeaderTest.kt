package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MushafSurahHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

    private fun surah(
        number: Int = 2,
        nameArabic: String = "البقرة",
        nameEnglish: String = "Al-Baqarah",
        ayahCount: Int = 286
    ) = Surah(
        number = number,
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        nameTransliteration = "The Cow",
        revelationType = RevelationType.MEDINAN,
        ayahCount = ayahCount,
        juzStart = 1,
        orderInMushaf = number
    )

    // ---- Surah-based overload ----

    @Test
    fun `surah overload renders number english name and ayah count`() {
        composeRule.setThemedContent {
            MushafSurahHeader(surah = surah(number = 2, ayahCount = 286))
        }

        composeRule.onNodeWithText("2").assertExists()
        composeRule.onNodeWithText("Al-Baqarah").assertExists()
        composeRule.onNodeWithText("286 Ayahs").assertExists()
    }

    @Test
    fun `surah overload shows bismillah by default for surah 2`() {
        composeRule.setThemedContent {
            MushafSurahHeader(surah = surah(number = 2))
        }

        composeRule.onNodeWithText(bismillah).assertExists()
    }

    @Test
    fun `surah overload hides bismillah for surah 1 by default`() {
        composeRule.setThemedContent {
            MushafSurahHeader(
                surah = surah(
                    number = 1,
                    nameArabic = "الفاتحة",
                    nameEnglish = "Al-Fatihah",
                    ayahCount = 7
                )
            )
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText(bismillah).assertDoesNotExist()
    }

    @Test
    fun `surah overload hides bismillah for surah 9 by default`() {
        composeRule.setThemedContent {
            MushafSurahHeader(
                surah = surah(
                    number = 9,
                    nameArabic = "التوبة",
                    nameEnglish = "At-Tawbah",
                    ayahCount = 129
                )
            )
        }

        composeRule.onNodeWithText(bismillah).assertDoesNotExist()
    }

    @Test
    fun `surah overload can force bismillah off`() {
        composeRule.setThemedContent {
            MushafSurahHeader(surah = surah(number = 2), showBismillah = false)
        }

        composeRule.onNodeWithText(bismillah).assertDoesNotExist()
    }

    // ---- Explicit-params overload ----

    @Test
    fun `explicit overload renders fields and bismillah when true`() {
        composeRule.setThemedContent {
            MushafSurahHeader(
                surahNumber = 3,
                surahNameArabic = "آل عمران",
                surahNameEnglish = "Ali 'Imran",
                ayahCount = 200,
                showBismillah = true
            )
        }

        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText("Ali 'Imran").assertExists()
        composeRule.onNodeWithText("200 Ayahs").assertExists()
        composeRule.onNodeWithText(bismillah).assertExists()
    }

    @Test
    fun `explicit overload hides bismillah when false`() {
        composeRule.setThemedContent {
            MushafSurahHeader(
                surahNumber = 9,
                surahNameArabic = "التوبة",
                surahNameEnglish = "At-Tawbah",
                ayahCount = 129,
                showBismillah = false
            )
        }

        composeRule.onNodeWithText("At-Tawbah").assertExists()
        composeRule.onNodeWithText(bismillah).assertDoesNotExist()
    }

    @Test
    fun `explicit overload omits arabic name when empty`() {
        composeRule.setThemedContent {
            MushafSurahHeader(
                surahNumber = 5,
                surahNameArabic = "",
                surahNameEnglish = "Al-Ma'idah",
                ayahCount = 120
            )
        }

        composeRule.onNodeWithText("Al-Ma'idah").assertExists()
    }

    // ---- Separator ----

    @Test
    fun `separator renders number english and bismillah`() {
        composeRule.setThemedContent {
            MushafSurahSeparator(
                surahNumber = 3,
                surahNameArabic = "آل عمران",
                surahNameEnglish = "Ali 'Imran",
                showBismillah = true
            )
        }

        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText("Ali 'Imran").assertExists()
        composeRule.onNodeWithText(bismillah).assertExists()
    }

    @Test
    fun `separator hides bismillah when false`() {
        composeRule.setThemedContent {
            MushafSurahSeparator(
                surahNumber = 1,
                surahNameArabic = "الفاتحة",
                surahNameEnglish = "Al-Fatihah",
                showBismillah = false
            )
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText(bismillah).assertDoesNotExist()
    }

    @Test
    fun `separator omits arabic name when empty`() {
        composeRule.setThemedContent {
            MushafSurahSeparator(
                surahNumber = 7,
                surahNameArabic = "",
                surahNameEnglish = "Al-A'raf",
                showBismillah = false
            )
        }

        composeRule.onNodeWithText("Al-A'raf").assertExists()
    }
}
