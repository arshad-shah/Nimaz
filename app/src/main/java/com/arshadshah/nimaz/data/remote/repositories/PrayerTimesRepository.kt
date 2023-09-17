package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import com.apollographql.apollo3.api.ApolloResponse
import com.arshadshah.nimaz.GetPrayerTimesForMonthQuery
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.type.Parameters
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.api.ApiResponse
import com.arshadshah.nimaz.utils.api.NimazServicesImpl
import com.arshadshah.nimaz.utils.api.PrayerTimesParamMapper.getParams
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
						NimazServicesImpl.getPrayerTimesMonthlyCustom(getParams(context))
					val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
					return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() } !!)
				}

				return ApiResponse.Success(prayerTimesLocal)
			} else
			{
				val prayerTimesResponse =
					NimazServicesImpl.getPrayerTimesMonthlyCustom(getParams(context))
				val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
				return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() } !!)
			}
		}catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}


	suspend fun updatePrayerTimes(parameters : Parameters) : ApiResponse<PrayerTimes>
	{
		val dataStore = LocalDataStore.getDataStore()
		val prayerTimesResponse =
			NimazServicesImpl.getPrayerTimesMonthlyCustom(parameters)
		val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
		return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() } !!)
	}


	private suspend fun processPrayerTimes(
		dataStore : DataStore ,
		prayerTimesResponse : ApolloResponse<GetPrayerTimesForMonthQuery.Data>
										  ) : MutableList<PrayerTimes>
	{
		val prayerTimesList = mutableListOf<PrayerTimes>()
		prayerTimesResponse.data!!.getPrayerTimesForMonthCustom?.map { prayerTimes ->
			val prayerTime = PrayerTimes(
					 date = LocalDate.parse(prayerTimes!!.date) ,
					 LocalDateTime.parse(prayerTimes.fajr) ,
					 LocalDateTime.parse(prayerTimes.sunrise) ,
					 LocalDateTime.parse(prayerTimes.dhuhr) ,
					 LocalDateTime.parse(prayerTimes.asr) ,
					 LocalDateTime.parse(prayerTimes.maghrib) ,
					 LocalDateTime.parse(prayerTimes.isha) ,
										)
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
			prayerTimesList.add(prayerTime)
			dataStore.saveAllPrayerTimes(prayerTime)
		}

		return prayerTimesList
	}
}
