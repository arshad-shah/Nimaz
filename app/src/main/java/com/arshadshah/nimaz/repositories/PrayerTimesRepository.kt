package com.arshadshah.nimaz.repositories

import android.content.Context
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalPrayerTimes
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.libs.prayertimes.PrayerTimesCalculated
import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.CalculationParameters
import com.arshadshah.nimaz.libs.prayertimes.enums.Prayer
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.utils.ApiResponse
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper.getParams
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimesRepository @Inject constructor(
    private val dataStore: DataStore
) {
    companion object;

    suspend fun getPrayerTimes(
        context: Context,
        dateForTimes: String = LocalDate.now().toString(),
    ): ApiResponse<LocalPrayerTimes> {
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
                    val prayerTimesList = processPrayerTimes(prayerTimesResponse)
                    return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
                }

                return ApiResponse.Success(prayerTimesLocal)
            } else {
                val prayerTimesResponse = getPrayerTimesForMonthCustom(getParams(context))
                val prayerTimesList = processPrayerTimes(prayerTimesResponse)
                return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
            }
        } catch (e: IOException) {
            ApiResponse.Error(e.message!!, null)
        }
    }

    suspend fun updatePrayerTimes(parameters: Parameters): ApiResponse<LocalPrayerTimes> {
        val prayerTimesResponse = getPrayerTimesForMonthCustom(parameters)
        val prayerTimesList = processPrayerTimes(prayerTimesResponse)
        return ApiResponse.Success(prayerTimesList.find { it.date == LocalDate.now() }!!)
    }

    private suspend fun processPrayerTimes(
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

            // Timezone adjustment logic
            val timezoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())
            val timezoneOffsetHours = timezoneOffset.totalSeconds / 3600
            val isPositive = timezoneOffsetHours > 0
            val isNegative = timezoneOffsetHours < 0

            if (isPositive) {
                prayerTime.apply {
                    fajr = fajr?.plusHours(timezoneOffsetHours.toLong())
                    sunrise = sunrise?.plusHours(timezoneOffsetHours.toLong())
                    dhuhr = dhuhr?.plusHours(timezoneOffsetHours.toLong())
                    asr = asr?.plusHours(timezoneOffsetHours.toLong())
                    maghrib = maghrib?.plusHours(timezoneOffsetHours.toLong())
                    isha = isha?.plusHours(timezoneOffsetHours.toLong())
                }
            } else if (isNegative) {
                prayerTime.apply {
                    fajr = fajr?.minusHours((-timezoneOffsetHours).toLong())
                    sunrise = sunrise?.minusHours((-timezoneOffsetHours).toLong())
                    dhuhr = dhuhr?.minusHours((-timezoneOffsetHours).toLong())
                    asr = asr?.minusHours((-timezoneOffsetHours).toLong())
                    maghrib = maghrib?.minusHours((-timezoneOffsetHours).toLong())
                    isha = isha?.minusHours((-timezoneOffsetHours).toLong())
                }
            }

            prayerTimesList.add(prayerTime)
            dataStore.saveAllPrayerTimes(prayerTime)
        }

        return prayerTimesList
    }

    private fun getPrayerTimesForMonthCustom(params: Parameters): List<LocalPrayerTimes> {
        val coordinates = Coordinates(params.latitude, params.longitude)
        val date = LocalDateTime.parse(params.date)

        val calculationParameters = CalculationParameters(
            params.fajrAngle,
            params.ishaAngle,
            params.method
        ).apply {
            madhab = params.madhab
            highLatitudeRule = params.highLatitudeRule
            adjustments.apply {
                fajr = params.fajrAdjustment
                sunrise = params.sunriseAdjustment
                dhuhr = params.dhuhrAdjustment
                asr = params.asrAdjustment
                maghrib = params.maghribAdjustment
                isha = params.ishaAdjustment
            }
            this.coordinates = coordinates
        }

        val daysInMonth = date.month.length(date.toLocalDate().isLeapYear)
        return (1..daysInMonth).map { day ->
            val newDate = LocalDateTime.of(date.year, date.month, day, 0, 0)
            val prayertimes = PrayerTimesCalculated(coordinates, newDate, calculationParameters)

            LocalPrayerTimes(
                newDate.toLocalDate(),
                prayertimes.timeForPrayer(Prayer.FAJR),
                prayertimes.timeForPrayer(Prayer.SUNRISE),
                prayertimes.timeForPrayer(Prayer.DHUHR),
                prayertimes.timeForPrayer(Prayer.ASR),
                prayertimes.timeForPrayer(Prayer.MAGHRIB),
                prayertimes.timeForPrayer(Prayer.ISHA)
            )
        }
    }
}