package com.arshadshah.nimaz.presentation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val currentPage: Int = 0,
    val locationPermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val batteryOptimizationDisabled: Boolean = false,
    val locationDetected: Boolean = false,
    val locationName: String = "",
    val error: String? = null
)

sealed interface OnboardingEvent {
    data object CheckOnboardingStatus : OnboardingEvent
    data object CompleteOnboarding : OnboardingEvent
    data class SetCurrentPage(val page: Int) : OnboardingEvent
    data object CheckLocationPermission : OnboardingEvent
    data object CheckNotificationPermission : OnboardingEvent
    data object CheckBatteryOptimization : OnboardingEvent
    data object DetectLocation : OnboardingEvent
    data object DismissError : OnboardingEvent
    data class UpdatePermissionStatus(
        val location: Boolean? = null,
        val notification: Boolean? = null,
        val battery: Boolean? = null
    ) : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    init {
        checkOnboardingStatus()
        checkAllPermissions()
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.CheckOnboardingStatus -> checkOnboardingStatus()
            OnboardingEvent.CompleteOnboarding -> completeOnboarding()
            is OnboardingEvent.SetCurrentPage -> _state.update { it.copy(currentPage = event.page) }
            OnboardingEvent.CheckLocationPermission -> checkLocationPermission()
            OnboardingEvent.CheckNotificationPermission -> checkNotificationPermission()
            OnboardingEvent.CheckBatteryOptimization -> checkBatteryOptimization()
            OnboardingEvent.DetectLocation -> detectLocation()
            OnboardingEvent.DismissError -> _state.update { it.copy(error = null) }
            is OnboardingEvent.UpdatePermissionStatus -> {
                _state.update { state ->
                    state.copy(
                        locationPermissionGranted = event.location ?: state.locationPermissionGranted,
                        notificationPermissionGranted = event.notification ?: state.notificationPermissionGranted,
                        batteryOptimizationDisabled = event.battery ?: state.batteryOptimizationDisabled
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
        viewModelScope.launch {
            try {
                val completed = preferencesDataStore.onboardingCompleted.first()
                _state.update {
                    it.copy(
                        onboardingCompleted = completed,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
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
        viewModelScope.launch {
            try {
                preferencesDataStore.setOnboardingCompleted(true)
                _state.update { it.copy(onboardingCompleted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun checkAllPermissions() {
        checkLocationPermission()
        checkNotificationPermission()
        checkBatteryOptimization()
    }

    private fun checkLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _state.update { it.copy(locationPermissionGranted = hasPermission) }
    }

    private fun checkNotificationPermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required before Android 13
        }

        _state.update { it.copy(notificationPermissionGranted = hasPermission) }
    }

    private fun checkBatteryOptimization() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        _state.update { it.copy(batteryOptimizationDisabled = isIgnoringBatteryOptimizations) }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectLocation() {
        if (!hasLocationPermission()) {
            _state.update { it.copy(error = "Location permission not granted") }
            return
        }

        viewModelScope.launch {
            try {
                val location = getCurrentLocation()
                if (location != null) {
                    val locationName = withContext(Dispatchers.IO) {
                        reverseGeocode(location.first, location.second)
                    }

                    // Save location to DataStore
                    preferencesDataStore.updateLocation(
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
                _state.update { it.copy(error = "Failed to detect location: ${e.message}") }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            }

            val address = addresses.firstOrNull()
            if (address != null) {
                buildString {
                    address.locality?.let { append(it) }
                    if (isEmpty() && address.subAdminArea != null) {
                        append(address.subAdminArea)
                    }
                    if (isEmpty() && address.adminArea != null) {
                        append(address.adminArea)
                    }
                    address.countryName?.let { country ->
                        if (isNotEmpty()) append(", ")
                        append(country)
                    }
                }.ifEmpty { "Unknown Location" }
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            "Unknown Location"
        }
    }
}
