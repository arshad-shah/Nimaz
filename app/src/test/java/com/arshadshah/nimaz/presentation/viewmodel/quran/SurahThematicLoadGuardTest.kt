package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.GetTopicsForSurahUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The load guard on the surah's thematic layer.
 *
 * `loadedSurah` was assigned **on entry**, so it recorded a surah that might never arrive. That
 * is the difference between a guard that says "already showing it" and one that says "already
 * asked for it", and only the first is safe to short-circuit on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SurahThematicLoadGuardTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var useCases: QuranUseCases
    private lateinit var settings: SettingsRepository

    /** How many times each surah's overview was fetched. */
    private val overviewReads = mutableListOf<Int>()

    /** Held open so a test can decide when a surah's read resolves. */
    private val gates = mutableMapOf<Int, CompletableDeferred<Unit>>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        every { settings.quranTranslationFontSize } returns MutableStateFlow(16f)

        coEvery { useCases.getSurahOverview(any()) } coAnswers {
            val surah = firstArg<Int>()
            overviewReads += surah
            gates[surah]?.await()
            null
        }
        coEvery { useCases.getSurahByNumber(any()) } returns null
        coEvery { useCases.getSurahThemes(any()) } returns emptyList()

        val topicsForSurah = mockk<GetTopicsForSurahUseCase>()
        coEvery { topicsForSurah.count(any()) } returns 3
        every { useCases.getTopicsForSurah } returns topicsForSurah
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SurahThematicViewModel(useCases, settings, telemetry)

    /**
     * **This passes on the pre-fix code too, and that is the finding.**
     *
     * #364 R12 predicts a permanent strand: cancel the job, and `loadedSurah == surahNumber`
     * with an inactive job makes the guard short-circuit every retry. That state is not
     * reachable. `loadJob?.cancel()` appears at exactly one site — inside `load()` — and the
     * old code assigned `loadedSurah = surahNumber` on the line immediately before it, so a
     * cancel was always followed by a relaunch for whichever surah was just recorded. The
     * marker and the job could not diverge. The failure path did not strand either, because
     * `onFailure` already reset the marker.
     *
     * What was real is the sibling defect the issue notes second — see the restart test below.
     * Kept as the regression guard for the property the fix now provides *by construction*
     * rather than by that coincidence: the marker means "loaded", so nothing that failed to
     * load can ever block a retry.
     */
    @Test
    fun `an interrupted load does not block coming back to that surah`() = runTest {
        val stuck = CompletableDeferred<Unit>()
        gates[2] = stuck

        val vm = viewModel()
        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahThematicEvent.Load(3))
        advanceUntilIdle()
        gates.remove(2)
        stuck.complete(Unit)

        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        assertThat(vm.backgroundState.value.isLoading).isFalse()
        assertThat(vm.thematic.value.isLoading).isFalse()
    }

    @Test
    fun `re-sending Load for the surah already in flight does not restart it`() = runTest {
        val slow = CompletableDeferred<Unit>()
        gates[2] = slow

        val vm = viewModel()
        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()
        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        slow.complete(Unit)
        advanceUntilIdle()

        // The event's KDoc calls it "idempotent — safe to re-send". That was true of the result
        // and false of the work: the second send fell through and re-ran all three reads.
        assertThat(overviewReads).containsExactly(2)
    }

    @Test
    fun `re-sending Load for a surah already shown does nothing`() = runTest {
        val vm = viewModel()
        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        assertThat(overviewReads).containsExactly(2)
    }

    @Test
    fun `the info screen's thematic state comes from the same single load`() = runTest {
        val vm = viewModel()
        vm.onEvent(SurahThematicEvent.Load(2))
        advanceUntilIdle()

        // R14: `QuranViewModel` used to run these same three reads into its own copy of this
        // state, from `loadSurahInfo`. One load path now feeds the info screen and the two
        // prose screens alike.
        assertThat(vm.thematic.value.subjectCount).isEqualTo(3)
        assertThat(vm.thematic.value.isLoading).isFalse()
        assertThat(overviewReads).containsExactly(2)
    }
}
