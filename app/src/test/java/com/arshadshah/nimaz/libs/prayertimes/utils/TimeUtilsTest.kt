package com.arshadshah.nimaz.libs.prayertimes.utils

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

internal class TimeUtilsTest {
    private val timeUtils = TimeUtils(12, 0, 0)

    @Test
    fun date() {
        val date = timeUtils.date(LocalDateTime.of(2020, 1, 1, 0, 0, 0))
        assertEquals(LocalDateTime.of(2020, 1, 1, 12, 0, 0), date)
    }

    @Test
    fun fromDouble() {
        val timeUtils = TimeUtils.fromDouble(12.0)
        assertEquals(12, timeUtils?.date(LocalDateTime.of(2020, 1, 1, 0, 0, 0))?.hour)
        assertEquals(0, timeUtils?.date(LocalDateTime.of(2020, 1, 1, 0, 0, 0))?.minute)
        assertEquals(0, timeUtils?.date(LocalDateTime.of(2020, 1, 1, 0, 0, 0))?.second)
    }
}