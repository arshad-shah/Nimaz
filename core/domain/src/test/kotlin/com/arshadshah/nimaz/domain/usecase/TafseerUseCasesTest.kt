package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TafseerUseCasesTest {

    private lateinit var tafseerRepo: TafseerRepository
    private lateinit var useCases: TafseerUseCases

    private val now = System.currentTimeMillis()

    private val tafseerText = TafseerText(
        id = 1L, tafseerId = "ibn_kathir_en",
        surahNumber = 1, ayahStart = 1, ayahEnd = 1,
        text = "Bismillah is the start of every chapter"
    )

    private val highlight = TafseerHighlight(
        id = 1L, ayahId = 101, tafseerId = "ibn_kathir_en",
        startOffset = 0, endOffset = 10, color = "#FFFF00",
        note = "Important", createdAt = now, updatedAt = now
    )

    private val note = TafseerNote(
        id = 1L, ayahId = 101, tafseerId = "ibn_kathir_en",
        text = "My note", createdAt = now, updatedAt = now
    )

    @Before
    fun setUp() {
        val quranRepo = mockk<com.arshadshah.nimaz.domain.repository.QuranRepository>(relaxed = true)
        tafseerRepo = mockk(relaxed = true)
        useCases = TafseerUseCases(
            getTafseerForAyah = GetTafseerForAyahUseCase(tafseerRepo),
            getHighlightsForRange = GetHighlightsForRangeUseCase(tafseerRepo),
            addHighlight = AddHighlightUseCase(tafseerRepo),
            updateHighlight = UpdateHighlightUseCase(tafseerRepo),
            deleteHighlight = DeleteHighlightUseCase(tafseerRepo),
            getNotesForRange = GetNotesForRangeUseCase(tafseerRepo),
            addNote = AddNoteUseCase(tafseerRepo),
            updateNote = UpdateNoteUseCase(tafseerRepo),
            deleteNote = DeleteNoteUseCase(tafseerRepo),
            getTafseerNotes = GetTafseerNotesUseCase(tafseerRepo, quranRepo)
        )
    }

    @Test
    fun `getTafseerForAyah returns tafseer text`() = runTest {
        coEvery { tafseerRepo.getTafseerForAyah(1, 1, "ibn_kathir_en") } returns tafseerText

        val result = useCases.getTafseerForAyah(1, 1, "ibn_kathir_en")

        assertThat(result).isNotNull()
        assertThat(result!!.surahNumber).isEqualTo(1)
        assertThat(result.text).contains("Bismillah")
    }

    @Test
    fun `getTafseerForAyah returns null when not found`() = runTest {
        coEvery { tafseerRepo.getTafseerForAyah(any(), any(), any()) } returns null

        assertThat(useCases.getTafseerForAyah(99, 99, "unknown")).isNull()
    }

    @Test
    fun `getHighlightsForRange returns flow of highlights`() = runTest {
        every { tafseerRepo.getHighlightsForRange(1, 1, 7, "ibn_kathir_en") } returns
            flowOf(listOf(highlight))

        val result = useCases.getHighlightsForRange(1, 1, 7, "ibn_kathir_en").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].color).isEqualTo("#FFFF00")
    }

    @Test
    fun `addHighlight delegates to repo and returns id`() = runTest {
        coEvery {
            tafseerRepo.addHighlight(101, "ibn_kathir_en", 0, 10, "#FFFF00", "note")
        } returns 1L

        val id = useCases.addHighlight(101, "ibn_kathir_en", 0, 10, "#FFFF00", "note")

        assertThat(id).isEqualTo(1L)
    }

    @Test
    fun `deleteHighlight delegates to repo`() = runTest {
        useCases.deleteHighlight(1L)
        coVerify { tafseerRepo.deleteHighlight(1L) }
    }

    @Test
    fun `addNote delegates to repo and returns id`() = runTest {
        coEvery { tafseerRepo.addNote(101, "ibn_kathir_en", "text") } returns 5L

        val id = useCases.addNote(101, "ibn_kathir_en", "text")

        assertThat(id).isEqualTo(5L)
    }

    @Test
    fun `deleteNote delegates to repo`() = runTest {
        useCases.deleteNote(3L)
        coVerify { tafseerRepo.deleteNote(3L) }
    }

    @Test
    fun `getNotesForRange returns flow of notes`() = runTest {
        every { tafseerRepo.getNotesForRange(1, 1, 7, "ibn_kathir_en") } returns
            flowOf(listOf(note))

        val result = useCases.getNotesForRange(1, 1, 7, "ibn_kathir_en").first()

        assertThat(result).hasSize(1)
        assertThat(result[0].text).isEqualTo("My note")
    }
}
