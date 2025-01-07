package com.arshadshah.nimaz.repositories

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.arshadshah.nimaz.viewModel.ViewModelLogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "Nimaz: AutoLocationRepo"
        private const val UPDATE_INTERVAL = 30 * 60 * 1000L // 30 minutes
    }

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var locationCallback: LocationCallback? = null
    private var locationDataCallback: ((Location) -> Unit)? = null

    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL * 2)
            .build()

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

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        ViewModelLogger.d(TAG, "🚀 Starting location updates")
        try {
            if (!hasLocationPermission()) {
                ViewModelLogger.e(TAG, "❌ Location permission not granted")
                throw SecurityException("Location permission not granted")
            }

            initializeLocationCallback()

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback ?: throw IllegalStateException("LocationCallback not initialized"),
                Looper.getMainLooper()
            ).addOnSuccessListener {
                ViewModelLogger.d(TAG, "✅ Location updates started successfully")
            }.addOnFailureListener { e ->
                ViewModelLogger.e(TAG, "❌ Failed to start location updates: ${e.message}", e)
            }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error starting location updates", e)
            throw e
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        ViewModelLogger.d(TAG, "🔍 Requesting last known location")
        try {
            if (!hasLocationPermission()) {
                ViewModelLogger.e(TAG, "❌ Location permission not granted")
                throw SecurityException("Location permission not granted")
            }

            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        ViewModelLogger.d(TAG, "✅ Last known location retrieved")
                        handleNewLocation(location)
                    } else {
                        ViewModelLogger.w(TAG, "⚠️ Last known location is null")
                    }
                }
                .addOnFailureListener { e ->
                    ViewModelLogger.e(TAG, "❌ Failed to get last known location: ${e.message}", e)
                }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error getting last known location", e)
            throw e
        }
    }

    private fun handleNewLocation(location: Location) {
        ViewModelLogger.d(
            TAG,
            "📍 New location received: lat=${location.latitude}, lon=${location.longitude}"
        )
        try {
            locationDataCallback?.let { callback ->
                callback(location)
            }
        } catch (e: Exception) {
            // Only log the error, don't throw it since it might be a duplicate resume
            ViewModelLogger.d(TAG, "⚠️ Callback error (possibly duplicate location): ${e.message}")
        }
    }

    private fun initializeLocationCallback() {
        ViewModelLogger.d(TAG, "🎯 Initializing location callback")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.firstOrNull()?.let { location ->
                    handleNewLocation(location)
                }
            }
        }
    }

    fun setLocationDataCallback(callback: (Location) -> Unit) {
        ViewModelLogger.d(TAG, "✏️ Setting new location data callback")
        locationDataCallback = callback
    }

    fun stopLocationUpdates() {
        ViewModelLogger.d(TAG, "🛑 Stopping location updates")
        try {
            locationCallback?.let { callback ->
                fusedLocationProviderClient.removeLocationUpdates(callback)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            ViewModelLogger.d(TAG, "✅ Location updates stopped successfully")
                        } else {
                            ViewModelLogger.w(
                                TAG,
                                "⚠️ Failed to stop location updates: ${task.exception?.message}"
                            )
                        }
                        cleanup()
                    }
            }
        } catch (e: Exception) {
            ViewModelLogger.e(TAG, "❌ Error stopping location updates", e)
            cleanup()
            throw e
        }
    }

    private fun cleanup() {
        ViewModelLogger.d(TAG, "🧹 Cleaning up location resources")
        locationCallback = null
        locationDataCallback = null
    }
}