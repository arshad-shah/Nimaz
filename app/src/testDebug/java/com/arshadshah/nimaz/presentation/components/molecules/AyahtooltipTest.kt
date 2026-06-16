package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AyahtooltipTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders default action icons (below-tap layout)`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 100f,
                    parentHeight = 1000f,
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Play").assertExists()
        composeRule.onNodeWithContentDescription("Bookmark").assertExists()
        composeRule.onNodeWithContentDescription("Favorite").assertExists()
        composeRule.onNodeWithContentDescription("Copy").assertExists()
        composeRule.onNodeWithContentDescription("Share").assertExists()
        composeRule.onNodeWithContentDescription("Tafseer").assertExists()
        // Default showTranslationButton = true
        composeRule.onNodeWithContentDescription("Translation").assertExists()
    }

    @Test
    fun `renders in above-tap layout when tapY is large`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 700f,
                    parentHeight = 800f,
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Play").assertExists()
    }

    @Test
    fun `onPlayClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onPlayClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Play").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onBookmarkClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onBookmarkClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Bookmark").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onFavoriteClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onFavoriteClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Favorite").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onCopyClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onCopyClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Copy").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onShareClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onShareClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Share").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onTafseerClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onTafseerClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Tafseer").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `onTranslationClick fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    onTranslationClick = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Translation").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `bookmarked and favorite states still expose toggles`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    isBookmarked = true,
                    isFavorite = true
                )
            }
        }

        composeRule.onNodeWithContentDescription("Bookmark").assertExists()
        composeRule.onNodeWithContentDescription("Favorite").assertExists()
    }

    @Test
    fun `khatam inactive hides khatam toggle`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    isKhatamActive = false
                )
            }
        }

        composeRule.onNodeWithContentDescription("Mark read").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Mark unread").assertDoesNotExist()
    }

    @Test
    fun `khatam active unread shows mark read and toggle fires`() {
        var fired = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    isKhatamActive = true,
                    isKhatamRead = false,
                    onKhatamToggle = { fired = true }
                )
            }
        }

        composeRule.onNodeWithContentDescription("Mark read").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `khatam active read shows mark unread`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    isKhatamActive = true,
                    isKhatamRead = true
                )
            }
        }

        composeRule.onNodeWithContentDescription("Mark unread").assertExists()
    }

    @Test
    fun `translation button hidden when showTranslationButton false`() {
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = {},
                    showTranslationButton = false
                )
            }
        }

        composeRule.onNodeWithContentDescription("Translation").assertDoesNotExist()
        // Other buttons still present.
        composeRule.onNodeWithContentDescription("Play").assertExists()
    }

    @Test
    fun `tapping scrim dismisses`() {
        var dismissed = false
        composeRule.setThemedContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AyahTooltip(
                    tapY = 300f,
                    parentHeight = 1000f,
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Tap near the top-left corner: the tooltip card is offset well below,
        // so this lands on the scrim, not the card.
        composeRule.onRoot().performTouchInput {
            click(Offset(5f, 5f))
        }
        assertThat(dismissed).isTrue()
    }
}
