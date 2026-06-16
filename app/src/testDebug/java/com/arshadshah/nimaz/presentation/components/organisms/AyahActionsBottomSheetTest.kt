package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests target the public [AyahActionsContent] composable, which renders the
 * sheet body inline. The full [AyahActionsBottomSheet] wraps this in a Material3
 * ModalBottomSheet (via NimazBottomSheet); ModalBottomSheet content renders in a
 * separate window under Robolectric and is unreliable to query, so it is only
 * smoke-tested for non-crashing composition.
 */
@RunWith(RobolectricTestRunner::class)
class AyahActionsBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

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
    fun `content renders surah name and ayah number header`() {
        composeRule.setThemedContent {
            AyahActionsContent(
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
            AyahActionsContent(
                ayah = ayah(surahNumber = 36),
                surahName = null
            )
        }

        composeRule.onNodeWithText("Surah 36").assertExists()
    }

    @Test
    fun `content renders juz and page chip`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(juzNumber = 1, pageNumber = 1),
                surahName = "Al-Fatihah"
            )
        }

        composeRule.onNodeWithText("Juz 1 | P1").assertExists()
    }

    @Test
    fun `content renders translation when present`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(translation = "All praise is due to Allah")
            )
        }

        composeRule.onNodeWithText("All praise is due to Allah").assertExists()
    }

    @Test
    fun `content hides translation block when translation null`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(translation = null)
            )
        }

        composeRule.onNodeWithText("All praise is due to Allah").assertDoesNotExist()
    }

    @Test
    fun `content renders all default action buttons`() {
        composeRule.setThemedContent {
            AyahActionsContent(ayah = ayah())
        }

        composeRule.onNodeWithContentDescription("Play").assertExists()
        composeRule.onNodeWithContentDescription("Bookmark").assertExists()
        composeRule.onNodeWithContentDescription("Favorite").assertExists()
        composeRule.onNodeWithContentDescription("Copy").assertExists()
        composeRule.onNodeWithContentDescription("Share").assertExists()
        composeRule.onNodeWithContentDescription("Tafseer").assertExists()
    }

    @Test
    fun `onPlayClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(),
                onPlayClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Play").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onBookmarkClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(),
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
            AyahActionsContent(
                ayah = ayah(),
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
            AyahActionsContent(
                ayah = ayah(),
                onTafseerClick = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Tafseer").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `khatam active shows read toggle and fires`() {
        var fired = false
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(),
                isKhatamActive = true,
                isKhatamRead = false,
                onKhatamToggle = { fired = true }
            )
        }

        composeRule.onNodeWithContentDescription("Read").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `khatam read shows unread label`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(),
                isKhatamActive = true,
                isKhatamRead = true
            )
        }

        composeRule.onNodeWithContentDescription("Unread").assertExists()
    }

    @Test
    fun `khatam inactive hides read toggle`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(),
                isKhatamActive = false
            )
        }

        composeRule.onNodeWithContentDescription("Read").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Unread").assertDoesNotExist()
    }

    @Test
    fun `obligatory sajda shows wajib indicator`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(sajdaType = SajdaType.OBLIGATORY, sajdaNumber = 1)
            )
        }

        composeRule.onNodeWithText("Sajdah (Wajib)").assertExists()
    }

    @Test
    fun `recommended sajda shows recommended indicator`() {
        composeRule.setThemedContent {
            AyahActionsContent(
                ayah = ayah(sajdaType = SajdaType.RECOMMENDED, sajdaNumber = 2)
            )
        }

        composeRule.onNodeWithText("Sajdah (Recommended)").assertExists()
    }
}
