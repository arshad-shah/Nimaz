package com.arshadshah.nimaz.presentation.viewmodel.onboarding

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PowerSettings
import com.arshadshah.nimaz.domain.repository.settings.AppSettings
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val deviceLocation: DeviceLocationRepository,
    private val permissions: PermissionChecker,
    private val powerSettings: PowerSettings,
    private val appSettings: AppSettings,
    private val locationSettings: LocationSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()


    init {
        checkOnboardingStatus()
        checkAllPermissions()
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.CheckOnboardingStatus -> checkOnboardingStatus()
            OnboardingEvent.CompleteOnboarding -> completeOnboarding()
            is OnboardingEvent.SetCurrentPage -> {
                _state.update { it.copy(currentPage = event.page) }
                AppAnalytics.logOnboardingStep(event.page)
            }

            OnboardingEvent.CheckLocationPermission -> checkLocationPermission()
            OnboardingEvent.CheckNotificationPermission -> checkNotificationPermission()
            OnboardingEvent.CheckBatteryOptimization -> checkBatteryOptimization()
            OnboardingEvent.DetectLocation -> detectLocation()
            OnboardingEvent.DismissError -> _state.update { it.copy(error = null) }
            is OnboardingEvent.UpdatePermissionStatus -> {
                _state.update { state ->
                    state.copy(
                        locationPermissionGranted = event.location
                            ?: state.locationPermissionGranted,
                        notificationPermissionGranted = event.notification
                            ?: state.notificationPermissionGranted,
                        batteryOptimizationDisabled = event.battery
                            ?: state.batteryOptimizationDisabled
                    )
                }
                // If location was just granted, try to detect location
                if (event.location == true) {
                    detectLocation()
                }
            }
        }
    }

    private fun checkOnboardingStatus() {
        launchSafely(telemetry, AppAnalytics.Feature.ONBOARDING, "check_onboarding_status") {
            try {
                val completed = appSettings.onboardingCompleted.first()
                _state.update {
                    it.copy(
                        onboardingCompleted = completed,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.ONBOARDING, "check_status", e.message)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun completeOnboarding() {
        launchSafely(telemetry, AppAnalytics.Feature.ONBOARDING, "complete_onboarding") {
            try {
                appSettings.setOnboardingCompleted(true)
                val current = _state.value
                AppAnalytics.logOnboardingCompleted(
                    locationGranted = current.locationPermissionGranted,
                    notificationGranted = current.notificationPermissionGranted,
                    batteryOptimizationDisabled = current.batteryOptimizationDisabled,
                )
                _state.update { it.copy(onboardingCompleted = true) }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                _state.update { it.copy(error = e.message) }
                // The `type` is the operation, as at every other logError site. Passing
                // `e.javaClass.simpleName` made this one dimension unbounded — a new
                // exception class is a new value — so onboarding failures never grouped
                // with anything and could not be counted. The class still reaches
                // Crashlytics above, where cardinality is the point.
                AppAnalytics.logError(AppAnalytics.Feature.ONBOARDING, "complete", e.message)
            }
        }
    }

    private fun checkAllPermissions() {
        checkLocationPermission()
        checkNotificationPermission()
        checkBatteryOptimization()
    }

    private fun checkLocationPermission() {
        _state.update { it.copy(locationPermissionGranted = permissions.hasLocationPermission()) }
    }

    private fun checkNotificationPermission() {
        _state.update {
            it.copy(notificationPermissionGranted = permissions.hasNotificationPermission())
        }
    }

    /**
     * Whether the app is exempt from battery optimisation.
     *
     * `as?` rather than `as`, matching `AppAnalytics` — which does the same lookup, defensively,
     * two hundred lines away. This runs from `init`, so on any device or emulator without the
     * service the hard cast threw during **ViewModel construction**: an onboarding crash on
     * first launch, with nothing else on screen to fall back to. Absent service reads as "not
     * exempt", which is the safe answer — it shows the user the prompt rather than silently
     * skipping it.
     */
    private fun checkBatteryOptimization() {
        _state.update {
            it.copy(batteryOptimizationDisabled = powerSettings.isIgnoringBatteryOptimizations())
        }
    }

    fun hasLocationPermission(): Boolean = permissions.hasLocationPermission()

    fun hasNotificationPermission(): Boolean = permissions.hasNotificationPermission()


    private fun detectLocation() {
        if (!hasLocationPermission()) {
            _state.update { it.copy(error = "Location permission not granted") }
            return
        }

        launchSafely(telemetry, AppAnalytics.Feature.ONBOARDING, "detect_location") {
            try {
                val location = getCurrentLocation()
                if (location != null) {
                    // No `withContext(Dispatchers.IO)` here any more: geocoding moved behind
                    // DeviceLocationRepository, whose implementation already does its own
                    // `withContext(ioDispatcher)`. Wrapping it again pinned the work to a real
                    // dispatcher that no test scheduler can advance — which is half of why this
                    // ViewModel had no tests.
                    val locationName = reverseGeocode(location.first, location.second)

                    // Save location to DataStore
                    locationSettings.updateLocation(
                        latitude = location.first,
                        longitude = location.second,
                        name = locationName
                    )

                    _state.update {
                        it.copy(
                            locationDetected = true,
                            locationName = locationName
                        )
                    }
                } else {
                    _state.update { it.copy(error = "Could not detect location") }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.ONBOARDING, "detect_location", e.message)
                _state.update { it.copy(error = "Failed to detect location: ${e.message}") }
            }
        }
    }

    private suspend fun getCurrentLocation(): Pair<Double, Double>? =
        deviceLocation.currentCoordinates()?.let { it.latitude to it.longitude }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String =
        deviceLocation.reverseGeocode(latitude, longitude) ?: "Unknown Location"
}
