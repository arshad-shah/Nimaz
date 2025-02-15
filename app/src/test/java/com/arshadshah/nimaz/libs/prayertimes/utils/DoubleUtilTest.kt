package com.arshadshah.nimaz.libs.prayertimes.utils

import com.arshadshah.nimaz.libs.prayertimes.utils.DoubleUtil.normalizeWithBound
import org.junit.Assert.*
import org.junit.Test

class DoubleUtilTest {

    @Test
    fun normalizeWithBoundTest() {
        assertEquals(-3.0, normalizeWithBound(2.0, -5.0), 0.00001)
        assertEquals(-4.0, normalizeWithBound(-4.0, -5.0), 0.00001)
        assertEquals(-1.0, normalizeWithBound(-6.0, -5.0), 0.00001)
        assertEquals(23.0, normalizeWithBound(-1.0, 24.0), 0.00001)
        assertEquals(1.0, normalizeWithBound(1.0, 24.0), 0.00001)
        assertEquals(1.0, normalizeWithBound(49.0, 24.0), 0.00001)
        assertEquals(1.0, normalizeWithBound(361.0, 360.0), 0.00001)
        assertEquals(0.0, normalizeWithBound(360.0, 360.0), 0.00001)
        assertEquals(259.0, normalizeWithBound(259.0, 360.0), 0.00001)
        assertEquals(72.0, normalizeWithBound(2592.0, 360.0), 0.00001)

    }

    @Test
    fun unwindAngle() {
        assertEquals(315.0, DoubleUtil.unwindAngle(-45.0), 0.00001)
        assertEquals(1.0, DoubleUtil.unwindAngle(361.0), 0.00001)
        assertEquals(0.0, DoubleUtil.unwindAngle(360.0), 0.00001)
        assertEquals(259.0, DoubleUtil.unwindAngle(259.0), 0.00001)
        assertEquals(72.0, DoubleUtil.unwindAngle(2592.0), 0.00001)
    }

    @Test
    fun closestAngle() {
        assertEquals(0.0, DoubleUtil.closestAngle(360.0), 0.00001)
        assertEquals(1.0, DoubleUtil.closestAngle(361.0), 0.00001)
        assertEquals(1.0, DoubleUtil.closestAngle(1.0), 0.00001)
        assertEquals(-1.0, DoubleUtil.closestAngle(-1.0), 0.00001)
        assertEquals(179.0, DoubleUtil.closestAngle(-181.0), 0.00001)
        assertEquals(180.0, DoubleUtil.closestAngle(180.0), 0.00001)
        assertEquals(-1.0, DoubleUtil.closestAngle(359.0), 0.00001)
        assertEquals(1.0, DoubleUtil.closestAngle(-359.0), 0.00001)
        assertEquals(-179.0, DoubleUtil.closestAngle(1261.0), 0.00001)
        assertEquals(-0.1, DoubleUtil.closestAngle(-360.1), 0.01)
    }
}