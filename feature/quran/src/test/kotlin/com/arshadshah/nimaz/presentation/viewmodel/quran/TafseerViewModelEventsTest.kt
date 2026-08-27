package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
 * `TafseerViewModel`'s event table.
 *
 * Two things it does are worth stating. Swiping between verses and turning a commentary page
 * are *how this screen is read*, and for a long time only opening it and switching source were
 * counted — so "opened tafseer" looked like the whole of the engagement. And the commentary page
 * is held here rather than in the composable, because a block spans several verses and a swipe
 * within one must not reopen the passage at page 1.
 *
 * `TafseerViewModelAnnotationScopeTest` covers the collector scoping that stops one verse's
 * highlights landing on another's; this is the rest of the table.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TafseerViewModelEventsTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var tafseerUseCases: TafseerUseCases
    private lateinit var quranUseCases: QuranUseCases

    private val available = MutableStateFlow(setOf(TafseerSource.IBN_KATHIR))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tafseerUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)

        coEvery { quranUseCases.getSurahByNumber(any()) } returns null
        every { quranUseCases.getAyahsBySurah(any()) } returns
            flowOf(listOf(ayah(1), ayah(2), ayah(3)))
        coEvery { tafseerUseCases.getTafseerForAyah(any(), any(), any()) } answers {
            val source = thirdArg<String>()
            if (TafseerSource.entries.first { it.id == source } !in available.value) null
            else TafseerText(
                id = 1,
                tafseerId = source,
                surahNumber = 1,
                ayahStart = 1,
                ayahEnd = 3,
                text = "commentary from $source",
            )
        }
        every { tafseerUseCases.getHighlightsForRange(any(), any(), any(), any()) } returns
            flowOf(emptyList())
        every { tafseerUseCases.getNotesForRange(any(), any(), any(), any()) } returns
            flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun settings(): QuranPreferences = mockk(relaxed = true) {
        every { quranTranslatorId } returns flowOf("sahih_international")
    }

    private fun viewModel() =
        TafseerViewModel(tafseerUseCases, quranUseCases, settings(), telemetry)

    private fun ayah(number: Int) = Ayah(
        id = number,
        surahNumber = 1,
        ayahNumber = number,
        textArabic = "نص $number",
        textSimple = "nass $number",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    private fun loaded(): TafseerViewModel = viewModel().also {
        it.onEvent(TafseerEvent.LoadSurah(surahNumber = 1, ayahNumber = 1))
    }

    // ---- Moving through the surah ----

    @Test
    fun `opening the commentary at a verse puts the reader on it`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        assertThat(vm.state.value.ayahs).hasSize(3)
        assertThat(vm.state.value.currentAyahIndex).isEqualTo(0)
    }

    @Test
    fun `swiping to another verse is counted, and moves the reader`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.NavigateToAyah(2))
        advanceUntilIdle()

        assertThat(vm.state.value.currentAyahIndex).isEqualTo(2)
        // Reading is swiping; counting only "opened" made engagement look like one tap.
        assertThat(telemetry.featureUsages.map { it.action }).contains("navigate_ayah")
    }

    @Test
    fun `turning a commentary page is held here, so a swipe does not reset it`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.NavigateToTafseerPage(2))
        advanceUntilIdle()

        assertThat(vm.state.value.currentTafseerPage).isEqualTo(2)
        assertThat(telemetry.featureUsages.map { it.action }).contains("navigate_page")
    }

    @Test
    fun `moving to a verse in the same block keeps the commentary page`() = runTest {
        // The block covers 1–3, so a swipe within it is the same passage — reopening it at
        // page 1 would throw away where the reader was in it.
        val vm = loaded()
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.NavigateToTafseerPage(2))
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        assertThat(vm.state.value.currentTafseerPage).isEqualTo(2)
    }

    // ---- Sources ----

    @Test
    fun `switching source reloads the commentary from it`() = runTest {
        available.value = setOf(TafseerSource.IBN_KATHIR, TafseerSource.MAARIFUL_QURAN)
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.SwitchSource(TafseerSource.MAARIFUL_QURAN))
        advanceUntilIdle()

        assertThat(vm.state.value.selectedSource).isEqualTo(TafseerSource.MAARIFUL_QURAN)
        assertThat(vm.state.value.currentTafseer?.text)
            .contains(TafseerSource.MAARIFUL_QURAN.id)
    }

    @Test
    fun `a source with nothing for this verse leaves the commentary empty rather than stale`() =
        runTest {
            available.value = setOf(TafseerSource.IBN_KATHIR)
            val vm = loaded()
            advanceUntilIdle()

            vm.onEvent(TafseerEvent.SwitchSource(TafseerSource.MAARIFUL_QURAN))
            advanceUntilIdle()

            // Showing the previous source's text under the new source's name is worse than
            // showing nothing.
            assertThat(vm.state.value.currentTafseer).isNull()
        }

    // ---- Highlights ----

    @Test
    fun `a highlight is written against the verse on screen and the source showing`() = runTest {
        val vm = loaded()
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        vm.onEvent(
            TafseerEvent.AddHighlight(
                startOffset = 0,
                endOffset = 10,
                color = "#EAB308",
                note = "worth returning to",
            )
        )
        advanceUntilIdle()

        coVerify {
            tafseerUseCases.addHighlight(
                ayahId = 2,
                tafseerId = TafseerSource.IBN_KATHIR.id,
                startOffset = 0,
                endOffset = 10,
                color = "#EAB308",
                note = "worth returning to",
            )
        }
    }

    @Test
    fun `a highlight with a blank note is stored as having none`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddHighlight(0, 10, "#EAB308", note = "   "))
        advanceUntilIdle()

        coVerify {
            tafseerUseCases.addHighlight(
                ayahId = any(),
                tafseerId = any(),
                startOffset = any(),
                endOffset = any(),
                color = any(),
                note = null,
            )
        }
    }

    @Test
    fun `a highlight cannot be made before the verses have arrived`() = runTest {
        every { quranUseCases.getAyahsBySurah(any()) } returns flowOf(emptyList())
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddHighlight(0, 10, "#EAB308", note = null))
        advanceUntilIdle()

        coVerify(exactly = 0) {
            tafseerUseCases.addHighlight(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `editing a highlight the reader cannot see does nothing`() = runTest {
        // The edit is built from the highlight in state, so an id that is not on screen has
        // nothing to copy — writing a fresh row from the id alone would lose its offsets.
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.UpdateHighlight(highlightId = 5, color = "#22C55E", note = "x"))
        advanceUntilIdle()

        coVerify(exactly = 0) { tafseerUseCases.updateHighlight(any()) }
    }

    @Test
    fun `editing a highlight on screen keeps its offsets and changes its colour`() = runTest {
        val existing = TafseerHighlight(
            id = 5,
            ayahId = 1,
            tafseerId = TafseerSource.IBN_KATHIR.id,
            startOffset = 4,
            endOffset = 22,
            color = "#EAB308",
            note = null,
            createdAt = 100,
            updatedAt = 100,
        )
        every { tafseerUseCases.getHighlightsForRange(any(), any(), any(), any()) } returns
            flowOf(listOf(existing))
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.UpdateHighlight(highlightId = 5, color = "#22C55E", note = "x"))
        advanceUntilIdle()

        val updated = slot<TafseerHighlight>()
        coVerify { tafseerUseCases.updateHighlight(capture(updated)) }
        assertThat(updated.captured.color).isEqualTo("#22C55E")
        assertThat(updated.captured.note).isEqualTo("x")
        // The span is what makes it a highlight; the editor only ever changes the colour and
        // the note.
        assertThat(updated.captured.startOffset).isEqualTo(4)
        assertThat(updated.captured.endOffset).isEqualTo(22)
    }

    @Test
    fun `deleting a highlight reaches its use case`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.DeleteHighlight(highlightId = 5))
        advanceUntilIdle()

        coVerify { tafseerUseCases.deleteHighlight(5) }
        assertThat(telemetry.featureUsages.map { it.action }).contains("delete_highlight")
    }

    // ---- Notes ----

    @Test
    fun `a note is written against the verse on screen`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddNote("a thought"))
        advanceUntilIdle()

        coVerify { tafseerUseCases.addNote(1, TafseerSource.IBN_KATHIR.id, "a thought") }
    }

    @Test
    fun `editing and deleting a note reach their use cases`() = runTest {
        val vm = loaded()
        advanceUntilIdle()
        val note = TafseerNote(
            id = 9,
            ayahId = 1,
            tafseerId = TafseerSource.IBN_KATHIR.id,
            text = "edited",
            createdAt = 0,
            updatedAt = 0,
        )

        vm.onEvent(TafseerEvent.UpdateNote(note))
        vm.onEvent(TafseerEvent.DeleteNote(9))
        advanceUntilIdle()

        coVerify { tafseerUseCases.updateNote(note) }
        coVerify { tafseerUseCases.deleteNote(9) }
    }

    @Test
    fun `a note that fails to save reaches the reader rather than being dropped`() = runTest {
        coEvery { tafseerUseCases.addNote(any(), any(), any()) } throws
            IllegalStateException("disk full")
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.AddNote("a thought"))
        advanceUntilIdle()

        // From where the reader is standing, a note that silently failed is a note they wrote
        // and lost — so it surfaces, without replacing the commentary they are reading.
        assertThat(vm.state.value.noteError).isNotNull()
        assertThat(vm.state.value.currentTafseer).isNotNull()

        vm.onEvent(TafseerEvent.DismissNoteError)
        advanceUntilIdle()
        assertThat(vm.state.value.noteError).isNull()
    }
}
