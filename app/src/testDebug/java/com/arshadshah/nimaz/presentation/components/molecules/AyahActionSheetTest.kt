package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.arshadshah.nimaz.domain.model.AyahReference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AyahActionSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun actions(
        onBookmark: () -> Unit = {},
        onMarkRead: () -> Unit = {},
    ) = AyahSheetActions(
        onPlayFromHere = {}, onRepeatAyah = {}, onBookmark = onBookmark,
        onFavourite = {}, onNote = {}, onTafseer = {}, onSubjects = {},
        onCopy = {}, onShare = {}, onMarkReadForKhatam = onMarkRead,
    )

    private fun show(
        isKhatamActive: Boolean = false,
        isBookmarked: Boolean = false,
        actions: AyahSheetActions = actions(),
    ) {
        composeRule.setThemedContent {
            AyahActionSheet(
                reference = AyahReference(18, 54, "Al-Kahf"),
                juzNumber = 15,
                pageNumber = 299,
                isBookmarked = isBookmarked,
                isFavourite = false,
                isKhatamActive = isKhatamActive,
                actions = actions,
                onDismiss = {},
            )
        }
    }

    @Test
    fun `the sheet titles itself with the shared reference format`() {
        show()

        composeRule.onNodeWithText("Al-Kahf 18:54").assertExists()
    }

    /**
     * The sheet is actions only. The verse is on the screen behind it — the reader tapped that
     * verse to open this — so reprinting it pushed the actions down for no information the
     * header's reference does not already carry.
     */
    @Test
    fun `the sheet names the verse without reprinting it`() {
        show()

        composeRule.onNodeWithText("Al-Kahf 18:54").assertExists()
        composeRule.onNodeWithText("Juz 15", substring = true).assertExists()
        composeRule.onNodeWithText("All praise belongs to God.").assertDoesNotExist()
    }

    @Test
    fun `the khatam action is hidden without an active khatam`() {
        show(isKhatamActive = false)

        composeRule.onNodeWithText("Mark read").assertDoesNotExist()
    }

    @Test
    fun `the khatam action appears with an active khatam`() {
        show(isKhatamActive = true)

        composeRule.onNodeWithText("Mark read").assertExists()
    }

    @Test
    fun `tapping bookmark reports it once`() {
        var calls = 0
        show(actions = actions(onBookmark = { calls++ }))

        composeRule.onNodeWithText("Bookmark").performClick()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `tapping mark-read reports it once`() {
        var calls = 0
        show(isKhatamActive = true, actions = actions(onMarkRead = { calls++ }))

        // Scrolled to first: it is the last tile in a ten-action grid, so on a phone-sized
        // sheet it starts below the fold and a click at its centre would land off-window.
        composeRule.onNodeWithText("Mark read").performScrollTo().performClick()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `bookmark and favourite are toggles that show their state`() {
        show(isBookmarked = true)

        // A reader can undo from where they did it, so the label has to say which way it goes.
        composeRule.onNodeWithText("Unbookmark").assertExists()
        composeRule.onNodeWithText("Bookmark").assertDoesNotExist()
    }
}
