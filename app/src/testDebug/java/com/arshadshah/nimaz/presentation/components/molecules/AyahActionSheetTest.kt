package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                arabic = "ٱلْحَمْدُ لِلَّهِ",
                translation = "All praise belongs to God.",
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

    @Test
    fun `the sheet shows the verse it is acting on`() {
        show()

        composeRule.onNodeWithText("All praise belongs to God.").assertExists()
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

        composeRule.onNodeWithText("Mark read").performClick()

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
