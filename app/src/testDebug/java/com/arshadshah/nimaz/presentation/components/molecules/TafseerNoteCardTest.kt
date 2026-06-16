package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerNoteCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleNote() = TafseerNote(
        id = 42L,
        ayahId = 1,
        tafseerId = "ibn_kathir_en",
        text = "This is my reflection note",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L
    )

    @Test
    fun `renders note text`() {
        composeRule.setThemedContent {
            TafseerNoteCard(
                note = sampleNote(),
                onEdit = {},
                onDelete = {}
            )
        }
        composeRule.onNodeWithText("This is my reflection note").assertExists()
    }

    @Test
    fun `edit button invokes onEdit with note`() {
        var editedNote: TafseerNote? = null
        val note = sampleNote()
        composeRule.setThemedContent {
            TafseerNoteCard(
                note = note,
                onEdit = { editedNote = it },
                onDelete = {}
            )
        }
        composeRule.onNodeWithContentDescription("Edit").performClick()
        assertThat(editedNote).isEqualTo(note)
    }

    @Test
    fun `delete button invokes onDelete with id`() {
        var deletedId: Long? = null
        composeRule.setThemedContent {
            TafseerNoteCard(
                note = sampleNote(),
                onEdit = {},
                onDelete = { deletedId = it }
            )
        }
        composeRule.onNodeWithContentDescription("Delete").performClick()
        assertThat(deletedId).isEqualTo(42L)
    }
}
