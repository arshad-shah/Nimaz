package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuranQuickActionsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders all four action labels`() {
        composeRule.setThemedContent {
            QuranQuickActions(
                onJuzClick = {},
                onPageClick = {},
                onBookmarksClick = {},
                onFavoritesClick = {},
            )
        }
        composeRule.onNodeWithText("Juz").assertExists()
        composeRule.onNodeWithText("Page").assertExists()
        composeRule.onNodeWithText("Bookmarks").assertExists()
        composeRule.onNodeWithText("Favorites").assertExists()
    }

    @Test
    fun `each action fires its callback`() {
        var juz = false
        var page = false
        var bookmarks = false
        var favorites = false
        composeRule.setThemedContent {
            QuranQuickActions(
                onJuzClick = { juz = true },
                onPageClick = { page = true },
                onBookmarksClick = { bookmarks = true },
                onFavoritesClick = { favorites = true },
            )
        }
        composeRule.onNodeWithText("Juz").performClick()
        composeRule.onNodeWithText("Page").performClick()
        composeRule.onNodeWithText("Bookmarks").performClick()
        composeRule.onNodeWithText("Favorites").performClick()
        assertThat(juz).isTrue()
        assertThat(page).isTrue()
        assertThat(bookmarks).isTrue()
        assertThat(favorites).isTrue()
    }
}
