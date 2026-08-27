package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
 * The one note editor, shared by the Saved screen's bookmark menu and the reader's ayah sheet.
 *
 * Two things it must get right. An empty field saves `null` and not `""` — a bookmark holding an
 * empty string advertises an annotation that is not there. And the draft is keyed on the subject,
 * so reopening the sheet for a different verse starts blank instead of offering the previous
 * verse's note as this one's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NoteEditorSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var dismissed = 0
    private val saved = mutableListOf<String?>()

    private fun render(subject: String = "2:255", initialNote: String? = null) {
        composeRule.setThemedContent {
            NoteEditorSheet(
                subject = subject,
                initialNote = initialNote,
                onDismiss = { dismissed++ },
                onSave = { saved += it },
            )
        }
    }

    private fun type(text: String) {
        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextInput(text)
    }

    @Test
    fun `the sheet names what the note is about`() {
        render(subject = "Al-Baqarah 2:255")

        composeRule.onNodeWithText("Al-Baqarah 2:255").assertIsDisplayed()
    }

    @Test
    fun `a note already written comes back in the field`() {
        render(initialNote = "the verse of the throne")

        composeRule.onNodeWithText("the verse of the throne").assertIsDisplayed()
    }

    @Test
    fun `an empty field has nothing to save`() {
        render()

        composeRule.onNodeWithText(str(R.string.save)).assertIsNotEnabled()
    }

    @Test
    fun `typing something makes it saveable`() {
        render()

        type("worth returning to")

        composeRule.onNodeWithText(str(R.string.save)).assertIsEnabled()
    }

    @Test
    fun `saving hands back the trimmed text`() {
        render()
        type("   worth returning to   ")

        composeRule.onNodeWithText(str(R.string.save)).performClick()

        assertThat(saved).containsExactly("worth returning to")
    }

    @Test
    fun `clearing an existing note saves nothing rather than an empty one`() {
        // A bookmark holding "" claims a note it does not have; the list then shows a note
        // marker on a row with nothing behind it.
        render(initialNote = "the verse of the throne")

        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextClearance()
        composeRule.onNodeWithText(str(R.string.save)).performClick()

        assertThat(saved).containsExactly(null)
    }

    @Test
    fun `clearing an existing note leaves save reachable`() {
        // Unlike a blank new note, a cleared existing one is an edit worth committing.
        render(initialNote = "the verse of the throne")

        composeRule.onAllNodes(hasSetTextAction()).onFirst().performTextClearance()

        composeRule.onNodeWithText(str(R.string.save)).assertIsEnabled()
    }

    @Test
    fun `cancelling dismisses without saving`() {
        render()
        type("half a thought")

        composeRule.onNodeWithText(str(R.string.cancel)).performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(saved).isEmpty()
    }

    @Test
    fun `reopening for a different verse does not carry the last draft across`() {
        var subject by mutableStateOf("2:255")
        composeRule.setThemedContent {
            NoteEditorSheet(
                subject = subject,
                initialNote = null,
                onDismiss = { dismissed++ },
                onSave = { saved += it },
            )
        }

        type("about the throne")
        composeRule.runOnIdle { subject = "18:10" }

        composeRule.onNodeWithText("about the throne").assertDoesNotExist()
    }
}
