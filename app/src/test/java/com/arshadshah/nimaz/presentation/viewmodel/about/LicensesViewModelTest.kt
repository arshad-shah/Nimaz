package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.atoms.NimazErrorKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The screens this ViewModel serves had no ViewModel at all: a `LaunchedEffect` built
 * `Libs` and a `remember { mutableStateOf }` held the result, so a failed parse left
 * `isLoading` true forever and none of it was reachable from a test.
 *
 * These are the cases that could not previously be written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: LicensesUseCases

    private val compose = OpenSourceLibrary(
        id = 1,
        name = "Compose UI",
        version = "1.7.0",
        author = "Google",
        website = null,
        licenses = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a loaded list clears loading and carries no error`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose)

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertThat(state.libraries).containsExactly(compose)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `a failed load stops loading and says so`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        val state = viewModel.listState.value
        // The defect this pins: the old screen left the spinner running forever.
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.message).isEqualTo(R.string.licenses_load_failed)
        // The exception text is carried, but as detail — never as the readable message.
        assertThat(state.error?.details).isEqualTo("asset missing")
        assertThat(telemetry.errors).hasSize(1)
    }

    @Test
    fun `retry clears the error and asks again`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")
        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        coEvery { useCases.getLibraries() } returns listOf(compose)
        viewModel.onEvent(LicensesEvent.Retry)
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertThat(state.error).isNull()
        assertThat(state.libraries).containsExactly(compose)
    }

    @Test
    fun `a library the id does not match is not found, not a crash`() = runTest(dispatcher) {
        coEvery { useCases.getLibrary(any()) } returns null

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibrary(404))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.library).isNull()
        // A missing id is a real answer, not a thrown failure — so nothing is reported
        // to telemetry, but the reader is still told.
        assertThat(state.error?.kind).isEqualTo(NimazErrorKind.NOT_FOUND)
        assertThat(telemetry.errors).isEmpty()
    }

    @Test
    fun `a found library clears the not-found error from a previous lookup`() = runTest(dispatcher) {
        coEvery { useCases.getLibrary(404) } returns null
        coEvery { useCases.getLibrary(1) } returns compose

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibrary(404))
        advanceUntilIdle()
        viewModel.onEvent(LicensesEvent.LoadLibrary(1))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.library).isEqualTo(compose)
        assertThat(state.error).isNull()
    }
}
