package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.presentation.screens.str
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.TafseerViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Writing on the commentary: the notes dialog on the tafseer screen.
 *
 * One field does two jobs — writing a new note and editing an existing one — and the button
 * beside it changes what it says accordingly. That is the part worth pinning: a dialog stuck in
 * "edit" mode writes the reader's next note over the one they were editing, and there is no way
 * back from that.
 *
 * The empty draft is the other. A note with nothing in it is not a note, so the button is
 * disabled rather than saving a blank row the list then shows as an empty card.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerNotesDialogTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(TafseerUiState())
    private val events = mutableListOf<TafseerEvent>()

    private val viewModel: TafseerViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@TafseerNotesDialogTest.state
        every { onEvent(any()) } answers { events += firstArg<TafseerEvent>() }
    }

    private fun ayah(number: Int) = Ayah(
        id = 43_000 + number,
        surahNumber = 43,
        ayahNumber = number,
        textArabic = "نص",
        textSimple = "nass",
        juzNumber = 25,
        hizbNumber = 49,
        rubNumber = 0,
        pageNumber = 495,
        sajdaType = null,
        sajdaNumber = null,
        translation = "a translation",
    )

    private fun note(id: Long, text: String) = TafseerNote(
        id = id,
        ayahId = 43_081,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        text = text,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun render(notes: List<TafseerNote> = emptyList()) {
        state.value = TafseerUiState(
            isLoading = false,
            surahNumber = 43,
            surahName = "Az-Zukhruf",
            ayahs = listOf(ayah(81)),
            currentAyahIndex = 0,
            currentTafseer = TafseerText(
                id = 1,
                tafseerId = TafseerSource.IBN_KATHIR.id,
                surahNumber = 43,
                ayahStart = 81,
                ayahEnd = 89,
                text = "The commentary.",
            ),
            availableSources = setOf(TafseerSource.IBN_KATHIR),
            notes = notes,
        )
        composeRule.setThemedContent {
            TafseerScreen(
                surahNumber = 43,
                ayahNumber = 81,
                onNavigateBack = {},
                viewModel = viewModel,
            )
        }
    }

    private fun openNotes() {
        // The reader's bottom bar carries a second control also described as "Notes" (the
        // highlight notes sheet), so the app-bar one is picked by being the higher of the two.
        val candidates = composeRule.onAllNodesWithContentDescription(str(R.string.tafseer_notes))
        val appBar = candidates.fetchSemanticsNodes().indices
            .minByOrNull { candidates[it].fetchSemanticsNode().boundsInRoot.top }!!
        candidates[appBar].performClick()
    }

    private fun typeDraft(text: String) {
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(text)
    }

    // ---- Writing one ----

    @Test
    fun `the notes dialog opens from the app bar`() {
        render()

        openNotes()

        composeRule.onNodeWithText(str(R.string.tafseer_note_hint)).assertIsDisplayed()
    }

    @Test
    fun `an empty note cannot be saved`() {
        render()
        openNotes()

        // A blank row shows in the list as an empty card and cannot be told apart from a bug.
        composeRule.onNodeWithText(str(R.string.tafseer_note_add)).assertIsNotEnabled()
    }

    @Test
    fun `typing something makes the note saveable`() {
        render()
        openNotes()

        typeDraft("a thought")

        composeRule.onNodeWithText(str(R.string.tafseer_note_add)).assertIsEnabled()
    }

    @Test
    fun `saving a new note hands the text to the view model`() {
        render()
        openNotes()
        typeDraft("a thought")

        composeRule.onNodeWithText(str(R.string.tafseer_note_add)).performClick()

        assertThat(events.filterIsInstance<TafseerEvent.AddNote>().map { it.text })
            .contains("a thought")
    }

    // ---- Reading and changing them ----

    @Test
    fun `notes already written are listed`() {
        render(listOf(note(1, "worth returning to"), note(2, "and this")))

        openNotes()

        composeRule.onNodeWithText("worth returning to").assertIsDisplayed()
        composeRule.onNodeWithText("and this").assertIsDisplayed()
    }

    @Test
    fun `editing a note loads it into the field and offers to save`() {
        render(listOf(note(1, "worth returning to")))
        openNotes()

        composeRule.onNodeWithText(str(R.string.tafseer_note_edit)).performClick()

        // The button changes what it says, because the field is now doing the other job.
        composeRule.onNodeWithText(str(R.string.save)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tafseer_note_add)).assertDoesNotExist()
    }

    @Test
    fun `saving an edit updates that note rather than writing a new one`() {
        render(listOf(note(1, "worth returning to")))
        openNotes()
        composeRule.onNodeWithText(str(R.string.tafseer_note_edit)).performClick()

        composeRule.onNodeWithText(str(R.string.save)).performClick()

        assertThat(events.filterIsInstance<TafseerEvent.UpdateNote>().single().note.id)
            .isEqualTo(1L)
        assertThat(events.filterIsInstance<TafseerEvent.AddNote>()).isEmpty()
    }

    @Test
    fun `the dialog leaves edit mode once the edit is saved`() {
        render(listOf(note(1, "worth returning to")))
        openNotes()
        composeRule.onNodeWithText(str(R.string.tafseer_note_edit)).performClick()

        composeRule.onNodeWithText(str(R.string.save)).performClick()

        // Stuck in edit mode, the reader's next note overwrites the one they just changed.
        composeRule.onNodeWithText(str(R.string.tafseer_note_add)).assertIsDisplayed()
    }

    @Test
    fun `deleting a note asks the view model to delete it`() {
        render(listOf(note(9, "worth returning to")))
        openNotes()

        composeRule.onNodeWithText(str(R.string.delete)).performClick()

        assertThat(events.filterIsInstance<TafseerEvent.DeleteNote>().map { it.noteId })
            .contains(9L)
    }
}
