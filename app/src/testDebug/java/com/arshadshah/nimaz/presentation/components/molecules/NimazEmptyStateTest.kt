package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazEmptyStateTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders title and message without action`() {
        composeRule.setThemedContent {
            NimazEmptyState(
                title = "All Caught Up",
                message = "You have no missed prayers"
            )
        }
        composeRule.onNodeWithText("All Caught Up").assertExists()
        composeRule.onNodeWithText("You have no missed prayers").assertExists()
    }

    @Test
    fun `does not render action when only label provided`() {
        composeRule.setThemedContent {
            NimazEmptyState(
                title = "No Bookmarks",
                message = "Save your favorites",
                actionLabel = "Browse Quran"
            )
        }
        composeRule.onNodeWithText("Browse Quran").assertDoesNotExist()
    }

    @Test
    fun `renders and triggers action when label and callback provided`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazEmptyState(
                title = "No Bookmarks",
                message = "Save your favorites",
                actionLabel = "Browse Quran",
                onAction = { clicked = true }
            )
        }
        composeRule.onNodeWithText("Browse Quran").assertExists()
        composeRule.onNodeWithText("Browse Quran").performClick()
        assertThat(clicked).isTrue()
    }
}
