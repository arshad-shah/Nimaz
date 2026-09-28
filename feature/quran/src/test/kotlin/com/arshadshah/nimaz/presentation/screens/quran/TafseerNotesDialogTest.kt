package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.*
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.presentation.components.organisms.TafseerNotesSheet
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.*
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** One list and one editor: no field mixed into the saved-note cards. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp")
class TafseerNotesDialogTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private val note = TafseerNote(1, 1, TafseerSource.IBN_KATHIR.id, "Remember this passage", 1, 1).toNoteItem(1, 1)
    private val state = MutableStateFlow(TafseerChaptersUiState(isLoading = false, notes = listOf(note)))
    private val vm = mockk<TafseerChaptersViewModel>(relaxed = true) {
        every { this@mockk.state } returns this@TafseerNotesDialogTest.state
        every { edit(any()) } answers { state.value = state.value.copy(editing = firstArg()) }
    }
    private fun render() {
        composeRule.setThemedContent {
            TafseerNotesSheet(1, 1, 1, 1, 7, TafseerSource.IBN_KATHIR, {}, {}, vm)
        }
    }
    @Test fun `list has notes and no text editor`() {
        render()
        composeRule.onNodeWithText(note.note).assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }
    @Test fun `edit opens a focused editor and hides the list`() {
        render()
        composeRule.onNodeWithContentDescription(str(R.string.cd_more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.edit_note)).performClick()
        composeRule.onNodeWithText(str(R.string.save)).assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        composeRule.onNodeWithText(str(R.string.tafseer_tab_notes)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.save)).performClick()
        verify { vm.save(note.note) }
    }
    @Test fun `write error keeps the draft visible`() {
        state.value = state.value.copy(editing = note,
            writeError = com.arshadshah.nimaz.presentation.viewmodel.UiError(R.string.tafseer_note_save_failed))
        render()
        composeRule.onNodeWithText(str(R.string.tafseer_note_save_failed)).assertIsDisplayed()
        composeRule.onNodeWithText(note.note).assertIsDisplayed()
    }
    @Test fun `the list excludes other sources and passages`() {
        state.value = state.value.copy(notes = listOf(note, note.copy(highlightId = 9, surahNumber = 2, note = "Elsewhere")))
        render()
        composeRule.onNodeWithText("Elsewhere").assertDoesNotExist()
    }
}
