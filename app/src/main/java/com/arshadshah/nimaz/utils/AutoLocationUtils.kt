package com.arshadshah.nimaz.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object AutoLocationUtils {
    private const val TAG = "Nimaz: AutoLocationUtils"
    private const val UPDATE_INTERVAL = 30 * 60 * 1000L // 30 minutes in milliseconds

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationDataCallback: ((Location) -> Unit)? = null

    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL * 2)
            .build()
    }

    fun init(context: Context) {
        Log.d(TAG, "Initializing location services")
        try {
            if (fusedLocationProviderClient == null) {
                fusedLocationProviderClient =
                    LocationServices.getFusedLocationProviderClient(context)
                initializeLocationCallback()
                Log.d(TAG, "Location client initialized successfully")
            } else {
                Log.d(TAG, "Location client already initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location client: ${e.message}", e)
            throw e
        }
    }

    private fun initializeLocationCallback() {
        Log.d(TAG, "Initializing location callback")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { location ->
                    handleNewLocation(location)
                }
            }
        }
    }

    private fun handleNewLocation(location: Location) {
        Log.d(
            TAG, "New location received: lat=${location.latitude}, lon=${location.longitude}, " +
                    "accuracy=${location.accuracy}m, provider=${location.provider}"
        )

        try {
            locationDataCallback?.invoke(location)
        } catch (e: Exception) {
            Log.e(TAG, "Error delivering location update to callback: ${e.message}", e)
        }
    }

    fun isInitialized(): Boolean {
        val isInit = fusedLocationProviderClient != null
        Log.v(TAG, "Checking initialization status: $isInit")
        return isInit
    }

    fun setLocationDataCallback(callback: (Location) -> Unit) {
        Log.d(TAG, "Setting new location data callback")
        locationDataCallback = callback
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        Log.d(TAG, "Starting location updates")
        try {
            requireLocationClient()

            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                locationCallback ?: throw IllegalStateException("LocationCallback not initialized"),
                Looper.getMainLooper()
            )?.addOnSuccessListener {
                Log.d(TAG, "Location updates started successfully")
                getLastKnownLocation() // Try to get immediate location
            }?.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start location updates: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}", e)
            throw e
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        Log.d(TAG, "Requesting last known location")
        try {
            requireLocationClient()

            fusedLocationProviderClient?.lastLocation
                ?.addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Last known location retrieved successfully")
                        handleNewLocation(location)
                    } else {
                        Log.w(TAG, "Last known location is null")
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get last known location: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location: ${e.message}", e)
            throw e
        }
    }

    fun stopLocationUpdates() {
        Log.d(TAG, "Stopping location updates")
        try {
            fusedLocationProviderClient?.removeLocationUpdates(
                locationCallback ?: return
            )?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location updates stopped successfully")
                } else {
                    Log.w(TAG, "Failed to stop location updates: ${task.exception?.message}")
                }
                cleanup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates: ${e.message}", e)
            cleanup()
            throw e
        }
    }

    private fun cleanup() {
        Log.d(TAG, "Cleaning up location resources")
        fusedLocationProviderClient = null
        locationCallback = null
        locationDataCallback = null
    }

    private fun requireLocationClient() {
        if (!isInitialized()) {
            val error = "Location client not initialized. Call init() first."
            Log.e(TAG, error)
            throw IllegalStateException(error)
        }
    }
}