package com.arshadshah.nimaz.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.AutoLocationRepository
import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.repositories.ManualLocationRepository
import com.arshadshah.nimaz.utils.NetworkChecker
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.ViewModelLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationService @Inject constructor(
    private val context: Context,
    private val manualLocationRepository: ManualLocationRepository,
    private val autoLocationRepository: AutoLocationRepository,
    private val sharedPreferences: PrivateSharedPreferences,
    private val locationStateManager: LocationStateManager
) {
    companion object {
        private const val TAG = "Nimaz: LocationService"
        private const val LOCATION_TIMEOUT = 15_000L
    }

    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    suspend fun loadLocation(isAutoLocation: Boolean): Result<Location> =
        withContext(Dispatchers.IO) {
            ViewModelLogger.d(TAG, "📍 Loading location (Auto: $isAutoLocation)")
            ViewModelLogger.d(TAG, "Are permissions granted? : ${hasLocationPermission()}")

            try {
                // First check if auto location is requested but we don't have permission
                if (isAutoLocation && !hasLocationPermission()) {
                    ViewModelLogger.e(TAG, "❌ Location permission not granted")
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error("Location permission not granted")
                    )
                    return@withContext Result.failure(Exception("Location permission not granted"))
                }
                ViewModelLogger.d(
                    TAG,
                    "📍 Loading location (Auto: $isAutoLocation) permission (Granted: ${hasLocationPermission()})"
                )

                // Check if there's already a location request in progress
                val existingRequest =
                    locationStateManager.requestLocation("location-${System.currentTimeMillis()}")
                if (existingRequest != null && locationStateManager.locationState.value is LocationStateManager.LocationState.Loading) {
                    ViewModelLogger.d(TAG, "⏳ Waiting for existing location request to complete")
                    return@withContext Result.success(waitForLocation())
                }

                ViewModelLogger.d(TAG, "🚀 Starting location service")

                if (!NetworkChecker().networkCheck(context)) {
                    ViewModelLogger.e(TAG, "❌ Network check failed")
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error(
                            "No Network"
                        )
                    )
                    return@withContext Result.failure(Exception("No Network"))
                }

                ViewModelLogger.d(TAG, "📍 Network check passed")

                locationStateManager.updateLocationState(LocationStateManager.LocationState.Loading)

                val result = if (isAutoLocation) {
                    ViewModelLogger.d(TAG, "🔄 Using automatic location")
                    getAutoLocation()
                } else {
                    ViewModelLogger.d(TAG, "📝 Using manual location")
                    getManualLocation()
                }

                result.onSuccess { location ->
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Success(
                            location
                        )
                    )
                }.onFailure { error ->
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error(error.message ?: "Unknown error")
                    )
                }

                result

            } catch (e: Exception) {
                ViewModelLogger.e(TAG, "❌ Error in location service", e)
                locationStateManager.updateLocationState(
                    LocationStateManager.LocationState.Error(e.message ?: "Unknown error")
                )
                Result.failure(e)
            }
        }

    private suspend fun waitForLocation(): Location =
        locationStateManager.locationState.first { state ->
            state !is LocationStateManager.LocationState.Loading
        }.let { finalState ->
            when (finalState) {
                is LocationStateManager.LocationState.Success -> finalState.location
                is LocationStateManager.LocationState.Error -> throw Exception(finalState.message)
                is LocationStateManager.LocationState.Idle -> throw CancellationException("Location request cancelled")
                else -> throw IllegalStateException("Unexpected state: $finalState")
            }
        }

    private var hasResumedLocation = false

    private suspend fun getAutoLocation(): Result<Location> = withContext(Dispatchers.IO) {
        ViewModelLogger.d(TAG, "🚀 Starting automatic location retrieval")
        try {
            hasResumedLocation = false // Reset flag
            ViewModelLogger.d(
                TAG,
                "⏳ Waiting for device location update (timeout: ${LOCATION_TIMEOUT}ms)"
            )

            val deviceLocation = withTimeout(LOCATION_TIMEOUT) {
                suspendCancellableCoroutine { continuation ->
                    autoLocationRepository.apply {
                        setLocationDataCallback { location ->
                            if (!hasResumedLocation) {
                                hasResumedLocation = true
                                ViewModelLogger.d(
                                    TAG,
                                    "📍 Location received: lat=${location.latitude}, lon=${location.longitude}"
                                )
                                continuation.resume(location)
                                stopLocationUpdates()
                            } else {
                                ViewModelLogger.d(TAG, "📍 Ignoring duplicate location update")
                            }
                        }

                        // First try to get last known location
                        getLastKnownLocation()
                        // Then start updates if we didn't get a location
                        if (!hasResumedLocation) {
                            startLocationUpdates()
                        }
                    }

                    continuation.invokeOnCancellation {
                        ViewModelLogger.d(TAG, "🛑 Location update cancelled, stopping updates")
                        autoLocationRepository.stopLocationUpdates()
                    }
                }
            }

            ViewModelLogger.d(
                TAG,
                "🔄 Reverse geocoding coordinates: (${deviceLocation.latitude}, ${deviceLocation.longitude})"
            )
            manualLocationRepository.reverseGeocode(
                deviceLocation.latitude,
                deviceLocation.longitude
            )
                .onSuccess { location ->
                    ViewModelLogger.d(TAG, "✅ Location resolved: ${location.locationName}")
                    updateSharedPrefsForLocation(location)
                }
                .onFailure { error ->
                    ViewModelLogger.e(TAG, "❌ Reverse geocoding failed", error)
                }

        } catch (e: TimeoutCancellationException) {
            ViewModelLogger.e(TAG, "⏰ Location retrieval timed out after ${LOCATION_TIMEOUT}ms")
            autoLocationRepository.stopLocationUpdates()
            Result.failure(e)
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error in automatic location retrieval", e)
            Result.failure(e)
        } finally {
            hasResumedLocation = false
        }
    }

    private suspend fun getManualLocation(): Result<Location> = withContext(Dispatchers.IO) {
        ViewModelLogger.d(TAG, "📝 Starting manual location retrieval")
        try {
            autoLocationRepository.stopLocationUpdates()

            val locationNameFromStorage = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
            ViewModelLogger.d(TAG, "📍 Retrieved location name: $locationNameFromStorage")

            manualLocationRepository.forwardGeocode(locationNameFromStorage)
                .onSuccess { location ->
                    ViewModelLogger.d(
                        TAG,
                        "✅ Location resolved: ${location.locationName} (${location.latitude}, ${location.longitude})"
                    )
                    updateSharedPrefsForLocation(location)
                }
                .onFailure { error ->
                    ViewModelLogger.e(
                        TAG,
                        "❌ Forward geocoding failed for '$locationNameFromStorage'",
                        error
                    )
                }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error in manual location retrieval", e)
            Result.failure(e)
        }
    }

    private fun updateSharedPrefsForLocation(location: Location) {
        ViewModelLogger.d(TAG, "💾 Updating preferences: ${location.locationName}")
        try {
            with(sharedPreferences) {
                saveDataDouble(AppConstants.LATITUDE, location.latitude)
                saveDataDouble(AppConstants.LONGITUDE, location.longitude)
                saveData(AppConstants.LOCATION_INPUT, location.locationName)
            }
            ViewModelLogger.d(TAG, "✅ Preferences updated successfully")
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error updating preferences", e)
            throw e
        }
    }
}