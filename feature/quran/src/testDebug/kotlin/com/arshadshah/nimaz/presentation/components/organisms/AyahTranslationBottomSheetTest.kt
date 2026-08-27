package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests target the public [AyahTranslationContent] composable, which renders the
 * sheet body inline. The full [AyahTranslationBottomSheet] wraps this in a
 * Material3 ModalBottomSheet (via NimazBottomSheet); ModalBottomSheet content
 * renders in a separate window under Robolectric and is unreliable to query, so
 * it is only smoke-tested for non-crashing composition.
 */
@RunWith(RobolectricTestRunner::class)
class AyahTranslationBottomSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        id: Int = 2,
        surahNumber: Int = 1,
        ayahNumber: Int = 2,
        textArabic: String = "نص عربي",
        textSimple: String = "nas arabi",
        juzNumber: Int = 1,
        hizbNumber: Int = 1,
        rubNumber: Int = 0,
        pageNumber: Int = 1,
        sajdaType: SajdaType? = null,
        sajdaNumber: Int? = null,
        translation: String? = "All praise is due to Allah",
        isBookmarked: Boolean = false,
        transliteration: String? = "Al-hamdu lillahi",
        textTajweed: String? = null
    ) = Ayah(
        id = id,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textSimple = textSimple,
        juzNumber = juzNumber,
        hizbNumber = hizbNumber,
        rubNumber = rubNumber,
        pageNumber = pageNumber,
        sajdaType = sajdaType,
        sajdaNumber = sajdaNumber,
        translation = translation,
        isBookmarked = isBookmarked,
        transliteration = transliteration,
        textTajweed = textTajweed
    )

    @Test
    fun `content renders header with surah name and ayah number`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(ayahNumber = 2),
                surahName = "Al-Fatihah"
            )
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("Ayah 2").assertExists()
    }

    @Test
    fun `content falls back to Surah number when surahName null`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(surahNumber = 18),
                surahName = null
            )
        }

        composeRule.onNodeWithText("Surah 18").assertExists()
    }

    @Test
    fun `content renders juz and page chip`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(juzNumber = 1, pageNumber = 1)
            )
        }

        composeRule.onNodeWithText("Juz 1 | P1").assertExists()
    }

    @Test
    fun `translation section shows label and text when enabled`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(translation = "All praise is due to Allah"),
                showTranslation = true
            )
        }

        composeRule.onNodeWithText("Translation").assertExists()
        composeRule.onNodeWithText("All praise is due to Allah").assertExists()
    }

    @Test
    fun `translation section hidden when showTranslation false`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(translation = "All praise is due to Allah"),
                showTranslation = false,
                showTransliteration = false
            )
        }

        composeRule.onNodeWithText("Translation").assertDoesNotExist()
        composeRule.onNodeWithText("All praise is due to Allah").assertDoesNotExist()
    }

    @Test
    fun `transliteration section shows label and text when enabled`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(transliteration = "Al-hamdu lillahi"),
                showTransliteration = true
            )
        }

        composeRule.onNodeWithText("Transliteration").assertExists()
        composeRule.onNodeWithText("Al-hamdu lillahi").assertExists()
    }

    @Test
    fun `transliteration hidden by default`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(transliteration = "Al-hamdu lillahi")
            )
        }

        composeRule.onNodeWithText("Transliteration").assertDoesNotExist()
        composeRule.onNodeWithText("Al-hamdu lillahi").assertDoesNotExist()
    }

    @Test
    fun `obligatory sajda shows wajib indicator`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(sajdaType = SajdaType.OBLIGATORY, sajdaNumber = 1)
            )
        }

        composeRule.onNodeWithText("۩ Sajdah (Wajib)").assertExists()
    }

    @Test
    fun `recommended sajda shows recommended indicator`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(sajdaType = SajdaType.RECOMMENDED, sajdaNumber = 2)
            )
        }

        composeRule.onNodeWithText("۩ Sajdah (Recommended)").assertExists()
    }

    @Test
    fun `no sajda hides indicator`() {
        composeRule.setThemedContent {
            AyahTranslationContent(
                ayah = ayah(sajdaType = null)
            )
        }

        composeRule.onNodeWithText("۩ Sajdah (Wajib)").assertDoesNotExist()
        composeRule.onNodeWithText("۩ Sajdah (Recommended)").assertDoesNotExist()
    }
}
