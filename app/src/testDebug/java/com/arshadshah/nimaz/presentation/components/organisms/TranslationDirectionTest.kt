package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextDirection
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions
import com.arshadshah.nimaz.domain.model.quran.catalogue.TranslationEdition
import com.arshadshah.nimaz.presentation.theme.fontFamily
import com.arshadshah.nimaz.presentation.theme.textDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Proves the RTL translation path against a **synthetic** edition rather than a shipped one.
 *
 * The app currently ships one translation, and it is English — so nothing in the suite would
 * catch translation text being rendered with no direction until the day an Urdu or Persian
 * edition lands, which is exactly the day the registry is supposed to make easy. Declaring a
 * throwaway RTL edition here exercises the path now, so adding a real one is a data change
 * with a passing test behind it rather than a leap of faith.
 *
 * The direction matters beyond glyph shaping: Compose infers *shaping* from the content, but
 * paragraph alignment and wrapping follow the resolved text direction, so an RTL translation
 * without it renders right-to-left words flush against the left edge.
 */
@RunWith(RobolectricTestRunner::class)
class TranslationDirectionTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val urdu = TranslationEdition(
        id = "synthetic_urdu",
        displayName = "Synthetic Urdu",
        translatorName = "Test",
        languageTag = "ur",
        isRightToLeft = true,
        fontId = "indopak"
    )

    private fun ayah(translation: String) = Ayah(
        id = 2,
        surahNumber = 1,
        ayahNumber = 2,
        textArabic = "نص عربي",
        textSimple = "nas arabi",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        isBookmarked = false,
        transliteration = null,
        textTajweed = null
    )

    @Test
    fun `an RTL edition resolves to RTL text direction`() {
        assertThat(urdu.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun `the shipped English edition stays LTR with the default body font`() {
        val english = QuranEditions.defaultTranslation
        assertThat(english.isRightToLeft).isFalse()
        assertThat(english.textDirection).isEqualTo(TextDirection.Ltr)
        // No fontId means "keep the body font" — a Latin translation must not be forced
        // into an Arabic face.
        assertThat(english.fontFamily).isNull()
    }

    @Test
    fun `an edition declaring a font resolves it to a real family`() {
        assertThat(urdu.fontFamily).isNotNull()
        assertThat(urdu.fontFamily)
            .isEqualTo(QuranEditions.arabicFonts.first { it.id == "indopak" }
                .let { com.arshadshah.nimaz.presentation.theme.QuranArabicFont.fromId(it.id) }
                .fontFamily)
    }

    @Test
    fun `an unknown font id falls back rather than crashing the reader`() {
        val broken = urdu.copy(fontId = "no_such_font")
        assertThat(broken.fontFamily)
            .isEqualTo(com.arshadshah.nimaz.presentation.theme.QuranArabicFont.DEFAULT.fontFamily)
    }

    @Test
    fun `the ayah item renders an RTL translation`() {
        val text = "اللہ کے نام سے جو نہایت مہربان ہے"
        composeRule.setContent {
            AyahItem(
                ayah = ayah(text),
                showTranslation = true,
                arabicFontSize = 24f,
                fontSize = 16f,
                translationEdition = urdu,
                onBookmarkClick = {}
            )
        }
        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `the translation sheet renders an RTL translation`() {
        val text = "اللہ کے نام سے جو نہایت مہربان ہے"
        composeRule.setContent {
            AyahTranslationContent(
                ayah = ayah(text),
                translationEdition = urdu
            )
        }
        composeRule.onNodeWithText(text).assertExists()
    }
}
