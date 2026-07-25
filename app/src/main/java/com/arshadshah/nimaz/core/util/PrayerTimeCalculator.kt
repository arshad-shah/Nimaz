package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.model.PrayerType
import com.batoulapps.adhan2.CalculationParameters
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan2.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan2.SunnahTimes as AdhanSunnahTimes
import kotlinx.datetime.toLocalDateTime as toKotlinLocalDateTime

/** Sunnah night instants (adhan2), UTC [Instant]s in the same units as [PrayerTime.time]. */
data class SunnahTimeInstants(
    val middleOfTheNight: Instant,
    val lastThirdOfTheNight: Instant
)

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

        val method = adhanMethodFor(calculationMethod)

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

    /**
     * The Sunnah night times for [date] (adhan2 `SunnahTimes`): the middle of the night and the
     * start of the last third, computed from this day's Maghrib to the next day's Fajr — so both
     * land in the early hours of the *following* morning. Drives the Tahajjud/Qiyam reminder.
     */
    fun getSunnahTimes(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule: HighLatitudeRule? = null
    ): SunnahTimeInstants {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val parameters = buildParameters(calculationMethod, asrCalculation, highLatitudeRule)
        val sunnah = AdhanSunnahTimes(AdhanPrayerTimes(coordinates, dateComponents, parameters))
        return SunnahTimeInstants(
            middleOfTheNight = sunnah.middleOfTheNight,
            lastThirdOfTheNight = sunnah.lastThirdOfTheNight
        )
    }

    /** Shared adhan2 [CalculationParameters] builder (method + madhab + optional high-lat rule). */
    private fun buildParameters(
        calculationMethod: CalculationMethod,
        asrCalculation: AsrCalculation,
        highLatitudeRule: HighLatitudeRule?
    ): CalculationParameters {
        var parameters = adhanMethodFor(calculationMethod).parameters.copy(
            madhab = when (asrCalculation) {
                AsrCalculation.STANDARD -> Madhab.SHAFI
                AsrCalculation.HANAFI -> Madhab.HANAFI
            }
        )
        highLatitudeRule?.let { rule ->
            parameters = parameters.copy(
                highLatitudeRule = when (rule) {
                    HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
                    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
                    HighLatitudeRule.TWILIGHT_ANGLE -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
                }
            )
        }
        return parameters
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
        val method = adhanMethodFor(location.calculationMethod)

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
        return this.toKotlinLocalDateTime(timeZone).toJavaLocalDateTime()
    }

    /** Single mapping from our [CalculationMethod] to the adhan2 library method. */
    private fun adhanMethodFor(method: CalculationMethod): AdhanMethod = when (method) {
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
}
