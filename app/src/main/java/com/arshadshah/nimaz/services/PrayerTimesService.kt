package com.arshadshah.nimaz.services

import android.content.Context
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

class PrayerTimesService(
    private val context: Context,
    private val prayerTimesRepository: PrayerTimesRepository
) {

    suspend fun getPrayerTimes(): PrayerTimesData {
        return withContext(Dispatchers.IO) {
            val response = prayerTimesRepository.getPrayerTimes(context)
            val data = response.data ?: throw Exception("Failed to load prayer times")
            PrayerTimesData(
                fajr = data.fajr!!,
                sunrise = data.sunrise!!,
                dhuhr = data.dhuhr!!,
                asr = data.asr!!,
                maghrib = data.maghrib!!,
                isha = data.isha!!
            )
        }
    }

    fun getCurrentAndNextPrayer(prayerTimes: PrayerTimesData): CurrentAndNextPrayer {
        val currentTime = LocalDateTime.now()
        val currentPrayerName = determineCurrentPrayer(currentTime, prayerTimes)
        val nextPrayerName = determineNextPrayer(currentTime, prayerTimes)

        return CurrentAndNextPrayer(
            currentPrayer = currentPrayerName.first,
            currentPrayerTime = currentPrayerName.second,
            nextPrayer = nextPrayerName.first,
            nextPrayerTime = nextPrayerName.second
        )
    }

    private fun determineCurrentPrayer(
        currentTime: LocalDateTime,
        prayerTimes: PrayerTimesData
    ): Pair<String, LocalDateTime> {
        val times = mapOf(
            "fajr" to prayerTimes.fajr,
            "sunrise" to prayerTimes.sunrise,
            "dhuhr" to prayerTimes.dhuhr,
            "asr" to prayerTimes.asr,
            "maghrib" to prayerTimes.maghrib,
            "isha" to prayerTimes.isha
        )

        return currentPrayer(currentTime, times)
    }

    private fun determineNextPrayer(
        currentTime: LocalDateTime,
        prayerTimes: PrayerTimesData
    ): Pair<String, LocalDateTime> {
        val times = mapOf(
            "fajr" to prayerTimes.fajr,
            "sunrise" to prayerTimes.sunrise,
            "dhuhr" to prayerTimes.dhuhr,
            "asr" to prayerTimes.asr,
            "maghrib" to prayerTimes.maghrib,
            "isha" to prayerTimes.isha
        )

        return nextPrayer(currentTime, times)
    }

    private fun currentPrayer(
        time: LocalDateTime,
        mapOfPrayerTimes: Map<String, LocalDateTime?>,
    ): Pair<String, LocalDateTime> {
        val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
        val `when` = time.toInstant(ZoneOffset.UTC).toEpochMilli()
        return when {
            //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
            mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("isha", mapOfPrayerTimes["isha"]!!)
            }

            mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
            }

            mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("asr", mapOfPrayerTimes["asr"]!!)
            }

            mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
            }

            mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
            }

            mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! - `when` <= 0 -> {
                Pair("fajr", mapOfPrayerTimes["fajr"]!!)
            }

            `when` in fajrTommorow?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!!..mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)
                ?.toEpochMilli()!! -> {
                Pair("isha", mapOfPrayerTimes["isha"]!!)
            }

            `when` < mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
                Pair("fajr", mapOfPrayerTimes["fajr"]!!)
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
        val fajrTommorow = mapOfPrayerTimes["fajr"]?.plusDays(1)
        val isha = mapOfPrayerTimes["isha"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val fajr = mapOfPrayerTimes["fajr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val sunrise = mapOfPrayerTimes["sunrise"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val dhuhr = mapOfPrayerTimes["dhuhr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val asr = mapOfPrayerTimes["asr"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!
        val maghrib = mapOfPrayerTimes["maghrib"]?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!!

        return when {
            //if the difference between the current time and the isha time is less than 0 or equal to 0 than the current prayer is isha
            isha - `when` <= 0 -> {
                Pair("fajr", mapOfPrayerTimes["fajr"]!!)
            }

            maghrib - `when` <= 0 -> {
                Pair("isha", mapOfPrayerTimes["isha"]!!)
            }

            asr - `when` <= 0 -> {
                Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
            }

            dhuhr - `when` <= 0 -> {
                Pair("asr", mapOfPrayerTimes["asr"]!!)
            }

            sunrise - `when` <= 0 -> {
                Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
            }

            fajr - `when` <= 0 -> {
                Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
            }

            `when` in fajr..sunrise -> {
                Pair("sunrise", mapOfPrayerTimes["sunrise"]!!)
            }

            `when` in sunrise..dhuhr -> {
                Pair("dhuhr", mapOfPrayerTimes["dhuhr"]!!)
            }

            `when` in dhuhr..asr -> {
                Pair("asr", mapOfPrayerTimes["asr"]!!)
            }

            `when` in asr..maghrib -> {
                Pair("maghrib", mapOfPrayerTimes["maghrib"]!!)
            }

            `when` in maghrib..isha -> {
                Pair("isha", mapOfPrayerTimes["isha"]!!)
            }

            `when` in isha..fajrTommorow?.toInstant(ZoneOffset.UTC)?.toEpochMilli()!! -> {
                Pair("fajr", mapOfPrayerTimes["fajr"]!!)
            }
            //if the current time is less than the fajr time than the next prayer is fajr
            `when` < fajr -> {
                Pair("fajr", mapOfPrayerTimes["fajr"]!!)
            }

            else -> {
                Pair("none", mapOfPrayerTimes["none"]!!)
            }
        }
    }
}

data class PrayerTimesData(
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime
)

data class CurrentAndNextPrayer(
    val currentPrayer: String,
    val currentPrayerTime: LocalDateTime,
    val nextPrayer: String,
    val nextPrayerTime: LocalDateTime
)