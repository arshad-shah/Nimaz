package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `header renders title and subtitle`() {
        composeRule.setThemedContent {
            NimazSheetHeader(title = "Al-Fatihah", subtitle = "Ayah 2")
        }
        composeRule.onNodeWithText("Al-Fatihah").assertExists()
        composeRule.onNodeWithText("Ayah 2").assertExists()
    }

    @Test
    fun `header close button invokes callback`() {
        var closed = false
        composeRule.setThemedContent {
            NimazSheetHeader(title = "T", onClose = { closed = true })
        }
        composeRule.onNodeWithContentDescription("Close").performClick()
        assertEquals(true, closed)
    }

    @Test
    fun `section label renders its text`() {
        composeRule.setThemedContent {
            NimazSheetSectionLabel(text = "Translation")
        }
        composeRule.onNodeWithText("Translation").assertExists()
    }

    @Test
    fun `action row renders each action and fires its click`() {
        var played = 0
        composeRule.setThemedContent {
            NimazSheetActionRow(
                actions = listOf(
                    NimazSheetAction(Icons.Default.PlayArrow, "Play", { played++ }),
                    NimazSheetAction(Icons.Default.Share, "Share", {})
                )
            )
        }
        composeRule.onNodeWithText("Play").assertExists()
        composeRule.onNodeWithText("Share").assertExists()
        composeRule.onNodeWithContentDescription("Play").assertHasClickAction().performClick()
        assertEquals(1, played)
    }

    @Test
    fun `footer primary button fires its click`() {
        var confirmed = false
        composeRule.setThemedContent {
            NimazSheetFooterButtons(
                primaryText = "Confirm",
                onPrimary = { confirmed = true },
                secondaryText = "Cancel",
                onSecondary = {}
            )
        }
        composeRule.onNodeWithText("Cancel").assertExists()
        composeRule.onNodeWithText("Confirm").performClick()
        assertEquals(true, confirmed)
    }
}
