package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TranslationLanguage
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The per-verse actions over a Mushaf page.
 *
 * There is no room on a printed page for a row of buttons, so the actions arrive as a tooltip
 * over the verse that was tapped. Two things follow. The tooltip's bookmark and favourite icons
 * have to flip **on tap**, before the database answers — the real value comes back through a
 * Flow a frame or two later, and an icon that waits for it feels broken. And the translation
 * action is offered only where there is a translation to show: a Mushaf page is Arabic, and the
 * verse may have neither a translation nor a transliteration loaded.
 *
 * `AyahtooltipTest` covers the tooltip's own layout; this is the state around it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class MushafAyahActionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val played = mutableListOf<Int>()
    private val bookmarked = mutableListOf<Int>()
    private val favourited = mutableListOf<Int>()
    private val shared = mutableListOf<Int>()
    private val tafseered = mutableListOf<Int>()
    private val khatamToggled = mutableListOf<Int>()

    private val cave = Surah(
        number = 18,
        nameArabic = "الكهف",
        nameEnglish = "The Cave",
        nameTransliteration = "Al-Kahf",
        revelationType = RevelationType.MECCAN,
        ayahCount = 110,
        orderInMushaf = 18,
        startPage = 293,
    )

    private fun ayah(
        id: Int = 18_010,
        translation: String? = "a translation",
        transliteration: String? = null,
        isBookmarked: Boolean = false,
    ) = Ayah(
        id = id,
        surahNumber = 18,
        ayahNumber = 10,
        textArabic = "نص الآية",
        textSimple = "nass",
        juzNumber = 15,
        hizbNumber = 29,
        rubNumber = 0,
        pageNumber = 293,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
        isBookmarked = isBookmarked,
        transliteration = transliteration,
    )

    private lateinit var state: MushafAyahActionsState

    private fun render(
        favouriteAyahIds: Set<Int> = emptySet(),
        isKhatamActive: Boolean = false,
        khatamReadAyahIds: Set<Int> = emptySet(),
        showTranslation: Boolean = true,
        showTransliteration: Boolean = false,
        content: @Composable () -> Unit = {},
    ) {
        composeRule.setThemedContent {
            state = rememberMushafAyahActionsState()
            content()
            MushafAyahActions(
                state = state,
                parentHeight = 2000f,
                surahMap = mapOf(18 to cave),
                favoriteAyahIds = favouriteAyahIds,
                isKhatamActive = isKhatamActive,
                khatamReadAyahIds = khatamReadAyahIds,
                showTranslation = showTranslation,
                showTransliteration = showTransliteration,
                translationLanguage = TranslationLanguage.ENGLISH,
                onPlayClick = { played += it.id },
                onBookmarkClick = { bookmarked += it.id },
                onFavoriteClick = { favourited += it.id },
                onCopyClick = {},
                onShareClick = { shared += it.id },
                onTafseerClick = { tafseered += it.id },
                onKhatamToggle = { khatamToggled += it.id },
            )
        }
    }

    private fun show(ayah: Ayah = ayah(), tapY: Float = 400f) {
        composeRule.runOnIdle { state.show(ayah, tapY) }
    }

    // ---- Nothing selected ----

    @Test
    fun `with no verse selected nothing is drawn`() {
        // It sits unconditionally at the end of the renderer's Box, so it has to be inert.
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_play)).assertDoesNotExist()
    }

    // ---- The tooltip ----

    @Test
    fun `tapping a verse opens its actions`() {
        render()

        show()

        composeRule.onNodeWithContentDescription(str(R.string.cd_play)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.cd_bookmark)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.cd_share)).assertIsDisplayed()
    }

    @Test
    fun `playing from the tooltip plays that verse and closes it`() {
        render()
        show()

        composeRule.onNodeWithContentDescription(str(R.string.cd_play)).performClick()

        assertThat(played).containsExactly(18_010)
        composeRule.onNodeWithContentDescription(str(R.string.cd_play)).assertDoesNotExist()
    }

    @Test
    fun `bookmarking flips the icon before the database answers`() {
        render()
        show(ayah(isBookmarked = false))

        composeRule.onNodeWithContentDescription(str(R.string.cd_bookmark)).performClick()

        // The real value arrives through a Flow a frame or two later; an icon that waits for it
        // reads as a tap that did nothing.
        assertThat(bookmarked).containsExactly(18_010)
        assertThat(state.bookmarkOverride).isTrue()
    }

    @Test
    fun `reopening the tooltip forgets the optimistic flip`() {
        render()
        show(ayah(isBookmarked = false))
        composeRule.onNodeWithContentDescription(str(R.string.cd_bookmark)).performClick()

        show(ayah(isBookmarked = false))

        // The override belongs to one opening — a new opening is exactly when it stops applying.
        assertThat(state.bookmarkOverride).isNull()
    }

    @Test
    fun `favouriting is remembered per verse, so a page can flip several`() {
        render()
        show(ayah(id = 18_010))
        composeRule.onNodeWithContentDescription(str(R.string.cd_favorite)).performClick()

        show(ayah(id = 18_011))
        composeRule.onNodeWithContentDescription(str(R.string.cd_favorite)).performClick()

        assertThat(favourited).containsExactly(18_010, 18_011).inOrder()
        assertThat(state.favoriteOverrides.keys).containsExactly(18_010, 18_011)
    }

    @Test
    fun `the commentary and share actions carry the verse that was tapped`() {
        render()
        show(ayah(id = 18_042))

        composeRule.onNodeWithContentDescription(str(R.string.cd_tafseer)).performClick()
        show(ayah(id = 18_042))
        composeRule.onNodeWithContentDescription(str(R.string.cd_share)).performClick()

        assertThat(tafseered).containsExactly(18_042)
        assertThat(shared).containsExactly(18_042)
    }

    // ---- Khatam ----

    @Test
    fun `the khatam toggle is offered only while a khatam is active`() {
        render(isKhatamActive = false)
        show()

        composeRule.onNodeWithContentDescription(str(R.string.cd_mark_as_read)).assertDoesNotExist()
    }

    @Test
    fun `an unread verse is offered marking, and a read one unmarking`() {
        render(isKhatamActive = true, khatamReadAyahIds = emptySet())
        show()
        composeRule.onNodeWithContentDescription(str(R.string.cd_mark_as_read)).performClick()

        assertThat(khatamToggled).containsExactly(18_010)
    }

    @Test
    fun `a verse already read for the khatam offers to unmark it`() {
        render(isKhatamActive = true, khatamReadAyahIds = setOf(18_010))
        show()

        composeRule.onNodeWithContentDescription(str(R.string.cd_mark_as_unread))
            .assertIsDisplayed()
    }

    // ---- The translation action ----

    @Test
    fun `a verse with a translation loaded offers to show it`() {
        render(showTranslation = true)
        show(ayah(translation = "a translation"))

        composeRule.onNodeWithContentDescription(str(R.string.cd_translation)).assertIsDisplayed()
    }

    @Test
    fun `a verse with nothing to show is not offered the translation action`() {
        // A Mushaf page is Arabic; the verse may have neither loaded.
        render(showTranslation = true)
        show(ayah(translation = null, transliteration = null))

        composeRule.onNodeWithContentDescription(str(R.string.cd_translation)).assertDoesNotExist()
    }

    @Test
    fun `a reader who has both layers turned off is not offered them either`() {
        render(showTranslation = false, showTransliteration = false)
        show(ayah(translation = "a translation"))

        composeRule.onNodeWithContentDescription(str(R.string.cd_translation)).assertDoesNotExist()
    }

    @Test
    fun `a transliteration alone is enough to offer the action`() {
        render(showTranslation = false, showTransliteration = true)
        show(ayah(translation = null, transliteration = "nass al-ayah"))

        composeRule.onNodeWithContentDescription(str(R.string.cd_translation)).assertIsDisplayed()
    }

    @Test
    fun `opening the translation shows the verse's reference and text`() {
        render(showTranslation = true)
        show(ayah(translation = "a translation"))

        composeRule.onNodeWithContentDescription(str(R.string.cd_translation)).performClick()

        composeRule.onNodeWithText("a translation", substring = true).assertIsDisplayed()
    }
}
