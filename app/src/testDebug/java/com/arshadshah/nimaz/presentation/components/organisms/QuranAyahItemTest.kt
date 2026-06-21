package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.components.atoms.formatAyahEndMarker
import com.arshadshah.nimaz.presentation.components.atoms.formatAyahWithEndMarker
import com.arshadshah.nimaz.presentation.components.atoms.getDisplayArabicText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranAyahItemTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        id: Int = 2,
        surahNumber: Int = 2,
        ayahNumber: Int = 5,
        textArabic: String = "نص عربي",
        textSimple: String = "nas arabi",
        juzNumber: Int = 1,
        hizbNumber: Int = 1,
        rubNumber: Int = 0,
        pageNumber: Int = 1,
        sajdaType: SajdaType? = null,
        sajdaNumber: Int? = null,
        translation: String? = null,
        isBookmarked: Boolean = false,
        transliteration: String? = null,
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
    fun `renders ayah number badge and juz page footer`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(ayahNumber = 5, juzNumber = 1, pageNumber = 3),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        // Number badge shows numberInSurah (== ayahNumber)
        composeRule.onNodeWithText("5").assertExists()
        // Footer: "Juz 1 • Page 3"
        composeRule.onNodeWithText("Juz 1 • Page 3").assertExists()
    }

    @Test
    fun `renders translation when showTranslation true and translation present`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(translation = "All praise is due to Allah"),
                showTranslation = true,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithText("All praise is due to Allah").assertExists()
    }

    @Test
    fun `hides translation when showTranslation false`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(translation = "All praise is due to Allah"),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithText("All praise is due to Allah").assertDoesNotExist()
    }

    @Test
    fun `renders transliteration when enabled and present`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(transliteration = "Al-hamdu lillahi"),
                showTranslation = false,
                showTransliteration = true,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithText("Al-hamdu lillahi").assertExists()
    }

    @Test
    fun `onBookmarkClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Bookmark").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onFavoriteClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {},
                onFavoriteClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Favorite").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onTafseerClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {},
                onTafseerClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Tafseer").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onPlayAyahClick fires via play icon`() {
        var fired = false
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                isAudioPlaying = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {},
                onPlayAyahClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Play").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `audio playing shows pause icon instead of play`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                isAudioPlaying = true,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Pause").assertExists()
        composeRule.onNodeWithContentDescription("Play").assertDoesNotExist()
    }

    @Test
    fun `khatam mode shows mark as read toggle and fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                isKhatamMode = true,
                isKhatamRead = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {},
                onKhatamToggle = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Mark as read").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `khatam read shows mark as unread`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                isKhatamMode = true,
                isKhatamRead = true,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Mark as unread").assertExists()
    }

    @Test
    fun `non-khatam mode hides khatam toggle`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(),
                showTranslation = false,
                isKhatamMode = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithContentDescription("Mark as read").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Mark as unread").assertDoesNotExist()
    }

    @Test
    fun `obligatory sajda shows wajib label`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(sajdaType = SajdaType.OBLIGATORY, sajdaNumber = 1),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithText("Sajdah (Wajib)").assertExists()
    }

    @Test
    fun `recommended sajda shows plain sajdah label`() {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah(sajdaType = SajdaType.RECOMMENDED, sajdaNumber = 2),
                showTranslation = false,
                arabicFontSize = 28f,
                fontSize = 16f,
                onBookmarkClick = {}
            )
        }

        composeRule.onNodeWithText("Sajdah").assertExists()
    }

    @Test
    fun `getDisplayArabicText strips bismillah for non-fatiha first ayah`() {
        val verse = ayah(
            surahNumber = 2,
            ayahNumber = 1,
            textArabic = "$BISMILLAH_TEXT بَعْض"
        )
        assertThat(verse.getDisplayArabicText()).isEqualTo("بَعْض")
    }

    @Test
    fun `getDisplayArabicText keeps bismillah for surah al-fatiha`() {
        val verse = ayah(
            surahNumber = 1,
            ayahNumber = 1,
            textArabic = BISMILLAH_TEXT
        )
        assertThat(verse.getDisplayArabicText()).isEqualTo(BISMILLAH_TEXT)
    }

    @Test
    fun `getDisplayArabicText keeps text unchanged for surah at-tawbah`() {
        val verse = ayah(
            surahNumber = 9,
            ayahNumber = 1,
            textArabic = "براءة"
        )
        assertThat(verse.getDisplayArabicText()).isEqualTo("براءة")
    }

    @Test
    fun `formatAyahWithEndMarker appends ornamental end marker`() {
        val result = formatAyahWithEndMarker("نص", 3)
        // "نص <U+FD3F>٣<U+FD3E>"
        assertThat(result).isEqualTo("نص ﴿٣﴾")
    }

    @Test
    fun `formatAyahEndMarker wraps arabic numeral in brackets`() {
        assertThat(formatAyahEndMarker(1)).isEqualTo("﴿١﴾")
    }
}
