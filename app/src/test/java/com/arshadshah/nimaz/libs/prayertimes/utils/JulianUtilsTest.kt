package com.arshadshah.nimaz.libs.prayertimes.utils

import org.junit.Assert.*
import org.junit.Test

internal class JulianUtilsTest {

    @Test
    fun calculateJulianDay() {
        //test it using 10 different dates
        val julianDay = JulianUtils.calculateJulianDay(2019, 1, 1)
        assertEquals(2458484.5, julianDay, 0.0001)

        val julianDay2 = JulianUtils.calculateJulianDay(2019, 2, 1)
        assertEquals(2458515.5, julianDay2, 0.0001)

        val julianDay3 = JulianUtils.calculateJulianDay(2019, 3, 1)
        assertEquals(2458543.5, julianDay3, 0.0001)

        val julianDay4 = JulianUtils.calculateJulianDay(2019, 4, 1)
        assertEquals(2458574.5, julianDay4, 0.0001)

        val julianDay5 = JulianUtils.calculateJulianDay(2019, 5, 1)
        assertEquals(2458604.5, julianDay5, 0.0001)

        val julianDay6 = JulianUtils.calculateJulianDay(2019, 6, 1)
        assertEquals(2458635.5, julianDay6, 0.0001)

        val julianDay7 = JulianUtils.calculateJulianDay(2019, 7, 1)
        assertEquals(2458665.5, julianDay7, 0.0001)

        val julianDay8 = JulianUtils.calculateJulianDay(2019, 8, 1)
        assertEquals(2458696.5, julianDay8, 0.0001)

        val julianDay9 = JulianUtils.calculateJulianDay(2019, 9, 1)
        assertEquals(2458727.5, julianDay9, 0.0001)

        val julianDay10 = JulianUtils.calculateJulianDay(2019, 10, 1)
        assertEquals(2458757.5, julianDay10, 0.0001)
    }

    @Test
    fun calculateJulianCentury() {
        //get julian day
        val julianDay = JulianUtils.calculateJulianDay(2019, 1, 1)
        val julianCentury = JulianUtils.calculateJulianCentury(julianDay)
        assertEquals(0.18999315537303216, julianCentury, 0.0001)

        val julianDay2 = JulianUtils.calculateJulianDay(2019, 2, 1)
        val julianCentury2 = JulianUtils.calculateJulianCentury(julianDay2)
        assertEquals(0.1908418891170431, julianCentury2, 0.0001)

        val julianDay3 = JulianUtils.calculateJulianDay(2019, 3, 1)
        val julianCentury3 = JulianUtils.calculateJulianCentury(julianDay3)
        assertEquals(0.1916084873374401, julianCentury3, 0.0001)

        val julianDay4 = JulianUtils.calculateJulianDay(2019, 4, 1)
        val julianCentury4 = JulianUtils.calculateJulianCentury(julianDay4)
        assertEquals(0.19245722108145105, julianCentury4, 0.0001)
    }
}