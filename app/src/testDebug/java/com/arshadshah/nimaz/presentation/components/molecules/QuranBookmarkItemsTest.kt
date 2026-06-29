package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranBookmarkItemsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun bookmark(
        surahName: String? = "Al-Fatihah",
        ayahText: String? = "In the name of Allah",
        surahNumber: Int = 1,
        ayahNumber: Int = 1
    ) = QuranBookmark(
        id = 1L,
        ayahId = 1,
        surahNumber = surahNumber,
        ayahNumber = ayahNumber,
        surahName = surahName,
        ayahText = ayahText,
        note = null,
        color = null,
        createdAt = 0L,
        updatedAt = 0L
    )

    // ----- BookmarkListItem -----

    @Test
    fun bookmarkListItem_withNameAndText_showsAllFields() {
        composeRule.setThemedContent {
            BookmarkListItem(bookmark = bookmark(), onClick = {})
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("Verse 1").assertExists()
        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun bookmarkListItem_nullSurahName_usesFallback() {
        composeRule.setThemedContent {
            BookmarkListItem(
                bookmark = bookmark(surahName = null, surahNumber = 5),
                onClick = {}
            )
        }

        composeRule.onNodeWithText("Surah 5").assertExists()
    }

    @Test
    fun bookmarkListItem_blankAyahText_hidesText() {
        composeRule.setThemedContent {
            BookmarkListItem(
                bookmark = bookmark(ayahText = ""),
                onClick = {}
            )
        }

        composeRule.onNodeWithText("In the name of Allah").assertDoesNotExist()
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
    }

    @Test
    fun bookmarkListItem_click_invokesCallback() {
        var clicked = false
        composeRule.setThemedContent {
            BookmarkListItem(bookmark = bookmark(), onClick = { clicked = true })
        }

        composeRule.onNodeWithText("Al-Fatihah").performClick()
        assertThat(clicked).isTrue()
    }

    // ----- BookmarkCard -----

    @Test
    fun bookmarkCard_withNameAndText_showsAllFields() {
        composeRule.setThemedContent {
            BookmarkCard(bookmark = bookmark(ayahNumber = 3), onClick = {})
        }

        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("Verse 3").assertExists()
        composeRule.onNodeWithText("In the name of Allah").assertExists()
    }

    @Test
    fun bookmarkCard_nullSurahName_usesFallback_andBlankTextHidden() {
        composeRule.setThemedContent {
            BookmarkCard(
                bookmark = bookmark(surahName = null, surahNumber = 7, ayahText = null),
                onClick = {}
            )
        }

        composeRule.onNodeWithText("Surah 7").assertExists()
        composeRule.onNodeWithText("In the name of Allah").assertDoesNotExist()
    }

    @Test
    fun bookmarkCard_click_invokesCallback() {
        var clicked = false
        composeRule.setThemedContent {
            BookmarkCard(bookmark = bookmark(), onClick = { clicked = true })
        }

        composeRule.onNodeWithText("Al-Fatihah").performClick()
        assertThat(clicked).isTrue()
    }
}
