package com.arshadshah.nimaz.utils.api

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.type.HighLatitudeRule
import com.arshadshah.nimaz.type.Madhab
import com.arshadshah.nimaz.type.Method
import com.arshadshah.nimaz.type.Parameters
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.time.LocalDateTime

object PrayerTimesParamMapper {

    fun getParams(context: Context): Parameters {
        val sharedPreferences = PrivateSharedPreferences(context)
        val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0)
        val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)
        val fajrAngle: String = sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18")
        val ishaAngle: String = sharedPreferences.getData(AppConstants.ISHA_ANGLE, "17")
        val ishaInterval: String = sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0")
        val calculationMethod: String =
            sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND")
        val madhab: String = sharedPreferences.getData(AppConstants.MADHAB, "SHAFI")
        val highLatitudeRule: String =
            sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE, "TWILIGHT_ANGLE")
        val fajrAdjustment: String = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0")
        val sunriseAdjustment: String =
            sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0")
        val dhuhrAdjustment: String =
            sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0")
        val asrAdjustment: String = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0")
        val maghribAdjustment: String =
            sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0")
        val ishaAdjustment: String = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0")

        //log the adjustments
        Log.d("Nimaz: PrayerTimesParamMapper", "fajrAdjustment: $fajrAdjustment")
        Log.d("Nimaz: PrayerTimesParamMapper", "sunriseAdjustment: $sunriseAdjustment")
        Log.d("Nimaz: PrayerTimesParamMapper", "dhuhrAdjustment: $dhuhrAdjustment")
        Log.d("Nimaz: PrayerTimesParamMapper", "asrAdjustment: $asrAdjustment")
        Log.d("Nimaz: PrayerTimesParamMapper", "maghribAdjustment: $maghribAdjustment")
        Log.d("Nimaz: PrayerTimesParamMapper", "ishaAdjustment: $ishaAdjustment")


        return Parameters(
            latitude,
            longitude,
            date = LocalDateTime.now().toString(),
            fajrAngle = fajrAngle.toDouble(),
            ishaAngle = ishaAngle.toDouble(),
            method = Method.valueOf(calculationMethod),
            madhab = Madhab.valueOf(madhab),
            highLatitudeRule = HighLatitudeRule.valueOf(highLatitudeRule),
            fajrAdjustment = fajrAdjustment.toInt(),
            sunriseAdjustment = sunriseAdjustment.toInt(),
            dhuhrAdjustment = dhuhrAdjustment.toInt(),
            asrAdjustment = asrAdjustment.toInt(),
            maghribAdjustment = maghribAdjustment.toInt(),
            ishaAdjustment = ishaAdjustment.toInt(),
            ishaInterval = ishaInterval.toInt(),
        )
    }
}