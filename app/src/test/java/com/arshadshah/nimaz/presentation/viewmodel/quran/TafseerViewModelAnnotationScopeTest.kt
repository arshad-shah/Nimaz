package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.domain.model.TafseerHighlight
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
 * Annotation streams in [TafseerViewModel] must belong to the ayah on screen.
 *
 * `loadTafseerForCurrentAyah()` runs on **every** ayah swipe (and on every source switch),
 * and each run launched two collectors on Room flows — highlights and notes for that ayah's
 * commentary block. A Room flow never completes, and nothing was cancelled, so swiping
 * through a surah left one live collector per ayah *per stream*, every one of them writing
 * the same `_state.highlights` / `_state.notes`.
 *
 * That is not merely wasteful. Room re-emits to **all** live collectors whenever the
 * underlying rows change, so adding a highlight on ayah 5 wakes the collectors for ayahs
 * 1-4 as well, and whichever lands last wins — the reader shows another ayah's annotations
 * over the one being read. These tests pin the requirement that only the current ayah's
 * streams can write state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TafseerViewModelAnnotationScopeTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var tafseerUseCases: TafseerUseCases
    private lateinit var quranUseCases: QuranUseCases

    /** One highlight stream per ayah, exactly as Room hands out one flow per query. */
    private val highlightsForAyah = (1..2).associateWith {
        MutableStateFlow<List<TafseerHighlight>>(emptyList())
    }
    private val notesForAyah = (1..2).associateWith {
        MutableStateFlow<List<TafseerNote>>(emptyList())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        tafseerUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)

        coEvery { quranUseCases.getSurahByNumber(any()) } returns null
        every { quranUseCases.getAyahsBySurah(any()) } returns flowOf(listOf(ayah(1), ayah(2)))

        // Each ayah has its own commentary block, so each gets its own annotation streams.
        coEvery { tafseerUseCases.getTafseerForAyah(any(), any(), any()) } answers {
            val ayahNumber = secondArg<Int>()
            TafseerText(
                id = ayahNumber.toLong(),
                tafseerId = TafseerSource.IBN_KATHIR.id,
                surahNumber = 1,
                ayahStart = ayahNumber,
                ayahEnd = ayahNumber,
                text = "commentary for ayah $ayahNumber"
            )
        }
        every {
            tafseerUseCases.getHighlightsForRange(any(), any(), any(), any())
        } answers { highlightsForAyah.getValue(secondArg<Int>()) }
        every {
            tafseerUseCases.getNotesForRange(any(), any(), any(), any())
        } answers { notesForAyah.getValue(secondArg<Int>()) }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a previous ayah's highlights cannot overwrite the ayah on screen`() = runTest {
        val viewModel = TafseerViewModel(tafseerUseCases, quranUseCases)

        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber = 1, ayahNumber = 1))
        advanceUntilIdle()

        // Swipe to ayah 2. Ayah 1's collector must not survive this.
        viewModel.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        highlightsForAyah.getValue(2).value = listOf(highlight(id = 200, ayahId = 2))
        advanceUntilIdle()
        assertThat(viewModel.state.value.highlights.map { it.id }).containsExactly(200L)

        // Room wakes every live collector when the table changes. If ayah 1's is still
        // subscribed it fires here and overwrites the reader with another ayah's highlights.
        highlightsForAyah.getValue(1).value = listOf(highlight(id = 100, ayahId = 1))
        advanceUntilIdle()

        assertThat(viewModel.state.value.highlights.map { it.id }).containsExactly(200L)
    }

    @Test
    fun `a previous ayah's notes cannot overwrite the ayah on screen`() = runTest {
        val viewModel = TafseerViewModel(tafseerUseCases, quranUseCases)

        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber = 1, ayahNumber = 1))
        advanceUntilIdle()
        viewModel.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        notesForAyah.getValue(2).value = listOf(note(id = 200, ayahId = 2))
        advanceUntilIdle()
        assertThat(viewModel.state.value.notes.map { it.id }).containsExactly(200L)

        notesForAyah.getValue(1).value = listOf(note(id = 100, ayahId = 1))
        advanceUntilIdle()

        assertThat(viewModel.state.value.notes.map { it.id }).containsExactly(200L)
    }

    @Test
    fun `swiping back re-subscribes, so the earlier ayah's annotations return`() = runTest {
        // The cancellation must be scoped to "not the current ayah", not "load once" — a
        // reader swiping 1 -> 2 -> 1 has to see ayah 1's highlights again.
        val viewModel = TafseerViewModel(tafseerUseCases, quranUseCases)

        viewModel.onEvent(TafseerEvent.LoadSurah(surahNumber = 1, ayahNumber = 1))
        advanceUntilIdle()
        highlightsForAyah.getValue(1).value = listOf(highlight(id = 100, ayahId = 1))
        advanceUntilIdle()

        viewModel.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()
        viewModel.onEvent(TafseerEvent.NavigateToAyah(0))
        advanceUntilIdle()

        assertThat(viewModel.state.value.highlights.map { it.id }).containsExactly(100L)
    }

    private fun ayah(number: Int) = Ayah(
        id = number,
        surahNumber = 1,
        ayahNumber = number,
        textArabic = "",
        textSimple = "",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null as SajdaType?,
        sajdaNumber = null
    )

    private fun highlight(id: Long, ayahId: Int) = TafseerHighlight(
        id = id,
        ayahId = ayahId,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        startOffset = 0,
        endOffset = 1,
        color = "yellow",
        note = null,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun note(id: Long, ayahId: Int) = TafseerNote(
        id = id,
        ayahId = ayahId,
        tafseerId = TafseerSource.IBN_KATHIR.id,
        text = "note $id",
        createdAt = 0L,
        updatedAt = 0L
    )
}
