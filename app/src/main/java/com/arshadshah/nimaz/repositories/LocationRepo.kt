package com.arshadshah.nimaz.repositories

import android.content.Context
import android.location.Geocoder
import java.util.Locale

class LocationRepository(context: Context) {
    private val geocoder = Geocoder(context, Locale.getDefault())

    fun reverseGeocode(latitude: Double, longitude: Double): Result<Location> {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val locationName = address.locality ?: address.adminArea ?: address.countryName
                Result.success(Location(latitude, longitude, locationName))
            } else {
                Result.failure(Exception("No address found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun forwardGeocode(cityName: String): Result<Location> {
        return try {
            val addresses = geocoder.getFromLocationName(cityName, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val locationName = address.locality ?: address.adminArea ?: address.countryName
                Result.success(Location(address.latitude, address.longitude, locationName))
            } else {
                Result.failure(Exception("No location found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class Location(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)
