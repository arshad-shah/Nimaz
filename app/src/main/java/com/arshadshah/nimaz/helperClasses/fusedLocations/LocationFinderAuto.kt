package com.arshadshah.nimaz.helperClasses.fusedLocations

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import kotlin.math.ceil

class LocationFinderAuto {

    var fusedLocationProviderClient: FusedLocationProviderClient? = null
    var locationCallback: LocationCallback? = null

    /**
     * make the location callback object to be given to fusedLocationProviderClient
     */
    private fun getLocationCallback(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                for (location in p0.locations) {
                    with(sharedPreferences.edit()) {
                        putString("latitude", ceil(location.latitude).toString())
                        putString("longitude", ceil(location.longitude).toString())
                        apply()
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun setUpLocationListener(context: Context) {
        getLocationCallback(context)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        // for getting the current location update after every 2 seconds with high accuracy
        val locationRequest = LocationRequest().apply {
            interval = 7200000
            fastestInterval = 3600000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient!!.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun getLocations(context: Context, requestCode: Int) {
        when {
            PermissionUtils.checkAccessLocationGranted(context) -> {
                when {
                    PermissionUtils.isLocationEnabled(context) -> {
                        setUpLocationListener(context)
                    }
                    else -> {
                        PermissionUtils.showGPSNotEnabledDialog(context)
                    }
                }
            }
            else -> {
                PermissionUtils.askAccessLocationPermission(
                    context as AppCompatActivity,
                    requestCode
                )
            }
        }
    }
}