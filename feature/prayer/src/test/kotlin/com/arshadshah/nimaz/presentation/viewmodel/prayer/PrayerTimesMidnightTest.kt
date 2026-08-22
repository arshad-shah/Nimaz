package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.FakePrayerTimetableRepository
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.arshadshah.nimaz.domain.usecase.prayerCalculationSettings
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
import java.time.LocalDate

/**
 * Prayer Times follows the day across midnight — and only when it should.
 *
 * `selectedDate` was a `LocalDate.now()` **data-class default**, evaluated once when the state was
 * constructed, and nothing re-evaluated it. Leaving the screen open across 00:00 — a plausible
 * state for an app whose whole point is Fajr — left it on yesterday. The part that made it more
 * than cosmetic: the "Today" chip renders only `if (!state.isToday)`, and `isToday` kept its last
 * published value of `true`, so **the affordance to get back never appeared**.
 *
 * The opposite mistake is just as bad, so it is tested too: someone who has paged forward to
 * check Friday must not be yanked back to today at midnight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTimesMidnightTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 3, 14)
    private lateinit var todayProvider: FakeTodayProvider
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        todayProvider = FakeTodayProvider(today)
        settings = mockk(relaxed = true) {
            every { use24HourFormat } returns flowOf(true)
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = PrayerTimesViewModel(
        buildPrayerUseCases(FakePrayerTimetableRepository(prayerCalculationSettings())),
        todayProvider,
        dispatcher,
        RecordingTelemetry(),
    )

    @Test
    fun `the screen opens on today`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.selectedDate).isEqualTo(today)
        assertThat(vm.state.value.isToday).isTrue()
    }

    @Test
    fun `midnight moves a screen showing today onto the new day`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        todayProvider.now = today.plusDays(1)
        advanceUntilIdle()

        assertThat(vm.state.value.selectedDate).isEqualTo(today.plusDays(1))
        assertThat(vm.state.value.isToday).isTrue()
    }

    /**
     * The whole reason the staleness was unrecoverable rather than merely wrong: with `isToday`
     * stuck true, `PrayerTimesScreen`'s `if (!state.isToday)` never rendered the chip that
     * returns you to today.
     */
    @Test
    fun `a screen left on yesterday reports that it is not showing today`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            // Page back one day, then let midnight pass. The user is now two days behind.
            vm.onEvent(PrayerTimesEvent.PreviousDay)
            advanceUntilIdle()
            todayProvider.now = today.plusDays(1)
            advanceUntilIdle()

            assertThat(vm.state.value.selectedDate).isEqualTo(today.minusDays(1))
            assertThat(vm.state.value.isToday).isFalse()
        }

    /** Someone who has paged forward stays where they put themselves. */
    @Test
    fun `midnight does not move a screen the user has paged away from`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(PrayerTimesEvent.NextDay)
        advanceUntilIdle()
        val chosen = vm.state.value.selectedDate

        todayProvider.now = today.plusDays(1)
        advanceUntilIdle()

        assertThat(vm.state.value.selectedDate).isEqualTo(chosen)
    }

    /** Tomorrow's Fajr is only carried for today — it is there for the after-Isha wrap. */
    @Test
    fun `tomorrows fajr is published for today and not for another day`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.state.value.tomorrowFajrAt).isNotNull()

        vm.onEvent(PrayerTimesEvent.NextDay)
        advanceUntilIdle()

        assertThat(vm.state.value.tomorrowFajrAt).isNull()
    }
}
