package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class TopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ── NimazTopAppBar ─────────────────────────────────────────────────────

    @Test
    fun `renders title`() {
        composeRule.setThemedContent {
            NimazTopAppBar(title = "Nimaz")
        }

        composeRule.onNodeWithText("Nimaz").assertExists()
    }

    @Test
    fun `renders subtitle when provided`() {
        composeRule.setThemedContent {
            NimazTopAppBar(
                title = "Nimaz",
                subtitle = "Next: Dhuhr at 12:30 PM"
            )
        }

        composeRule.onNodeWithText("Nimaz").assertExists()
        composeRule.onNodeWithText("Next: Dhuhr at 12:30 PM").assertExists()
    }

    @Test
    fun `does not render subtitle when null`() {
        composeRule.setThemedContent {
            NimazTopAppBar(
                title = "Nimaz",
                subtitle = null
            )
        }

        composeRule.onNodeWithText("Next: Dhuhr at 12:30 PM").assertDoesNotExist()
    }

    @Test
    fun `renders actions content`() {
        composeRule.setThemedContent {
            NimazTopAppBar(
                title = "Nimaz",
                actions = { Text("ActionLabel") }
            )
        }

        composeRule.onNodeWithText("ActionLabel").assertExists()
    }

    // ── NimazBackTopAppBar ─────────────────────────────────────────────────

    @Test
    fun `back bar renders title and back button`() {
        composeRule.setThemedContent {
            NimazBackTopAppBar(
                title = "Settings",
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Settings").assertExists()
        // Back icon contentDescription is the literal "Navigate back"
        composeRule.onNodeWithContentDescription("Navigate back").assertExists()
    }

    @Test
    fun `back button click fires onBackClick`() {
        var backClicked = false
        composeRule.setThemedContent {
            NimazBackTopAppBar(
                title = "Settings",
                onBackClick = { backClicked = true }
            )
        }

        composeRule.onNodeWithContentDescription("Navigate back").performClick()
        assertThat(backClicked).isTrue()
    }

    @Test
    fun `back bar renders subtitle when provided`() {
        composeRule.setThemedContent {
            NimazBackTopAppBar(
                title = "Settings",
                onBackClick = {},
                subtitle = "Customize your experience"
            )
        }

        composeRule.onNodeWithText("Customize your experience").assertExists()
    }
}
