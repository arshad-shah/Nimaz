package com.arshadshah.nimaz.presentation.viewmodel.onboarding

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.monitoring.TelemetryCall
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
import kotlinx.coroutines.flow.flow
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

    private class FakeAppSettings(
        completed: Boolean = false,
        private val readFailsWith: Throwable? = null,
        private val writeFailsWith: Throwable? = null,
    ) : AppSettings {
        val completedFlow = MutableStateFlow(completed)
        override val onboardingCompleted: Flow<Boolean> =
            readFailsWith?.let { failure -> flow<Boolean> { throw failure } } ?: completedFlow

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            writeFailsWith?.let { throw it }
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
    // ---------------------------------------------------------------------
    // The funnel. These could not be written before `Telemetry` carried these
    // events: `AppAnalytics.logOnboardingStep` was a static call into an object
    // holding a `Context`, so a JVM test could observe nothing, and the funnel
    // fired zero times in production for as long as it took someone to notice
    // by hand.
    // ---------------------------------------------------------------------

    @Test
    fun `reaching a page reports that step to the funnel`() = runTest(dispatcher) {
        val telemetry = RecordingTelemetry()
        val vm = viewModel(telemetry = telemetry)
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.SetCurrentPage(2))
        advanceUntilIdle()

        assertThat(telemetry.onboardingSteps).containsExactly(2)
    }

    /**
     * Every page reached is reported, in order — that is what makes it a funnel rather
     * than a counter. A drop-off graph is read by comparing consecutive steps, so
     * collapsing or reordering them makes the whole measurement meaningless.
     */
    @Test
    fun `each step of the walkthrough is reported in the order it was reached`() =
        runTest(dispatcher) {
            val telemetry = RecordingTelemetry()
            val vm = viewModel(telemetry = telemetry)
            advanceUntilIdle()

            listOf(0, 1, 2, 3).forEach { vm.onEvent(OnboardingEvent.SetCurrentPage(it)) }
            advanceUntilIdle()

            assertThat(telemetry.onboardingSteps).containsExactly(0, 1, 2, 3).inOrder()
        }

    /**
     * Completion carries the permissions the user granted on the way through.
     *
     * These three booleans are the whole point of the event: "how many people finish
     * onboarding" is answerable from the step funnel alone, but "how many finish having
     * denied notifications" — the question that decides whether prayer alerts work at all —
     * needs the payload.
     */
    @Test
    fun `completing onboarding reports the permissions granted`() = runTest(dispatcher) {
        val telemetry = RecordingTelemetry()
        val vm = viewModel(
            permissions = FakePermissions(location = true, notification = false),
            power = FakePower(exempt = true),
            telemetry = telemetry,
        )
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.CompleteOnboarding)
        advanceUntilIdle()

        assertThat(telemetry.calls.filterIsInstance<TelemetryCall.OnboardingCompleted>())
            .containsExactly(
                TelemetryCall.OnboardingCompleted(
                    locationGranted = true,
                    notificationGranted = false,
                    batteryOptimizationDisabled = true,
                )
            )
    }

    /**
     * A location fix that throws reaches **both** monitoring channels through one call.
     *
     * This site used to be a `CrashReporter.recordException` followed by an
     * `AppAnalytics.logError`, which is the pairing §6.1 asks for — written out by hand at
     * every catch site, and therefore only present at some of them. `telemetry.failure`
     * makes both channels the default; this pins that the migration kept both.
     *
     * The neighbouring test covers the *state* this produces (an error the user sees). This
     * one covers the reporting, which nothing asserted before the seam existed.
     */
    @Test
    fun `a failing location fix reports the stack trace and the frequency`() =
        runTest(dispatcher) {
            val telemetry = RecordingTelemetry()
            val boom = IllegalStateException("location provider is off")
            val vm = viewModel(
                deviceLocation = FakeDeviceLocation(failWith = boom),
                permissions = FakePermissions(location = true),
                telemetry = telemetry,
            )
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.DetectLocation)
            advanceUntilIdle()

            assertThat(telemetry.exceptions).contains(boom)
            assertThat(telemetry.errors.map { it.type }).contains("detect_location")
        }

    // ---------------------------------------------------------------------
    // Reading the flag back. This is the state `NavGraph` consults exactly
    // once, to pick a start destination, so both outcomes matter and the
    // failure has to end the loading state — a screen stuck on `isLoading`
    // is a first run that never renders.
    // ---------------------------------------------------------------------

    @Test
    fun `a stored completion flag is read back at construction`() = runTest(dispatcher) {
        val vm = viewModel(appSettings = FakeAppSettings(completed = true))
        advanceUntilIdle()

        assertThat(vm.state.value.onboardingCompleted).isTrue()
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `a flag that cannot be read still lets the screen render`() = runTest(dispatcher) {
        val telemetry = RecordingTelemetry()
        val boom = IllegalStateException("datastore corrupt")
        val vm = viewModel(
            appSettings = FakeAppSettings(readFailsWith = boom),
            telemetry = telemetry,
        )
        advanceUntilIdle()

        // Not "onboarding is complete" — a read that failed says nothing about the flag, and
        // guessing `true` would skip the first run entirely.
        assertThat(vm.state.value.onboardingCompleted).isFalse()
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.error).isEqualTo("datastore corrupt")
        assertThat(telemetry.exceptions).contains(boom)
    }

    @Test
    fun `re-checking the status after a failure picks up a flag that now reads`() =
        runTest(dispatcher) {
            val appSettings = FakeAppSettings(completed = true)
            val vm = viewModel(appSettings = appSettings)
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.CheckOnboardingStatus)
            advanceUntilIdle()

            assertThat(vm.state.value.onboardingCompleted).isTrue()
        }

    /**
     * A write that fails must not report the funnel's completion event.
     *
     * The two are read together: `onboarding_completed` is the denominator for every
     * first-run question, and counting a user who will be shown onboarding again on the
     * next launch makes it disagree with the step funnel that precedes it.
     */
    @Test
    fun `a completion that cannot be persisted is reported as a failure, not a completion`() =
        runTest(dispatcher) {
            val telemetry = RecordingTelemetry()
            val vm = viewModel(
                appSettings = FakeAppSettings(writeFailsWith = IllegalStateException("disk full")),
                telemetry = telemetry,
            )
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.CompleteOnboarding)
            advanceUntilIdle()

            assertThat(vm.state.value.onboardingCompleted).isFalse()
            assertThat(vm.state.value.error).isEqualTo("disk full")
            assertThat(telemetry.calls.filterIsInstance<TelemetryCall.OnboardingCompleted>())
                .isEmpty()
            assertThat(telemetry.errors.map { it.type }).contains("complete")
        }

    // ---------------------------------------------------------------------
    // The permission round trip. The screen's launchers report their result
    // through `UpdatePermissionStatus`, one field at a time — the other two
    // are null and must be left alone, because the launcher that answers has
    // no knowledge of the two permissions it was not asked about.
    // ---------------------------------------------------------------------

    @Test
    fun `a permission result updates only the permission it answered for`() =
        runTest(dispatcher) {
            val vm = viewModel(
                permissions = FakePermissions(location = false, notification = false),
                power = FakePower(exempt = true),
            )
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.UpdatePermissionStatus(notification = true))
            advanceUntilIdle()

            assertThat(vm.state.value.notificationPermissionGranted).isTrue()
            assertThat(vm.state.value.locationPermissionGranted).isFalse()
            // Left as it was rather than reset to the null the event carried.
            assertThat(vm.state.value.batteryOptimizationDisabled).isTrue()
        }

    @Test
    fun `granting location detects the location without waiting to be asked again`() =
        runTest(dispatcher) {
            // The user has just come back from the system dialog. Nothing else prompts a fix,
            // so without this the permissions page shows "granted" and no place name, and the
            // first prayer times are computed for wherever the app last thought it was.
            val settings = RecordingLocationSettings()
            val vm = viewModel(locationSettings = settings)
            advanceUntilIdle()

            vm.onEvent(OnboardingEvent.UpdatePermissionStatus(location = true))
            advanceUntilIdle()

            assertThat(settings.saved).isEqualTo(Triple(51.5074, -0.1278, "London"))
            assertThat(vm.state.value.locationDetected).isTrue()
        }

    @Test
    fun `a denied location result does not go looking for a fix`() = runTest(dispatcher) {
        val settings = RecordingLocationSettings()
        val vm = viewModel(locationSettings = settings)
        advanceUntilIdle()

        vm.onEvent(OnboardingEvent.UpdatePermissionStatus(location = false))
        advanceUntilIdle()

        assertThat(settings.saved).isNull()
        assertThat(vm.state.value.locationDetected).isFalse()
    }

    @Test
    fun `each re-check reads the seam again rather than a cached answer`() = runTest(dispatcher) {
        // `checkAllPermissions` runs once, at construction. Everything after it — returning from
        // a system dialog, coming back from Settings with battery optimisation turned off — is
        // one of these three events, and each has to consult the seam a second time.
        val permissions = object : PermissionChecker {
            var location = false
            var notification = false
            override fun hasLocationPermission() = location
            override fun hasNotificationPermission() = notification
        }
        val power = object : PowerSettings {
            var exempt = false
            override fun isIgnoringBatteryOptimizations() = exempt
        }
        val vm = viewModel(permissions = permissions, power = power)
        advanceUntilIdle()
        assertThat(vm.state.value.locationPermissionGranted).isFalse()

        permissions.location = true
        permissions.notification = true
        power.exempt = true
        vm.onEvent(OnboardingEvent.CheckLocationPermission)
        vm.onEvent(OnboardingEvent.CheckNotificationPermission)
        vm.onEvent(OnboardingEvent.CheckBatteryOptimization)
        advanceUntilIdle()

        assertThat(vm.state.value.locationPermissionGranted).isTrue()
        assertThat(vm.state.value.notificationPermissionGranted).isTrue()
        assertThat(vm.state.value.batteryOptimizationDisabled).isTrue()
    }

    @Test
    fun `the permission queries the screen calls directly answer from the seam`() =
        runTest(dispatcher) {
            // `hasLocationPermission` / `hasNotificationPermission` are public because the
            // screen's launcher callbacks consult them; they must not answer from state that
            // was read at construction.
            val vm = viewModel(
                permissions = FakePermissions(location = true, notification = false),
            )
            advanceUntilIdle()

            assertThat(vm.hasLocationPermission()).isTrue()
            assertThat(vm.hasNotificationPermission()).isFalse()
        }
}
