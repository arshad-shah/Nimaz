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

    @Test
    fun timeForPrayerPakistan() {
        // Test for Karachi, Pakistan (24.8607, 67.0011)
        val pakistanCoordinates = Coordinates(24.8607, 67.0011)
        val pakistanDate = LocalDateTime.of(2024, 1, 15, 0, 0)
        val pakistanCalculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        val prayerTimes = PrayerTimesCalculated(
            pakistanCoordinates,
            pakistanDate,
            pakistanCalculationParameters
        )
        
        // Verify that prayer times are calculated (not null)
        val fajrTime = prayerTimes.timeForPrayer(Prayer.FAJR)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val asrTime = prayerTimes.timeForPrayer(Prayer.ASR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("Fajr time should not be null", fajrTime)
        assertNotNull("Dhuhr time should not be null", dhuhrTime)
        assertNotNull("Asr time should not be null", asrTime)
        assertNotNull("Maghrib time should not be null", maghribTime)
        assertNotNull("Isha time should not be null", ishaTime)
        
        // Verify times are in reasonable range (UTC times)
        // Fajr should be early morning (before 6 AM UTC for Pakistan in January)
        assertTrue("Fajr should be before 6 AM UTC", fajrTime!!.hour < 6)
        // Dhuhr should be around midday (UTC)
        assertTrue("Dhuhr should be between 6 AM and 9 AM UTC", 
            dhuhrTime!!.hour in 6..9)
    }
}