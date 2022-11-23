package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import io.ktor.client.features.*
import java.io.IOException
import java.time.LocalDateTime

object PrayerTimesRepository {
    suspend fun getPrayerTimes(context: Context): ApiResponse<PrayerTimes> {

        val sharedPreferences = PrivateSharedPreferences(context)
        val latitude = sharedPreferences.getDataDouble("latitude", 53.3498)
        val longitude = sharedPreferences.getDataDouble("longitude", -6.2603)
        val fajrAngle: String = sharedPreferences.getData("fajr_angle", "14.0")
        val ishaAngle: String = sharedPreferences.getData("isha_angle", "14.0")
        val calculationMethod: String =
            sharedPreferences.getData("calculation_method", "IRELAND")
        val madhab: String = sharedPreferences.getData("madhab", "HANAFI")
        val highLatitudeRule: String =
            sharedPreferences.getData("high_latitude_rule", "TWILIGHT_ANGLE")
        val fajrAdjustment: String = sharedPreferences.getData("fajr_adjustment", "0")
        val dhuhrAdjustment: String = sharedPreferences.getData("dhuhr_adjustment", "0")
        val asrAdjustment: String = sharedPreferences.getData("asr_adjustment", "0")
        val maghribAdjustment: String = sharedPreferences.getData("maghrib_adjustment", "0")
        val ishaAdjustment: String = sharedPreferences.getData("isha_adjustment", "0")

        val mapOfParams = mutableMapOf<String, String>()
        mapOfParams["latitude"] = latitude.toString()
        mapOfParams["longitude"] = longitude.toString()
        mapOfParams["date"] = LocalDateTime.now().toString()
        mapOfParams["fajrAngle"] = fajrAngle
        mapOfParams["ishaAngle"] = ishaAngle
        mapOfParams["method"] = calculationMethod
        mapOfParams["madhab"] = madhab
        mapOfParams["highLatitudeRule"] = highLatitudeRule
        mapOfParams["fajrAdjustment"] = fajrAdjustment
        mapOfParams["dhuhrAdjustment"] = dhuhrAdjustment
        mapOfParams["asrAdjustment"] = asrAdjustment
        mapOfParams["maghribAdjustment"] = maghribAdjustment
        mapOfParams["ishaAdjustment"] = ishaAdjustment

        return try {
            val response = NimazServicesImpl.getPrayerTimes(mapOfParams)

            Log.d("PrayerTimesRepository", "getPrayerTimes: $response")
            val prayerTimes = PrayerTimes(
                LocalDateTime.parse(response.fajr),
                LocalDateTime.parse(response.sunrise),
                LocalDateTime.parse(response.dhuhr),
                LocalDateTime.parse(response.asr),
                LocalDateTime.parse(response.maghrib),
                LocalDateTime.parse(response.isha),
            )

            ApiResponse.Success(prayerTimes)
        } catch (e: ClientRequestException) {
            ApiResponse.Error(e.message, null)

        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }
}