package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
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
import java.time.LocalDate

/**
 * The history range in [PrayerTrackerViewModel] must follow the range the user picked.
 *
 * `loadHistory` collected a Room flow with no handle, unlike its sibling `loadForDate`, which
 * has always had `dateRecordsJob`. Every range change — week, month, custom — left its
 * collector alive on `_historyState`, so a change to the prayer table re-emitted to all of
 * them and an earlier range could redraw the chart under a later one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerViewModelHistoryScopeTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var prayerUseCases: PrayerUseCases

    private val week = MutableStateFlow(listOf(record(id = 1)))
    private val month = MutableStateFlow(listOf(record(id = 2)))

    private val weekStart = LocalDate.of(2026, 1, 8)
    private val monthStart = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 1, 15)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk(relaxed = true)

        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        // Two distinct ranges, each with its own flow — as Room hands out one per query.
        every { prayerUseCases.getPrayerRecordsInRange(any(), any()) } answers {
            if (firstArg<Long>() == weekStart.toEpochDay() * 86_400_000L) week else month
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an earlier history range cannot redraw over the range on screen`() = runTest {
        val vm = PrayerTrackerViewModel(prayerUseCases, FakeTodayProvider(LocalDate.now()), RecordingTelemetry())

        vm.onEvent(PrayerTrackerEvent.LoadHistory(weekStart, end))
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.LoadHistory(monthStart, end))
        advanceUntilIdle()

        assertThat(vm.historyState.value.records.map { it.id }).containsExactly(2L)

        // Marking a prayer anywhere re-emits to every live range collector.
        week.value = listOf(record(id = 1), record(id = 11))
        advanceUntilIdle()

        assertThat(vm.historyState.value.records.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `returning to a range re-subscribes to it`() = runTest {
        val vm = PrayerTrackerViewModel(prayerUseCases, FakeTodayProvider(LocalDate.now()), RecordingTelemetry())

        vm.onEvent(PrayerTrackerEvent.LoadHistory(weekStart, end))
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.LoadHistory(monthStart, end))
        advanceUntilIdle()
        vm.onEvent(PrayerTrackerEvent.LoadHistory(weekStart, end))
        advanceUntilIdle()

        week.value = listOf(record(id = 1), record(id = 11))
        advanceUntilIdle()

        assertThat(vm.historyState.value.records.map { it.id }).containsExactly(1L, 11L)
    }
}

private fun record(id: Long) = PrayerRecord(
    id = id,
    date = 0L,
    prayerName = PrayerName.FAJR,
    status = PrayerStatus.PRAYED,
    prayedAt = null,
    scheduledTime = 0L,
    isJamaah = false,
    isQadaFor = null,
    note = null,
    createdAt = 0L,
    updatedAt = 0L
)
