package com.arshadshah.nimaz.libs.prayertimes.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class CalenderUtilsTest {

    @Test
    fun isLeapYear() {
        val calenderUtils = CalenderUtils()
        assertTrue(calenderUtils.isLeapYear(2000))
        assertTrue(calenderUtils.isLeapYear(2004))
        assertTrue(calenderUtils.isLeapYear(2008))
        assertTrue(calenderUtils.isLeapYear(2012))
        assertTrue(calenderUtils.isLeapYear(2016))
        assertTrue(calenderUtils.isLeapYear(2020))
        assertTrue(calenderUtils.isLeapYear(2024))
        assertTrue(calenderUtils.isLeapYear(2028))
        assertTrue(calenderUtils.isLeapYear(2032))
        assertTrue(calenderUtils.isLeapYear(2036))
        assertTrue(calenderUtils.isLeapYear(2040))
        assertTrue(calenderUtils.isLeapYear(2044))
        assertTrue(calenderUtils.isLeapYear(2048))
        assertTrue(calenderUtils.isLeapYear(2052))
        assertTrue(calenderUtils.isLeapYear(2056))
        assertTrue(calenderUtils.isLeapYear(2060))
        assertTrue(calenderUtils.isLeapYear(2064))
        assertTrue(calenderUtils.isLeapYear(2068))
        assertTrue(calenderUtils.isLeapYear(2072))
        assertTrue(calenderUtils.isLeapYear(2076))
        assertTrue(calenderUtils.isLeapYear(2080))
        assertTrue(calenderUtils.isLeapYear(2084))
        assertTrue(calenderUtils.isLeapYear(2088))
        assertTrue(calenderUtils.isLeapYear(2092))
        assertTrue(calenderUtils.isLeapYear(2096))
    }

    @Test
    fun roundedMinute() {
        val calenderUtils = CalenderUtils()
        val date = calenderUtils.resolveTime(2020, 1, 1)
        val roundedDate = calenderUtils.roundedMinute(date)
        assertEquals(0, roundedDate.second)
        assertEquals(0, roundedDate.minute)
    }

    @Test
    fun resolveTime() {
        val calenderUtils = CalenderUtils()
        val date = calenderUtils.resolveTime(2020, 1, 1)
        assertEquals(2020, date.year)
        assertEquals(1, date.monthValue)
        assertEquals(1, date.dayOfMonth)
        assertEquals(0, date.hour)
        assertEquals(0, date.minute)
        assertEquals(0, date.second)
    }

    @Test
    fun toMillis() {
        val calenderUtils = CalenderUtils()
        val date = calenderUtils.resolveTime(2020, 1, 1)
        val millis = calenderUtils.toMillis(date)
        assertEquals(1577836800000, millis)
    }
}