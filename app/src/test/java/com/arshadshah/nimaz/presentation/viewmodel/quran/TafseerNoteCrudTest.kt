package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The tafseer note loop, end to end at the ViewModel boundary.
 *
 * `AddNote`, `UpdateNote` and `DeleteNote` are declared, handled — and emitted by nothing, so
 * **none of these three handlers has ever executed in production**, and no test covered them:
 * the only Tafseer test pins collector cancellation. #357 says to treat them as unproven and
 * to expect at least one to be wrong before wiring the UI. These are the tests that decide it.
 *
 * The notes themselves are stored against the *ayah*, while the reader collects them for the
 * whole commentary **block** — so a note added on one ayah must show up while reading any ayah
 * of the same block, and must survive the ayah-by-ayah swipe that re-arms the collector.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TafseerNoteCrudTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var tafseerUseCases: TafseerUseCases
    private lateinit var quranUseCases: QuranUseCases
    private val telemetry = RecordingTelemetry()

    /** Stands in for the notes table: what the reader's collector sees. */
    private val notes = MutableStateFlow(emptyList<TafseerNote>())
    private var nextId = 1L

    private val block = TafseerText(
        id = 1L,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        surahNumber = 2,
        ayahStart = 1,
        ayahEnd = 5,
        text = "commentary",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tafseerUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)

        every { quranUseCases.getAyahsBySurah(any()) } returns
            flowOf(listOf(ayah(id = 101, number = 1), ayah(id = 102, number = 2)))
        coEvery { tafseerUseCases.getTafseerForAyah(any(), any(), any()) } returns block
        every { tafseerUseCases.getHighlightsForRange(any(), any(), any(), any()) } returns
            flowOf(emptyList())
        every { tafseerUseCases.getNotesForRange(any(), any(), any(), any()) } returns notes

        // A fake store, so the round-trip is real rather than a verified call.
        coEvery { tafseerUseCases.addNote(any(), any(), any()) } answers {
            val id = nextId++
            notes.value = notes.value + TafseerNote(
                id = id,
                ayahId = firstArg(),
                tafseerId = secondArg(),
                text = thirdArg(),
                createdAt = 0L,
                updatedAt = 0L,
            )
            id
        }
        coEvery { tafseerUseCases.updateNote(any()) } answers {
            val updated = firstArg<TafseerNote>()
            notes.value = notes.value.map { if (it.id == updated.id) updated else it }
        }
        coEvery { tafseerUseCases.deleteNote(any()) } answers {
            val id = firstArg<Long>()
            notes.value = notes.value.filterNot { it.id == id }
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TafseerViewModel(tafseerUseCases, quranUseCases, telemetry)

    private fun loadedViewModel() = viewModel().also {
        it.onEvent(TafseerEvent.LoadSurah(surahNumber = 2, ayahNumber = 1))
    }

    @Test
    fun `a note created on the ayah being read appears in the reader`() = runTest {
        val vm = loadedViewModel()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddNote("a reflection"))
        advanceUntilIdle()

        assertThat(vm.state.value.notes.map { it.text }).containsExactly("a reflection")
        // Stored against the ayah on screen, not the block's first ayah.
        assertThat(vm.state.value.notes.single().ayahId).isEqualTo(101)
    }

    @Test
    fun `updating a note changes that row instead of adding a second`() = runTest {
        val vm = loadedViewModel()
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.AddNote("first draft"))
        advanceUntilIdle()

        val existing = vm.state.value.notes.single()
        vm.onEvent(TafseerEvent.UpdateNote(existing.copy(text = "second draft")))
        advanceUntilIdle()

        assertThat(vm.state.value.notes).hasSize(1)
        assertThat(vm.state.value.notes.single().text).isEqualTo("second draft")
        // The id has to round-trip, or the update targets nothing and the reader
        // silently keeps the old text.
        assertThat(vm.state.value.notes.single().id).isEqualTo(existing.id)
    }

    @Test
    fun `deleting a note removes it from the reader`() = runTest {
        val vm = loadedViewModel()
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.AddNote("to be deleted"))
        advanceUntilIdle()
        val existing = vm.state.value.notes.single()

        vm.onEvent(TafseerEvent.DeleteNote(existing.id))
        advanceUntilIdle()

        assertThat(vm.state.value.notes).isEmpty()
    }

    @Test
    fun `a note added on the second ayah is still there after swiping back`() = runTest {
        val vm = loadedViewModel()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.AddNote("on the second ayah"))
        advanceUntilIdle()

        // Swiping re-arms the notes collector for the same commentary block.
        vm.onEvent(TafseerEvent.NavigateToAyah(0))
        advanceUntilIdle()

        assertThat(vm.state.value.notes.map { it.text }).containsExactly("on the second ayah")
        assertThat(vm.state.value.notes.single().ayahId).isEqualTo(102)
    }

    @Test
    fun `a blank note is not stored`() = runTest {
        val vm = loadedViewModel()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddNote("   "))
        advanceUntilIdle()

        assertThat(vm.state.value.notes).isEmpty()
    }

    @Test
    fun `adding a note before the ayahs load does not crash`() = runTest {
        // The handler indexed `ayahs[currentAyahIndex]` outside its coroutine, so an
        // out-of-range index threw straight out of onEvent — on the UI thread, from a tap.
        val vm = viewModel()

        vm.onEvent(TafseerEvent.AddNote("too early"))
        advanceUntilIdle()

        assertThat(vm.state.value.notes).isEmpty()
    }

    @Test
    fun `a note write that fails is reported instead of vanishing`() = runTest {
        coEvery { tafseerUseCases.addNote(any(), any(), any()) } throws
            IllegalStateException("disk full")

        val vm = loadedViewModel()
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.AddNote("a reflection"))
        advanceUntilIdle()

        assertThat(telemetry.errors.map { it.type }).contains("add_note")
    }
}

private fun ayah(id: Int, number: Int) = Ayah(
    id = id,
    surahNumber = 2,
    ayahNumber = number,
    textArabic = "",
    textSimple = "",
    juzNumber = 1,
    hizbNumber = 1,
    rubNumber = 1,
    pageNumber = 1,
    sajdaType = null,
    sajdaNumber = null,
)
