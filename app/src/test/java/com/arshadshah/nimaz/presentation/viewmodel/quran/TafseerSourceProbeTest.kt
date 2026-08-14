package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Ayah
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
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import org.junit.Test

/**
 * What the tafseer reader costs to swipe through.
 *
 * The source probe existed so an ayah with no commentary in the selected source could suggest
 * one that has some — `availableSources` reaches exactly one consumer, `TafseerEmptyState`,
 * which is drawn only when the selection is empty. It ran unconditionally anyway, one read per
 * source on top of the selected one, on every swipe and every source switch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TafseerSourceProbeTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var tafseerUseCases: TafseerUseCases
    private lateinit var quranUseCases: QuranUseCases

    /** Every (ayah, sourceId) the reader asked for, in order. */
    private val reads = mutableListOf<Pair<Int, String>>()

    /** Which sources have text for ayah 1. Ayah 2 has none anywhere. */
    private var textFor: (Int, String) -> String? = { ayah, _ -> if (ayah == 1) "text" else null }

    private val ayahs = listOf(ayah(1), ayah(2))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tafseerUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)

        coEvery { tafseerUseCases.getTafseerForAyah(any(), any(), any()) } coAnswers {
            val ayahNumber = secondArg<Int>()
            val sourceId = thirdArg<String>()
            reads += ayahNumber to sourceId
            textFor(ayahNumber, sourceId)?.let { body ->
                TafseerText(
                    id = ayahNumber * 100L,
                    surahNumber = 1,
                    ayahStart = ayahNumber,
                    ayahEnd = ayahNumber,
                    tafseerId = sourceId,
                    text = body,
                )
            }
        }
        coEvery { quranUseCases.getSurahByNumber(any()) } returns null
        every { quranUseCases.getAyahsBySurah(any()) } returns flowOf(ayahs)
        coEvery { quranUseCases.getTopicsForAyah(any()) } returns emptyList()
        every { tafseerUseCases.getHighlightsForRange(any(), any(), any(), any()) } returns
            MutableStateFlow(emptyList())
        every { tafseerUseCases.getNotesForRange(any(), any(), any(), any()) } returns
            MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = TafseerViewModel(tafseerUseCases, quranUseCases, quranSettings(), telemetry)

    @Test
    fun `an ayah with commentary costs one read`() = runTest {
        val vm = viewModel()
        vm.onEvent(TafseerEvent.LoadSurah(1, 1))
        advanceUntilIdle()

        // Was `1 + TafseerSource.entries.size`. With five sources, swiping a 286-ayah surah
        // issued 1,716 reads to populate a set almost none of those verses looked at.
        assertThat(reads).hasSize(1)
    }

    @Test
    fun `an ayah with no commentary still probes for an alternate`() = runTest {
        val vm = viewModel()
        vm.onEvent(TafseerEvent.LoadSurah(1, 1))
        advanceUntilIdle()
        reads.clear()

        vm.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        // The one case the probe is for. It must still run, and must not re-ask the source it
        // already knows is empty.
        assertThat(reads.size).isGreaterThan(1)
        assertThat(reads.count { it.second == TafseerSource.entries.first().id }).isEqualTo(1)
    }

    @Test
    fun `an alternate source is offered when the selected one is empty`() = runTest {
        val selected = TafseerSource.entries.first()
        val other = TafseerSource.entries.first { it != selected }
        textFor = { ayahNumber, sourceId ->
            if (ayahNumber == 2 && sourceId == other.id) "elsewhere" else null
        }

        val vm = viewModel()
        vm.onEvent(TafseerEvent.LoadSurah(1, 2))
        advanceUntilIdle()

        assertThat(vm.state.value.currentTafseer).isNull()
        assertThat(vm.state.value.availableSources).contains(other)
    }

    @Test
    fun `swiping within one block holds the reading position`() = runTest {
        // Both ayahs resolve to the same block id, which is what "same block" means here.
        coEvery { tafseerUseCases.getTafseerForAyah(any(), any(), any()) } returns TafseerText(
            id = 7L,
            surahNumber = 1,
            ayahStart = 1,
            ayahEnd = 2,
            tafseerId = TafseerSource.entries.first().id,
            text = "one block spanning both",
        )

        val vm = viewModel()
        vm.onEvent(TafseerEvent.LoadSurah(1, 1))
        advanceUntilIdle()
        vm.onEvent(TafseerEvent.NavigateToTafseerPage(3))
        advanceUntilIdle()

        vm.onEvent(TafseerEvent.NavigateToAyah(1))
        advanceUntilIdle()

        // The `sameBlock` check moved inside the `update {}` lambda; this is what says the move
        // did not change its meaning.
        assertThat(vm.state.value.currentTafseerPage).isEqualTo(3)
    }

    private fun ayah(number: Int) = Ayah(
        id = number,
        surahNumber = 1,
        ayahNumber = number,
        textArabic = "نص",
        textSimple = "نص",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
    )

    /**
     * The commentary screen reads the translator preference only to pick the *face* the verse
     * above the commentary is drawn in, so a relaxed stub with the default id is enough here.
     */
    private fun quranSettings(): QuranPreferences = mockk(relaxed = true) {
        every { quranTranslatorId } returns flowOf("sahih_international")
    }

}
