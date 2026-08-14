package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.StringProvider
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.khatam.GetTodaysPortion
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import com.arshadshah.nimaz.domain.model.KhatamStats
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KhatamViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var quranUseCases: QuranUseCases
    private lateinit var getTodaysPortion: GetTodaysPortion
    private lateinit var strings: StringProvider
    private lateinit var viewModel: KhatamViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        khatamUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)
        getTodaysPortion = mockk(relaxed = true)
        strings = mockk(relaxed = true)

        every { khatamUseCases.observeInProgressKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeCompletedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeAbandonedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeKhatamStats() } returns flowOf(KhatamStats(0, 0, 0, 0, 0))
        every { khatamUseCases.observeActiveKhatam() } returns flowOf(null)
        every { khatamUseCases.observeKhatamDetail(any()) } returns flowOf(null)

        viewModel = KhatamViewModel(
            khatamUseCases = khatamUseCases,
            quranUseCases = quranUseCases,
            getTodaysPortion = getTodaysPortion,
            strings = strings,
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial list state has empty lists`() = runTest {
        advanceUntilIdle()
        val state = viewModel.listState.value
        assertThat(state.inProgressKhatams).isEmpty()
        assertThat(state.completedKhatams).isEmpty()
        assertThat(state.abandonedKhatams).isEmpty()
    }

    @Test
    fun `initial activeKhatam is null`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.listState.value.activeKhatam).isNull()
    }

    @Test
    fun `hasAnyKhatam is false when all lists empty`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.listState.value.hasAnyKhatam).isFalse()
    }

    @Test
    fun `initial detailState has null khatam`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.detailState.value.khatam).isNull()
    }

    @Test
    fun `StartCreate event resets form state`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(KhatamEvent.StartCreate())
        advanceUntilIdle()
        assertThat(viewModel.formState.value.name).isEmpty()
    }

    @Test
    fun `UpdateName event updates form name`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(KhatamEvent.UpdateName("My Khatam"))
        advanceUntilIdle()
        assertThat(viewModel.formState.value.name).isEqualTo("My Khatam")
    }
}
