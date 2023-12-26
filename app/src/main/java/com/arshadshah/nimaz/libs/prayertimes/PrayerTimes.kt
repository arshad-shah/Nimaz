package com.arshadshah.nimaz.libs.prayertimes

import com.arshadshah.nimaz.libs.prayertimes.astronomical.SolarTime
import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.CalculationParameters
import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.SeasonalAdjustments
import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.Prayer
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.libs.prayertimes.utils.CalenderUtils
import com.arshadshah.nimaz.libs.prayertimes.utils.ParametersUtils.getShadowLengthForMadhab
import com.arshadshah.nimaz.libs.prayertimes.utils.TimeUtils.Companion.fromDouble
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrayerTimesCalculated(
    coordinates: Coordinates,
    date: LocalDateTime,
    calculationParameters: CalculationParameters
) {

    private var fajr: LocalDateTime? = null
    private var sunrise: LocalDateTime? = null
    private var dhuhr: LocalDateTime? = null
    private var asr: LocalDateTime? = null
    private var maghrib: LocalDateTime? = null
    private var isha: LocalDateTime? = null

    private val calenderUtils =
        CalenderUtils()
    private val seasonalAdjustments =
        SeasonalAdjustments()


    //separate the date parameter into day,month,year
    private val day: Int = date.dayOfMonth
    private val month: Int = date.monthValue
    private val year: Int = date.year

    fun timeForPrayer(prayer: Prayer?): LocalDateTime? {
        return when (prayer) {
            Prayer.FAJR -> fajr
            Prayer.SUNRISE -> sunrise
            Prayer.DHUHR -> dhuhr
            Prayer.ASR -> asr
            Prayer.MAGHRIB -> maghrib
            Prayer.ISHA -> isha
            Prayer.NONE -> null
            else -> null
        }
    }

    init {
        var tempFajr: LocalDateTime? = null
        var tempSunrise: LocalDateTime? = null
        var tempDhuhr: LocalDateTime? = null
        var tempAsr: LocalDateTime? = null
        var tempMaghrib: LocalDateTime? = null
        var tempIsha: LocalDateTime? = null
        val prayerDate = calenderUtils.resolveTime(year, month, day)
        val dayOfYear = date.dayOfYear
        val tomorrow = prayerDate.plusDays(1)
        val solarTime = SolarTime(
            date,
            coordinates
        )

        var timeComponents = fromDouble(solarTime.transit)
        val transit = timeComponents?.date(date)
        timeComponents = fromDouble(solarTime.sunrise)
        val sunriseComponents = timeComponents?.date(
            date
        )
        timeComponents = fromDouble(solarTime.sunset)
        val sunsetComponents = timeComponents?.date(
            date
        )
        val tomorrowSolarTime =
            SolarTime(
                tomorrow,
                coordinates
            )
        val tomorrowSunriseComponents = fromDouble(tomorrowSolarTime.sunrise)
        val error =
            transit == null || sunriseComponents == null || sunsetComponents == null || tomorrowSunriseComponents == null
        if (!error) {
            tempDhuhr = transit
            tempSunrise = sunriseComponents
            tempMaghrib = sunsetComponents
            timeComponents =
                fromDouble(solarTime.afternoon(getShadowLengthForMadhab(calculationParameters.madhab)))
            if (timeComponents != null) {
                tempAsr = timeComponents.date(date)
            }

            // get night length
            val tomorrowSunrise = tomorrowSunriseComponents!!.date(tomorrow)
            val night =
                calenderUtils.toMillis(tomorrowSunrise) - calenderUtils.toMillis(sunsetComponents!!)
            timeComponents =
                fromDouble(solarTime.hourAngle(-calculationParameters.fajrAngle, false))
            if (timeComponents != null) {
                tempFajr = timeComponents.date(date)
            }
            if (calculationParameters.method === CalculationMethod.MOONSIGHTING && coordinates.latitude >= 55) {
                tempFajr = sunriseComponents!!.plus(
                    (-1 * (night / 7000).toInt()).toLong(),
                    ChronoUnit.SECONDS
                )
            }
            val nightPortions = calculationParameters.nightPortions()
            val safeFajr: LocalDateTime =
                if (calculationParameters.method === CalculationMethod.MOONSIGHTING) {
                    seasonalAdjustments.seasonAdjustedMorningTwilight(
                        coordinates.latitude,
                        dayOfYear,
                        year,
                        sunriseComponents
                    )
                } else {
                    val portion = nightPortions.fajr
                    val nightFraction = (portion * night / 1000).toLong()
                    sunriseComponents!!.plus(
                        (-1 * nightFraction.toInt()).toLong(),
                        ChronoUnit.SECONDS
                    )
                }
            if (tempFajr == null || tempFajr.isBefore(safeFajr)) {
                tempFajr = safeFajr
            }

            // Isha calculation with check against safe value
            if (calculationParameters.ishaInterval > 0) {
                tempIsha =
                    tempMaghrib!!.plus(
                        (calculationParameters.ishaInterval * 60).toLong(),
                        ChronoUnit.SECONDS
                    )
            } else {
                timeComponents =
                    fromDouble(solarTime.hourAngle(-calculationParameters.ishaAngle, true))
                if (timeComponents != null) {
                    tempIsha = timeComponents.date(date)
                }
                if (calculationParameters.method === CalculationMethod.MOONSIGHTING && coordinates.latitude >= 55) {
                    val nightFraction = night / 7000
                    tempIsha = sunsetComponents.plus(nightFraction, ChronoUnit.SECONDS)
                }
                val safeIsha: LocalDateTime =
                    if (calculationParameters.method === CalculationMethod.MOONSIGHTING) {
                        seasonalAdjustments.seasonAdjustedEveningTwilight(
                            coordinates.latitude, dayOfYear, year,
                            sunsetComponents
                        )
                    } else {
                        val portion = nightPortions.isha
                        val nightFraction = (portion * night / 1000).toLong()
                        sunsetComponents.plus(nightFraction, ChronoUnit.SECONDS)
                    }
                if (tempIsha == null || tempIsha.isAfter(safeIsha)) {
                    tempIsha = safeIsha
                }
            }
        }
        if (error || tempAsr == null) {
            // if we don't have all prayer times then initialization failed
            fajr = null
            sunrise = null
            dhuhr = null
            asr = null
            maghrib = null
            isha = null
        } else {
            fajr = calenderUtils.roundedMinute(
                tempFajr!!.plus(calculationParameters.adjustments.fajr.toLong(), ChronoUnit.MINUTES)
            )

            sunrise = calenderUtils.roundedMinute(
                tempSunrise!!.plus(
                    calculationParameters.adjustments.sunrise.toLong(),
                    ChronoUnit.MINUTES
                )
            )
            dhuhr = calenderUtils.roundedMinute(
                tempDhuhr!!.plus(
                    calculationParameters.adjustments.dhuhr.toLong(),
                    ChronoUnit.MINUTES
                )
            )
            asr = calenderUtils.roundedMinute(
                tempAsr.plus(calculationParameters.adjustments.asr.toLong(), ChronoUnit.MINUTES)
            )
            maghrib = calenderUtils.roundedMinute(
                tempMaghrib!!.plus(
                    calculationParameters.adjustments.maghrib.toLong(),
                    ChronoUnit.MINUTES
                )
            )
            isha = calenderUtils.roundedMinute(
                tempIsha!!.plus(calculationParameters.adjustments.isha.toLong(), ChronoUnit.MINUTES)
            )
        }
    }


}