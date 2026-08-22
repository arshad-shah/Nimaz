package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Composable
    private fun teal(): NamesAccent = NamesAccents.allah()

    @Test
    fun `renders number, arabic, transliteration and meaning`() {
        composeRule.setThemedContent {
            NameCard(
                number = 52,
                arabicName = "ArabicRahman",
                primaryLabel = "Ar-Rahman",
                secondaryLabel = "The Most Compassionate",
                isFavorite = false,
                accent = teal(),
                onClick = {},
                onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithText("52").assertExists()
        composeRule.onNodeWithText("ArabicRahman").assertExists()
        composeRule.onNodeWithText("Ar-Rahman").assertExists()
        composeRule.onNodeWithText("The Most Compassionate").assertExists()
    }

    @Test
    fun `card click invokes callback`() {
        var clicked = false
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = false, accent = teal(),
                onClick = { clicked = true }, onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithText("P").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `favorite click invokes callback and shows add description when not favorite`() {
        var favClicked = false
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = false, accent = teal(),
                onClick = {}, onFavoriteClick = { favClicked = true },
            )
        }
        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertTrue(favClicked)
    }

    @Test
    fun `shows remove description when favorite`() {
        composeRule.setThemedContent {
            NameCard(
                number = 1, arabicName = "A", primaryLabel = "P", secondaryLabel = "S",
                isFavorite = true, accent = teal(),
                onClick = {}, onFavoriteClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Remove from favorites").assertExists()
    }

    @Test
    fun `prophets variant renders title line and era chip`() {
        composeRule.setThemedContent {
            NameCard(
                number = 10,
                arabicName = "ArabicYusuf",
                primaryLabel = "Yusuf",
                secondaryLabel = "The Chosen of Allah",
                isFavorite = false,
                accent = NamesAccents.prophets(),
                onClick = {},
                onFavoriteClick = {},
                titleLabel = "Safiyyullah",
                eraChip = "Ancient Egypt",
            )
        }
        composeRule.onNodeWithText("Yusuf").assertExists()
        composeRule.onNodeWithText("Safiyyullah").assertExists()
        composeRule.onNodeWithText("Ancient Egypt").assertExists()
    }
}
