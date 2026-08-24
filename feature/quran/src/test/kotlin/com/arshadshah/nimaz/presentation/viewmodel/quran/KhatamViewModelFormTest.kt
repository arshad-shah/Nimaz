package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.khatam.GetTodaysPortion
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
 * Making and editing a khatam.
 *
 * The form's one hard rule is that **editing must never touch what has been read**. A khatam
 * carries `totalAyahsRead`, and the save path builds its update from the stored row rather than
 * from the form's own state for exactly that reason — a save assembled from the form would write
 * back whatever the form happened to hold, which is zero.
 *
 * The pace control is the other thing worth pinning. A preset and a hand-dialled number are two
 * ways of setting one field, so they have to stay in step in both directions: choosing "one juz
 * a day" sets the number, and typing that same number selects the preset back.
 *
 * `KhatamViewModelTest` covers the list state; this is the form and its save.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KhatamViewModelFormTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var khatamUseCases: KhatamUseCases
    private lateinit var quranUseCases: QuranUseCases
    private lateinit var getTodaysPortion: GetTodaysPortion

    private val stored = MutableStateFlow<Khatam?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        khatamUseCases = mockk(relaxed = true)
        quranUseCases = mockk(relaxed = true)
        getTodaysPortion = mockk(relaxed = true)

        every { khatamUseCases.observeInProgressKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeCompletedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeAbandonedKhatams() } returns flowOf(emptyList())
        every { khatamUseCases.observeKhatamStats() } returns flowOf(KhatamStats(0, 0, 0, 0, 0))
        every { khatamUseCases.observeActiveKhatam() } returns flowOf(null)
        every { khatamUseCases.observeKhatamDetail(any()) } returns flowOf(null)
        every { khatamUseCases.observeKhatamById(any()) } returns stored
        coEvery { khatamUseCases.createKhatam(any()) } returns 42L
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = KhatamViewModel(
        khatamUseCases = khatamUseCases,
        quranUseCases = quranUseCases,
        getTodaysPortion = getTodaysPortion,
        strings = FakeStringProvider(),
        telemetry = telemetry,
    )

    private fun existing(read: Int = 1200) = Khatam(
        id = 7,
        name = "Ramadan",
        notes = "before fajr",
        status = KhatamStatus.ACTIVE,
        isActive = true,
        dailyTarget = 20,
        totalAyahsRead = read,
        createdAt = 100,
        updatedAt = 100,
    )

    // ---- The pace control ----

    @Test
    fun `choosing a preset sets the daily target it stands for`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SelectPreset(KhatamPacePreset.JUZ_DAILY))
        advanceUntilIdle()

        // Derived from the corpus rather than hardcoded, so the number is whatever a juz a day
        // actually is.
        // Rounded up, not down: a target that divides short leaves a tail nobody reads.
        assertThat(vm.formState.value.dailyTarget)
            .isEqualTo(KhatamPacePreset.JUZ_DAILY.targetAyahs())
        assertThat(vm.formState.value.preset).isEqualTo(KhatamPacePreset.JUZ_DAILY)
    }

    @Test
    fun `choosing the custom preset leaves the number the reader had`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateDailyTarget(37))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SelectPreset(KhatamPacePreset.CUSTOM))
        advanceUntilIdle()

        assertThat(vm.formState.value.dailyTarget).isEqualTo(37)
    }

    @Test
    fun `dialling in a preset's number selects that preset back`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.UpdateDailyTarget(KhatamPacePreset.JUZ_DAILY.targetAyahs()!!))
        advanceUntilIdle()

        // One field, two controls: they have to agree in both directions or the chips lie.
        assertThat(vm.formState.value.preset).isEqualTo(KhatamPacePreset.JUZ_DAILY)
    }

    @Test
    fun `a target outside the offered range is pulled into it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.UpdateDailyTarget(0))
        advanceUntilIdle()
        val floor = vm.formState.value.dailyTarget

        vm.onEvent(KhatamEvent.UpdateDailyTarget(100_000))
        advanceUntilIdle()

        // A stepper can only ever hand this a small integer, so it clamps rather than throwing.
        assertThat(floor).isAtLeast(1)
        assertThat(vm.formState.value.dailyTarget).isLessThan(Khatam.TOTAL_QURAN_AYAHS)
    }

    // ---- Validation ----

    @Test
    fun `a khatam with no name is not saved`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        assertThat(vm.formState.value.errorRes).isEqualTo(R.string.khatam_error_name_required)
        coVerify(exactly = 0) { khatamUseCases.createKhatam(any()) }
    }

    @Test
    fun `typing a name clears the complaint about not having one`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.UpdateName("Ramadan"))
        advanceUntilIdle()

        assertThat(vm.formState.value.errorRes).isNull()
    }

    // ---- Creating ----

    @Test
    fun `a new khatam is created active, and made the active one`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateName("  Ramadan  "))
        vm.onEvent(KhatamEvent.UpdateNotes("before fajr"))
        vm.onEvent(KhatamEvent.UpdateDailyTarget(30))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        val created = slot<Khatam>()
        coVerify { khatamUseCases.createKhatam(capture(created)) }
        assertThat(created.captured.name).isEqualTo("Ramadan")
        assertThat(created.captured.dailyTarget).isEqualTo(30)
        assertThat(created.captured.notes).isEqualTo("before fajr")
        assertThat(created.captured.isActive).isTrue()
        // Creating one and not making it active leaves the reader with a plan nothing follows.
        coVerify { khatamUseCases.setActiveKhatam(42L) }
    }

    @Test
    fun `notes left blank are stored as nothing rather than as an empty string`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateName("Ramadan"))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        val created = slot<Khatam>()
        coVerify { khatamUseCases.createKhatam(capture(created)) }
        assertThat(created.captured.notes).isNull()
    }

    @Test
    fun `a saved khatam tells the screen to leave, once`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateName("Ramadan"))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()
        assertThat(vm.formState.value.saveComplete).isTrue()
        assertThat(vm.formState.value.isSaving).isFalse()

        vm.onEvent(KhatamEvent.ConsumeSaveComplete)
        advanceUntilIdle()

        // Consumed, so a recomposition cannot navigate a second time.
        assertThat(vm.formState.value.saveComplete).isFalse()
    }

    @Test
    fun `a save that fails says so and does not claim to have saved`() = runTest {
        coEvery { khatamUseCases.createKhatam(any()) } throws IllegalStateException("disk full")
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.UpdateName("Ramadan"))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        assertThat(vm.formState.value.saveComplete).isFalse()
        assertThat(vm.formState.value.isSaving).isFalse()
        assertThat(vm.formState.value.errorRes).isEqualTo(R.string.khatam_error_save_failed)
    }

    // ---- Editing ----

    @Test
    fun `opening an existing khatam fills the form from the stored row`() = runTest {
        stored.value = existing()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()

        val form = vm.formState.value
        assertThat(form.isEdit).isTrue()
        assertThat(form.name).isEqualTo("Ramadan")
        assertThat(form.notes).isEqualTo("before fajr")
        assertThat(form.dailyTarget).isEqualTo(20)
        // Shown so the reader can see that editing is not going to reset it.
        assertThat(form.totalAyahsRead).isEqualTo(1200)
    }

    @Test
    fun `editing a khatam never touches what has been read`() = runTest {
        stored.value = existing(read = 1200)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.UpdateName("Ramadan 1447"))
        vm.onEvent(KhatamEvent.UpdateDailyTarget(40))
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        val updated = slot<Khatam>()
        coVerify { khatamUseCases.updateKhatam(capture(updated)) }
        assertThat(updated.captured.name).isEqualTo("Ramadan 1447")
        assertThat(updated.captured.dailyTarget).isEqualTo(40)
        // The update is built from the stored row, not from the form — a form-built save would
        // write back the zero the form holds.
        assertThat(updated.captured.totalAyahsRead).isEqualTo(1200)
        assertThat(updated.captured.id).isEqualTo(7)
        assertThat(updated.captured.createdAt).isEqualTo(100)
    }

    @Test
    fun `editing does not create a second khatam`() = runTest {
        stored.value = existing()
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.SaveKhatam)
        advanceUntilIdle()

        coVerify(exactly = 0) { khatamUseCases.createKhatam(any()) }
    }

    // ---- The list's actions ----

    @Test
    fun `the three lifecycle actions reach their use cases`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.AbandonKhatam(7))
        vm.onEvent(KhatamEvent.ReactivateKhatam(7))
        vm.onEvent(KhatamEvent.DeleteKhatam(7))
        vm.onEvent(KhatamEvent.SetActiveKhatam(7))
        advanceUntilIdle()

        coVerify { khatamUseCases.abandonKhatam(7) }
        coVerify { khatamUseCases.reactivateKhatam(7) }
        coVerify { khatamUseCases.deleteKhatam(7) }
        coVerify { khatamUseCases.setActiveKhatam(7) }
    }

    @Test
    fun `each action is counted as itself`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(KhatamEvent.AbandonKhatam(7))
        vm.onEvent(KhatamEvent.DeleteKhatam(7))
        vm.onEvent(KhatamEvent.LoadKhatamDetail(7))
        vm.onEvent(KhatamEvent.StartEdit(7))
        advanceUntilIdle()

        assertThat(telemetry.featureUsages.map { it.action })
            .containsAtLeast("abandon", "delete", "open_detail", "open_edit")
    }

    @Test
    fun `typing in the form is not telemetry`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        telemetry.clear()

        vm.onEvent(KhatamEvent.UpdateName("Ramadan"))
        vm.onEvent(KhatamEvent.UpdateNotes("before fajr"))
        vm.onEvent(KhatamEvent.UpdateDailyTarget(30))
        advanceUntilIdle()

        // Every keystroke as an event would drown the dashboard the deliberate acts are on.
        assertThat(telemetry.featureUsages).isEmpty()
    }
}
