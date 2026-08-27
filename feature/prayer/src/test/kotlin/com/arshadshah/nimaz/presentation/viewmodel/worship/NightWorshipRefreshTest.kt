package com.arshadshah.nimaz.presentation.viewmodel.worship

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.FakePrayerTimetableRepository
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.arshadshah.nimaz.domain.usecase.prayerCalculationSettings
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
 * The manual retry, and what happens when the astronomy behind the hub cannot be worked out.
 *
 * `Refresh` is not a leftover: the window card renders an inline error with a retry button, and
 * this event is what that button dispatches. It is therefore the **only** way out of a failed
 * hub without leaving the screen — and it is a second, separate code path from the observation
 * the ViewModel starts in `init`, with its own error handling. A retry that reported success
 * while leaving the error in place leaves a button that visibly does nothing.
 *
 * The failure arm matters for the same reason it was written: a settings read that throws does
 * so *outside* `computeNightTimes`' `runCatching`, so before `launchSafely` wrapped it the
 * exception reached `viewModelScope`'s uncaught handler — a crash, from a screen someone opened
 * in the middle of the night.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NightWorshipRefreshTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 3, 15)

    private lateinit var prayers: FakePrayerTimetableRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayers = FakePrayerTimetableRepository(
            prayerCalculationSettings(
                latitude = 51.5074,
                longitude = -0.1278,
                highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
            ),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a manual refresh recomputes the window`() = runTest(dispatcher) {
        val vm = NightWorshipViewModel(
            buildPrayerUseCases(prayers),
            FakeTodayProvider(today),
            dispatcher,
            RecordingTelemetry(),
        )
        advanceUntilIdle()

        vm.onEvent(NightWorshipEvent.Refresh)
        advanceUntilIdle()

        val state = vm.state.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.lastThirdAt).isNotNull()
        assertThat(state.fajrAt).isNotNull()
    }

    @Test
    fun `a refresh whose settings read throws lands as an error, not a crash`() =
        runTest(dispatcher) {
            val telemetry = RecordingTelemetry()
            val settings = MutableStateFlow<PrayerCalculationSettings?>(
                prayerCalculationSettings(latitude = 51.5074, longitude = -0.1278)
            )
            val useCases = mockk<PrayerUseCases>(relaxed = true)
            every { useCases.observeCalculationSettings() } answers {
                val current = settings.value
                if (current == null) {
                    kotlinx.coroutines.flow.flow { throw IllegalStateException("settings gone") }
                } else {
                    flowOf(current)
                }
            }
            every { useCases.getSunnahNightTimes(any(), any()) } throws
                IllegalStateException("no times")

            val vm = NightWorshipViewModel(
                useCases,
                FakeTodayProvider(today),
                dispatcher,
                telemetry,
            )
            advanceUntilIdle()

            // The settings stream fails only on the retry, so this is the refresh path's own
            // handler rather than the one `init` already exercised.
            settings.value = null
            vm.onEvent(NightWorshipEvent.Refresh)
            advanceUntilIdle()

            assertThat(vm.state.value.isLoading).isFalse()
            assertThat(vm.state.value.error).isNotNull()
            assertThat(telemetry.errors).isNotEmpty()
        }

    @Test
    fun `times that cannot be computed are reported inline and counted`() = runTest(dispatcher) {
        val telemetry = RecordingTelemetry()
        val useCases = mockk<PrayerUseCases>(relaxed = true)
        every { useCases.observeCalculationSettings() } returns
            flowOf(prayerCalculationSettings(latitude = 51.5074, longitude = -0.1278))
        every { useCases.getSunnahNightTimes(any(), any()) } throws
            IllegalStateException("sunnah times unavailable")

        val vm = NightWorshipViewModel(
            useCases,
            FakeTodayProvider(today),
            dispatcher,
            telemetry,
        )
        advanceUntilIdle()

        // Reported through `failure`, not a bare `recordException`: the stack trace used to reach
        // Crashlytics while the *frequency* reached nothing, so how often the hub fails to
        // resolve a night was invisible.
        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(telemetry.errors).isNotEmpty()
    }
}
