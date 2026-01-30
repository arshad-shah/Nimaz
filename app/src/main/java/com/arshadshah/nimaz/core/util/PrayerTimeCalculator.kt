package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.PrayerType
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime as toKotlinLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimeCalculator @Inject constructor() {

    /**
     * Simple method to get prayer times using just coordinates
     * Uses default calculation method (Muslim World League) and current timezone
     */
    fun getPrayerTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule: HighLatitudeRule? = null,
        adjustments: Map<PrayerType, Int> = emptyMap()
    ): List<PrayerTime> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)

        val method = when (calculationMethod) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
            CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
            CalculationMethod.KARACHI -> AdhanMethod.KARACHI
            CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
            CalculationMethod.DUBAI -> AdhanMethod.DUBAI
            CalculationMethod.MOON_SIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
            CalculationMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
            CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT
            CalculationMethod.QATAR -> AdhanMethod.QATAR
            CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE
            CalculationMethod.TURKEY -> AdhanMethod.TURKEY
        }

        var parameters = method.parameters.copy(
            madhab = when (asrCalculation) {
                AsrCalculation.STANDARD -> Madhab.SHAFI
                AsrCalculation.HANAFI -> Madhab.HANAFI
            }
        )

        highLatitudeRule?.let { rule ->
            val adhanRule = when (rule) {
                HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
                HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
                HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
            }
            parameters = parameters.copy(highLatitudeRule = adhanRule)
        }

        val adhanTimes = AdhanPrayerTimes(coordinates, dateComponents, parameters)

        fun adjustTime(instant: Instant, prayerType: PrayerType): Instant {
            val adj = adjustments[prayerType] ?: 0
            return if (adj != 0) {
                Instant.fromEpochMilliseconds(instant.toEpochMilliseconds() + adj * 60_000L)
            } else {
                instant
            }
        }

        return listOf(
            PrayerTime(PrayerType.FAJR, adjustTime(adhanTimes.fajr, PrayerType.FAJR)),
            PrayerTime(PrayerType.SUNRISE, adjustTime(adhanTimes.sunrise, PrayerType.SUNRISE)),
            PrayerTime(PrayerType.DHUHR, adjustTime(adhanTimes.dhuhr, PrayerType.DHUHR)),
            PrayerTime(PrayerType.ASR, adjustTime(adhanTimes.asr, PrayerType.ASR)),
            PrayerTime(PrayerType.MAGHRIB, adjustTime(adhanTimes.maghrib, PrayerType.MAGHRIB)),
            PrayerTime(PrayerType.ISHA, adjustTime(adhanTimes.isha, PrayerType.ISHA))
        )
    }

    fun calculatePrayerTimes(
        date: LocalDate,
        location: Location
    ): PrayerTimes {
        val coordinates = Coordinates(location.latitude, location.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = getCalculationParameters(location)

        val adhanTimes = AdhanPrayerTimes(coordinates, dateComponents, parameters)
        val timeZone = TimeZone.of(location.timezone)

        return PrayerTimes(
            fajr = adhanTimes.fajr.toJavaLocalDateTime(timeZone),
            sunrise = adhanTimes.sunrise.toJavaLocalDateTime(timeZone),
            dhuhr = adhanTimes.dhuhr.toJavaLocalDateTime(timeZone),
            asr = adhanTimes.asr.toJavaLocalDateTime(timeZone),
            maghrib = adhanTimes.maghrib.toJavaLocalDateTime(timeZone),
            isha = adhanTimes.isha.toJavaLocalDateTime(timeZone),
            date = date,
            location = location
        )
    }

    fun calculatePrayerTimesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        location: Location
    ): List<PrayerTimes> {
        val result = mutableListOf<PrayerTimes>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            result.add(calculatePrayerTimes(currentDate, location))
            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private fun getCalculationParameters(location: Location): CalculationParameters {
        val method = when (location.calculationMethod) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
            CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
            CalculationMethod.KARACHI -> AdhanMethod.KARACHI
            CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
            CalculationMethod.DUBAI -> AdhanMethod.DUBAI
            CalculationMethod.MOON_SIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
            CalculationMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
            CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT
            CalculationMethod.QATAR -> AdhanMethod.QATAR
            CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE
            CalculationMethod.TURKEY -> AdhanMethod.TURKEY
        }

        val parameters = method.parameters.copy(
            madhab = when (location.asrCalculation) {
                AsrCalculation.STANDARD -> Madhab.SHAFI
                AsrCalculation.HANAFI -> Madhab.HANAFI
            }
        )

        // Apply high latitude rule if set
        val highLatRule = location.highLatitudeRule?.let {
            when (it) {
                HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
                HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
                HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
            }
        }

        return if (highLatRule != null) {
            parameters.copy(highLatitudeRule = highLatRule)
        } else {
            parameters
        }
    }

    private fun Instant.toJavaLocalDateTime(timeZone: TimeZone): LocalDateTime {
        val kotlinLocalDateTime = this.toKotlinLocalDateTime(timeZone)
        return LocalDateTime.of(
            kotlinLocalDateTime.year,
            kotlinLocalDateTime.monthNumber,
            kotlinLocalDateTime.dayOfMonth,
            kotlinLocalDateTime.hour,
            kotlinLocalDateTime.minute,
            kotlinLocalDateTime.second,
            kotlinLocalDateTime.nanosecond
        )
    }
}
