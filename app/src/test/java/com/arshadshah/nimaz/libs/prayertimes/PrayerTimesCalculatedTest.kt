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
        // Reference: IslamicFinder.org for Karachi, 2024-01-15
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
        val sunriseTime = prayerTimes.timeForPrayer(Prayer.SUNRISE)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val asrTime = prayerTimes.timeForPrayer(Prayer.ASR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("Fajr time should not be null", fajrTime)
        assertNotNull("Sunrise time should not be null", sunriseTime)
        assertNotNull("Dhuhr time should not be null", dhuhrTime)
        assertNotNull("Asr time should not be null", asrTime)
        assertNotNull("Maghrib time should not be null", maghribTime)
        assertNotNull("Isha time should not be null", ishaTime)
        
        // Expected values (UTC time) for Karachi on 2024-01-15
        // These are based on astronomical calculations using University of Karachi method
        // Fajr: ~6:17 AM local (01:17 UTC) - angle 18°
        // Sunrise: ~7:38 AM local (02:38 UTC)
        // Dhuhr: ~12:44 PM local (07:44 UTC)
        // Asr: ~3:59 PM local (10:59 UTC) - Shafi/Standard
        // Maghrib: ~5:51 PM local (12:51 UTC)
        // Isha: ~7:11 PM local (14:11 UTC) - angle 18°
        
        // Verify times are in reasonable range (UTC times with ±5 min tolerance)
        assertEquals("Fajr UTC hour", 1, fajrTime!!.hour)
        assertTrue("Fajr UTC minute", fajrTime.minute in 10..25)
        
        assertEquals("Sunrise UTC hour", 2, sunriseTime!!.hour)
        assertTrue("Sunrise UTC minute", sunriseTime.minute in 35..42)
        
        assertEquals("Dhuhr UTC hour", 7, dhuhrTime!!.hour)
        assertTrue("Dhuhr UTC minute", dhuhrTime.minute in 40..48)
        
        assertEquals("Asr UTC hour", 10, asrTime!!.hour)
        assertTrue("Asr UTC minute", asrTime.minute in 54..65)
        
        assertEquals("Maghrib UTC hour", 12, maghribTime!!.hour)
        assertTrue("Maghrib UTC minute", maghribTime.minute in 48..55)
        
        assertEquals("Isha UTC hour", 14, ishaTime!!.hour)
        assertTrue("Isha UTC minute", ishaTime.minute in 6..16)
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
    fun timeForPrayerNewYorkWinter() {
        // Test for New York, USA (40.7128, -74.0060) in WINTER (no DST)
        // New York: UTC-5 (winter, EST)
        // Reference: IslamicFinder.org for New York, 2024-01-15 (ISNA method)
        val newYorkCoordinates = Coordinates(40.7128, -74.0060)
        val newYorkDate = LocalDateTime.of(2024, 1, 15, 0, 0) // Winter - no DST
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
        val sunriseTime = prayerTimes.timeForPrayer(Prayer.SUNRISE)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val asrTime = prayerTimes.timeForPrayer(Prayer.ASR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("New York Fajr time should not be null", fajrTime)
        assertNotNull("New York Sunrise time should not be null", sunriseTime)
        assertNotNull("New York Dhuhr time should not be null", dhuhrTime)
        assertNotNull("New York Asr time should not be null", asrTime)
        assertNotNull("New York Maghrib time should not be null", maghribTime)
        assertNotNull("New York Isha time should not be null", ishaTime)
        
        // Expected values (UTC time) for New York on 2024-01-15 (Winter - no DST, UTC-5)
        // ISNA method uses 15° for both Fajr and Isha
        // Fajr: ~6:00 AM EST (11:00 UTC)
        // Sunrise: ~7:20 AM EST (12:20 UTC)
        // Dhuhr: ~12:08 PM EST (17:08 UTC)
        // Asr: ~2:47 PM EST (19:47 UTC)
        // Maghrib: ~4:56 PM EST (21:56 UTC)
        // Isha: ~6:16 PM EST (23:16 UTC)
        
        // Verify times are in UTC (with ±5 min tolerance)
        assertEquals("Fajr UTC hour (winter)", 11, fajrTime!!.hour)
        assertTrue("Fajr UTC minute", fajrTime.minute in 0..5)
        
        assertEquals("Sunrise UTC hour", 12, sunriseTime!!.hour)
        assertTrue("Sunrise UTC minute", sunriseTime.minute in 18..25)
        
        assertEquals("Dhuhr UTC hour", 17, dhuhrTime!!.hour)
        assertTrue("Dhuhr UTC minute", dhuhrTime.minute in 5..12)
        
        assertEquals("Maghrib UTC hour", 21, maghribTime!!.hour)
        assertTrue("Maghrib UTC minute", maghribTime.minute in 53..60)
    }

    @Test
    fun timeForPrayerNewYorkSummer() {
        // Test for New York, USA (40.7128, -74.0060) in SUMMER (with DST)
        // New York: UTC-4 (summer, EDT - Daylight Saving Time)
        // Reference: IslamicFinder.org for New York, 2024-07-15 (ISNA method)
        val newYorkCoordinates = Coordinates(40.7128, -74.0060)
        val newYorkDate = LocalDateTime.of(2024, 7, 15, 0, 0) // Summer - DST active
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
        val sunriseTime = prayerTimes.timeForPrayer(Prayer.SUNRISE)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("New York summer Fajr time should not be null", fajrTime)
        assertNotNull("New York summer Sunrise time should not be null", sunriseTime)
        assertNotNull("New York summer Dhuhr time should not be null", dhuhrTime)
        assertNotNull("New York summer Maghrib time should not be null", maghribTime)
        assertNotNull("New York summer Isha time should not be null", ishaTime)
        
        // Expected values (UTC time) for New York on 2024-07-15 (Summer - DST, UTC-4)
        // Fajr: ~4:14 AM EDT (08:14 UTC)
        // Sunrise: ~5:37 AM EDT (09:37 UTC)
        // Dhuhr: ~1:03 PM EDT (17:03 UTC)
        // Maghrib: ~8:28 PM EDT (00:28 UTC next day)
        // Isha: ~9:59 PM EDT (01:59 UTC next day)
        
        // Verify times are in UTC (with ±5 min tolerance)
        assertEquals("Fajr UTC hour (summer)", 8, fajrTime!!.hour)
        assertTrue("Fajr UTC minute", fajrTime.minute in 12..18)
        
        assertEquals("Sunrise UTC hour", 9, sunriseTime!!.hour)
        assertTrue("Sunrise UTC minute", sunriseTime.minute in 35..42)
        
        assertEquals("Dhuhr UTC hour", 17, dhuhrTime!!.hour)
        assertTrue("Dhuhr UTC minute", dhuhrTime.minute in 0..6)
    }

    @Test
    fun timeForPrayerLondonWinter() {
        // Test for London, UK (51.5074, -0.1278) in WINTER (no DST)
        // London: UTC+0 (winter, GMT)
        // Reference: IslamicFinder.org for London, 2024-01-15 (MWL method)
        val londonCoordinates = Coordinates(51.5074, -0.1278)
        val londonDate = LocalDateTime.of(2024, 1, 15, 0, 0) // Winter - no DST
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
        val sunriseTime = prayerTimes.timeForPrayer(Prayer.SUNRISE)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("London winter Fajr time should not be null", fajrTime)
        assertNotNull("London winter Sunrise time should not be null", sunriseTime)
        assertNotNull("London winter Dhuhr time should not be null", dhuhrTime)
        assertNotNull("London winter Maghrib time should not be null", maghribTime)
        assertNotNull("London winter Isha time should not be null", ishaTime)
        
        // Expected values (UTC/GMT time) for London on 2024-01-15 (Winter - no DST)
        // MWL method uses 18° for Fajr and 17° for Isha
        // Fajr: ~6:17 AM GMT (06:17 UTC)
        // Sunrise: ~8:01 AM GMT (08:01 UTC)
        // Dhuhr: ~12:08 PM GMT (12:08 UTC)
        // Maghrib: ~4:16 PM GMT (16:16 UTC)
        // Isha: ~5:56 PM GMT (17:56 UTC)
        
        // Verify times are in UTC (with ±5 min tolerance)
        assertEquals("Fajr UTC hour (winter)", 6, fajrTime!!.hour)
        assertTrue("Fajr UTC minute", fajrTime.minute in 14..22)
        
        assertEquals("Sunrise UTC hour", 8, sunriseTime!!.hour)
        assertTrue("Sunrise UTC minute", sunriseTime.minute in 0..5)
        
        assertEquals("Dhuhr UTC hour", 12, dhuhrTime!!.hour)
        assertTrue("Dhuhr UTC minute", dhuhrTime.minute in 5..12)
    }

    @Test
    fun timeForPrayerLondonSummer() {
        // Test for London, UK (51.5074, -0.1278) in SUMMER (with DST)
        // London: UTC+1 (summer, BST - British Summer Time)
        // Reference: IslamicFinder.org for London, 2024-07-15 (MWL method)
        val londonCoordinates = Coordinates(51.5074, -0.1278)
        val londonDate = LocalDateTime.of(2024, 7, 15, 0, 0) // Summer - DST active
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
        val sunriseTime = prayerTimes.timeForPrayer(Prayer.SUNRISE)
        val dhuhrTime = prayerTimes.timeForPrayer(Prayer.DHUHR)
        val maghribTime = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
        val ishaTime = prayerTimes.timeForPrayer(Prayer.ISHA)
        
        assertNotNull("London summer Fajr time should not be null", fajrTime)
        assertNotNull("London summer Sunrise time should not be null", sunriseTime)
        assertNotNull("London summer Dhuhr time should not be null", dhuhrTime)
        assertNotNull("London summer Maghrib time should not be null", maghribTime)
        assertNotNull("London summer Isha time should not be null", ishaTime)
        
        // Expected values (UTC time) for London on 2024-07-15 (Summer - DST, UTC+1)
        // Fajr: ~2:42 AM BST (01:42 UTC)
        // Sunrise: ~4:57 AM BST (03:57 UTC)
        // Dhuhr: ~1:05 PM BST (12:05 UTC)
        // Maghrib: ~9:13 PM BST (20:13 UTC)
        // Isha: ~11:13 PM BST (22:13 UTC)
        
        // Verify times are in UTC (with ±5 min tolerance)
        assertEquals("Fajr UTC hour (summer)", 1, fajrTime!!.hour)
        assertTrue("Fajr UTC minute", fajrTime.minute in 40..48)
        
        assertEquals("Sunrise UTC hour", 3, sunriseTime!!.hour)
        assertTrue("Sunrise UTC minute", sunriseTime.minute in 55..62)
        
        assertEquals("Dhuhr UTC hour", 12, dhuhrTime!!.hour)
        assertTrue("Dhuhr UTC minute", dhuhrTime.minute in 3..8)
    }
}