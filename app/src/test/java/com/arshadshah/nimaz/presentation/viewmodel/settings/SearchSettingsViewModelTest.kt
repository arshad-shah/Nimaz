package com.arshadshah.nimaz.presentation.viewmodel.settings

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.LibrarySource
import com.arshadshah.nimaz.domain.model.MatchStrictness
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ObserveSearchPreferencesUseCase
import com.arshadshah.nimaz.presentation.viewmodel.FakeSearchSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The consent gate for "Ask with Proof" — the whole privacy control for the one feature
 * that sends anything off the device, and until now the ViewModel with zero tests.
 *
 * `onConsentAccepted` closed the sheet with a synchronous `_uiState.update` and wrote the
 * flag in a `launch`, so the sheet was already gone before the write had a chance to fail.
 * When it did fail the user had consented, the sheet was closed, and `aiEnabled` stayed
 * false — a switch that flips itself back with no explanation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchSettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val searchSettings = FakeSearchSettings()
    private val telemetry = RecordingTelemetry()

    private val aiEnabled = MutableStateFlow(false)
    private val historyEnabled = MutableStateFlow(true)
    private val questionHistory = MutableStateFlow("""["what is zakat"]""")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.aiAskEnabled } returns aiEnabled
        every { settings.aiHistoryEnabled } returns historyEnabled
        every { settings.aiQuestionHistory } returns questionHistory
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(search: FakeSearchSettings = searchSettings) =
        SearchSettingsViewModel(
            settings,
            search,
            ObserveSearchPreferencesUseCase(search),
            telemetry,
        )

    @Test
    fun `asking to enable opens the consent sheet rather than enabling anything`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
        advanceUntilIdle()

        assertThat(vm.uiState.value.showConsentSheet).isTrue()
        coVerify(exactly = 0) { settings.setAiAskEnabled(true) }
    }

    @Test
    fun `the sheet stays up until the consent write has actually committed`() = runTest {
        coEvery { settings.setAiAskEnabled(true) } coAnswers { delay(500) }

        val vm = viewModel()
        vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ConsentAccepted)
        advanceTimeBy(50)
        assertThat(vm.uiState.value.showConsentSheet).isTrue()

        advanceUntilIdle()
        assertThat(vm.uiState.value.showConsentSheet).isFalse()
    }

    @Test
    fun `a consent write that fails leaves the sheet open instead of silently reverting`() =
        runTest {
            coEvery { settings.setAiAskEnabled(true) } throws IllegalStateException("disk full")

            val vm = viewModel()
            vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
            advanceUntilIdle()
            vm.onEvent(SearchSettingsEvent.ConsentAccepted)
            advanceUntilIdle()

            // The alternative is what shipped: sheet closed, switch off, nothing said.
            assertThat(vm.uiState.value.showConsentSheet).isTrue()
            assertThat(vm.uiState.value.consentFailed).isTrue()
            assertThat(telemetry.errors.map { it.type }).contains("consent")
        }

    @Test
    fun `accepting consent records the flag and when it was given`() = runTest {
        val vm = viewModel()
        vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
        advanceUntilIdle()
        vm.onEvent(SearchSettingsEvent.ConsentAccepted)
        advanceUntilIdle()

        coVerify(exactly = 1) { settings.setAiAskEnabled(true) }
        coVerify(exactly = 1) { settings.setAiConsentTimestamp(any()) }
    }

    @Test
    fun `turning the feature off is instant and asks for nothing`() = runTest {
        aiEnabled.value = true
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
        advanceUntilIdle()

        assertThat(vm.uiState.value.showConsentSheet).isFalse()
        coVerify(exactly = 1) { settings.setAiAskEnabled(false) }
    }

    @Test
    fun `turning history off also throws away what was already stored`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.SetHistoryEnabled(false))
        advanceUntilIdle()

        coVerify(exactly = 1) { settings.setAiHistoryEnabled(false) }
        coVerify(exactly = 1) { settings.setAiQuestionHistory("") }
    }

    @Test
    fun `dismissing the sheet clears a previous failure so the next attempt starts clean`() =
        runTest {
            coEvery { settings.setAiAskEnabled(true) } throws IllegalStateException("disk full")

            val vm = viewModel()
            vm.onEvent(SearchSettingsEvent.ToggleAiRequested)
            advanceUntilIdle()
            vm.onEvent(SearchSettingsEvent.ConsentAccepted)
            advanceUntilIdle()
            assertThat(vm.uiState.value.consentFailed).isTrue()

            vm.onEvent(SearchSettingsEvent.ConsentDismissed)
            advanceUntilIdle()

            assertThat(vm.uiState.value.showConsentSheet).isFalse()
            assertThat(vm.uiState.value.consentFailed).isFalse()
        }

    // ── local search settings ─────────────────────────────────────────────────

    @Test
    fun `switching a source off removes it and leaves the rest`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ToggleSource(LibrarySource.HADITH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.sources)
            .containsExactlyElementsIn(LibrarySource.entries - LibrarySource.HADITH)
    }

    @Test
    fun `the last source on cannot be switched off`() = runTest {
        // Obeying would store an empty set, which the sanitiser reads straight back as
        // "everything" — the switch would turn itself on again with no explanation.
        val vm = viewModel(FakeSearchSettings(sources = LibrarySource.QURAN.name))
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ToggleSource(LibrarySource.QURAN))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.sources).containsExactly(LibrarySource.QURAN)
    }

    @Test
    fun `switching off the source the default scope points at clears the scope`() = runTest {
        // Otherwise search opens filtered to a source it will never query, and the empty
        // results list reads as "nothing matched".
        val vm = viewModel(FakeSearchSettings(defaultScope = LibrarySource.HADITH.name))
        advanceUntilIdle()
        assertThat(vm.uiState.value.search.defaultScope).isEqualTo(LibrarySource.HADITH)

        vm.onEvent(SearchSettingsEvent.ToggleSource(LibrarySource.HADITH))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.defaultScope).isNull()
    }

    @Test
    fun `switching off some other source leaves the default scope alone`() = runTest {
        val vm = viewModel(FakeSearchSettings(defaultScope = LibrarySource.HADITH.name))
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.ToggleSource(LibrarySource.DUAS))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.defaultScope).isEqualTo(LibrarySource.HADITH)
    }

    @Test
    fun `a result cap outside the allowed range never reaches storage`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.SetResultsPerSource(100_000))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.resultsPerSource).isEqualTo(200)
    }

    @Test
    fun `strictness and scope round-trip through the screen`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(SearchSettingsEvent.SetStrictness(MatchStrictness.BROAD))
        vm.onEvent(SearchSettingsEvent.SetDefaultScope(LibrarySource.DUAS))
        advanceUntilIdle()

        assertThat(vm.uiState.value.search.strictness).isEqualTo(MatchStrictness.BROAD)
        assertThat(vm.uiState.value.search.defaultScope).isEqualTo(LibrarySource.DUAS)
    }
}
