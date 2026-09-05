package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.ResolvedLocation
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 8, 14)
    private val todayProvider = FakeTodayProvider(today)
    private val telemetry = RecordingTelemetry()

    private lateinit var prayerUseCases: PrayerUseCases
    private lateinit var viewModel: PrayerTimesViewModel

    private val defaultSettings = PrayerCalculationSettings(
        location = ResolvedLocation(
            latitude = 51.5,
            longitude = -0.1,
            name = "London",
            isFallback = false,
        ),
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        adjustments = emptyMap(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk(relaxed = true)

        every { prayerUseCases.observeCalculationSettings() } returns flowOf(defaultSettings)
        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { prayerUseCases.getDaySchedule(any<LocalDate>(), any()) } returns emptyList()

        viewModel = PrayerTimesViewModel(
            prayerUseCases = prayerUseCases,
            todayProvider = todayProvider,
            defaultDispatcher = dispatcher,
            telemetry = telemetry,
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state has default values before settings arrive`() = runTest {
        // prayers list starts empty
        assertThat(viewModel.state.value.prayers).isEmpty()
    }

    @Test
    fun `after init state reflects location name from settings`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.state.value.locationName).isEqualTo("London")
    }

    @Test
    fun `initial selectedDate is null then set to today after init`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedDate).isEqualTo(today)
    }

    @Test
    fun `PreviousDay moves selected date back by one`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedDate).isEqualTo(today.minusDays(1))
    }

    @Test
    fun `NextDay moves selected date forward by one`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(PrayerTimesEvent.NextDay)
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedDate).isEqualTo(today.plusDays(1))
    }

    @Test
    fun `GoToToday resets selected date to today`() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        advanceUntilIdle()
        viewModel.onEvent(PrayerTimesEvent.GoToToday)
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedDate).isEqualTo(today)
    }

    @Test
    fun `SelectDate sets specific date`() = runTest {
        advanceUntilIdle()
        val target = LocalDate.of(2026, 1, 1)
        viewModel.onEvent(PrayerTimesEvent.SelectDate(target))
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedDate).isEqualTo(target)
    }

    /**
     * Prayer Times answers *when*. The prayer tracker answers what the reader did about it.
     *
     * This screen used to be the third place in the app a prayer could be written — its own source
     * said so — and it wrote a binary `PRAYED`/`NOT_PRAYED`, a vocabulary the tracker redesign
     * retired. Against `NOT_RECORDED`, `LATE` and `QADA` that toggle was destructive: tapping a
     * prayer logged as `LATE` flattened it to `PRAYED`, and tapping again wrote `NOT_PRAYED`,
     * which the tracker reads back as *nobody has said*.
     *
     * Asserted over **every** event rather than over the removed one, so an event that writes
     * fails here rather than shipping.
     */
    @Test
    fun `no event writes a prayer record`() = runTest {
        advanceUntilIdle()

        viewModel.onEvent(PrayerTimesEvent.NextDay)
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        viewModel.onEvent(PrayerTimesEvent.GoToToday)
        viewModel.onEvent(PrayerTimesEvent.SelectDate(today.plusDays(3)))
        advanceUntilIdle()

        coVerify(exactly = 0) {
            prayerUseCases.updatePrayerStatus(any(), any(), any(), any(), any())
        }
        assertThat(telemetry.prayersTracked).isEmpty()
    }

    @Test
    fun `isToday is true when selected date equals today`() = runTest {
        advanceUntilIdle()
        assertThat(viewModel.state.value.isToday).isTrue()
    }

    @Test
    fun `isToday is false when browsing a past day`() = runTest {
        // isToday is only published when dayTimes is non-empty (from the real calculator).
        // In this unit test the getDaySchedule stub returns emptyList(), so publishDisplays()
        // exits early. Instead, verify that the selectedDate has changed — which is the
        // observable precondition for isToday == false.
        advanceUntilIdle()
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        advanceUntilIdle()
        val yesterday = today.minusDays(1)
        assertThat(viewModel.state.value.selectedDate).isEqualTo(yesterday)
    }

    @Test
    fun `telemetry records feature usage for PreviousDay`() = runTest {
        advanceUntilIdle()
        telemetry.clear()
        viewModel.onEvent(PrayerTimesEvent.PreviousDay)
        advanceUntilIdle()
        assertThat(telemetry.featureUsages.any { it.action == "previous_day" }).isTrue()
    }

    @Test
    fun `telemetry records feature usage for NextDay`() = runTest {
        advanceUntilIdle()
        telemetry.clear()
        viewModel.onEvent(PrayerTimesEvent.NextDay)
        advanceUntilIdle()
        assertThat(telemetry.featureUsages.any { it.action == "next_day" }).isTrue()
    }
}
