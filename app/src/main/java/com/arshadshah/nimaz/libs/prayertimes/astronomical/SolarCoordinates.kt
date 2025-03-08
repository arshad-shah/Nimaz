package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.apparentObliquityOfTheEcliptic
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.apparentSolarLongitude
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.ascendingLunarNodeLongitude
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.meanLunarLongitude
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.meanObliquityOfTheEcliptic
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.meanSiderealTime
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.meanSolarLongitude
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.nutationInLongitude
import com.arshadshah.nimaz.libs.prayertimes.astronomical.Astronomical.nutationInObliquity
import com.arshadshah.nimaz.libs.prayertimes.utils.DoubleUtil.unwindAngle
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class SolarCoordinates(julianCentury: Double) {
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
        val meanSolarLongitude = meanSolarLongitude(julianCentury)
        val meanLunarLongitude = meanLunarLongitude(julianCentury)
        val ascendingLunarNodeLongitude = ascendingLunarNodeLongitude(julianCentury)
        val apparentSolarLongitudeRadians = Math.toRadians(
            apparentSolarLongitude(julianCentury, meanSolarLongitude)
        )
        val meanSiderealTime = meanSiderealTime(julianCentury)
        val nutationInLongitude = nutationInLongitude(
            meanSolarLongitude,
            meanLunarLongitude,
            ascendingLunarNodeLongitude
        )
        val nutationInObliquity = nutationInObliquity(
            meanSolarLongitude,
            meanLunarLongitude,
            ascendingLunarNodeLongitude
        )
        val meanObliquityOfTheEcliptic = meanObliquityOfTheEcliptic(julianCentury)
        val apparentObliquityOfTheEclipticRadians = Math.toRadians(
            apparentObliquityOfTheEcliptic(
                julianCentury,
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