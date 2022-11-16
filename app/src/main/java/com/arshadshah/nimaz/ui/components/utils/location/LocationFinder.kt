package com.arshadshah.nimaz.ui.components.utils.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import java.util.*

/**
 * Finds the location from a string using Geocoder
 * @author Arshad Shah
 */
class LocationFinder {

    // coordinates for the calculation of prayer time
    var latitudeValue = 0.0
    var longitudeValue = 0.0

    // name of city
    var cityName: String = " "


    /**
     * Finds the longitude and latitude from city name
     * @author Arshad Shah
     * @param context context of the application
     * @param name Name of the city
     */
    fun findLongAndLan(context: Context, name: String) {
        // city name
        if (name == "No Network") {
            val isNetworkAvailable = NetworkChecker().networkCheck(context)
            if (isNetworkAvailable) {
                val sharedPreferences = PrivateSharedPreferences(context)
                val latitude = sharedPreferences.getDataDouble("latitude", 53.3498)
                val longitude = sharedPreferences.getDataDouble("longitude", -6.2603)
                findCityName(context, latitude, longitude)
            } else {
                val sharedPreferences = PrivateSharedPreferences(context)
                sharedPreferences.saveData("location_input", "No Network")
            }
        } else {
            val gcd = Geocoder(context, Locale.getDefault())

            val sharedPreferences = PrivateSharedPreferences(context)
            val isNetworkAvailable = NetworkChecker().networkCheck(context)
            if (isNetworkAvailable) {
                try {
                    val addresses: List<Address> = gcd.getFromLocationName(name, 1) as List<Address>
                    if (addresses.isNotEmpty()) {
                        cityName = addresses[0].locality
                        latitudeValue = addresses[0].latitude
                        longitudeValue = addresses[0].longitude
                        sharedPreferences.saveData("location_input", cityName)
                        sharedPreferences.saveDataDouble("latitude", latitudeValue)
                        sharedPreferences.saveDataDouble("longitude", longitudeValue)

                        Log.i("Location", "Location Found From value $cityName")
                    } else {
                        latitudeValue =
                            sharedPreferences.getDataDouble("latitude", 53.3498)
                        longitudeValue =
                            sharedPreferences.getDataDouble("longitude", -6.2603)
                        cityName =
                            sharedPreferences.getData("location_input", "Abbeyleix")
                        Log.i("Location", "Location Found From Storage $cityName")
                    }
                } catch (e: Exception) {
                    Log.e("Geocoder", "Geocoder has failed")
                    latitudeValue = sharedPreferences.getDataDouble("latitude", 53.3498)
                    longitudeValue = sharedPreferences.getDataDouble("longitude", -6.2603)
                    val cityNameFromStorage =
                        sharedPreferences.getData("location_input", "Abbeyleix")
                    cityName = cityNameFromStorage
                    Log.i("Location", "Location Found From Storage $cityName")
                }
            } else {
                latitudeValue = sharedPreferences.getDataDouble("latitude", 53.3498)
                longitudeValue = sharedPreferences.getDataDouble("longitude", -6.2603)
                cityName = sharedPreferences.getData("location_input", "Abbeyleix")
                Log.i("Location", "Location Found From Storage $cityName")
            }
        }
    }


    /**
     * Finds the longitude and latitude from city name
     * @author Arshad Shah
     * @param context context of the application
     */
    fun findCityName(context: Context, latitude: Double, longitude: Double) {
        // city name
        val gcd = Geocoder(context, Locale.getDefault())
        val sharedPreferences = PrivateSharedPreferences(context)
        val isNetworkAvailable = NetworkChecker().networkCheck(context)
        if (isNetworkAvailable) {
            try {
                val addresses: List<Address> =
                    gcd.getFromLocation(latitude, longitude, 1) as List<Address>
                if (addresses.isNotEmpty()) {
                    cityName = addresses[0].locality
                    sharedPreferences.saveData("location_input", cityName)

                    Log.i("Location", "Location Found From value $latitude, and $longitude")
                } else {
                    latitudeValue = sharedPreferences.getDataDouble("latitude", 53.3498)
                    longitudeValue = sharedPreferences.getDataDouble("longitude", 53.3498)
                    cityName = sharedPreferences.getData("location_input", "14.0")
                    Log.i("Location", "Location Found From Storage $cityName")
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Geocoder has failed")
                latitudeValue = sharedPreferences.getDataDouble("latitude", 53.3498)
                longitudeValue = sharedPreferences.getDataDouble("longitude", 53.3498)
                val cityNameFromStorage =
                    sharedPreferences.getData("location_input", "14.0")

                cityName = cityNameFromStorage
                Log.i("Location", "Location Found From value $latitude, and $longitude")
            }
        } else {
            latitudeValue = sharedPreferences.getDataDouble("latitude", 53.3498)
            longitudeValue = sharedPreferences.getDataDouble("longitude", 53.3498)
            cityName = sharedPreferences.getData("location_input", "14.0")
            Log.i("Location", "Location Found From Storage $cityName")
        }
    }
}
