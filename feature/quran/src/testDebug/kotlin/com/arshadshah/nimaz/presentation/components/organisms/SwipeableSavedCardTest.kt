package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.core.ui.R
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
 * The card every saved item is drawn as, and the two controls attached to it.
 *
 * The overflow menu is the part with a real invariant: it closes itself *before* the action
 * fires. A menu left open over a row the action is about to remove outlives the row it was
 * anchored to.
 *
 * The rest is about not showing what is not there. A card with no note must not leave an empty
 * italic line where the note preview goes, and a card the caller has opted out of swiping must
 * not delete itself when the reader scrolls the list sideways.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class SwipeableSavedCardTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var clicked = 0
    private var deleted = 0
    private val menuTaps = mutableListOf<String>()

    private fun actions(destructive: Boolean = true) = listOf(
        NimazMenuAction(
            text = "Share",
            icon = Icons.Default.Share,
            onClick = { menuTaps += "share" },
        ),
        NimazMenuAction(
            text = "Edit note",
            icon = Icons.Default.Edit,
            onClick = { menuTaps += "edit" },
        ),
        NimazMenuAction(
            text = "Remove",
            icon = Icons.Default.Delete,
            onClick = { menuTaps += "remove" },
            destructive = destructive,
        ),
    )

    private fun render(
        subtitle: String? = null,
        arabicText: String? = null,
        note: String? = null,
        accent: Color? = null,
        kindLabel: String? = null,
        enableSwipeToDelete: Boolean = true,
    ) {
        composeRule.setThemedContent {
            SwipeableSavedCard(
                title = "Al-Fatihah · 1",
                timestamp = System.currentTimeMillis(),
                menuActions = actions(),
                onClick = { clicked++ },
                onDelete = { deleted++ },
                enableSwipeToDelete = enableSwipeToDelete,
                subtitle = subtitle,
                arabicText = arabicText,
                note = note,
                accent = accent,
                kindLabel = kindLabel,
                leading = { Text("BOOKMARK") },
            )
        }
    }

    // ---- What the card shows ----

    @Test
    fun `the locator and the leading badge are always on the card`() {
        render()

        composeRule.onNodeWithText("Al-Fatihah · 1").assertIsDisplayed()
        composeRule.onNodeWithText("BOOKMARK").assertIsDisplayed()
    }

    @Test
    fun `the optional lines are drawn when the caller supplies them`() {
        render(
            subtitle = "The Noble Qur'an",
            arabicText = "بِسْمِ ٱللَّهِ",
            note = "worth returning to",
            accent = Color.Red,
            kindLabel = "NOTE",
        )

        composeRule.onNodeWithText("The Noble Qur'an").assertIsDisplayed()
        composeRule.onNodeWithText("بِسْمِ ٱللَّهِ").assertIsDisplayed()
        composeRule.onNodeWithText("worth returning to").assertIsDisplayed()
        composeRule.onNodeWithText("NOTE").assertIsDisplayed()
    }

    @Test
    fun `a blank note is not drawn as an empty line`() {
        render(note = "   ", subtitle = "  ")

        composeRule.onNodeWithText("   ").assertDoesNotExist()
    }

    @Test
    fun `the kind label needs a colour to be drawn in`() {
        // kindLabel is set *in* the accent; without one there is nothing to colour it with.
        render(kindLabel = "NOTE", accent = null)

        composeRule.onNodeWithText("NOTE").assertDoesNotExist()
    }

    @Test
    fun `tapping the card body opens the item`() {
        render()

        composeRule.onNodeWithText("Al-Fatihah · 1").performClick()

        assertThat(clicked).isEqualTo(1)
    }

    // ---- The overflow menu ----

    @Test
    fun `the overflow menu holds every action the caller gave it`() {
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()

        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Edit note").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()
    }

    @Test
    fun `choosing an action runs it`() {
        render()
        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()

        composeRule.onNodeWithText("Edit note").performClick()

        assertThat(menuTaps).containsExactly("edit")
    }

    @Test
    fun `the menu closes itself before the action fires`() {
        // A menu left open over a row the action is about to remove outlives its anchor.
        render()
        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()

        composeRule.onNodeWithText("Remove").performClick()

        composeRule.onNodeWithText("Share").assertDoesNotExist()
    }

    // ---- Swiping it away ----

    @Test
    fun `swiping the card away asks for it to be deleted`() {
        render()

        composeRule.onNodeWithText("Al-Fatihah · 1").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun `a card the caller opted out of swiping stays put`() {
        render(enableSwipeToDelete = false)

        composeRule.onNodeWithText("Al-Fatihah · 1").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertThat(deleted).isEqualTo(0)
    }
}
