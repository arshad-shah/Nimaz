package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import android.icu.util.TimeZone
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import com.arshadshah.nimaz.utils.network.PrayerTimeResponse
import io.ktor.client.plugins.*
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

		//check if the local datastore has been initialized if not initialize it
		if (! LocalDataStore.isInitialized())
		{
			LocalDataStore.init(context)
		}
		val dataStore = LocalDataStore.getDataStore()
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

		return try
		{
			val prayerTimesAvailable = dataStore.countPrayerTimes() > 0
			val timeZone = TimeZone.getDefault()
			val isDaylightSavingTime = timeZone.inDaylightTime(Date())
			if (prayerTimesAvailable)
			{
				val prayerTimesLocal = dataStore.getPrayerTimesForADate(LocalDate.now().toString())

				//check if the date is for current month if not update the prayer times
				val date = prayerTimesLocal?.date
				val currentDate = LocalDate.now()
				val currentMonth = currentDate.monthValue
				val currentYear = currentDate.year
				val dateMonth = date?.monthValue
				val dateYear = date?.year
				if (dateMonth != currentMonth || dateYear != currentYear)
				{
					val prayerTimesResponse =
						NimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParams)
					val prayerTimes = mutableListOf<PrayerTimes>()
					dataStore.deleteAllPrayerTimes()
					for (prayerTimeResponse in prayerTimesResponse)
					{
						val prayerTime = mapPrayerTimesResponseToPrayerTimes(prayerTimeResponse)
						if(isDaylightSavingTime){
							prayerTime.fajr = prayerTime.fajr?.plusHours(1)
							prayerTime.sunrise = prayerTime.sunrise?.plusHours(1)
							prayerTime.dhuhr = prayerTime.dhuhr?.plusHours(1)
							prayerTime.asr = prayerTime.asr?.plusHours(1)
							prayerTime.maghrib = prayerTime.maghrib?.plusHours(1)
							prayerTime.isha = prayerTime.isha?.plusHours(1)
						}
						prayerTimes.add(prayerTime)
						dataStore.saveAllPrayerTimes(prayerTime)
					}
					return ApiResponse.Success(prayerTimes.find { it.date == LocalDate.now() } !!)
				}

				if (prayerTimesLocal != null)
				{
					return ApiResponse.Success(prayerTimesLocal)
				}
			} else
			{
				val prayerTimesResponse = NimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParams)
				val prayerTimes = mutableListOf<PrayerTimes>()
				for (prayerTimeResponse in prayerTimesResponse)
				{
					val prayerTime = mapPrayerTimesResponseToPrayerTimes(prayerTimeResponse)
					if(isDaylightSavingTime){
						prayerTime.fajr = prayerTime.fajr?.plusHours(1)
						prayerTime.sunrise = prayerTime.sunrise?.plusHours(1)
						prayerTime.dhuhr = prayerTime.dhuhr?.plusHours(1)
						prayerTime.asr = prayerTime.asr?.plusHours(1)
						prayerTime.maghrib = prayerTime.maghrib?.plusHours(1)
						prayerTime.isha = prayerTime.isha?.plusHours(1)
					}
					prayerTimes.add(prayerTime)
					dataStore.saveAllPrayerTimes(prayerTime)
				}
				return ApiResponse.Success(prayerTimes.find { it.date == LocalDate.now() } !!)
			}
			ApiResponse.Error("Prayer Times Not Available" , null)
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}


	suspend fun updatePrayerTimes(mapOfParameters : Map<String , String>) : ApiResponse<PrayerTimes>
	{
		val timeZone = TimeZone.getDefault()
		val isDaylightSavingTime = timeZone.inDaylightTime(Date())
		val dataStore = LocalDataStore.getDataStore()
		val prayerTimesResponse = NimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParameters)
		val prayerTimes = mutableListOf<PrayerTimes>()
		dataStore.deleteAllPrayerTimes()
		for (prayerTimeResponse in prayerTimesResponse)
		{
			val prayerTime = mapPrayerTimesResponseToPrayerTimes(prayerTimeResponse)
			if(isDaylightSavingTime){
				prayerTime.fajr = prayerTime.fajr?.plusHours(1)
				prayerTime.sunrise = prayerTime.sunrise?.plusHours(1)
				prayerTime.dhuhr = prayerTime.dhuhr?.plusHours(1)
				prayerTime.asr = prayerTime.asr?.plusHours(1)
				prayerTime.maghrib = prayerTime.maghrib?.plusHours(1)
				prayerTime.isha = prayerTime.isha?.plusHours(1)
			}
			prayerTimes.add(prayerTime)
			dataStore.saveAllPrayerTimes(prayerTime)
		}
		return ApiResponse.Success(prayerTimes.find { it.date == LocalDate.now() } !!)
	}

	//a function to map a prayer times response to a prayer times object
	private fun mapPrayerTimesResponseToPrayerTimes(prayerTimesResponse : PrayerTimeResponse) : PrayerTimes
	{
		return PrayerTimes(
				date = LocalDate.parse(prayerTimesResponse.date) ,
				LocalDateTime.parse(prayerTimesResponse.fajr) ,
				LocalDateTime.parse(prayerTimesResponse.sunrise) ,
				LocalDateTime.parse(prayerTimesResponse.dhuhr) ,
				LocalDateTime.parse(prayerTimesResponse.asr) ,
				LocalDateTime.parse(prayerTimesResponse.maghrib) ,
				LocalDateTime.parse(prayerTimesResponse.isha) ,
						  )
	}
}