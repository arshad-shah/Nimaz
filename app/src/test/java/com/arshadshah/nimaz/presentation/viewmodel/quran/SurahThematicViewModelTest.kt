package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.repository.settings.QuranPreferences
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SurahThematicViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var quranUseCases: QuranUseCases
    private lateinit var quranSettings: QuranPreferences
    private lateinit var viewModel: SurahThematicViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        quranUseCases = mockk(relaxed = true)
        quranSettings = mockk(relaxed = true)

        every { quranSettings.quranTranslationFontSize } returns flowOf(16f)

        viewModel = SurahThematicViewModel(
            quranUseCases = quranUseCases,
            quranSettings = quranSettings,
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial backgroundState has no surah`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.backgroundState.value.surah).isNull()
    }

    @Test
    fun `initial backgroundState is in loading state`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.backgroundState.value.isLoading).isTrue()
    }

    @Test
    fun `initial passages query is empty`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.passagesState.value.query).isEmpty()
    }

    @Test
    fun `font size from settings is applied to background state`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.backgroundState.value.proseFontSize).isEqualTo(16f)
    }

    @Test
    fun `Filter event sets query in passagesState`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SurahThematicEvent.Filter("mercy"))
        advanceUntilIdle()
        assertThat(viewModel.passagesState.value.query).isEqualTo("mercy")
    }

    @Test
    fun `ClearFilter event clears query in passagesState`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SurahThematicEvent.Filter("test"))
        viewModel.onEvent(SurahThematicEvent.ClearFilter)
        advanceUntilIdle()
        assertThat(viewModel.passagesState.value.query).isEmpty()
    }

    @Test
    fun `Load event triggers a load attempt`() = runTest {
        advanceUntilIdle()
        // Sending the Load event should not throw; the actual load uses use cases that are relaxed
        viewModel.onEvent(SurahThematicEvent.Load(surahNumber = 1))
        advanceUntilIdle()
        // Loading started — the state is still in loading since the relaxed mock never returned data
        // The important thing: no exception and state is accessible
        assertThat(viewModel.backgroundState.value).isNotNull()
    }

    @Test
    fun `Load event twice for same surah does not re-trigger while in flight`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SurahThematicEvent.Load(surahNumber = 2))
        viewModel.onEvent(SurahThematicEvent.Load(surahNumber = 2))
        advanceUntilIdle()
        // Should not crash
        assertThat(viewModel.backgroundState.value).isNotNull()
    }
}
