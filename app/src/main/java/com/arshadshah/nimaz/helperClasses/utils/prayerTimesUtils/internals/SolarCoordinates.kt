package com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals

import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.apparentObliquityOfTheEcliptic
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.apparentSolarLongitude
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.ascendingLunarNodeLongitude
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.meanLunarLongitude
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.meanObliquityOfTheEcliptic
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.meanSiderealTime
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.meanSolarLongitude
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.nutationInLongitude
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.Astronomical.nutationInObliquity
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.CalendricalHelper.julianCentury
import com.arshadshah.nimaz.helperClasses.utils.prayerTimesUtils.internals.DoubleUtil.unwindAngle
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal class SolarCoordinates(julianDay: Double) {

    /**
     * The declination of the sun, the angle between the rays of the Sun and the
     * plane of the Earth's equator, in degrees.
     */
    @JvmField
    val declination: Double

    /**
     * Right ascension of the Sun, the angular distance on the celestial equator
     * from the vernal equinox to the hour circle, in degrees.
     */
    @JvmField
    val rightAscension: Double

    /**
     * Apparent sidereal time, the hour angle of the vernal equinox, in degrees.
     */
    @JvmField
    val apparentSiderealTime: Double

    init {
        val julianCentury = julianCentury(julianDay)
        val meanSolarLongitude = meanSolarLongitude( /* julianCentury */julianCentury)
        val meanLunarLongitude = meanLunarLongitude( /* julianCentury */julianCentury)
        val ascendingLunarNodeLongitude =
            ascendingLunarNodeLongitude( /* julianCentury */julianCentury)
        val apparentSolarLongitudeRadians = Math.toRadians(
            apparentSolarLongitude( /* julianCentury */julianCentury,  /* meanLongitude */
                meanSolarLongitude
            )
        )
        val meanSiderealTime = meanSiderealTime( /* julianCentury */julianCentury)
        val nutationInLongitude = nutationInLongitude(/* solarLongitude */
            meanSolarLongitude,  /* lunarLongitude */
            meanLunarLongitude,  /* ascendingNode */
            ascendingLunarNodeLongitude
        )
        val nutationInObliquity = nutationInObliquity(
            /* solarLongitude */
            meanSolarLongitude,  /* lunarLongitude */
            meanLunarLongitude,  /* ascendingNode */
            ascendingLunarNodeLongitude
        )
        val meanObliquityOfTheEcliptic =
            meanObliquityOfTheEcliptic( /* julianCentury */julianCentury)
        val apparentObliquityOfTheEclipticRadians = Math.toRadians(
            apparentObliquityOfTheEcliptic( /* julianCentury */julianCentury,  /* meanObliquityOfTheEcliptic */
                meanObliquityOfTheEcliptic
            )
        )

        /* Equation from Astronomical Algorithms page 165 */
        declination = Math.toDegrees(
            asin(sin(apparentObliquityOfTheEclipticRadians) * sin(apparentSolarLongitudeRadians))
        )

        /* Equation from Astronomical Algorithms page 165 */
        rightAscension = unwindAngle(
            Math.toDegrees(
                atan2(
                    cos(apparentObliquityOfTheEclipticRadians) * sin(
                        apparentSolarLongitudeRadians
                    ), cos(apparentSolarLongitudeRadians)
                )
            )
        )

        /* Equation from Astronomical Algorithms page 88 */
        apparentSiderealTime =
            meanSiderealTime + nutationInLongitude * 3600 * cos(
                Math.toRadians(meanObliquityOfTheEcliptic + nutationInObliquity)
            ) / 3600
    }
}