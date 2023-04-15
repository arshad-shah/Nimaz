package com.arshadshah.nimaz.utils.api

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.time.LocalDateTime

object PrayerTimesParamMapper
{

	fun getParams(context : Context) : MutableMap<String , String>
	{
		val sharedPreferences = PrivateSharedPreferences(context)
		val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 0.0)
		val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , 0.0)
		val fajrAngle : String = sharedPreferences.getData(AppConstants.FAJR_ANGLE , "18")
		val ishaAngle : String = sharedPreferences.getData(AppConstants.ISHA_ANGLE , "17")
		val ishaInterval : String = sharedPreferences.getData(AppConstants.ISHA_INTERVAL , "0")
		val calculationMethod : String =
			sharedPreferences.getData(AppConstants.CALCULATION_METHOD , "IRELAND")
		val madhab : String = sharedPreferences.getData(AppConstants.MADHAB , "SHAFI")
		val highLatitudeRule : String =
			sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE , "TWILIGHT_ANGLE")
		val fajrAdjustment : String = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT , "0")
		val sunriseAdjustment : String =
			sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT , "0")
		val dhuhrAdjustment : String =
			sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT , "0")
		val asrAdjustment : String = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT , "0")
		val maghribAdjustment : String =
			sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT , "0")
		val ishaAdjustment : String = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT , "0")

		//log the adjustments
		Log.d("Nimaz: PrayerTimesParamMapper" , "fajrAdjustment: $fajrAdjustment")
		Log.d("Nimaz: PrayerTimesParamMapper" , "sunriseAdjustment: $sunriseAdjustment")
		Log.d("Nimaz: PrayerTimesParamMapper" , "dhuhrAdjustment: $dhuhrAdjustment")
		Log.d("Nimaz: PrayerTimesParamMapper" , "asrAdjustment: $asrAdjustment")
		Log.d("Nimaz: PrayerTimesParamMapper" , "maghribAdjustment: $maghribAdjustment")
		Log.d("Nimaz: PrayerTimesParamMapper" , "ishaAdjustment: $ishaAdjustment")


		val mapOfParams = mutableMapOf<String , String>()
		mapOfParams["latitude"] = latitude.toString()
		mapOfParams["longitude"] = longitude.toString()
		mapOfParams["date"] = LocalDateTime.now().toString()
		mapOfParams["fajrAngle"] = fajrAngle
		mapOfParams["ishaAngle"] = ishaAngle
		mapOfParams["ishaInterval"] = ishaInterval
		mapOfParams["method"] = calculationMethod
		mapOfParams["madhab"] = madhab
		mapOfParams["highLatitudeRule"] = highLatitudeRule
		mapOfParams["fajrAdjustment"] = fajrAdjustment
		mapOfParams["sunriseAdjustment"] = sunriseAdjustment
		mapOfParams["dhuhrAdjustment"] = dhuhrAdjustment
		mapOfParams["asrAdjustment"] = asrAdjustment
		mapOfParams["maghribAdjustment"] = maghribAdjustment
		mapOfParams["ishaAdjustment"] = ishaAdjustment

		return mapOfParams
	}
}