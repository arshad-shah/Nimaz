package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The marks a verse carries, and the division markers printed beside it.
 *
 * A verse can be bookmarked, favourited and annotated at once — three independent flags on one
 * row — and the marker strip is drawn only when at least one of them is set. Nothing about a
 * missing marker is visible, which is why each is asserted on its own and in combination: the
 * shape that breaks is "we drew the bookmark and forgot the note", and a reader whose note is
 * invisible has no way to find it again.
 *
 * The rukūʿ and hizb-quarter markers are the same kind of thing at the page's edge. They come
 * from columns computed at build time, and on an install whose artifact predates them the
 * fields are null — the marker is then absent rather than drawn wrong.
 *
 * `QuranAyahItemTest` covers the text itself, the khatam toggle and the sajda labels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class QuranAyahItemMarkersTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun ayah(
        rukuNumber: Int? = null,
        isRukuEnd: Boolean = false,
        rubNumber: Int = 0,
        isRubStart: Boolean = false,
        id: Int = 2,
        translation: String? = "a translation",
        transliteration: String? = null,
        textTajweed: String? = null,
    ) = Ayah(
        id = id,
        surahNumber = 2,
        ayahNumber = 5,
        textArabic = "نص عربي",
        textSimple = "nas arabi",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = rubNumber,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        transliteration = transliteration,
        textTajweed = textTajweed,
        rukuNumber = rukuNumber,
        isRukuEnd = isRukuEnd,
        isRubStart = isRubStart,
    )

    private fun render(
        ayah: Ayah = ayah(),
        isBookmarked: Boolean = false,
        isFavorite: Boolean = false,
        hasNote: Boolean = false,
        showTajweed: Boolean = false,
        showTransliteration: Boolean = false,
    ) {
        composeRule.setThemedContent {
            AyahItem(
                ayah = ayah.copy(isBookmarked = isBookmarked),
                showTranslation = true,
                showTransliteration = showTransliteration,
                arabicFontSize = 28f,
                fontSize = 16f,
                isFavorite = isFavorite,
                hasNote = hasNote,
                showTajweed = showTajweed,
            )
        }
    }

    private val bookmark get() = str(R.string.ayah_action_bookmark)
    private val favourite get() = str(R.string.ayah_action_favourite)
    private val note get() = str(R.string.ayah_action_note)

    // ---- The three marks ----

    @Test
    fun `a verse with no marks carries no marker strip`() {
        render()

        composeRule.onNodeWithContentDescription(bookmark).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(favourite).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(note).assertDoesNotExist()
    }

    @Test
    fun `a bookmarked verse is marked as one`() {
        render(isBookmarked = true)

        composeRule.onNodeWithContentDescription(bookmark).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(favourite).assertDoesNotExist()
    }

    @Test
    fun `a favourited verse is marked as one`() {
        render(isFavorite = true)

        composeRule.onNodeWithContentDescription(favourite).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(bookmark).assertDoesNotExist()
    }

    @Test
    fun `an annotated verse is marked as one`() {
        // A note used to be invisible on the row that carried it.
        render(hasNote = true)

        composeRule.onNodeWithContentDescription(note).assertIsDisplayed()
    }

    @Test
    fun `a verse marked three ways shows all three`() {
        // Independent flags on one row, so drawing one and dropping the others is the shape
        // that actually breaks.
        render(isBookmarked = true, isFavorite = true, hasNote = true)

        composeRule.onNodeWithContentDescription(bookmark).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(favourite).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(note).assertIsDisplayed()
    }

    // ---- Division markers ----

    @Test
    fun `a verse that closes a rukū prints its number`() {
        render(ayah = ayah(id = 7, rukuNumber = 2, isRukuEnd = true))

        composeRule.onNodeWithText(str(R.string.ruku_format, 2)).assertIsDisplayed()
    }

    @Test
    fun `a verse in the middle of a rukū prints no marker`() {
        render(ayah = ayah(id = 5, rukuNumber = 2, isRukuEnd = false))

        composeRule.onNodeWithText(str(R.string.ruku_format, 2)).assertDoesNotExist()
    }

    @Test
    fun `an install whose content predates the rukū columns prints no marker`() {
        // Null rather than zero, and absent rather than drawn wrong: the columns arrive with a
        // later artifact than the migration that creates them.
        render(ayah = ayah(rukuNumber = null, isRukuEnd = true))

        composeRule.onNodeWithText(str(R.string.ruku_format, 1)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.ruku_format, 2)).assertDoesNotExist()
    }

    @Test
    fun `a verse that opens a hizb quarter says which quarter`() {
        // `rubNumber` counts quarters across the whole Quran (1..240), not 1..4 — reading it as
        // a quarter directly labelled the first four in the book and dropped the other 236.
        // Quarter 6 is the second quarter of hizb 2.
        render(ayah = ayah(id = 9, rubNumber = 6, isRubStart = true))

        composeRule.onNodeWithText(str(R.string.hizb_quarter_format, 2)).assertIsDisplayed()
    }

    @Test
    fun `a verse that opens a hizb itself is labelled as the hizb, not a quarter of it`() {
        // The first quarter of a hizb *is* the hizb.
        render(ayah = ayah(id = 9, rubNumber = 5, isRubStart = true))

        composeRule.onNodeWithText(str(R.string.hizb_format, 2)).assertIsDisplayed()
    }

    @Test
    fun `a verse inside a hizb quarter opens nothing`() {
        render(ayah = ayah(id = 12, rubNumber = 6, isRubStart = false))

        composeRule.onNodeWithText(str(R.string.hizb_quarter_format, 2)).assertDoesNotExist()
    }

    // ---- Optional text layers ----

    @Test
    fun `tajweed colouring falls back to the plain text when the verse carries none`() {
        // The parse is guarded, so a verse with no tajweed string still renders its Arabic.
        render(ayah = ayah(textTajweed = null), showTajweed = true)

        composeRule.onNodeWithText("a translation").assertIsDisplayed()
    }

    @Test
    fun `a verse with no transliteration renders without one`() {
        render(ayah = ayah(transliteration = null), showTransliteration = true)

        composeRule.onNodeWithText("a translation").assertIsDisplayed()
    }

    @Test
    fun `a verse with no translation still renders its Arabic`() {
        render(ayah = ayah(translation = null))

        composeRule.onNodeWithText("a translation").assertDoesNotExist()
    }

    @Test
    fun `a sajda verse is still marked when it also carries other marks`() {
        val sajda = ayah().copy(sajdaType = SajdaType.OBLIGATORY, sajdaNumber = 1)

        render(ayah = sajda, isBookmarked = true)

        composeRule.onNodeWithText(str(R.string.sajdah_wajib)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(bookmark).assertIsDisplayed()
    }
}
