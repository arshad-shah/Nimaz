package com.arshadshah.nimaz.services

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.utils.AutoLocationUtils
import com.arshadshah.nimaz.utils.NetworkChecker
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationService(
    private val context: Context,
    private val locationRepository: LocationRepository
) {

    private val sharedPreferences = PrivateSharedPreferences(context)

    fun loadLocation(checked: Boolean, onSuccess: (Location) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!NetworkChecker().networkCheck(context)) {
                    onError("No Network")
                    return@launch
                }

                if (checked) {
                    initializeLocationUpdates()
                    AutoLocationUtils.getLastKnownLocation()
                    AutoLocationUtils.setLocationDataCallback { location ->
                        val locationData =
                            locationRepository.reverseGeocode(location.latitude, location.longitude)
                        locationData.onSuccess { loc ->
                            updateSharedPrefsForLocation(loc)
                            onSuccess(loc)
                        }.onFailure {
                            onError(it.message ?: "Error occurred")
                        }
                    }
                } else {
                    AutoLocationUtils.stopLocationUpdates()
                    val locationNameFromStorage =
                        sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                    val locationData = locationRepository.forwardGeocode(locationNameFromStorage)
                    locationData.onSuccess { loc ->
                        updateSharedPrefsForLocation(loc)
                        onSuccess(loc)
                    }.onFailure {
                        onError(it.message ?: "Error occurred")
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error occurred")
            }
        }
    }

    private fun initializeLocationUpdates() {
        if (!AutoLocationUtils.isInitialized()) {
            AutoLocationUtils.init(context)
            AutoLocationUtils.startLocationUpdates()
        }
    }

    private fun updateSharedPrefsForLocation(location: Location) {
        sharedPreferences.saveDataDouble(AppConstants.LATITUDE, location.latitude)
        sharedPreferences.saveDataDouble(AppConstants.LONGITUDE, location.longitude)
        sharedPreferences.saveData(AppConstants.LOCATION_INPUT, location.locationName)
    }
}