package com.arshadshah.nimaz.libs.prayertimes

import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.CalculationParameters
import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.Prayer
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

internal class PrayerTimesTest {
    private val coordinates = Coordinates(53.3498, -6.2603)
    private val date = LocalDateTime.of(2020, 1, 1, 0, 0)
    private val fajrAngle = 14.0
    private val ishaAngle = 14.0
    private val calculationMethod = CalculationMethod.IRELAND
    private val calculationParameters =
        CalculationParameters(
            fajrAngle,
            ishaAngle,
            calculationMethod
        )
    @Test
    fun timeForPrayer() {
        val prayerTimes =
            PrayerTimesCalculated(coordinates, date, calculationParameters)
        val timeForPrayer = prayerTimes.timeForPrayer(Prayer.FAJR)
        assertEquals(LocalDateTime.of(2020, 1, 1, 6, 58), timeForPrayer)
    }
}