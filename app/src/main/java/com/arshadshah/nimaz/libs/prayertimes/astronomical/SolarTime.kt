package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.approximateTransit
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.correctedHourAngle
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.correctedTransit
import com.arshadshah.nimaz.libs.prayertimes.enums.ShadowLength
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.libs.prayertimes.utils.JulianUtils.calculateJulianCentury
import com.arshadshah.nimaz.libs.prayertimes.utils.JulianUtils.calculateJulianDay
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.tan

class SolarTime(
    today: LocalDateTime,
    coordinates: Coordinates
) {
    @JvmField
    val transit: Double

    @JvmField
    val sunrise: Double

    @JvmField
    val sunset: Double
    private val observer: Coordinates
    private val solar: SolarCoordinates
    private val prevSolar: SolarCoordinates
    private val nextSolar: SolarCoordinates
    private val approximateTransit: Double

    // get the year, month, day from today date given in parameter
    private val year: Int = today.year
    private val month: Int = today.monthValue
    private val day: Int = today.dayOfMonth


    fun hourAngle(angle: Double, afterTransit: Boolean): Double {
        return correctedHourAngle(
            approximateTransit,
            angle,
            observer,
            afterTransit,
            solar.apparentSiderealTime,
            solar.rightAscension,
            prevSolar.rightAscension,
            nextSolar.rightAscension,
            solar.declination,
            prevSolar.declination,
            nextSolar.declination
        )
    }

    fun afternoon(shadowLength: ShadowLength): Double {
        val tangent = abs(observer.latitude - solar.declination)
        val inverse = shadowLength.shadowLength + tan(Math.toRadians(tangent))
        val angle = Math.toDegrees(atan(1.0 / inverse))
        return hourAngle(angle, true)
    }

    init {
        val julianDay = calculateJulianDay(year, month, day)
        val julianCentury = calculateJulianCentury(julianDay)
        val julianCenturyPrev = calculateJulianCentury(julianDay - 1)
        val julianCenturyNext = calculateJulianCentury(julianDay + 1)
        prevSolar = SolarCoordinates(julianCenturyPrev)
        solar = SolarCoordinates(julianCentury)
        nextSolar = SolarCoordinates(julianCenturyNext)
        approximateTransit = approximateTransit(
            coordinates.longitude, solar.apparentSiderealTime,
            solar.rightAscension
        )
        val solarAltitude = -50.0 / 60.0
        observer = coordinates
        transit = correctedTransit(
            approximateTransit,
            coordinates.longitude,
            solar.apparentSiderealTime,
            solar.rightAscension,
            prevSolar.rightAscension,
            nextSolar.rightAscension
        )
        sunrise = correctedHourAngle(
            approximateTransit,
            solarAltitude,
            coordinates,
            false,
            solar.apparentSiderealTime,
            solar.rightAscension,
            prevSolar.rightAscension,
            nextSolar.rightAscension,
            solar.declination,
            prevSolar.declination,
            nextSolar.declination
        )
        sunset = correctedHourAngle(
            approximateTransit,
            solarAltitude,
            coordinates,
            true,
            solar.apparentSiderealTime,
            solar.rightAscension,
            prevSolar.rightAscension,
            nextSolar.rightAscension,
            solar.declination,
            prevSolar.declination,
            nextSolar.declination
        )
    }
}