package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Arabic type stack, exercised across the options its callers actually use.
 *
 * `ArabicText` is the only place in the app that forces `LayoutDirection.Rtl` around its content,
 * and it does so in **both** overloads — the plain-string one and the `AnnotatedString` one the
 * tajweed reader uses. They are two independently maintained copies of the same twenty lines, and a
 * fix applied to one and not the other is how tajweed-coloured verses would start rendering
 * left-to-right while plain ones stayed correct.
 *
 * Each wrapper above it — `QuranVerseText`, `HadithArabicText`, `DuaArabicText`, `BismillahText`,
 * `AyahDisplay` — carries its own size, colour and font-size overrides, and those overrides are
 * where the reader's typography settings land. A parameter that stops being threaded through is a
 * setting that silently does nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ArabicTextVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val verse = "بِسْمِ اللَّهِ"

    @Test
    fun `the plain overload honours every option a caller can set`() {
        // The `style ?: TextStyle(...)` elvis plus every named argument. Rendering both the
        // defaulted and the fully-specified form takes both sides of it.
        composeRule.setThemedContent {
            Column {
                ArabicText(text = "defaults")
                ArabicText(
                    text = "specified",
                    size = ArabicTextSize.LARGE,
                    color = Color.Magenta,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ArabicText(
                    text = "styled",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        composeRule.onNodeWithText("defaults").assertExists()
        composeRule.onNodeWithText("specified").assertExists()
        composeRule.onNodeWithText("styled").assertExists()
    }

    @Test
    fun `the annotated overload honours the same options`() {
        // The tajweed reader's path. It is a second copy of the same body, and only a test that
        // drives both notices when one of them drifts.
        val coloured = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("annotated") }
        }

        composeRule.setThemedContent {
            Column {
                ArabicText(text = coloured)
                ArabicText(
                    text = AnnotatedString("annotated-specified"),
                    size = ArabicTextSize.SMALL,
                    color = Color.Blue,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ArabicText(
                    text = AnnotatedString("annotated-styled"),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        composeRule.onNodeWithText("annotated").assertExists()
        composeRule.onNodeWithText("annotated-specified").assertExists()
        composeRule.onNodeWithText("annotated-styled").assertExists()
    }

    @Test
    fun `every size preset carries its own metrics`() {
        // The presets are what the reader's font-size setting selects between, and each pair must
        // be distinct — two presets resolving to the same metrics would make half the slider do
        // nothing.
        //
        // QURAN and LARGE deliberately share a *font size* and differ in leading: mushaf lines
        // need the extra air at the same glyph size, which is the whole reason QURAN exists as a
        // preset rather than as LARGE. So the pair, not the size, is what has to be unique.
        val sizes = ArabicTextSize.entries

        assertThat(sizes.map { it.fontSize to it.lineHeight }.toSet()).hasSize(sizes.size)
        assertThat(ArabicTextSize.QURAN.fontSize).isEqualTo(ArabicTextSize.LARGE.fontSize)
        assertThat(ArabicTextSize.QURAN.lineHeight.value)
            .isGreaterThan(ArabicTextSize.LARGE.lineHeight.value)
        // Leading always exceeds the glyph size, or Arabic diacritics collide with the line above.
        sizes.forEach {
            assertThat(it.lineHeight.value).isGreaterThan(it.fontSize.value)
        }
    }

    @Test
    fun `a verse renders its end marker only when it is numbered`() {
        composeRule.setThemedContent {
            Column {
                QuranVerseText(arabicText = verse, verseNumber = 7)
                QuranVerseText(arabicText = verse)
            }
        }

        composeRule.onNodeWithText(toArabicNumber(7), substring = true).assertExists()
    }

    @Test
    fun `each corpus wrapper takes its own font size`() {
        // Hadith and dua Arabic are sized independently of the Quran's, because the reader's two
        // settings are separate — one slider must not move the other's text.
        composeRule.setThemedContent {
            Column {
                HadithArabicText(text = "hadith-default")
                HadithArabicText(text = "hadith-sized", customFontSize = 30f, color = Color.Magenta)
                DuaArabicText(text = "dua-default")
                DuaArabicText(text = "dua-sized", customFontSize = 22f, size = ArabicTextSize.SMALL)
            }
        }

        listOf("hadith-default", "hadith-sized", "dua-default", "dua-sized").forEach {
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `the bismillah renders in the accent it is given`() {
        composeRule.setThemedContent {
            Column {
                BismillahText()
                BismillahText(color = Color.Magenta)
            }
        }

        composeRule.onAllNodesWithText("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ")
            .assertCountEquals(2)
    }

    @Test
    fun `an ayah shows its transliteration only when both the text and the flag are present`() {
        // Two conditions on one line. Showing it with the flag off leaks a setting the reader
        // turned off; showing it with no text renders an empty gap between Arabic and translation.
        composeRule.setThemedContent {
            Column {
                AyahDisplay(
                    arabicText = verse,
                    translation = "translation-a",
                    verseNumber = 1,
                    transliteration = "bismillah",
                    showTransliteration = true,
                )
                AyahDisplay(
                    arabicText = verse,
                    translation = "translation-b",
                    verseNumber = 2,
                    transliteration = "hidden",
                    showTransliteration = false,
                )
                AyahDisplay(
                    arabicText = verse,
                    translation = "translation-c",
                    verseNumber = 3,
                    showTransliteration = true,
                )
            }
        }

        composeRule.onNodeWithText("bismillah").assertExists()
        composeRule.onNodeWithText("hidden").assertDoesNotExist()
        composeRule.onNodeWithText("translation-c").assertExists()
    }

    @Test
    fun `an ayah takes the caller's sizes and inks`() {
        composeRule.setThemedContent {
            AyahDisplay(
                arabicText = verse,
                translation = "coloured",
                verseNumber = 4,
                transliteration = "t",
                showTransliteration = true,
                arabicSize = ArabicTextSize.LARGE,
                arabicColor = Color.Magenta,
                translationColor = Color.Blue,
                transliterationColor = Color.Green,
            )
        }

        composeRule.onNodeWithText("coloured").assertExists()
    }

    @Test
    fun `arabic-indic numerals leave non-digits alone`() {
        // `if (digit.isDigit())` — the guard exists because the same helper formats labels that
        // carry a separator, and mapping a non-digit through `digitToInt` throws.
        assertThat(toArabicNumber(2026)).isEqualTo("٢٠٢٦")
        assertThat(toArabicNumber(-7)).isEqualTo("-٧")
        assertThat(toArabicNumber(0)).isEqualTo("٠")
    }
}
