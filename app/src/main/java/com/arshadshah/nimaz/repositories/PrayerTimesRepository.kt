package com.arshadshah.nimaz.repositories

import android.content.Context
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.libs.prayertimes.PrayerTimesCalculated
import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.CalculationParameters
import com.arshadshah.nimaz.libs.prayertimes.enums.Prayer
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.utils.ApiResponse
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper.getParams
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerTimesRepository {

    /**
     * Creates a map of prayer times parameters to be used in the API call
     * all the parameters are taken from the user's settings
     * returns an ApiResponse object of type PrayerTimes
     * @param context the context of the application
     * @return ApiResponse<PrayerTimes> the response from the API call see [ApiResponse]
     * */
    suspend fun getPrayerTimes(
        context: Context,
        dateForTimes: String = LocalDate.now().toString(),
    ): ApiResponse<LocalPrayerTimes> {

        //check if the local datastore has been initialized if not initialize it
        if (!LocalDataStore.isInitialized()) {
            LocalDataStore.init(context)
        }
        val dataStore = LocalDataStore.getDataStore()

        return try {
            val prayerTimesAvailable = dataStore.countPrayerTimes() > 0
            if (prayerTimesAvailable) {
                val prayerTimesLocal = dataStore.getPrayerTimesForADate(dateForTimes)

                //check if the date is for current month if not update the prayer times
                val date = prayerTimesLocal.date
                val currentDate = LocalDate.now()
                val currentMonth = currentDate.monthValue
                val currentYear = currentDate.year
                val dateMonth = date.monthValue
                val dateYear = date.year
                if (dateMonth != currentMonth || dateYear != currentYear) {
                    val prayerTimesResponse = getPrayerTimesForMonthCustom(getParams(context))
                    val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
                    return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
                }

                return ApiResponse.Success(prayerTimesLocal)
            } else {
                val prayerTimesResponse =
                    getPrayerTimesForMonthCustom(getParams(context))
                val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
                return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
            }
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }


    suspend fun updatePrayerTimes(parameters: Parameters): ApiResponse<LocalPrayerTimes> {
        val dataStore = LocalDataStore.getDataStore()
        val prayerTimesResponse = getPrayerTimesForMonthCustom(parameters)
        val prayerTimesList = processPrayerTimes(dataStore, prayerTimesResponse)
        return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
    }


    private suspend fun processPrayerTimes(
        dataStore: DataStore,
        prayerTimesResponse: List<LocalPrayerTimes>,
    ): MutableList<LocalPrayerTimes> {
        val prayerTimesList = mutableListOf<LocalPrayerTimes>()
        prayerTimesResponse.map { prayerTimes ->
            val prayerTime = LocalPrayerTimes(
                date = prayerTimes.date,
                prayerTimes.fajr,
                prayerTimes.sunrise,
                prayerTimes.dhuhr,
                prayerTimes.asr,
                prayerTimes.maghrib,
                prayerTimes.isha,
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
            if (isDayLightSaving && isPositive) {
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
            } else if (isDayLightSaving && isNegative) {
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

    private fun getPrayerTimesForMonthCustom(params: Parameters): List<LocalPrayerTimes> {
        //get the latitude and longitude from the parameters
        val coordinates =
            Coordinates(params.latitude, params.longitude)

        val date = LocalDateTime.parse(params.date)
        val calculationParameters =
            CalculationParameters(
                params.fajrAngle,
                params.ishaAngle,
                params.method
            )
        calculationParameters.madhab = params.madhab
        calculationParameters.highLatitudeRule = params.highLatitudeRule
        calculationParameters.adjustments.fajr = params.fajrAdjustment
        calculationParameters.adjustments.sunrise = params.sunriseAdjustment
        calculationParameters.adjustments.dhuhr = params.dhuhrAdjustment
        calculationParameters.adjustments.asr = params.asrAdjustment
        calculationParameters.adjustments.maghrib = params.maghribAdjustment
        calculationParameters.adjustments.isha = params.ishaAdjustment
        calculationParameters.coordinates = coordinates

        //get the number of days in the month
        val daysInMonth = date.month.length(date.toLocalDate().isLeapYear)

        //create a list of Prayertimes objects
        val prayertimesList = mutableListOf<LocalPrayerTimes>()
        //loop through the days in the month
        for (i in 1..daysInMonth) {
            val newDate = LocalDateTime.of(date.year, date.month, i, 0, 0)
            //get the prayer times for the day
            val prayertimes = PrayerTimesCalculated(
                coordinates,
                newDate,
                calculationParameters
            )
            //add the prayertimes object to the list
            prayertimesList.add(
                LocalPrayerTimes(
                    newDate.toLocalDate(),
                    prayertimes.timeForPrayer(Prayer.FAJR),
                    prayertimes.timeForPrayer(Prayer.SUNRISE),
                    prayertimes.timeForPrayer(Prayer.DHUHR),
                    prayertimes.timeForPrayer(Prayer.ASR),
                    prayertimes.timeForPrayer(Prayer.MAGHRIB),
                    prayertimes.timeForPrayer(Prayer.ISHA)
                )
            )
        }
        //return the list
        return prayertimesList
    }
}
