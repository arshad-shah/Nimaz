package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import com.arshadshah.nimaz.utils.network.PrayerTimeResponse
import io.ktor.client.plugins.*
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerTimesRepository
{

	/**
	 * Creates a map of prayer times parameters to be used in the API call
	 * all the parameters are taken from the user's settings
	 * returns an ApiResponse object of type PrayerTimes
	 * @param context the context of the application
	 * @return ApiResponse<PrayerTimes> the response from the API call see [ApiResponse]
	 * */
	suspend fun getPrayerTimes(
		context : Context ,
		dateForTimes : String = LocalDate.now().toString() ,
							  ) : ApiResponse<PrayerTimes>
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
		val fajrAngle : String =
			sharedPreferences.getData(AppConstants.FAJR_ANGLE , "18")
		val ishaAngle : String =
			sharedPreferences.getData(AppConstants.ISHA_ANGLE , "17")
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
			if (prayerTimesAvailable)
			{
				val prayerTimesLocal = dataStore.getPrayerTimesForADate(dateForTimes)

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
						//check if the day light saving is on or off
						val isDayLightSaving =
							ZoneId.systemDefault().rules.isDaylightSavings(Instant.now())
						//check if its an add or subtract
						val timezoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
						val timezoneOffsetHours = timezoneOffset.totalSeconds / 3600
						//check if the offset is positive or negative
						val isPositive = timezoneOffsetHours > 0
						val isNegative = timezoneOffsetHours < 0
						if (isDayLightSaving && isPositive)
						{
							prayerTime.fajr =
								prayerTime.fajr?.plusHours((timezoneOffsetHours.toLong()))
							prayerTime.sunrise =
								prayerTime.sunrise?.plusHours((timezoneOffsetHours.toLong()))
							prayerTime.dhuhr =
								prayerTime.dhuhr?.plusHours((timezoneOffsetHours.toLong()))
							prayerTime.asr =
								prayerTime.asr?.plusHours((timezoneOffsetHours.toLong()))
							prayerTime.maghrib =
								prayerTime.maghrib?.plusHours((timezoneOffsetHours.toLong()))
							prayerTime.isha =
								prayerTime.isha?.plusHours((timezoneOffsetHours.toLong()))
						} else if (isDayLightSaving && isNegative)
						{
							prayerTime.fajr =
								prayerTime.fajr?.minusHours((timezoneOffsetHours.toLong()))
							prayerTime.sunrise =
								prayerTime.sunrise?.minusHours((timezoneOffsetHours.toLong()))
							prayerTime.dhuhr =
								prayerTime.dhuhr?.minusHours((timezoneOffsetHours.toLong()))
							prayerTime.asr =
								prayerTime.asr?.minusHours((timezoneOffsetHours.toLong()))
							prayerTime.maghrib =
								prayerTime.maghrib?.minusHours((timezoneOffsetHours.toLong()))
							prayerTime.isha =
								prayerTime.isha?.minusHours((timezoneOffsetHours.toLong()))
						}
						prayerTimes.add(prayerTime)
						dataStore.saveAllPrayerTimes(prayerTime)
					}
					return ApiResponse.Success(prayerTimes.find { it.date == LocalDate.now() } !!)
				}

				return ApiResponse.Success(prayerTimesLocal)
			} else
			{
				val prayerTimesResponse = NimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParams)
				val prayerTimes = mutableListOf<PrayerTimes>()
				for (prayerTimeResponse in prayerTimesResponse)
				{
					val prayerTime = mapPrayerTimesResponseToPrayerTimes(prayerTimeResponse)
					//check if the day light saving is on or off
					val isDayLightSaving =
						ZoneId.systemDefault().rules.isDaylightSavings(Instant.now())
					//check if its an add or subtract
					val timezoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
					val timezoneOffsetHours = timezoneOffset.totalSeconds / 3600
					//check if the offset is positive or negative
					val isPositive = timezoneOffsetHours > 0
					val isNegative = timezoneOffsetHours < 0
					if (isDayLightSaving && isPositive)
					{
						prayerTime.fajr = prayerTime.fajr?.plusHours((timezoneOffsetHours.toLong()))
						prayerTime.sunrise =
							prayerTime.sunrise?.plusHours((timezoneOffsetHours.toLong()))
						prayerTime.dhuhr =
							prayerTime.dhuhr?.plusHours((timezoneOffsetHours.toLong()))
						prayerTime.asr = prayerTime.asr?.plusHours((timezoneOffsetHours.toLong()))
						prayerTime.maghrib =
							prayerTime.maghrib?.plusHours((timezoneOffsetHours.toLong()))
						prayerTime.isha = prayerTime.isha?.plusHours((timezoneOffsetHours.toLong()))
					} else if (isDayLightSaving && isNegative)
					{
						prayerTime.fajr =
							prayerTime.fajr?.minusHours((timezoneOffsetHours.toLong()))
						prayerTime.sunrise =
							prayerTime.sunrise?.minusHours((timezoneOffsetHours.toLong()))
						prayerTime.dhuhr =
							prayerTime.dhuhr?.minusHours((timezoneOffsetHours.toLong()))
						prayerTime.asr = prayerTime.asr?.minusHours((timezoneOffsetHours.toLong()))
						prayerTime.maghrib =
							prayerTime.maghrib?.minusHours((timezoneOffsetHours.toLong()))
						prayerTime.isha =
							prayerTime.isha?.minusHours((timezoneOffsetHours.toLong()))
					}
					prayerTimes.add(prayerTime)
					dataStore.saveAllPrayerTimes(prayerTime)
				}
				return ApiResponse.Success(prayerTimes.find { it.date == LocalDate.now() } !!)
			}
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
		val dataStore = LocalDataStore.getDataStore()
		val prayerTimesResponse = NimazServicesImpl.getPrayerTimesMonthlyCustom(mapOfParameters)
		val prayerTimes = mutableListOf<PrayerTimes>()
		dataStore.deleteAllPrayerTimes()
		for (prayerTimeResponse in prayerTimesResponse)
		{
			val prayerTime = mapPrayerTimesResponseToPrayerTimes(prayerTimeResponse)
			//check if the day light saving is on or off
			val isDayLightSaving = ZoneId.systemDefault().rules.isDaylightSavings(Instant.now())
			//check if its an add or subtract
			val timezoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
			val timezoneOffsetHours = timezoneOffset.totalSeconds / 3600
			//check if the offset is positive or negative
			val isPositive = timezoneOffsetHours > 0
			val isNegative = timezoneOffsetHours < 0
			if (isDayLightSaving && isPositive)
			{
				prayerTime.fajr = prayerTime.fajr?.plusHours(timezoneOffsetHours.toLong())
				prayerTime.sunrise = prayerTime.sunrise?.plusHours((timezoneOffsetHours.toLong()))
				prayerTime.dhuhr = prayerTime.dhuhr?.plusHours((timezoneOffsetHours.toLong()))
				prayerTime.asr = prayerTime.asr?.plusHours((timezoneOffsetHours.toLong()))
				prayerTime.maghrib = prayerTime.maghrib?.plusHours((timezoneOffsetHours.toLong()))
				prayerTime.isha = prayerTime.isha?.plusHours((timezoneOffsetHours.toLong()))
			} else if (isDayLightSaving && isNegative)
			{
				prayerTime.fajr = prayerTime.fajr?.minusHours((timezoneOffsetHours.toLong()))
				prayerTime.sunrise = prayerTime.sunrise?.minusHours((timezoneOffsetHours.toLong()))
				prayerTime.dhuhr = prayerTime.dhuhr?.minusHours((timezoneOffsetHours.toLong()))
				prayerTime.asr = prayerTime.asr?.minusHours((timezoneOffsetHours.toLong()))
				prayerTime.maghrib = prayerTime.maghrib?.minusHours((timezoneOffsetHours.toLong()))
				prayerTime.isha = prayerTime.isha?.minusHours((timezoneOffsetHours.toLong()))
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