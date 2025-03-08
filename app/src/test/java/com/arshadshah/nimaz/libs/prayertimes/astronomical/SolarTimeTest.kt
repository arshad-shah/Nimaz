package com.arshadshah.nimaz.libs.prayertimes.astronomical

import com.arshadshah.nimaz.libs.prayertimes.enums.ShadowLength
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class SolarTimeTest {
    //variables
    //localdatetime
    private val date = LocalDateTime.of(2018, 1, 1, 0, 0, 0)

    //coordinates of london
    private val coordinates =
        Coordinates(51.5085300, -0.1257400)

    @Test
    fun hourAngle() {
        val solarTime = SolarTime(date, coordinates)
        assertEquals(8.216276489977803, solarTime.hourAngle(0.0, false), 0.0001)
        assertEquals(13.82711689942916, solarTime.hourAngle(12.0, true), 0.0001)
        assertEquals(9.11469259902272, solarTime.hourAngle(6.0, false), 0.0001)
        assertEquals(15.640373222882817, solarTime.hourAngle(2.0, true), 0.0001)
    }

    @Test
    fun afternoon() {
        val solarTime = SolarTime(date, coordinates)
        assertEquals(13.766711547683265, solarTime.afternoon(ShadowLength.SINGLE), 0.0001)
        assertEquals(14.265491161178872, solarTime.afternoon(ShadowLength.DOUBLE), 0.0001)
    }
}