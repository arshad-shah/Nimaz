package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.presentation.theme.QuranArabicFont
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every Arabic wrapper called with its full argument list at once.
 *
 * `ArabicTextVariantsTest` covers what each option *does*. This covers the combination — every
 * wrapper takes a face, a size, a colour and a clamp, and the reader's settings screen sets all of
 * them together. A parameter that survives being passed alone and is dropped when another is also
 * present is a real shape of bug in a component whose body is a long `copy(...)` chain, and it only
 * shows up on the one screen that sets both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class ArabicTextFullOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val verse = "بِسْمِ اللَّهِ"

    @Test
    fun `both overloads accept every option at once`() {
        composeRule.setThemedContent {
            Column {
                ArabicText(
                    text = "plain-everything",
                    modifier = Modifier,
                    size = ArabicTextSize.QURAN,
                    color = Color.Magenta,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                ArabicText(
                    text = AnnotatedString("annotated-everything"),
                    modifier = Modifier,
                    size = ArabicTextSize.QURAN,
                    color = Color.Magenta,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        composeRule.onNodeWithText("plain-everything").assertExists()
        composeRule.onNodeWithText("annotated-everything").assertExists()
    }

    @Test
    fun `each corpus wrapper accepts every option at once`() {
        composeRule.setThemedContent {
            Column {
                QuranVerseText(
                    modifier = Modifier,
                    arabicText = verse,
                    verseNumber = 5,
                    size = ArabicTextSize.LARGE,
                    color = Color.Magenta,
                    customFontSize = 26f,
                    showVerseNumber = true,
                    fontFamily = QuranArabicFont.SCHEHERAZADE.fontFamily,
                )
                HadithArabicText(
                    text = "hadith-everything",
                    modifier = Modifier,
                    size = ArabicTextSize.SMALL,
                    customFontSize = 21f,
                    fontFamily = QuranArabicFont.INDOPAK.fontFamily,
                    color = Color.Magenta,
                )
                DuaArabicText(
                    text = "dua-everything",
                    modifier = Modifier,
                    size = ArabicTextSize.MEDIUM,
                    customFontSize = 19f,
                    fontFamily = QuranArabicFont.AMIRI.fontFamily,
                    color = Color.Magenta,
                )
                BismillahText(modifier = Modifier, color = Color.Magenta)
                AyahDisplay(
                    arabicText = verse,
                    translation = "ayah-everything",
                    verseNumber = 7,
                    modifier = Modifier,
                    transliteration = "bismillah",
                    showTransliteration = true,
                    arabicSize = ArabicTextSize.EXTRA_LARGE,
                    arabicColor = Color.Magenta,
                    translationColor = Color.Blue,
                    transliterationColor = Color.Green,
                )
            }
        }

        composeRule.onNodeWithText("hadith-everything").assertExists()
        composeRule.onNodeWithText("dua-everything").assertExists()
        composeRule.onNodeWithText("ayah-everything").assertExists()
    }
}
