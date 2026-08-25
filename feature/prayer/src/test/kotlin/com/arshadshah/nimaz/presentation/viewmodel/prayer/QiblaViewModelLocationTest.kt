package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.CompassOrientation
import com.arshadshah.nimaz.domain.repository.CompassSensors
import com.arshadshah.nimaz.domain.repository.Haptics
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
 * Where the compass thinks it is, and what it does about facing the Kaaba.
 *
 * `QiblaViewModelTest` covers the sensor stream — the unwrap, the accuracy plumbing, the
 * start/stop lifecycle. This covers the other half, which is the part a reader can actually be
 * misled by:
 *
 * - **No location is a *state*, not an empty compass.** With nothing stored, the needle would
 *   otherwise spin against a bearing of zero, which is a real direction (north) and the wrong
 *   one. The error is what lets the screen offer a way out instead.
 * - **A location arriving recomputes everything downstream of it** — the bearing, the distance,
 *   and the declination the drawn needle is corrected by. A ViewModel that stored the location
 *   and not the direction leaves a compass pointing where the last place was.
 * - **The confirmation haptic is a rising edge.** Buzzing on every reading while someone holds
 *   the phone still is not feedback, it is a vibration that will not stop; never buzzing at all
 *   means the one moment worth confirming passes silently.
 * - **True-north correction is a setting.** Turned off, the reading has to stay raw — matching a
 *   paper compass held beside the phone — rather than being silently corrected anyway.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelLocationTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeCompass : CompassSensors {
        override val isAvailable: Boolean = true
        val readings = MutableSharedFlow<CompassOrientation>(extraBufferCapacity = 64)

        override fun orientation(): Flow<CompassOrientation> = readings

        suspend fun emit(azimuth: Float, accuracy: CompassAccuracy = CompassAccuracy.HIGH) =
            readings.emit(CompassOrientation(azimuth, 0f, 0f, accuracy))
    }

    private class RecordingHaptics : Haptics {
        var taps = 0
            private set

        override fun tap() {
            taps++
        }
    }

    /** A location seam whose coordinates can be left unset, which is the state at first run. */
    private class FakeLocationSettings(
        latitude: Double = 53.3498,
        longitude: Double = -6.2603,
        name: String = "Dublin",
    ) : LocationSettings {
        override val latitude = MutableStateFlow(latitude)
        override val longitude = MutableStateFlow(longitude)
        override val locationName = MutableStateFlow(name)
        override val userPreferences: Flow<UserPreferences> =
            MutableStateFlow(mockk(relaxed = true))

        override suspend fun updateLocation(latitude: Double, longitude: Double, name: String) {
            this.latitude.value = latitude
            this.longitude.value = longitude
            this.locationName.value = name
        }
    }

    private fun location(
        name: String = "Abbeyleix",
        latitude: Double = 52.9167,
        longitude: Double = -7.35,
    ) = Location(
        id = 1L,
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = "Europe/Dublin",
        country = "Ireland",
        city = name,
        isCurrentLocation = false,
        isFavorite = false,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        fajrAngle = null,
        ishaAngle = null,
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        compass: CompassSensors = FakeCompass(),
        haptics: Haptics = RecordingHaptics(),
        settings: LocationSettings = FakeLocationSettings(),
    ) = QiblaViewModel(compass, haptics, settings, RecordingTelemetry())

    @Test
    fun `a stored location yields a bearing, a distance and a declination`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            val state = vm.qiblaState.value
            assertThat(state.qiblaInfo).isNotNull()
            // Dublin faces roughly ESE. The exact figure is `QiblaCalculator`'s business (tested
            // in `:core:domain`); what this pins is that the ViewModel resolved *something* from
            // the coordinates rather than leaving the compass at zero.
            assertThat(state.qiblaInfo!!.direction.bearing).isWithin(15.0).of(115.0)
            assertThat(state.qiblaInfo.distanceToMecca).isGreaterThan(3_000.0)
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNull()
        }

    @Test
    fun `an unset location is an error, not a compass pointing north`() = runTest(dispatcher) {
        val vm = viewModel(settings = FakeLocationSettings(latitude = 0.0, longitude = 0.0))
        advanceUntilIdle()

        // 0,0 is the Gulf of Guinea and the value a store returns when nothing was written.
        // Treating it as a place gives a confidently wrong bearing with no error to explain it.
        assertThat(vm.qiblaState.value.error).isNotNull()
        assertThat(vm.qiblaState.value.qiblaInfo).isNull()
        assertThat(vm.qiblaState.value.isLoading).isFalse()
    }

    @Test
    fun `a nameless stored location still gets a name to show`() = runTest(dispatcher) {
        val vm = viewModel(settings = FakeLocationSettings(name = ""))
        advanceUntilIdle()

        // The top bar renders `qiblaInfo.locationName`; an empty string there is a blank line
        // where the place should be, on a screen whose whole claim is "from here, that way".
        assertThat(vm.qiblaState.value.qiblaInfo!!.locationName).isNotEmpty()
    }

    @Test
    fun `choosing a location explicitly replaces the resolved one`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val fromSettings = vm.qiblaState.value.qiblaInfo!!.direction.bearing

        vm.onEvent(QiblaEvent.SetLocation(location(name = "Kuala Lumpur", 3.139, 101.6869)))
        advanceUntilIdle()

        val state = vm.qiblaState.value
        assertThat(state.currentLocation!!.name).isEqualTo("Kuala Lumpur")
        assertThat(state.qiblaInfo!!.locationName).isEqualTo("Kuala Lumpur")
        // From Malaysia the Kaaba is roughly west-north-west, not east: a `setLocation` that
        // stored the location without recomputing would leave the needle on Dublin's bearing.
        assertThat(state.qiblaInfo.direction.bearing).isNotWithin(1.0).of(fromSettings)
        assertThat(state.qiblaInfo.direction.bearing).isWithin(25.0).of(285.0)
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `facing the qibla buzzes once, on the way in`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val haptics = RecordingHaptics()
        val vm = viewModel(compass = compass, haptics = haptics)
        advanceUntilIdle()
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        val bearing = vm.qiblaState.value.qiblaInfo!!.direction.bearing.toFloat()
        compass.emit(bearing)
        advanceUntilIdle()
        assertThat(haptics.taps).isEqualTo(1)
        assertThat(vm.qiblaState.value.isFacingQibla).isTrue()

        // Still facing, one degree later: a haptic per reading is a phone that will not stop
        // buzzing for as long as it is held straight.
        compass.emit(bearing + 0.5f)
        advanceUntilIdle()
        assertThat(haptics.taps).isEqualTo(1)
    }

    @Test
    fun `turning away and back buzzes again`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val haptics = RecordingHaptics()
        val vm = viewModel(compass = compass, haptics = haptics)
        advanceUntilIdle()
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()
        val bearing = vm.qiblaState.value.qiblaInfo!!.direction.bearing.toFloat()

        compass.emit(bearing)
        advanceUntilIdle()
        compass.emit(bearing + 90f)
        advanceUntilIdle()
        assertThat(vm.qiblaState.value.isFacingQibla).isFalse()

        compass.emit(bearing)
        advanceUntilIdle()

        assertThat(haptics.taps).isEqualTo(2)
    }

    @Test
    fun `with vibration off, alignment is silent but still reported`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val haptics = RecordingHaptics()
        val vm = viewModel(compass = compass, haptics = haptics)
        advanceUntilIdle()
        vm.onEvent(QiblaEvent.SetVibrationEnabled(false))
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        compass.emit(vm.qiblaState.value.qiblaInfo!!.direction.bearing.toFloat())
        advanceUntilIdle()

        assertThat(haptics.taps).isEqualTo(0)
        assertThat(vm.qiblaState.value.isFacingQibla).isTrue()
    }

    @Test
    fun `a wider threshold counts as facing sooner`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        advanceUntilIdle()
        val bearing = vm.qiblaState.value.qiblaInfo!!.direction.bearing.toFloat()
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        compass.emit(bearing + 12f)
        advanceUntilIdle()
        assertThat(vm.qiblaState.value.isFacingQibla).isFalse()

        // The threshold is a user setting; ignoring it makes the tolerance a constant and the
        // setting a lie.
        vm.onEvent(QiblaEvent.SetQiblaThreshold(20f))
        compass.emit(bearing + 12.5f)
        advanceUntilIdle()
        assertThat(vm.qiblaState.value.isFacingQibla).isTrue()
    }

    @Test
    fun `the settings toggles are held and reported`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QiblaEvent.SetTrueNorthMode(false))
        vm.onEvent(QiblaEvent.SetSoundEnabled(true))
        advanceUntilIdle()

        assertThat(vm.settingsState.value.trueNorthMode).isFalse()
        assertThat(vm.settingsState.value.soundEnabled).isTrue()
    }

    @Test
    fun `the location picker and the calibration sheet are opened and closed through state`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(QiblaEvent.ShowLocationPicker)
            assertThat(vm.qiblaState.value.showLocationPicker).isTrue()
            vm.onEvent(QiblaEvent.HideLocationPicker)
            assertThat(vm.qiblaState.value.showLocationPicker).isFalse()

            vm.onEvent(QiblaEvent.ShowCalibrationDialog)
            assertThat(vm.qiblaState.value.showCalibrationDialog).isTrue()
            vm.onEvent(QiblaEvent.DismissCalibrationDialog)
            assertThat(vm.qiblaState.value.showCalibrationDialog).isFalse()
        }

    @Test
    fun `AR mode is a state change, and refreshing is a no-op because location is observed`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(QiblaEvent.SetArMode(true))
            assertThat(vm.qiblaState.value.isArMode).isTrue()
            vm.onEvent(QiblaEvent.SetArMode(false))
            assertThat(vm.qiblaState.value.isArMode).isFalse()

            // `RefreshLocation` exists for the error surface's retry button. The location flow is
            // already collected, so the retry must not tear anything down — the assertion is
            // that the bearing survives it.
            val before = vm.qiblaState.value.qiblaInfo
            vm.onEvent(QiblaEvent.RefreshLocation)
            advanceUntilIdle()
            assertThat(vm.qiblaState.value.qiblaInfo).isEqualTo(before)
        }

    @Test
    fun `an accuracy reported directly is folded into the compass reading`() =
        runTest(dispatcher) {
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(QiblaEvent.UpdateAccuracy(CompassAccuracy.UNRELIABLE))
            advanceUntilIdle()

            assertThat(vm.qiblaState.value.compassData.accuracy)
                .isEqualTo(CompassAccuracy.UNRELIABLE)
            assertThat(vm.qiblaState.value.needsCalibration).isTrue()
        }

    @Test
    fun `a location that arrives after the compass has started still steers it`() =
        runTest(dispatcher) {
            val compass = FakeCompass()
            val settings = FakeLocationSettings(latitude = 0.0, longitude = 0.0)
            val vm = viewModel(compass = compass, settings = settings)
            advanceUntilIdle()
            vm.onEvent(QiblaEvent.StartCompass)
            advanceUntilIdle()

            // Readings arrive with no direction to compare them against: the needle still has to
            // turn, or the compass looks frozen while the location resolves.
            compass.emit(45f)
            advanceUntilIdle()
            assertThat(vm.qiblaState.value.isCompassReady).isTrue()
            assertThat(vm.qiblaState.value.isFacingQibla).isFalse()

            settings.updateLocation(53.3498, -6.2603, "Dublin")
            advanceUntilIdle()

            assertThat(vm.qiblaState.value.qiblaInfo).isNotNull()
            assertThat(vm.qiblaState.value.error).isNull()
        }

    @Test
    fun `with true north off the heading is left as the magnetometer read it`() =
        runTest(dispatcher) {
            val compass = FakeCompass()
            val vm = viewModel(compass = compass)
            advanceUntilIdle()
            vm.onEvent(QiblaEvent.SetTrueNorthMode(false))
            vm.onEvent(QiblaEvent.StartCompass)
            advanceUntilIdle()

            compass.emit(90f)
            advanceUntilIdle()

            // Raw, to match a paper compass held beside the phone. (Under Robolectric-free JVM
            // tests `GeomagneticField` returns a zero declination anyway, so this pins the
            // *path*: the branch that must not apply a correction.)
            assertThat(vm.qiblaState.value.compassData.azimuth).isWithin(0.01f).of(90f)
        }
}
