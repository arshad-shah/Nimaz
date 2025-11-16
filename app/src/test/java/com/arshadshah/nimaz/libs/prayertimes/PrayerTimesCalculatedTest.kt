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
        // Pakistan: UTC+5, no DST
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

    @Test
    fun timeForPrayerDubai() {
        // Test for Dubai, UAE (25.2048, 55.2708)
        // Dubai: UTC+4, no DST
        val dubaiCoordinates = Coordinates(25.2048, 55.2708)
        val dubaiDate = LocalDateTime.of(2024, 1, 15, 0, 0)
        val dubaiCalculationParameters = CalculationParameters(
            18.2,
            18.2,
            CalculationMethod.DUBAI
        )
        val prayerTimes = PrayerTimesCalculated(
            dubaiCoordinates,
            dubaiDate,
            dubaiCalculationParameters
        )
        
        val fajrTime = prayerTimes.timeForPrayer(Prayer.FAJR)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        
        assertNotNull("Dubai Fajr time should not be null", fajrTime)
        assertNotNull("Dubai Dhuhr time should not be null", dhuhrTime)
        assertNotNull("Dubai Maghrib time should not be null", maghribTime)
        
        // All times should be valid
        assertTrue("Dubai prayer times should be calculated", 
            fajrTime != null && dhuhrTime != null && maghribTime != null)
    }

    @Test
    fun timeForPrayerMakkah() {
        // Test for Makkah, Saudi Arabia (21.3891, 39.8579)
        // Saudi Arabia: UTC+3, no DST
        val makkahCoordinates = Coordinates(21.3891, 39.8579)
        val makkahDate = LocalDateTime.of(2024, 1, 15, 0, 0)
        val makkahCalculationParameters = CalculationParameters(
            18.5,
            90,
            CalculationMethod.MAKKAH
        )
        val prayerTimes = PrayerTimesCalculated(
            makkahCoordinates,
            makkahDate,
            makkahCalculationParameters
        )
        
        val fajrTime = prayerTimes.timeForPrayer(Prayer.FAJR)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("Makkah Fajr time should not be null", fajrTime)
        assertNotNull("Makkah Dhuhr time should not be null", dhuhrTime)
        assertNotNull("Makkah Isha time should not be null", ishaTime)
        
        // All times should be valid
        assertTrue("Makkah prayer times should be calculated", 
            fajrTime != null && dhuhrTime != null && ishaTime != null)
    }

    @Test
    fun timeForPrayerNewYork() {
        // Test for New York, USA (40.7128, -74.0060)
        // New York: UTC-5 (winter) / UTC-4 (summer DST)
        val newYorkCoordinates = Coordinates(40.7128, -74.0060)
        val newYorkDate = LocalDateTime.of(2024, 1, 15, 0, 0) // Winter
        val newYorkCalculationParameters = CalculationParameters(
            15.0,
            15.0,
            CalculationMethod.ISNA
        )
        val prayerTimes = PrayerTimesCalculated(
            newYorkCoordinates,
            newYorkDate,
            newYorkCalculationParameters
        )
        
        val fajrTime = prayerTimes.timeForPrayer(Prayer.FAJR)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        
        assertNotNull("New York Fajr time should not be null", fajrTime)
        assertNotNull("New York Dhuhr time should not be null", dhuhrTime)
        assertNotNull("New York Maghrib time should not be null", maghribTime)
        
        // Times should be in afternoon/evening UTC for negative timezone offset
        assertTrue("New York prayer times should be calculated", 
            fajrTime != null && dhuhrTime != null && maghribTime != null)
    }

    @Test
    fun timeForPrayerLondon() {
        // Test for London, UK (51.5074, -0.1278)
        // London: UTC+0 (winter) / UTC+1 (summer DST)
        val londonCoordinates = Coordinates(51.5074, -0.1278)
        val londonDate = LocalDateTime.of(2024, 7, 15, 0, 0) // Summer
        val londonCalculationParameters = CalculationParameters(
            18.0,
            17.0,
            CalculationMethod.MWL
        )
        val prayerTimes = PrayerTimesCalculated(
            londonCoordinates,
            londonDate,
            londonCalculationParameters
        )
        
        val fajrTime = prayerTimes.timeForPrayer(Prayer.FAJR)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        
        assertNotNull("London Fajr time should not be null", fajrTime)
        assertNotNull("London Dhuhr time should not be null", dhuhrTime)
        assertNotNull("London Maghrib time should not be null", maghribTime)
        
        // All times should be valid for London in summer
        assertTrue("London prayer times should be calculated", 
            fajrTime != null && dhuhrTime != null && maghribTime != null)
    }
}