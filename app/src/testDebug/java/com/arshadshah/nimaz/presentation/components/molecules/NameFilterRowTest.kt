package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameFilterRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders both labels`() {
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = false,
                onShowAll = {}, onShowFavorites = {},
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("All").assertExists()
        composeRule.onNodeWithText("Favorites").assertExists()
    }

    @Test
    fun `clicking favorites invokes callback`() {
        var fav = false
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = false,
                onShowAll = {}, onShowFavorites = { fav = true },
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("Favorites").performClick()
        assertTrue(fav)
    }

    @Test
    fun `clicking all invokes callback`() {
        var all = false
        composeRule.setThemedContent {
            NameFilterRow(
                showFavoritesOnly = true,
                onShowAll = { all = true }, onShowFavorites = {},
                accent = NamesAccents.allah(),
                allLabel = "All", favoritesLabel = "Favorites",
            )
        }
        composeRule.onNodeWithText("All").performClick()
        assertTrue(all)
    }
}
