package com.arshadshah.nimaz.services

import android.content.Context
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_SUNRISE
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class PrayerTimesService(
    private val context: Context,
    private val prayerTimesRepository: PrayerTimesRepository
) {

    suspend fun getPrayerTimes(): PrayerTimesData? {
        return withContext(Dispatchers.IO) {
            try {
                val response = prayerTimesRepository.getPrayerTimes(context)
                val data = response.data

                // Check if data is not null
                if (data != null) {
                    // Calculate the new Isha time, ensuring null-safety
                    val ishaTime = data.isha?.toLocalTime()?.hour
                    val newIshaTime = if (ishaTime != null && ishaTime >= 22) {
                        data.maghrib?.plusMinutes(60)
                    } else {
                        data.isha
                    }

                    // Construct the PrayerTimesData object
                    PrayerTimesData(
                        fajr = data.fajr,
                        sunrise = data.sunrise,
                        dhuhr = data.dhuhr,
                        asr = data.asr,
                        maghrib = data.maghrib,
                        isha = newIshaTime
                    )
                } else {
                    // Return null if data is not available
                    null
                }
            } catch (e: Exception) {
                // Handle exceptions and return null
                null
            }
        }
    }


    fun getCurrentAndNextPrayer(prayerTimes: PrayerTimesData): CurrentAndNextPrayer {
        val currentTime = LocalDateTime.now()
        val currentPrayer = determineCurrentPrayer(currentTime, prayerTimes)
        var nextPrayer = determineNextPrayer(currentTime, prayerTimes)

        if (nextPrayer.first == PRAYER_NAME_FAJR && currentPrayer.first == PRAYER_NAME_ISHA && LocalTime.now().hour <= 24 && LocalTime.now().hour >= (prayerTimes.isha?.toLocalTime()?.hour
                ?: 0)
        ) {
            nextPrayer =
                Pair(nextPrayer.first, prayerTimes.fajr?.plusDays(1) ?: LocalDateTime.now())
        }

        return CurrentAndNextPrayer(
            currentPrayer = currentPrayer.first,
            currentPrayerTime = currentPrayer.second,
            nextPrayer = nextPrayer.first,
            nextPrayerTime = nextPrayer.second
        )
    }

    private fun determineCurrentPrayer(
        currentTime: LocalDateTime,
        prayerTimes: PrayerTimesData
    ): Pair<String, LocalDateTime> {
        val times = mapOf(
            PRAYER_NAME_FAJR to prayerTimes.fajr,
            PRAYER_NAME_SUNRISE to prayerTimes.sunrise,
            PRAYER_NAME_DHUHR to prayerTimes.dhuhr,
            PRAYER_NAME_ASR to prayerTimes.asr,
            PRAYER_NAME_MAGHRIB to prayerTimes.maghrib,
            PRAYER_NAME_ISHA to prayerTimes.isha
        )

        return currentPrayer(currentTime, times)
    }

    private fun determineNextPrayer(
        currentTime: LocalDateTime,
        prayerTimes: PrayerTimesData
    ): Pair<String, LocalDateTime> {
        val times = mapOf(
            PRAYER_NAME_FAJR to prayerTimes.fajr,
            PRAYER_NAME_SUNRISE to prayerTimes.sunrise,
            PRAYER_NAME_DHUHR to prayerTimes.dhuhr,
            PRAYER_NAME_ASR to prayerTimes.asr,
            PRAYER_NAME_MAGHRIB to prayerTimes.maghrib,
            PRAYER_NAME_ISHA to prayerTimes.isha
        )

        return nextPrayer(currentTime, times)
    }

    private fun currentPrayer(
        time: LocalDateTime,
        mapOfPrayerTimes: Map<String, LocalDateTime?>,
    ): Pair<String, LocalDateTime> {
        val fajrTommorow = mapOfPrayerTimes[PRAYER_NAME_FAJR]?.plusDays(1)
        val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
        return when {
            //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
            mapOfPrayerTimes[PRAYER_NAME_ISHA]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_ISHA, mapOfPrayerTimes[PRAYER_NAME_ISHA]!!)
            }

            mapOfPrayerTimes[PRAYER_NAME_MAGHRIB]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_MAGHRIB, mapOfPrayerTimes[PRAYER_NAME_MAGHRIB]!!)
            }

            mapOfPrayerTimes[PRAYER_NAME_ASR]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_ASR, mapOfPrayerTimes[PRAYER_NAME_ASR]!!)
            }

            mapOfPrayerTimes[PRAYER_NAME_DHUHR]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_DHUHR, mapOfPrayerTimes[PRAYER_NAME_DHUHR]!!)
            }

            mapOfPrayerTimes[PRAYER_NAME_SUNRISE]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_SUNRISE, mapOfPrayerTimes[PRAYER_NAME_SUNRISE]!!)
            }

            mapOfPrayerTimes[PRAYER_NAME_FAJR]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair(PRAYER_NAME_FAJR, mapOfPrayerTimes[PRAYER_NAME_FAJR]!!)
            }

            `when` in fajrTommorow?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!!..mapOfPrayerTimes[PRAYER_NAME_ISHA]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! -> {
                Pair(PRAYER_NAME_ISHA, mapOfPrayerTimes[PRAYER_NAME_ISHA]!!)
            }

            `when` < mapOfPrayerTimes[PRAYER_NAME_FAJR]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! -> {
                Pair(PRAYER_NAME_FAJR, mapOfPrayerTimes[PRAYER_NAME_FAJR]!!)
            }

            else -> {
                Pair("none", mapOfPrayerTimes["none"]!!)
            }
        }
    }

    private fun nextPrayer(
        time: LocalDateTime,
        mapOfPrayerTimes: Map<String, LocalDateTime?>,
    ): Pair<String, LocalDateTime> {
        val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
        val fajrTommorow = mapOfPrayerTimes[PRAYER_NAME_FAJR]?.plusDays(1)
        val isha = mapOfPrayerTimes[PRAYER_NAME_ISHA]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val fajr = mapOfPrayerTimes[PRAYER_NAME_FAJR]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val sunrise =
            mapOfPrayerTimes[PRAYER_NAME_SUNRISE]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val dhuhr = mapOfPrayerTimes[PRAYER_NAME_DHUHR]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val asr = mapOfPrayerTimes[PRAYER_NAME_ASR]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val maghrib =
            mapOfPrayerTimes[PRAYER_NAME_MAGHRIB]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!

        return when {
            //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
            isha - `when` <= 0 -> {
                Pair(PRAYER_NAME_FAJR, mapOfPrayerTimes[PRAYER_NAME_FAJR]!!)
            }

            maghrib - `when` <= 0 -> {
                Pair(PRAYER_NAME_ISHA, mapOfPrayerTimes[PRAYER_NAME_ISHA]!!)
            }

            asr - `when` <= 0 -> {
                Pair(PRAYER_NAME_MAGHRIB, mapOfPrayerTimes[PRAYER_NAME_MAGHRIB]!!)
            }

            dhuhr - `when` <= 0 -> {
                Pair(PRAYER_NAME_ASR, mapOfPrayerTimes[PRAYER_NAME_ASR]!!)
            }

            sunrise - `when` <= 0 -> {
                Pair(PRAYER_NAME_DHUHR, mapOfPrayerTimes[PRAYER_NAME_DHUHR]!!)
            }

            fajr - `when` <= 0 -> {
                Pair(PRAYER_NAME_SUNRISE, mapOfPrayerTimes[PRAYER_NAME_SUNRISE]!!)
            }

            `when` in fajr..sunrise -> {
                Pair(PRAYER_NAME_SUNRISE, mapOfPrayerTimes[PRAYER_NAME_SUNRISE]!!)
            }

            `when` in sunrise..dhuhr -> {
                Pair(PRAYER_NAME_DHUHR, mapOfPrayerTimes[PRAYER_NAME_DHUHR]!!)
            }

            `when` in dhuhr..asr -> {
                Pair(PRAYER_NAME_ASR, mapOfPrayerTimes[PRAYER_NAME_ASR]!!)
            }

            `when` in asr..maghrib -> {
                Pair(PRAYER_NAME_MAGHRIB, mapOfPrayerTimes[PRAYER_NAME_MAGHRIB]!!)
            }

            `when` in maghrib..isha -> {
                Pair(PRAYER_NAME_ISHA, mapOfPrayerTimes[PRAYER_NAME_ISHA]!!)
            }

            `when` in isha..fajrTommorow?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
                Pair(PRAYER_NAME_FAJR, mapOfPrayerTimes[PRAYER_NAME_FAJR]!!)
            }
            //if the current time is less than the fajr time than the next prayer is fajr
            `when` < fajr -> {
                Pair(PRAYER_NAME_FAJR, mapOfPrayerTimes[PRAYER_NAME_FAJR]!!)
            }

            else -> {
                Pair("none", mapOfPrayerTimes["none"]!!)
            }
        }
    }
}

data class PrayerTimesData(
    val fajr: LocalDateTime? = null,
    val sunrise: LocalDateTime? = null,
    val dhuhr: LocalDateTime? = null,
    val asr: LocalDateTime? = null,
    val maghrib: LocalDateTime? = null,
    val isha: LocalDateTime? = null
)

data class CurrentAndNextPrayer(
    val currentPrayer: String,
    val currentPrayerTime: LocalDateTime,
    val nextPrayer: String,
    val nextPrayerTime: LocalDateTime
)