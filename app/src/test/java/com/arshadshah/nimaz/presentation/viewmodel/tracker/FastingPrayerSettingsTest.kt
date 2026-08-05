package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.FastingRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.presentation.viewmodel.FakePrayerTimetableRepository
import com.arshadshah.nimaz.presentation.viewmodel.buildFastingUseCases
import com.arshadshah.nimaz.presentation.viewmodel.buildPrayerUseCases
import com.arshadshah.nimaz.presentation.viewmodel.prayerCalculationSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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

/**
 * Fast Tracker's suhoor and iftar honour the user's calculation settings.
 *
 * They did not. `FastingViewModel` called `prayerTimeCalculator.getPrayerTimes(lat, lng)` and took
 * **all four** calculation defaults — Muslim World League, Shafi, no high-latitude rule, no
 * adjustments — while Home passed the user's own. The same Fajr, two different times, in one app,
 * and nothing in the type system objected because every one of those four arguments has a default.
 *
 * This suite could not have been written before the seam existed: `PrayerTimeCalculator` is a
 * concrete class with no interface, so a test had no way to observe *which* settings a call used.
 * That is the whole argument for the extraction — the bug was not subtle, it was invisible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FastingPrayerSettingsTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fasting: FastingRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fasting = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        coEvery { fasting.getVoluntaryFastCount() } returns 0
        coEvery { fasting.getTotalFidyaPaid() } returns 0.0
        every { settings.use24HourFormat } returns flowOf(true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(prayers: FakePrayerTimetableRepository) = FastingViewModel(
        buildFastingUseCases(fasting),
        buildPrayerUseCases(prayers),
        RecordingTelemetry(),
    )

    /**
     * The **method** moves Fajr, and suhoor is Fajr. Egyptian uses a 19.5° Fajr angle against
     * MWL's 18° — a degree and a half, which at London in August is several minutes. If the
     * tracker were still taking the default, both runs would produce the same suhoor.
     *
     * Umm al-Qura would have been the more obvious contrast and is the wrong choice: its Fajr
     * angle is 18.5°, half a degree from MWL, and the calculator rounds to the minute — so the
     * two agree on some dates and the test would pass or fail with the calendar.
     *
     * Cairo, not the fake's default London. At 51.5°N in summer the sun never gets 18° below the
     * horizon, so every Fajr angle resolves through the same twilight fallback and *all* methods
     * agree — a location where this test cannot fail, whatever the code does.
     */
    @Test
    fun `suhoor follows the configured calculation method`() = runTest(dispatcher) {
        val mwl = viewModel(
            FakePrayerTimetableRepository(
                prayerCalculationSettings(
                    latitude = CAIRO_LAT,
                    longitude = CAIRO_LON,
                    calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
                ),
            ),
        )
        advanceUntilIdle()
        val mwlSuhoor = mwl.trackerState.value.suhoorAt

        val egyptian = viewModel(
            FakePrayerTimetableRepository(
                prayerCalculationSettings(
                    latitude = CAIRO_LAT,
                    longitude = CAIRO_LON,
                    calculationMethod = CalculationMethod.EGYPTIAN,
                ),
            ),
        )
        advanceUntilIdle()
        val egyptianSuhoor = egyptian.trackerState.value.suhoorAt

        assertThat(mwlSuhoor).isNotNull()
        assertThat(egyptianSuhoor).isNotNull()
        assertThat(egyptianSuhoor).isNotEqualTo(mwlSuhoor)
    }

    /**
     * A per-prayer adjustment reaches the tracker too. This is the half the old code could not
     * have got right even by accident: it passed no adjustments map at all, so a user who had
     * nudged Maghrib by five minutes to match their local mosque saw the adjusted time on Home
     * and the unadjusted one on the screen telling them when to break their fast.
     */
    @Test
    fun `iftar honours a maghrib adjustment`() = runTest(dispatcher) {
        val unadjusted = viewModel(FakePrayerTimetableRepository(prayerCalculationSettings()))
        advanceUntilIdle()
        val plain = unadjusted.trackerState.value.iftarAt

        val adjusted = viewModel(
            FakePrayerTimetableRepository(
                prayerCalculationSettings(adjustments = mapOf(PrayerType.MAGHRIB to 5)),
            ),
        )
        advanceUntilIdle()
        val nudged = adjusted.trackerState.value.iftarAt

        assertThat(plain).isNotNull()
        assertThat(nudged).isNotNull()
        assertThat((nudged!! - plain!!).inWholeMinutes).isEqualTo(5)
    }

    /**
     * A fallback location still produces times rather than nothing.
     *
     * The snapshot's location is already resolved, so the tracker can never compute against the
     * unset (0, 0) — which is in the Atlantic, and which the old code guarded against with its
     * own `resolveLocation` call that a future edit could have dropped.
     */
    @Test
    fun `a fallback location still yields suhoor and iftar`() = runTest(dispatcher) {
        val vm = viewModel(
            FakePrayerTimetableRepository(
                prayerCalculationSettings(isFallback = true, asrCalculation = AsrCalculation.HANAFI),
            ),
        )
        advanceUntilIdle()

        assertThat(vm.trackerState.value.suhoorAt).isNotNull()
        assertThat(vm.trackerState.value.iftarAt).isNotNull()
    }

    /**
     * Changing a setting recomputes. The old code observed latitude, longitude and the clock
     * format — so switching calculation method left the tracker showing the previous method's
     * times until something else moved the location.
     */
    @Test
    fun `changing the calculation method recomputes suhoor`() = runTest(dispatcher) {
        val prayers = FakePrayerTimetableRepository(
            prayerCalculationSettings(latitude = CAIRO_LAT, longitude = CAIRO_LON),
        )
        val vm = viewModel(prayers)
        advanceUntilIdle()
        val before = vm.trackerState.value.suhoorAt

        prayers.setSettings(
            prayerCalculationSettings(
                latitude = CAIRO_LAT,
                longitude = CAIRO_LON,
                calculationMethod = CalculationMethod.EGYPTIAN,
            ),
        )
        advanceUntilIdle()

        assertThat(vm.trackerState.value.suhoorAt).isNotEqualTo(before)
    }

    private companion object {
        /** Low enough that astronomical twilight actually happens in August. */
        const val CAIRO_LAT = 30.0444
        const val CAIRO_LON = 31.2357
    }
}
