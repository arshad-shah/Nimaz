package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import android.util.Log
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.models.Prayertime
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import io.ktor.client.plugins.*
import java.io.IOException
import java.time.LocalDateTime

object PrayerTimesRepository
{

	/**
	 * Creates a map of prayer times parameters to be used in the API call
	 * all the parameters are taken from the user's settings
	 * returns an ApiResponse object of type PrayerTimes
	 * @param context the context of the application
	 * @return ApiResponse<PrayerTimes> the response from the API call see [ApiResponse]
	 * */
	suspend fun getPrayerTimes(context : Context) : ApiResponse<PrayerTimes>
	{

		val sharedPreferences = PrivateSharedPreferences(context)
		val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
		val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
		val fajrAngle : String = sharedPreferences.getData(AppConstants.FAJR_ANGLE , "18")
		val ishaAngle : String = sharedPreferences.getData(AppConstants.ISHA_ANGLE , "17")
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

		val mapOfParams = mutableMapOf<String , String>()
		mapOfParams["latitude"] = latitude.toString()
		mapOfParams["longitude"] = longitude.toString()
		mapOfParams["date"] = LocalDateTime.now().toString()
		mapOfParams["fajrAngle"] = fajrAngle
		mapOfParams["ishaAngle"] = ishaAngle
		mapOfParams["method"] = calculationMethod
		mapOfParams["madhab"] = madhab
		mapOfParams["highLatitudeRule"] = highLatitudeRule
		mapOfParams["fajrAdjustment"] = fajrAdjustment
		mapOfParams["sunriseAdjustment"] = sunriseAdjustment
		mapOfParams["dhuhrAdjustment"] = dhuhrAdjustment
		mapOfParams["asrAdjustment"] = asrAdjustment
		mapOfParams["maghribAdjustment"] = maghribAdjustment
		mapOfParams["ishaAdjustment"] = ishaAdjustment

		return try
		{
			val response = NimazServicesImpl.getPrayerTimes(mapOfParams)

			val prayerTimes = PrayerTimes(
					timestamp = LocalDateTime.now() ,
					fajr = LocalDateTime.parse(response.fajr) ,
					sunrise = LocalDateTime.parse(response.sunrise) ,
					dhuhr = LocalDateTime.parse(response.dhuhr) ,
					asr = LocalDateTime.parse(response.asr) ,
					maghrib = LocalDateTime.parse(response.maghrib) ,
					isha = LocalDateTime.parse(response.isha) ,
					nextPrayer = Prayertime(
							name = response.nextPrayer.name ,
							time = LocalDateTime.parse(response.nextPrayer.time)
										   ) ,
					currentPrayer = Prayertime(
							name = response.currentPrayer.name ,
							time = LocalDateTime.parse(response.currentPrayer.time)
											  )
										 )
			Log.d("PrayerTimesRepository" , "getPrayerTimes: $prayerTimes")
			ApiResponse.Success(prayerTimes)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}
}