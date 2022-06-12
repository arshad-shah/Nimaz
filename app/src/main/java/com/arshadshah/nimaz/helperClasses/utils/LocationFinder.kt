package com.arshadshah.nimaz.helperClasses.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.math.ceil

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
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val latitudeInput = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                val longitudeInput = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                findCityName(context, latitudeInput, longitudeInput)
            } else {

                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                with(sharedPreferences.edit()) {
                    putString("location_input", "No Network")
                    apply()
                }

            }
        } else {

            val gcd = Geocoder(context, Locale.getDefault())
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val isNetworkAvailable = NetworkChecker().networkCheck(context)
            if (isNetworkAvailable) {
                try {
                    val addresses: List<Address> = gcd.getFromLocationName(name, 1)
                    if (addresses.isNotEmpty()) {
                        cityName = addresses[0].locality
                        latitudeValue = addresses[0].latitude
                        longitudeValue = addresses[0].longitude
                        with(sharedPreferences.edit()) {
                            putString("latitude", ceil(latitudeValue).toString())
                            putString("longitude", ceil(longitudeValue).toString())
                            putString("location_input", cityName)
                            apply()
                        }

                        Log.i("Location", "Location Found From value $cityName")
                    } else {
                        latitudeValue =
                            sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                        longitudeValue =
                            sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                        cityName =
                            sharedPreferences.getString("location_input", "Portlaoise").toString()
                        Log.i("Location", "Location Found From Storage $cityName")
                    }
                } catch (e: Exception) {
                    Log.e("Geocoder", "Geocoder has failed")
                    latitudeValue = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                    longitudeValue = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                    val cityNameFromStorage =
                        sharedPreferences.getString("location_input", "Portlaoise").toString()

                    cityName =
                        sharedPreferences.getString("location_input", "Portlaoise").toString()
                    Log.i("Location", "Location Found From Storage $cityName")
                }
            } else {
                latitudeValue = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                longitudeValue = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                cityName = sharedPreferences.getString("location_input", "Portlaoise").toString()
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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val isNetworkAvailable = NetworkChecker().networkCheck(context)
        if (isNetworkAvailable) {
            try {
                val addresses: List<Address> = gcd.getFromLocation(latitude, longitude, 1)
                if (addresses.isNotEmpty()) {
                    cityName = addresses[0].locality
                    with(sharedPreferences.edit()) {
                        putString("location_input", cityName)
                        apply()
                    }

                    Log.i("Location", "Location Found From value $latitude, and $longitude")
                } else {
                    latitudeValue = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                    longitudeValue = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                    cityName =
                        sharedPreferences.getString("location_input", "Portlaoise").toString()
                    Log.i("Location", "Location Found From Storage $cityName")
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Geocoder has failed")
                latitudeValue = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
                longitudeValue = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
                val cityNameFromStorage =
                    sharedPreferences.getString("location_input", "Portlaoise").toString()

                cityName = sharedPreferences.getString("location_input", "Portlaoise").toString()
                Log.i("Location", "Location Found From value $latitude, and $longitude")
            }
        } else {
            latitudeValue = sharedPreferences.getString("latitude", "0.0")!!.toDouble()
            longitudeValue = sharedPreferences.getString("longitude", "0.0")!!.toDouble()
            cityName = sharedPreferences.getString("location_input", "Portlaoise").toString()
            Log.i("Location", "Location Found From Storage $cityName")
        }
    }
}
