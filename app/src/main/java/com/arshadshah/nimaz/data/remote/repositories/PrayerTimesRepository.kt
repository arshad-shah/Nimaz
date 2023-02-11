package com.arshadshah.nimaz.data.remote.repositories

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.models.Prayertime
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.network.ApiResponse
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import com.arshadshah.nimaz.utils.network.PrayerTimeResponse
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
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

		val dataStore = LocalDataStore.getDataStore()
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
			val prayerTimesAvailable = dataStore.countPrayerTimes() > 0
			if(prayerTimesAvailable){
				val prayerTimesLocal = dataStore.getAllPrayerTimes()
				//check if the next prayer time is in the future
				val nextPrayerTime = prayerTimesLocal.nextPrayer?.time
				val currentTime = prayerTimesLocal.currentPrayer?.time
				//if it is in the future return the prayer times from the database
				if(nextPrayerTime?.isAfter(LocalDateTime.now()) == true && currentTime?.isBefore(LocalDateTime.now()) == true){
					ApiResponse.Success(dataStore.getAllPrayerTimes())
				}else{
					//map the Prayertime object to a map of names
					val prayerTimesToMapped = dataStore.getAllPrayerTimes()
					val mapConverted = mapOf(
							"FAJR" to prayerTimesToMapped.fajr,
							"SUNRISE" to prayerTimesToMapped.sunrise,
							"DHUHR" to prayerTimesToMapped.dhuhr,
							"ASR" to prayerTimesToMapped.asr,
							"MAGHRIB" to prayerTimesToMapped.maghrib,
							"ISHA" to prayerTimesToMapped.isha
											)
					val nextPrayerTimeName = prayerTimesToMapped.nextPrayer?.name

					//find out the next prayer time from the map
					//for example if the nextPrayerTimeName is DHUHR we return the time for ASR as the next prayer time from the map
					//we can use the index of the nextPrayerTimeName to get the next prayer time
					val nextPrayerTimeIndex = mapConverted.keys.indexOf(nextPrayerTimeName)
					val nextPrayerTimeFromMap = mapConverted.values.elementAt(nextPrayerTimeIndex + 1)
					val nextPrayerNameFromMap = mapConverted.keys.elementAt(nextPrayerTimeIndex + 1)


					val newNextPrayerTime = Prayertime(
							name = nextPrayerNameFromMap,
							time = nextPrayerTimeFromMap
													  )

					//find out the new current prayer time from the map
					//for example if the nextPrayerTimeName is DHUHR we return the time for DHUHR as the current prayer time from the map
					//we can use the index of the nextPrayerTimeName to get the current prayer time
					val currentPrayerTimeIndex = mapConverted.keys.indexOf(nextPrayerTimeName)
					val currentPrayerTimeFromMap = mapConverted.values.elementAt(currentPrayerTimeIndex)
					val currentPrayerNameFromMap = mapConverted.keys.elementAt(currentPrayerTimeIndex)

					val newCurrentPrayerTime = Prayertime(
							name = currentPrayerNameFromMap,
							time = currentPrayerTimeFromMap
														  )

					//delete all the prayer times from the local database
					dataStore.deleteAllPrayerTimes()
					val newPrayerTimesObject = prayerTimesToMapped.copy(nextPrayer = newNextPrayerTime, currentPrayer = newCurrentPrayerTime)
					//insert the prayer times into the local database
					dataStore.saveAllPrayerTimes(newPrayerTimesObject)

					ApiResponse.Success(newPrayerTimesObject)
				}
			}else{
				val prayerTimesResponse = NimazServicesImpl.getPrayerTimes(mapOfParams)
				val prayerTimes = mapPrayerTimesResponseToPrayerTimes(prayerTimesResponse)
				runBlocking {
					//delete all the prayer times from the local database
					dataStore.deleteAllPrayerTimes()
					//insert the prayer times into the local database
					dataStore.saveAllPrayerTimes(prayerTimes)
				}
				ApiResponse.Success(prayerTimes)
			}
		} catch (e : ClientRequestException)
		{
			ApiResponse.Error(e.message , null)

		} catch (e : IOException)
		{
			ApiResponse.Error(e.message !! , null)
		}
	}

	//a function to map a prayer times response to a prayer times object
	private fun mapPrayerTimesResponseToPrayerTimes(prayerTimesResponse : PrayerTimeResponse) : PrayerTimes
	{
		return PrayerTimes(
			timestamp = LocalDateTime.now() ,
			LocalDateTime.parse(prayerTimesResponse.fajr) ,
			LocalDateTime.parse(prayerTimesResponse.sunrise) ,
			LocalDateTime.parse(prayerTimesResponse.dhuhr) ,
			LocalDateTime.parse(prayerTimesResponse.asr) ,
			LocalDateTime.parse(prayerTimesResponse.maghrib) ,
			LocalDateTime.parse(prayerTimesResponse.isha) ,
			Prayertime(
				name = prayerTimesResponse.nextPrayer.name ,
				time = LocalDateTime.parse(prayerTimesResponse.nextPrayer.time)
			),
			Prayertime(
				name = prayerTimesResponse.currentPrayer.name ,
				time = LocalDateTime.parse(prayerTimesResponse.currentPrayer.time)
			)
		)
	}
}