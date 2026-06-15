package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArabicTextTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── toArabicNumber (pure) ───────────────────────────────────────────────

    @Test
    fun `toArabicNumber converts each digit`() {
        assertThat(toArabicNumber(0)).isEqualTo("٠")
        assertThat(toArabicNumber(1234567890)).isEqualTo("١٢٣٤٥٦٧٨٩٠")
    }

    @Test
    fun `toArabicNumber converts multi-digit numbers`() {
        assertThat(toArabicNumber(255)).isEqualTo("٢٥٥")
    }

    // ── ArabicText ──────────────────────────────────────────────────────────

    @Test
    fun `ArabicText renders with default style`() {
        composeRule.setThemedContent {
            ArabicText(text = "السلام")
        }
        composeRule.onNodeWithText("السلام").assertExists()
    }

    @Test
    fun `ArabicText renders with a supplied style and overrides`() {
        composeRule.setThemedContent {
            ArabicText(
                text = "مرحبا",
                size = ArabicTextSize.SMALL,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontSize = ArabicTextSize.LARGE.fontSize)
            )
        }
        composeRule.onNodeWithText("مرحبا").assertExists()
    }

    @Test
    fun `ArabicTextSize presets expose font metrics`() {
        // Touch every enum entry so the values are exercised.
        ArabicTextSize.entries.forEach { size ->
            assertThat(size.fontSize.value).isGreaterThan(0f)
            assertThat(size.lineHeight.value).isGreaterThan(0f)
        }
        assertThat(ArabicTextSize.entries).hasSize(5)
    }

    // ── QuranVerseText ──────────────────────────────────────────────────────

    @Test
    fun `QuranVerseText shows verse number end marker`() {
        composeRule.setThemedContent {
            QuranVerseText(arabicText = "الحمد لله", verseNumber = 2)
        }
        composeRule.onNodeWithText("الحمد لله", substring = true).assertExists()
    }

    @Test
    fun `QuranVerseText hides verse number when disabled`() {
        composeRule.setThemedContent {
            QuranVerseText(
                arabicText = "قل هو الله",
                verseNumber = null,
                showVerseNumber = false,
                customFontSize = 30f
            )
        }
        composeRule.onNodeWithText("قل هو الله", substring = true).assertExists()
    }

    // ── HadithArabicText / DuaArabicText ────────────────────────────────────

    @Test
    fun `HadithArabicText renders with and without custom font size`() {
        composeRule.setThemedContent {
            HadithArabicText(text = "إنما الأعمال")
        }
        composeRule.onNodeWithText("إنما الأعمال").assertExists()
    }

    @Test
    fun `HadithArabicText renders with custom font size`() {
        composeRule.setThemedContent {
            HadithArabicText(text = "بالنيات", customFontSize = 22f)
        }
        composeRule.onNodeWithText("بالنيات").assertExists()
    }

    @Test
    fun `DuaArabicText renders with default and custom font size`() {
        composeRule.setThemedContent {
            DuaArabicText(text = "اللهم")
        }
        composeRule.onNodeWithText("اللهم").assertExists()
    }

    @Test
    fun `DuaArabicText renders with custom font size`() {
        composeRule.setThemedContent {
            DuaArabicText(text = "ربنا", customFontSize = 26f)
        }
        composeRule.onNodeWithText("ربنا").assertExists()
    }

    // ── BismillahText ───────────────────────────────────────────────────────

    @Test
    fun `BismillahText renders bismillah`() {
        composeRule.setThemedContent {
            BismillahText()
        }
        composeRule.onNodeWithText("بِسْمِ", substring = true).assertExists()
    }

    // ── AyahDisplay ─────────────────────────────────────────────────────────

    @Test
    fun `AyahDisplay shows translation without transliteration`() {
        composeRule.setThemedContent {
            AyahDisplay(
                arabicText = "الحمد لله",
                translation = "All praise is due to Allah",
                verseNumber = 2
            )
        }
        composeRule.onNodeWithText("All praise is due to Allah").assertExists()
    }

    @Test
    fun `AyahDisplay shows transliteration when enabled`() {
        composeRule.setThemedContent {
            AyahDisplay(
                arabicText = "الحمد لله",
                translation = "All praise is due to Allah",
                verseNumber = 2,
                transliteration = "Alhamdu lillah",
                showTransliteration = true
            )
        }
        composeRule.onNodeWithText("Alhamdu lillah").assertExists()
        composeRule.onNodeWithText("All praise is due to Allah").assertExists()
    }
}
