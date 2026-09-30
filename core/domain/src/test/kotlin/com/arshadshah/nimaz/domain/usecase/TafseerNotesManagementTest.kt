package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.*
import com.arshadshah.nimaz.domain.repository.*
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TafseerNotesManagementTest {
    private val repo = mockk<TafseerRepository>(relaxed = true)
    private val quran = mockk<QuranRepository>(relaxed = true)
    private val reflection = TafseerNote(1, 1, "ibn_kathir_en", "Reflection", 200, 200)
    private val highlight = TafseerHighlight(1, 1, "maariful_quran_en", 2, 8, "#FDE68A", "Highlight note", 100, 100)
    private val actions = TafseerNoteActions(repo)

    @Test fun `both stores are included with unique keys and exact source quote`() = runTest {
        every { repo.getAllNotes() } returns flowOf(listOf(reflection))
        every { repo.getAllHighlights() } returns flowOf(listOf(highlight, highlight.copy(id = 2, note = null)))
        coEvery { quran.getAyahById(1) } returns Ayah(1, 1, 1, "نص", "text", 1, 1, 0, 1, null, null)
        coEvery { repo.getTafseerForAyah(1, 1, "maariful_quran_en") } returns TafseerText(1, "maariful_quran_en", 1, 1, 7, "0123456789")
        val notes = GetTafseerNotesUseCase(repo, quran)().first()
        assertThat(notes.map { it.key }).containsExactly("note:1", "highlight:1").inOrder()
        assertThat(notes[1].quote).isEqualTo("234567")
        assertThat(notes[1].tafseerId).isEqualTo("maariful_quran_en")
    }
    @Test fun `deleting highlight note removes its record and undo restores the full highlight`() = runTest {
        val item = highlight.toNoteItem(1, 1, "0123456789")
        actions.delete(item)
        coVerify { repo.deleteHighlight(highlight.id) }
        coVerify(exactly = 0) { repo.updateHighlight(any()) }
        coVerify(exactly = 0) { repo.deleteNote(any()) }
        actions.restore(item)
        coVerify { repo.restoreHighlight(highlight) }
    }
    @Test fun `reflection undo restores original identity and timestamps`() = runTest {
        val item = reflection.toNoteItem(1, 1)
        actions.delete(item)
        actions.restore(item)
        coVerify { repo.deleteNote(1) }
        coVerify { repo.restoreNote(reflection) }
    }
    @Test fun `save edits exactly the selected store`() = runTest {
        actions.save(reflection.toNoteItem(1, 1), "  changed  ")
        coVerify { repo.updateNote(reflection.copy(text = "changed")) }
        coVerify(exactly = 0) { repo.updateHighlight(any()) }
    }
    @Test fun `pagination preserves whitespace and mixed script offsets`() {
        val text = ("English.\n\nبسم الله الرحمن الرحيم\n  " + "a".repeat(850) + "\n").repeat(4)
        val pages = splitTafseerIntoPages(text)
        assertThat(pages.joinToString("") { it.text }).isEqualTo(text)
        pages.forEach {
            assertThat(text.substring(it.globalStartOffset, it.globalEndOffset)).isEqualTo(it.text)
        }
    }
}
