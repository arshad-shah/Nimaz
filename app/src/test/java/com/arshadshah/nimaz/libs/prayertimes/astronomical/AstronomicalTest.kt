package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.utils.DoubleUtil.unwindAngle
import com.arshadshah.nimaz.libs.prayertimes.utils.JulianUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.Math.*

class AstronomicalTest {
    private val julianDay = JulianUtils.calculateJulianDay(2019, 1, 1)
    private val julianCentury = JulianUtils.calculateJulianCentury(julianDay)
    private val longitude = -6.2597  // Dublin longitude

    @Test
    fun verifyMeanSolarLongitude() {
        val expected = 280.3663235046888
        val actual = Astronomical.meanSolarLongitude(julianCentury)
        assertEquals(expected, actual, 0.0001)
    }


    @Test
    fun meanLunarLongitude() {
        assertEquals(215.91984788089758, Astronomical.meanLunarLongitude(julianCentury), 0.0001)
    }

    @Test
    fun apparentSolarLongitude() {
        //get the value of mean solar longitude
        val meanSolarLongitude = Astronomical.meanSolarLongitude(julianCentury)
        assertEquals(280.2575919537504, Astronomical.apparentSolarLongitude(julianCentury, meanSolarLongitude), 0.0001)
    }

    @Test
    fun ascendingLunarNodeLongitude() {
        assertEquals(117.57194361694613, Astronomical.ascendingLunarNodeLongitude(julianCentury), 0.0001)
    }

    @Test
    fun meanObliquityOfTheEcliptic() {
        assertEquals(23.43682029481613, Astronomical.meanObliquityOfTheEcliptic(julianCentury), 0.0001)
    }

    @Test
    fun apparentObliquityOfTheEcliptic() {
        //get the value of mean obliquity of the ecliptic
        val meanObliquityOfTheEcliptic = Astronomical.meanObliquityOfTheEcliptic(julianCentury)
        assertEquals(
            23.43563554804811,
            Astronomical.apparentObliquityOfTheEcliptic(julianCentury, meanObliquityOfTheEcliptic),
            0.0001
        )
    }

    @Test
    fun meanSiderealTime() {
        assertEquals(100.36053074290976, Astronomical.meanSiderealTime(julianCentury), 0.0001)
    }

    @Test
    fun nutationInLongitude() {
        //get the value of mean solar longitude
        val meanSolarLongitude = Astronomical.meanSolarLongitude(julianCentury)
        //get the value of mean lunar longitude
        val meanLunarLongitude = Astronomical.meanLunarLongitude(julianCentury)
        //get the value of ascending lunar node longitude
        val ascendingLunarNodeLongitude = Astronomical.ascendingLunarNodeLongitude(julianCentury)
        assertEquals(
            -0.0042139385232168374,
            Astronomical.nutationInLongitude(meanSolarLongitude, meanLunarLongitude, ascendingLunarNodeLongitude),
            0.0001
        )
    }

    @Test
    fun nutationInObliquity() {
        //get the value of mean solar longitude
        val meanSolarLongitude = Astronomical.meanSolarLongitude(julianCentury)
        //get the value of mean lunar longitude
        val meanLunarLongitude = Astronomical.meanLunarLongitude(julianCentury)
        //get the value of ascending lunar node longitude
        val ascendingLunarNodeLongitude = Astronomical.ascendingLunarNodeLongitude(julianCentury)
        assertEquals(
            -0.0013080040587717839,
            Astronomical.nutationInObliquity(meanSolarLongitude, meanLunarLongitude, ascendingLunarNodeLongitude),
            0.0001
        )
    }

    @Test
    fun approximateTransit() {
        //get the value of mean solar longitude
        val meanSolarLongitude = Astronomical.meanSolarLongitude(julianCentury)
        //get the value of mean sidereal time
        val meanSiderealTime = Astronomical.meanSiderealTime(julianCentury)
        //get the value of right ascension
        val rightAscension = unwindAngle(
            toDegrees(
                kotlin.math.atan2(
                    kotlin.math.cos(
                        toRadians(
                            Astronomical.apparentObliquityOfTheEcliptic(
                                julianCentury,
                                Astronomical.meanObliquityOfTheEcliptic(julianCentury)
                            )
                        )
                    ) * sin(
                        toRadians(
                            Astronomical.apparentSolarLongitude(
                                julianCentury,
                                meanSolarLongitude
                            )
                        )
                    ),
                    cos(
                        toRadians(
                            Astronomical.apparentSolarLongitude(
                                julianCentury,
                                meanSolarLongitude
                            )
                        )
                    )
                )
            )
        )
        assertEquals(
            0.5196022074877339,
            Astronomical.approximateTransit(longitude, meanSiderealTime, rightAscension),
            0.0001
        )
    }

    @Test
    fun correctedTransit() {
        //get the value of mean solar longitude
        val meanSolarLongitude = Astronomical.meanSolarLongitude(julianCentury)
        //get the value of mean sidereal time
        val meanSiderealTime = Astronomical.meanSiderealTime(julianCentury)
        //get the value of right ascension
        val rightAscension = unwindAngle(
            toDegrees(
                atan2(
                    cos(
                        toRadians(
                            Astronomical.apparentObliquityOfTheEcliptic(
                                julianCentury,
                                Astronomical.meanObliquityOfTheEcliptic(julianCentury)
                            )
                        )
                    ) * sin(
                        toRadians(Astronomical.apparentSolarLongitude(julianCentury, meanSolarLongitude))
                    ), cos(toRadians(Astronomical.apparentSolarLongitude(julianCentury, meanSolarLongitude)))
                )
            )
        )
        //get the value of approximate transit
        val approximateTransit = Astronomical.approximateTransit(meanSolarLongitude, meanSiderealTime, rightAscension)
        //get the value of previous right ascension
        val prevRightAscension = unwindAngle(
            toDegrees(
                atan2(
                    cos(
                        toRadians(
                            Astronomical.apparentObliquityOfTheEcliptic(
                                julianCentury,
                                Astronomical.meanObliquityOfTheEcliptic(julianCentury)
                            )
                        )
                    ) * sin(
                        toRadians(
                            Astronomical.apparentSolarLongitude(
                                julianCentury,
                                meanSolarLongitude - 1
                            )
                        )
                    ), cos(toRadians(Astronomical.apparentSolarLongitude(julianCentury, meanSolarLongitude - 1)))
                )
            )
        )
        //get the value of next right ascension
        val nextRightAscension = unwindAngle(
            toDegrees(
                atan2(
                    cos(
                        toRadians(
                            Astronomical.apparentObliquityOfTheEcliptic(
                                julianCentury,
                                Astronomical.meanObliquityOfTheEcliptic(julianCentury)
                            )
                        )
                    ) * sin(
                        toRadians(
                            Astronomical.apparentSolarLongitude(
                                julianCentury,
                                meanSolarLongitude + 1
                            )
                        )
                    ), cos(toRadians(Astronomical.apparentSolarLongitude(julianCentury, meanSolarLongitude + 1)))
                )
            )
        )
        assertEquals(
            12.47514749813685,
            Astronomical.correctedTransit(
                approximateTransit,
                longitude,
                meanSiderealTime,
                rightAscension,
                prevRightAscension,
                nextRightAscension
            ),
            0.0001
        )
    }
}