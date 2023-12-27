package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.libs.prayertimes.utils.AstronomicalUtils.interpolate
import com.arshadshah.nimaz.libs.prayertimes.utils.AstronomicalUtils.interpolateAngles
import com.arshadshah.nimaz.libs.prayertimes.utils.DoubleUtil
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Astronomical equations
 */
internal object Astronomical {

    /**
     * The geometric mean longitude of the sun in degrees.
     *
     * @param julianCentury the julian century
     * @return the geometric longitude of the sun in degrees
     */
    @JvmStatic
    fun meanSolarLongitude(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 163 */
        val term1 = 280.4664567
        val term2 = 36000.76983 * julianCentury
        val term3 = 0.0003032 * julianCentury.pow(2.0)
        val result = term1 + term2 + term3
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * The geometric mean longitude of the moon in degrees
     *
     * @param julianCentury the julian century
     * @return the geometric mean longitude of the moon in degrees
     */
    @JvmStatic
    fun meanLunarLongitude(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 144 */
        val term1 = 218.3165
        val term2 = 481267.8813 * julianCentury
        val result = term1 + term2
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * The apparent longitude of the Sun, referred to the true equinox of the date.
     *
     * @param julianCentury  the julian century
     * @param meanLongitude the mean longitude
     * @return the true equinox of the date
     */
    @JvmStatic
    fun apparentSolarLongitude(julianCentury: Double, meanLongitude: Double): Double {
        /* Equation from Astronomical Algorithms page 164 */
        val longitude =
            meanLongitude + solarEquationOfTheCenter(
                julianCentury,
                meanSolarAnomaly(
                    julianCentury
                )
            )
        val term1 = 125.04 - 1934.136 * julianCentury
        val result = longitude - 0.00569 - 0.00478 * sin(Math.toRadians(term1))
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * The ascending lunar node longitude
     *
     * @param julianCentury the julian century
     * @return the ascending lunar node longitude
     */
    @JvmStatic
    fun ascendingLunarNodeLongitude(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 144 */
        val term1 = 125.04452
        val term2 = 1934.136261 * julianCentury
        val term3 = 0.0020708 * julianCentury.pow(2.0)
        val term4 = julianCentury.pow(3.0) / 450000
        val result = term1 - term2 + term3 + term4
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * The mean anomaly of the sun
     *
     * @param julianCentury the julian century
     * @return the mean solar anomaly
     */
    private fun meanSolarAnomaly(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 163 */
        val term1 = 357.52911
        val term2 = 35999.05029 * julianCentury
        val term3 = 0.0001537 * julianCentury.pow(2.0)
        val result = term1 + term2 - term3
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * The Sun's equation of the center in degrees.
     *
     * @param julianCentury the julian century
     * @param meanSolarAnamoly the mean anomaly
     * @return the sun's equation of the center in degrees
     */
    private fun solarEquationOfTheCenter(julianCentury: Double, meanSolarAnamoly: Double): Double {
        /* Equation from Astronomical Algorithms page 164 */
        val meanSolarAnamolyInRadians = Math.toRadians(meanSolarAnamoly)
        val term1 =
            (1.914602 - (0.004817 * julianCentury) - (0.000014 * julianCentury.pow(2.0))) * sin(
                meanSolarAnamolyInRadians
            )
        val term2 = (0.019993 - (0.000101 * julianCentury)) * sin(2 * meanSolarAnamolyInRadians)
        val term3 = 0.000289 * sin(3 * meanSolarAnamolyInRadians)
        return term1 + term2 + term3
    }

    /**
     * The mean obliquity of the ecliptic in degrees formula adopted by the
     * International Astronomical Union.
     *
     * @param julianCentury the julian century
     * @return the mean obliquity of the ecliptic in degrees
     */
    @JvmStatic
    fun meanObliquityOfTheEcliptic(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 147 */
        val term1 = 23.439291
        val term2 = 0.013004167 * julianCentury
        val term3 = 0.0000001639 * julianCentury.pow(2.0)
        val term4 = 0.0000005036 * julianCentury.pow(3.0)
        return term1 - term2 - term3 + term4
    }

    /**
     * The mean obliquity of the ecliptic, corrected for calculating the apparent
     * position of the sun, in degrees.
     *
     * @param julianCentury  the julian century
     * @param meanEclipticObliquity the mean obliquity of the ecliptic
     * @return the corrected mean obliquity of the ecliptic in degrees
     */
    @JvmStatic
    fun apparentObliquityOfTheEcliptic(
        julianCentury: Double,
        meanEclipticObliquity: Double
    ): Double {
        /* Equation from Astronomical Algorithms page 165 */
        val term1 = 125.04 - 1934.136 * julianCentury
        return meanEclipticObliquity + 0.00256 * cos(Math.toRadians(term1))
    }

    /**
     * Mean sidereal time, the hour angle of the vernal equinox, in degrees.
     *
     * @param julianCentury the julian century
     * @return the mean sidereal time in degrees
     */
    @JvmStatic
    fun meanSiderealTime(julianCentury: Double): Double {
        /* Equation from Astronomical Algorithms page 165 */
        val jd = julianCentury * 36525 + 2451545.0
        val term1 = 280.46061837
        val term2 = 360.98564736629 * (jd - 2451545)
        val term3 = 0.000387933 * julianCentury.pow(2.0)
        val term4 = julianCentury.pow(3.0) / 38710000
        val result = term1 + term2 + term3 - term4
        return DoubleUtil.unwindAngle(
            result
        )
    }

    /**
     * Get the nutation in longitude
     *
     * @param solarlongitude the solar longitude
     * @param lunarLongitude the lunar longitude
     * @param ascendingNode  the ascending node
     * @return the nutation in longitude
     */
    @JvmStatic
    fun nutationInLongitude(
        solarlongitude: Double,
        lunarLongitude: Double,
        ascendingNode: Double
    ): Double {
        /* Equation from Astronomical Algorithms page 144 */
        val term1 = -17.2 / 3600 * sin(Math.toRadians(ascendingNode))
        val term2 = 1.32 / 3600 * sin(2 * Math.toRadians(solarlongitude))
        val term3 = 0.23 / 3600 * sin(2 * Math.toRadians(lunarLongitude))
        val term4 = 0.21 / 3600 * sin(2 * Math.toRadians(ascendingNode))
        return term1 - term2 - term3 + term4
    }

    /**
     * Get the nutation in obliquity
     *
     * @param solarlongitude the solar longitude
     * @param lunarLongitude the lunar longitude
     * @param ascendingNode  the ascending node
     * @return the nutation in obliquity
     */
    @JvmStatic
    fun nutationInObliquity(
        solarlongitude: Double,
        lunarLongitude: Double,
        ascendingNode: Double
    ): Double {
        /* Equation from Astronomical Algorithms page 144 */
        val term1 = 9.2 / 3600 * cos(Math.toRadians(ascendingNode))
        val term2 = 0.57 / 3600 * cos(2 * Math.toRadians(solarlongitude))
        val term3 = 0.10 / 3600 * cos(2 * Math.toRadians(lunarLongitude))
        val term4 = 0.09 / 3600 * cos(2 * Math.toRadians(ascendingNode))
        return term1 + term2 + term3 - term4
    }

    /**
     * Return the altitude of the celestial body
     *
     * @param observerLatitude the observer latitude
     * @param declination the declination
     * @param localHourAngle the local hour angle
     * @return the altitude of the celestial body
     */
    private fun altitudeOfCelestialBody(
        observerLatitude: Double,
        declination: Double,
        localHourAngle: Double
    ): Double {
        /* Equation from Astronomical Algorithms page 93 */
        val term1 = sin(Math.toRadians(observerLatitude)) * sin(Math.toRadians(declination))
        val term2 =
            cos(Math.toRadians(observerLatitude)) * cos(Math.toRadians(declination)) * cos(
                Math.toRadians(
                    localHourAngle
                )
            )
        return Math.toDegrees(asin(term1 + term2))
    }

    /**
     * Return the approximate transit
     *
     * @param longitude  the longitude
     * @param sideRealTime the sidereal time
     * @param rightAscension the right ascension
     * @return the approximate transite
     */
    @JvmStatic
    fun approximateTransit(
        longitude: Double,
        sideRealTime: Double,
        rightAscension: Double
    ): Double {
        /* Equation from page Astronomical Algorithms 102 */
        val longitudeW = longitude * -1
        return DoubleUtil.normalizeWithBound(
            (rightAscension + longitudeW - sideRealTime) / 360,
            1.0
        )
    }

    /**
     * The time at which the sun is at its highest point in the sky (in universal
     * time)
     *
     * @param approximateTransit approximate transit of the sun
     * @param longitude  the longitude
     * @param sideRealTime the sidereal time
     * @param rightAscension the right ascension
     * @param prevRightAscension the previous right ascension
     * @param nextRightAscension the next right ascension
     * @return the time (in universal time) when the sun is at its highest point in
     * the sky
     */
    @JvmStatic
    fun correctedTransit(
        approximateTransit: Double,
        longitude: Double,
        sideRealTime: Double,
        rightAscension: Double,
        prevRightAscension: Double,
        nextRightAscension: Double
    ): Double {
        /* Equation from page Astronomical Algorithms 102 */
        val longitudeW = longitude * -1
        val theta =
            DoubleUtil.unwindAngle(
                sideRealTime + 360.985647 * approximateTransit
            )
        val alpha = DoubleUtil
            .unwindAngle(
                interpolateAngles( /* value */rightAscension,  /* previousValue */
                    prevRightAscension,  /* nextValue */
                    nextRightAscension,  /* factor */
                    approximateTransit
                )
            )
        val closestAngle =
            DoubleUtil.closestAngle(theta - longitudeW - alpha)
        val deltaM = closestAngle / -360
        return (approximateTransit + deltaM) * 24
    }

    /**
     * Get the corrected hour angle
     *
     * @param approximateTransit    the approximate transit
     * @param angle                 the angle to correct
     * @param coordinates           the coordinates
     * @param afterTransit          whether it's after transit
     * @param sideRealTime          the sidereal time
     * @param rightAscension        the right ascension
     * @param prevRightAscension    the previous right ascension
     * @param nextRightAscension    the next right ascension
     * @param declination           the declination
     * @param prevDeclination       the previous declination
     * @param nextDeclination       the next declination
     * @return the corrected hour angle
     */
    @JvmStatic
    fun correctedHourAngle(
        approximateTransit: Double,
        angle: Double,
        coordinates: Coordinates,
        afterTransit: Boolean,
        sideRealTime: Double,
        rightAscension: Double,
        prevRightAscension: Double,
        nextRightAscension: Double,
        declination: Double,
        prevDeclination: Double,
        nextDeclination: Double
    ): Double {
        /* Equation from page Astronomical Algorithms 102 */
        val longitudeW = coordinates.longitude * -1
        val term1 = (sin(Math.toRadians(angle))
                - sin(Math.toRadians(coordinates.latitude)) * sin(Math.toRadians(declination)))
        val term2 = cos(Math.toRadians(coordinates.latitude)) * cos(Math.toRadians(declination))
        val hInDegrees = Math.toDegrees(acos(term1 / term2))
        val factor =
            if (afterTransit) approximateTransit + hInDegrees / 360 else approximateTransit - hInDegrees / 360
        val theta =
            DoubleUtil.unwindAngle(
                sideRealTime + 360.985647 * factor
            )
        val alpha = DoubleUtil
            .unwindAngle(
                interpolateAngles( /* value */rightAscension,  /* previousValue */
                    prevRightAscension,  /* nextValue */
                    nextRightAscension,  /* factor */
                    factor
                )
            )
        val sigma =
            interpolate( /* value */declination,  /* previousValue */
                prevDeclination,  /* nextValue */
                nextDeclination,  /* factor */
                factor
            )
        val localHourAngle = theta - longitudeW - alpha
        val celestialBodyAltitude =
            altitudeOfCelestialBody(
                /* observerLatitude */coordinates.latitude,
                /* declination */sigma,
                /* localHourAngle */localHourAngle
            )
        val term3 = celestialBodyAltitude - angle
        val term4 =
            (360 * cos(Math.toRadians(sigma)) * cos(Math.toRadians(coordinates.latitude))
                    * sin(Math.toRadians(localHourAngle)))
        val result = term3 / term4
        return (factor + result) * 24
    }
}