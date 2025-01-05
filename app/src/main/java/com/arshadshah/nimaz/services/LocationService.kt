package com.arshadshah.nimaz.services

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.utils.AutoLocationUtils
import com.arshadshah.nimaz.utils.NetworkChecker
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationService @Inject constructor(
    private val context: Context,
    private val locationRepository: LocationRepository,
    private val sharedPreferences: PrivateSharedPreferences
) {
    companion object {
        private const val TAG = "Nimaz: LocationService"
    }

    init {
        Log.d(TAG, "LocationService initialized")
    }

    suspend fun loadLocation(isAutoLocation: Boolean): Result<Location> {
        Log.d(TAG, "Loading location (Auto: $isAutoLocation)")
        return try {
            // Check network first
            if (!NetworkChecker().networkCheck(context)) {
                Log.e(TAG, "Network check failed - no connection available")
                return Result.failure(Exception("No Network"))
            }
            Log.d(TAG, "Network check passed")

            withContext(Dispatchers.Main) {
                if (isAutoLocation) {
                    Log.d(TAG, "Using automatic location")
                    getAutoLocation()
                } else {
                    Log.d(TAG, "Using manual location")
                    getManualLocation()
                }
            }
        } catch (e: CancellationException) {
            Log.w(TAG, "Location loading cancelled")
            throw e // Let cancellation exceptions propagate
        } catch (e: Exception) {
            Log.e(TAG, "Error loading location: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getAutoLocation(): Result<Location> {
        Log.d(TAG, "Starting automatic location retrieval")
        return try {
            initializeLocationUpdates()

            // Get location using suspendCancellableCoroutine
            Log.d(TAG, "Waiting for device location update")
            val deviceLocation = suspendCancellableCoroutine { continuation ->
                AutoLocationUtils.setLocationDataCallback { location ->
                    Log.d(
                        TAG,
                        "Received device location: lat=${location.latitude}, lon=${location.longitude}"
                    )
                    continuation.resume(location)
                }

                // Clean up if coroutine is cancelled
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Location update coroutine cancelled, stopping updates")
                    AutoLocationUtils.stopLocationUpdates()
                }
            }

            // Reverse geocode the location
            Log.d(
                TAG,
                "Reverse geocoding coordinates: (${deviceLocation.latitude}, ${deviceLocation.longitude})"
            )
            locationRepository.reverseGeocode(deviceLocation.latitude, deviceLocation.longitude)
                .onSuccess { location ->
                    Log.d(TAG, "Reverse geocoding successful: ${location.locationName}")
                    updateSharedPrefsForLocation(location)
                }
                .onFailure { error ->
                    Log.e(TAG, "Reverse geocoding failed: ${error.message}", error)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in automatic location retrieval: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun getManualLocation(): Result<Location> {
        return try {
            Log.d(TAG, "Stopping any active location updates")
            AutoLocationUtils.stopLocationUpdates()

            val locationNameFromStorage = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
            Log.d(TAG, "Retrieved location name from storage: $locationNameFromStorage")

            locationRepository.forwardGeocode(locationNameFromStorage)
                .onSuccess { location ->
                    Log.d(
                        TAG,
                        "Forward geocoding successful for '$locationNameFromStorage': ${location.locationName} (${location.latitude}, ${location.longitude})"
                    )
                    updateSharedPrefsForLocation(location)
                }
                .onFailure { error ->
                    Log.e(
                        TAG,
                        "Forward geocoding failed for '$locationNameFromStorage': ${error.message}",
                        error
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in manual location retrieval: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun initializeLocationUpdates() {
        if (!AutoLocationUtils.isInitialized()) {
            Log.d(TAG, "Initializing location updates")
            AutoLocationUtils.init(context)
            AutoLocationUtils.startLocationUpdates()
            Log.d(TAG, "Location updates initialized and started")
        } else {
            Log.d(TAG, "Location updates already initialized")
        }
    }

    private fun updateSharedPrefsForLocation(location: Location) {
        Log.d(
            TAG,
            "Updating shared preferences with location: ${location.locationName} (${location.latitude}, ${location.longitude})"
        )
        try {
            with(sharedPreferences) {
                saveDataDouble(AppConstants.LATITUDE, location.latitude)
                saveDataDouble(AppConstants.LONGITUDE, location.longitude)
                saveData(AppConstants.LOCATION_INPUT, location.locationName)
            }
            Log.d(TAG, "Shared preferences updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shared preferences: ${e.message}", e)
            throw e
        }
    }
}