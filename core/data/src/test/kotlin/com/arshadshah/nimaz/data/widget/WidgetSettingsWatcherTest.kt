package com.arshadshah.nimaz.data.widget

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.RecordingWidgetRefresher
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
 * The widgets are told when a setting they are computed from changes.
 *
 * Only the prayer tracker had a push, and only for prayer status, so changing location or
 * calculation method left the home screen showing the previous answer for up to fifteen minutes —
 * six hours for the Hijri widgets. The setting had taken; the widget just had not been told, and
 * from the home screen those two look identical.
 */
class WidgetSettingsWatcherTest {

    private val dispatcher = StandardTestDispatcher()

    private val prayerRepository: PrayerRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val refresher = RecordingWidgetRefresher()

    private val baseSettings = PrayerCalculationSettings(
        location = resolveLocation(53.34, -6.26, "Dublin"),
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        adjustments = emptyMap(),
    )

    private val calculation = MutableStateFlow(baseSettings)
    private val use24Hour = MutableStateFlow(false)
    private val hijriOffset = MutableStateFlow(0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { prayerRepository.observeCalculationSettings() } returns calculation
        every { settingsRepository.use24HourFormat } returns use24Hour
        every { settingsRepository.hijriDayOffset } returns hijriOffset
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun watcher() =
        WidgetSettingsWatcher(prayerRepository, settingsRepository, refresher, dispatcher)

    /**
     * Collection starts at app launch, and the value it starts on is the one the widgets were
     * already refreshed for. Refreshing on it would mean six workers on every cold start.
     */
    @Test
    fun `the state at startup does not trigger a refresh`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        assertThat(refresher.refreshAllCount).isEqualTo(0)
    }

    @Test
    fun `changing the calculation method refreshes every widget`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        calculation.value = baseSettings.copy(calculationMethod = CalculationMethod.KARACHI)
        advanceUntilIdle()

        assertThat(refresher.refreshAllCount).isEqualTo(1)
    }

    @Test
    fun `moving location refreshes every widget`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        calculation.value = baseSettings.copy(location = resolveLocation(21.42, 39.82, "Makkah"))
        advanceUntilIdle()

        assertThat(refresher.refreshAllCount).isEqualTo(1)
    }

    /** The clock format and the Hijri offset are read by widgets too, not just by the app. */
    @Test
    fun `the clock format and hijri offset refresh every widget`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        use24Hour.value = true
        advanceUntilIdle()
        hijriOffset.value = 1
        advanceUntilIdle()

        assertThat(refresher.refreshAllCount).isEqualTo(2)
    }

    /**
     * Settings flows re-emit on any write to their store, including writes of the same value.
     * Refreshing on those would mean six workers enqueued for nothing.
     */
    @Test
    fun `writing the same values again does not refresh`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        calculation.value = baseSettings.copy()
        use24Hour.value = false
        advanceUntilIdle()

        assertThat(refresher.refreshAllCount).isEqualTo(0)
    }

    /** "Everything changed" is not "the tracker changed" — the two hooks stay distinct. */
    @Test
    fun `a settings change does not masquerade as a tracker change`() = runTest(dispatcher) {
        watcher().start()
        advanceUntilIdle()

        hijriOffset.value = -1
        advanceUntilIdle()

        assertThat(refresher.refreshCount).isEqualTo(0)
    }
}
