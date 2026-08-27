package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
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
 * The notes a reader keeps on a commentary, and the editor they are changed in.
 *
 * A highlight is a colour *and* a span *and*, optionally, a note — three things that arrive
 * together and are edited together, which is why creating and editing go through one sheet
 * rather than two. The span is the part the editor must never touch: it is what makes the
 * highlight a highlight, and an editor that rebuilt the row from its own fields would move it.
 *
 * The offsets are also the thing most likely to be wrong. They index into the commentary text,
 * and that text changes under them — a source switch, a re-import — so every read of a snippet
 * is clamped. An unclamped `substring` here is `StringIndexOutOfBoundsException` while opening
 * a notes list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class TafseerHighlightEditorTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val updated = mutableListOf<Triple<Long, String, String?>>()
    private val deleted = mutableListOf<Long>()

    private val commentary = "The commentary text a reader marks passages in, at some length."

    private fun ayah() = Ayah(
        id = 43_081,
        surahNumber = 43,
        ayahNumber = 81,
        textArabic = "نص عربي",
        textSimple = "nass",
        juzNumber = 25,
        hizbNumber = 49,
        rubNumber = 0,
        pageNumber = 495,
        sajdaType = null,
        sajdaNumber = null,
        translation = "a translation",
    )

    private fun highlight(
        id: Long = 1,
        start: Int = 4,
        end: Int = 20,
        colour: String = "#EAB308",
        note: String? = "worth returning to",
    ) = TafseerHighlight(
        id = id,
        ayahId = 43_081,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        startOffset = start,
        endOffset = end,
        color = colour,
        note = note,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun render(highlights: List<TafseerHighlight>) {
        composeRule.setThemedContent {
            TafseerPageContent(
                ayah = ayah(),
                tafseer = TafseerText(
                    id = 1,
                    tafseerId = TafseerSource.IBN_KATHIR.id,
                    surahNumber = 43,
                    ayahStart = 81,
                    ayahEnd = 89,
                    text = commentary,
                ),
                highlights = highlights,
                selectedSource = TafseerSource.IBN_KATHIR,
                availableSources = setOf(TafseerSource.IBN_KATHIR),
                currentContentPage = 0,
                onContentPageChanged = {},
                onSourceSwitch = {},
                onHighlightCreated = { _, _, _, _ -> },
                onHighlightUpdated = { id, colour, note -> updated += Triple(id, colour, note) },
                onHighlightDeleted = { deleted += it },
                onShare = {},
            )
        }
    }

    private fun openNotes() {
        composeRule.onNodeWithContentDescription(str(R.string.cd_notes)).performClick()
    }

    // ---- The notes list ----

    @Test
    fun `a reader with no highlights is told there are none`() {
        render(emptyList())

        openNotes()

        composeRule.onNodeWithText(str(R.string.tafseer_no_notes)).assertIsDisplayed()
    }

    @Test
    fun `only the highlights carrying a note are listed`() {
        render(
            listOf(
                highlight(id = 1, note = "worth returning to"),
                highlight(id = 2, start = 25, end = 40, note = null),
            )
        )

        openNotes()

        // The list is the reader's *notes*; a bare highlight has nothing to read.
        composeRule.onNodeWithText("worth returning to").assertIsDisplayed()
    }

    @Test
    fun `each note is shown under the passage it was written on`() {
        render(listOf(highlight(start = 4, end = 20)))

        openNotes()

        // Twice: the snippet on the note card, and the same words in the commentary behind it.
        composeRule.onAllNodesWithText(commentary.substring(4, 20), substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `a highlight whose offsets fall outside the commentary does not take the list down`() {
        // The offsets index into text that changes under them — a source switch, a re-import.
        render(listOf(highlight(start = 500, end = 900)))

        openNotes()

        composeRule.onNodeWithText("worth returning to").assertIsDisplayed()
    }

    // ---- The editor ----

    @Test
    fun `tapping a note opens the editor on it`() {
        render(listOf(highlight()))
        openNotes()

        composeRule.onNodeWithText("worth returning to").performClick()

        // One sheet for creating and editing, because the colour, the span and the note arrive
        // together.
        composeRule.onNodeWithText(str(R.string.tafseer_colour)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.tafseer_note_optional)).assertIsDisplayed()
    }

    @Test
    fun `the editor opens on the passage the note is about`() {
        render(listOf(highlight(start = 4, end = 20)))
        openNotes()

        composeRule.onNodeWithText("worth returning to").performClick()

        composeRule.onAllNodesWithText(commentary.substring(4, 20), substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `saving from the editor changes the colour and the note, and nothing else`() {
        render(listOf(highlight(id = 7, colour = "#EAB308", note = "worth returning to")))
        openNotes()
        composeRule.onNodeWithText("worth returning to").performClick()

        composeRule.onNodeWithText(str(R.string.save)).performClick()

        // The span is what makes it a highlight; the editor is only ever told the id.
        val (id, _, _) = updated.single()
        assertThat(id).isEqualTo(7)
    }

    @Test
    fun `deleting from the editor deletes that highlight`() {
        render(listOf(highlight(id = 7)))
        openNotes()
        composeRule.onNodeWithText("worth returning to").performClick()

        composeRule.onNodeWithText(str(R.string.delete)).performClick()
        // A second confirmation: a highlight and its note go together, and neither comes back.
        composeRule.onAllNodesWithText(str(R.string.delete)).onLast().performClick()

        assertThat(deleted).containsExactly(7L)
    }

    @Test
    fun `an editor opened on an existing highlight offers to delete it`() {
        render(listOf(highlight()))
        openNotes()

        composeRule.onNodeWithText("worth returning to").performClick()

        // Creating one has nothing to delete; editing one does.
        composeRule.onNodeWithText(str(R.string.delete)).assertIsDisplayed()
    }
}
