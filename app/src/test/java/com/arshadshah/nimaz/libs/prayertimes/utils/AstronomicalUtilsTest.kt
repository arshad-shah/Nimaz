package com.arshadshah.nimaz.libs.prayertimes.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AstronomicalUtilsTest {

    @Test
    fun interpolate() {
        val value = 1.0
        val prevValue = 0.0
        val nextValue = 2.0
        val factor = 0.5
        val expected = 1.5
        val actual = AstronomicalUtils.interpolate(value, prevValue, nextValue, factor)
        assertEquals(expected, actual, 0.01)
    }

    @Test
    fun interpolateAngles() {
        val value = 1.0
        val prevValue = 0.0
        val nextValue = 2.0
        val factor = 0.5
        val expected = 1.5
        val actual = AstronomicalUtils.interpolateAngles(value, prevValue, nextValue, factor)
        assertEquals(expected, actual,0.01)
    }
}