package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.ObserveKhatamByIdUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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
 * Opening a khatam for editing, while the first Room read is still in flight.
 *
 * `startEdit` assigned a whole fresh `KhatamFormUiState` once the record arrived, so anything
 * typed in the meantime was replaced by the stored values — silently, and looking exactly like
 * the app had discarded the edit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KhatamEditFormTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: KhatamUseCases
    private lateinit var quranUseCases: QuranUseCases

    /** Emits the stored khatam only when a test decides it has arrived. */
    private val stored = MutableStateFlow<Khatam?>(null)

    private val khatam = Khatam(
        id = 7,
        name = "Stored name",
        notes = "Stored notes",
        dailyTarget = 20,
        totalAyahsRead = 100,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)

        val observeById = mockk<ObserveKhatamByIdUseCase>()
        every { observeById.invoke(any()) } returns stored
        every { useCases.observeKhatamById } returns observeById
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = KhatamViewModel(useCases, quranUseCases)

    @Test
    fun `typing during the load survives it`() = runTest {
        val vm = viewModel()

        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()
        assertThat(vm.formState.value.isLoading).isTrue()

        // The reader types while the spinner is still up.
        vm.onEvent(KhatamEvent.UpdateName("Ramadan 1447"))
        advanceUntilIdle()

        stored.value = khatam
        advanceUntilIdle()

        // Was: reverted to "Stored name" the moment the read landed.
        assertThat(vm.formState.value.name).isEqualTo("Ramadan 1447")
        assertThat(vm.formState.value.isLoading).isFalse()
    }

    @Test
    fun `fields the reader did not touch take the stored values`() = runTest {
        val vm = viewModel()
        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateName("Ramadan 1447"))
        advanceUntilIdle()

        stored.value = khatam
        advanceUntilIdle()

        // Preserving the reader's edit must not cost them the rest of the record — an edit
        // screen missing the stored notes would be its own bug.
        assertThat(vm.formState.value.notes).isEqualTo("Stored notes")
        assertThat(vm.formState.value.dailyTarget).isEqualTo(20)
        assertThat(vm.formState.value.totalAyahsRead).isEqualTo(100)
    }

    @Test
    fun `an untouched form is filled entirely from the record`() = runTest {
        val vm = viewModel()
        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()

        stored.value = khatam
        advanceUntilIdle()

        assertThat(vm.formState.value.name).isEqualTo("Stored name")
        assertThat(vm.formState.value.notes).isEqualTo("Stored notes")
        assertThat(vm.formState.value.isLoading).isFalse()
    }
}
