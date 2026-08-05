package com.arshadshah.nimaz.presentation.viewmodel.onboarding

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.repository.Coordinates
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.settings.AppSettings
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
 * `OnboardingViewModel` had zero tests, and could not have had any: it built a
 * `FusedLocationProviderClient` in a **property initializer**, so merely constructing it on the
 * JVM reached into Play Services. `DeviceLocationRepository`'s own KDoc named this ViewModel as
 * the second of the two stuck that way.
 *
 * It now takes the seams, which is what makes everything below expressible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeDeviceLocation(
        private val coordinates: Coordinates? = Coordinates(51.5074, -0.1278),
        private val name: String? = "London",
        private val failWith: Throwable? = null,
    ) : DeviceLocationRepository {
        override suspend fun currentCoordinates(): Coordinates? {
            failWith?.let { throw it }
            return coordinates
        }

        override suspend fun search(query: String, limit: Int): List<SearchLocation> = emptyList()
        override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = name
    }

    private class FakePermissions(
        private val location: Boolean = true,
        private val notification: Boolean = true,
    ) : PermissionChecker {
        override fun hasLocationPermission() = location
        override fun hasNotificationPermission() = notification
    }

    private class FakePower(private val exempt: Boolean = false) : PowerSettings {
        override fun isIgnoringBatteryOptimizations() = exempt
    }

    private class FakeAppSettings(completed: Boolean = false) : AppSettings {
        val completedFlow = MutableStateFlow(completed)
        override val onboardingCompleted: Flow<Boolean> = completedFlow
        override suspend fun setOnboardingCompleted(completed: Boolean) {
            completedFlow.value = completed
        }

        override val appLanguage: Flow<String> = MutableStateFlow("en")
        override suspend fun setAppLanguage(language: String) = Unit
    }

    private class RecordingLocationSettings : LocationSettings {
        var saved: Triple<Double, Double, String>? = null
            private set

        override val latitude: Flow<Double> = MutableStateFlow(0.0)
        override val longitude: Flow<Double> = MutableStateFlow(0.0)
        override val locationName: Flow<String> = MutableStateFlow("")
        override val userPreferences: Flow<UserPreferences> = MutableStateFlow(mockk(relaxed = true))
        override suspend fun updateLocation(latitude: Double, longitude: Double, name: String) {
            saved = Triple(latitude, longitude, name)
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        deviceLocation: DeviceLocationRepository = FakeDeviceLocation(),
        permissions: PermissionChecker = FakePermissions(),
        power: PowerSettings = FakePower(),
        appSettings: AppSettings = FakeAppSettings(),
        locationSettings: LocationSettings = RecordingLocationSettings(),
        telemetry: RecordingTelemetry = RecordingTelemetry(),
    ) = OnboardingViewModel(
        deviceLocation, permissions, power, appSettings, locationSettings, telemetry,
    )

    @Test
    fun `permission and battery state is read from the seams at construction`() =
        runTest(dispatcher) {
            val vm = viewModel(
                permissions = FakePermissions(location = true, notification = false),
                power = FakePower(exempt = true),
            )
            advanceUntilIdle()

            assertThat(vm.state.value.locationPermissionGranted).isTrue()
            assertThat(vm.state.value.notificationPermissionGranted).isFalse()
            assertThat(vm.state.value.batteryOptimizationDisabled).isTrue()
        }

    @Test
    fun `a detected location is reverse-geocoded and persisted`() = runTest(dispatcher) {
        val settings = RecordingLocationSettings()
        val vm = viewModel(locationSettings = settings)
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.DetectLocation)
        advanceUntilIdle()

        assertThat(settings.saved).isEqualTo(Triple(51.5074, -0.1278, "London"))
        assertThat(vm.state.value.locationDetected).isTrue()
        assertThat(vm.state.value.locationName).isEqualTo("London")
    }

    @Test
    fun `detection without permission does not reach the device`() = runTest(dispatcher) {
        val settings = RecordingLocationSettings()
        val vm = viewModel(
            permissions = FakePermissions(location = false),
            locationSettings = settings,
        )
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.DetectLocation)
        advanceUntilIdle()

        assertThat(settings.saved).isNull()
        assertThat(vm.state.value.locationDetected).isFalse()
    }

    @Test
    fun `a location fix that throws is reported and surfaced, not swallowed`() =
        runTest(dispatcher) {
            val telemetry = RecordingTelemetry()
            val vm = viewModel(
                deviceLocation = FakeDeviceLocation(failWith = IllegalStateException("no provider")),
                telemetry = telemetry,
            )
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.DetectLocation)
            advanceUntilIdle()

            assertThat(vm.state.value.error).isNotNull()
            assertThat(vm.state.value.locationDetected).isFalse()
        }

    @Test
    fun `an empty fix says so rather than claiming a location`() = runTest(dispatcher) {
        val settings = RecordingLocationSettings()
        val vm = viewModel(
            deviceLocation = FakeDeviceLocation(coordinates = null),
            locationSettings = settings,
        )
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.DetectLocation)
        advanceUntilIdle()

        assertThat(settings.saved).isNull()
        assertThat(vm.state.value.error).isNotNull()
    }

    @Test
    fun `completing onboarding persists the flag`() = runTest(dispatcher) {
        val appSettings = FakeAppSettings(completed = false)
        val vm = viewModel(appSettings = appSettings)
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.CompleteOnboarding)
        advanceUntilIdle()

        assertThat(appSettings.completedFlow.value).isTrue()
    }

    @Test
    fun `dismissing the error clears it`() = runTest(dispatcher) {
        val vm = viewModel(
            deviceLocation = FakeDeviceLocation(failWith = IllegalStateException("no provider")),
        )
        advanceUntilIdle()
        vm.onEvent(OnboardingEvent.DetectLocation)
        advanceUntilIdle()
        assertThat(vm.state.value.error).isNotNull()

        vm.onEvent(OnboardingEvent.DismissError)
        advanceUntilIdle()

        assertThat(vm.state.value.error).isNull()
    }
}
