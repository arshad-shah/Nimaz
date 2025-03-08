package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.utils.JulianUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SolarCoordinatesTest {

    //julian century for 2019-01-01
    private val julianDate = JulianUtils.calculateJulianDay(2019, 1, 1)
    private val julianCentury = JulianUtils.calculateJulianCentury(julianDate)

    @Test
    fun `test solar coordinates`() {
        val solarCoordinates =
            SolarCoordinates(julianCentury)

        assertEquals(-23.039268966948026, solarCoordinates.declination, 0.0001)
        assertEquals(281.15762543849394, solarCoordinates.rightAscension, 0.0001)
        assertEquals(100.35666441936172, solarCoordinates.apparentSiderealTime, 0.0001)
    }
}