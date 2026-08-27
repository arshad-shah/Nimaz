package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.CompassAccuracy
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
 * `QiblaViewModel` held a `SensorManager` and a `Vibrator` off an `@ApplicationContext`, so it
 * could not be constructed on the JVM and none of the behaviour below was testable — including
 * the azimuth unwrap, which exists precisely because a compass that snaps through 360→0 sends
 * the needle the long way round.
 *
 * The sensors are now behind `CompassSensors`, which emits finished orientation, so a fake can
 * simply push readings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeCompass(override val isAvailable: Boolean = true) : CompassSensors {
        val readings = MutableSharedFlow<CompassOrientation>(extraBufferCapacity = 64)
        var collections = 0
            private set

        override fun orientation(): Flow<CompassOrientation> = kotlinx.coroutines.flow.flow {
            collections++
            readings.collect { emit(it) }
        }

        suspend fun emit(
            azimuth: Float,
            accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        ) = readings.emit(CompassOrientation(azimuth, 0f, 0f, accuracy))
    }

    private class RecordingHaptics : Haptics {
        var taps = 0
            private set

        override fun tap() {
            taps++
        }
    }

    private class StubLocationSettings : LocationSettings {
        override val latitude: Flow<Double> = MutableStateFlow(51.5074)
        override val longitude: Flow<Double> = MutableStateFlow(-0.1278)
        override val locationName: Flow<String> = MutableStateFlow("London")
        override val userPreferences: Flow<UserPreferences> = MutableStateFlow(mockk(relaxed = true))
        override suspend fun updateLocation(latitude: Double, longitude: Double, name: String) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        compass: CompassSensors = FakeCompass(),
        haptics: Haptics = RecordingHaptics(),
    ) = QiblaViewModel(compass, haptics, StubLocationSettings(), RecordingTelemetry())

    @Test
    fun `crossing north does not send the needle the long way round`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        compass.emit(350f)
        advanceUntilIdle()
        val before = vm.qiblaState.value.animatedAzimuth

        compass.emit(10f)
        advanceUntilIdle()
        val after = vm.qiblaState.value.animatedAzimuth

        // Raw, this is 350 -> 10, which reads as -340. Unwrapped it is +20.
        assertThat(after - before).isWithin(0.01f).of(20f)
    }

    @Test
    fun `a low accuracy reading asks for calibration`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        compass.emit(0f, accuracy = CompassAccuracy.LOW)
        advanceUntilIdle()

        assertThat(vm.qiblaState.value.needsCalibration).isTrue()
        assertThat(vm.qiblaState.value.compassData.accuracy).isEqualTo(CompassAccuracy.LOW)
    }

    @Test
    fun `a high accuracy reading clears the calibration prompt`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        compass.emit(0f, accuracy = CompassAccuracy.UNRELIABLE)
        advanceUntilIdle()
        assertThat(vm.qiblaState.value.needsCalibration).isTrue()

        compass.emit(1f, accuracy = CompassAccuracy.HIGH)
        advanceUntilIdle()

        assertThat(vm.qiblaState.value.needsCalibration).isFalse()
    }

    @Test
    fun `stopping the compass ends the collection`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()
        compass.emit(90f)
        advanceUntilIdle()
        val whileRunning = vm.qiblaState.value.animatedAzimuth

        vm.onEvent(QiblaEvent.StopCompass)
        advanceUntilIdle()
        compass.emit(180f)
        advanceUntilIdle()

        // The reading after StopCompass must not reach state — the old code relied on
        // unregisterListener being remembered; now cancelling the collection does it.
        assertThat(vm.qiblaState.value.animatedAzimuth).isEqualTo(whileRunning)
    }

    @Test
    fun `a device with no compass is not collected from at all`() = runTest(dispatcher) {
        val compass = FakeCompass(isAvailable = false)
        val vm = viewModel(compass = compass)

        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        assertThat(compass.collections).isEqualTo(0)
    }

    @Test
    fun `restarting the compass resets the accumulated azimuth`() = runTest(dispatcher) {
        val compass = FakeCompass()
        val vm = viewModel(compass = compass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()
        compass.emit(90f)
        advanceUntilIdle()
        assertThat(vm.qiblaState.value.animatedAzimuth).isNotEqualTo(0f)

        vm.onEvent(QiblaEvent.StopCompass)
        vm.onEvent(QiblaEvent.StartCompass)
        advanceUntilIdle()

        assertThat(vm.qiblaState.value.animatedAzimuth).isEqualTo(0f)
        assertThat(vm.qiblaState.value.isCompassReady).isFalse()
    }
}
