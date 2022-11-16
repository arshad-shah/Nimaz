package com.arshadshah.nimaz.ui.components.utils

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.time.LocalDateTime

class PrayerTimesAPI(context: Context) {
    val sharedPreferences = PrivateSharedPreferences(context)

    private val latitude: Double = sharedPreferences.getDataDouble("latitude", 53.3498)
    private val longitude: Double = sharedPreferences.getDataDouble("longitude", -6.2603)
    private val date: String = LocalDateTime.now().toString()
    private val fajrAngle: String = sharedPreferences.getData("fajr_angle", "14.0")
    private val ishaAngle: String = sharedPreferences.getData("isha_angle", "14.0")
    private val calcMethod: String =
        sharedPreferences.getData("calculation_method", "IRELAND")
    private val madhab: String = sharedPreferences.getData("madhab", "HANAFI")
    private val highLatitudeRule: String =
        sharedPreferences.getData("high_latitude_rule", "TWILIGHT_ANGLE")
    private val fajrAdjustment: String = sharedPreferences.getData("fajr_adjustment", "0")
    private val dhuhrAdjustment: String = sharedPreferences.getData("dhuhr_adjustment", "0")
    private val asrAdjustment: String = sharedPreferences.getData("asr_adjustment", "0")
    private val maghribAdjustment: String = sharedPreferences.getData("maghrib_adjustment", "0")
    private val ishaAdjustment: String = sharedPreferences.getData("isha_adjustment", "0")
    private val ipAddress: String = "178.62.35.217"
    private val port: String = "8080"
    private val protocol: String = "http"


    /**
     * Generates the URL for the prayer times API
     * */
    private fun generatePrayerTimeURL(custom: Boolean): String {
        return if (!custom) {
            "$protocol://$ipAddress:$port/prayertimes/byMethod"
        } else {
            "$protocol://$ipAddress:$port/prayertimes/getprayertimes"
        }
    }

    /**
     * request prayer times from the API
     * */
    fun requestPrayerTimes(context: Context) {
        val requestQueue = Volley.newRequestQueue(context)
        //call the server to get the prayer time
        val requestMethod = Request.Method.POST
        val url = generatePrayerTimeURL(true)
        //content type for the request body is application form url encoded
        val contentType = "application/json"
        //the request

        val stringRequest = object : StringRequest(requestMethod, url,
            { response ->
                sharedPreferences.saveData("prayer_times", response)
                Log.d("PrayerTimesAPI", response)
            },
            { error ->
                Log.d("PrayerTimesAPI", error.toString())
            }) {
            override fun getBody(): ByteArray {
                val body = """
                    {
                        "latitude": $latitude,
                        "longitude": $longitude,
                        "date": "$date",
                        "fajrAngle": $fajrAngle,
                        "ishaAngle": $ishaAngle,
                        "method": "$calcMethod",
                        "madhab": "$madhab",
                        "highLatitudeRule": "$highLatitudeRule",
                        "fajrAdjustment": $fajrAdjustment,
                        "dhuhrAdjustment": $dhuhrAdjustment,
                        "asrAdjustment": $asrAdjustment,
                        "maghribAdjustment": $maghribAdjustment,
                        "ishaAdjustment": $ishaAdjustment
                    }
                """.trimIndent()

                return body.toByteArray()
            }

            override fun getBodyContentType(): String {
                return contentType
            }
        }
        requestQueue.add(stringRequest)
    }

    //request prayer times from API by method
    fun requestPrayerTimesByMethod(context: Context): String {
        var responseFromAPI = ""
        val requestQueue = Volley.newRequestQueue(context)
        //call the server to get the prayer time
        val requestMethod = Request.Method.GET
        val url =
            "$protocol://$ipAddress:$port/prayerTimes/byMethod?latitude=$latitude&longitude=$longitude&date=$date&method=$calcMethod"
        val request = StringRequest(requestMethod, url, { response ->
            responseFromAPI = response
            Log.d("Response is: ", response)
        }, { error ->
            Log.d("That didn't work!", error.toString())
        })
        requestQueue.add(request)
        return responseFromAPI
    }
}