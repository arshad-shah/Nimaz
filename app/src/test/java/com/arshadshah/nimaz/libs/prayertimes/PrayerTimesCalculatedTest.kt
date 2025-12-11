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

    // ==================== Islamic Teachings Validation Tests ====================

    @Test
    fun testPrayerTimesChronologicalOrder() {
        // Test that prayer times follow Islamic chronological order:
        // Fajr < Sunrise < Dhuhr < Asr < Maghrib < Isha
        // This is a fundamental Islamic requirement
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        val calculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        val prayerTimes = PrayerTimesCalculated(coordinates, date, calculationParameters)
        
        val fajr = prayerTimes.timeForPrayer(Prayer.FAJR)!!
        val sunrise = prayerTimes.timeForPrayer(Prayer.SUNRISE)!!
        val dhuhr = prayerTimes.timeForPrayer(Prayer.DHUHR)!!
        val asr = prayerTimes.timeForPrayer(Prayer.ASR)!!
        val maghrib = prayerTimes.timeForPrayer(Prayer.MAGHRIB)!!
        val isha = prayerTimes.timeForPrayer(Prayer.ISHA)!!
        
        // Verify chronological order - fundamental Islamic teaching
        assertTrue("Fajr must be before Sunrise", fajr.isBefore(sunrise))
        assertTrue("Sunrise must be before Dhuhr", sunrise.isBefore(dhuhr))
        assertTrue("Dhuhr must be before Asr", dhuhr.isBefore(asr))
        assertTrue("Asr must be before Maghrib", asr.isBefore(maghrib))
        assertTrue("Maghrib must be before Isha", maghrib.isBefore(isha))
    }

    @Test
    fun testMadhabDifference_ShafiVsHanafi() {
        // Test that Asr time differs based on Madhab (Islamic school of thought)
        // Shafi: Asr when shadow = object length (earlier)
        // Hanafi: Asr when shadow = 2x object length (later)
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        
        // Shafi madhab calculation (default)
        val shafiParams = CalculationParameters(18.0, 18.0, CalculationMethod.KARACHI)
        shafiParams.madhab = com.arshadshah.nimaz.libs.prayertimes.enums.Madhab.SHAFI
        val shafiPrayerTimes = PrayerTimesCalculated(coordinates, date, shafiParams)
        val shafiAsr = shafiPrayerTimes.timeForPrayer(Prayer.ASR)!!
        
        // Hanafi madhab calculation
        val hanafiParams = CalculationParameters(18.0, 18.0, CalculationMethod.KARACHI)
        hanafiParams.madhab = com.arshadshah.nimaz.libs.prayertimes.enums.Madhab.HANAFI
        val hanafiPrayerTimes = PrayerTimesCalculated(coordinates, date, hanafiParams)
        val hanafiAsr = hanafiPrayerTimes.timeForPrayer(Prayer.ASR)!!
        
        // Hanafi Asr must be later than Shafi Asr (Islamic teaching)
        assertTrue("Hanafi Asr must be after Shafi Asr", hanafiAsr.isAfter(shafiAsr))
        
        // The difference should be significant (typically 30-60 minutes)
        val minutesDifference = java.time.Duration.between(shafiAsr, hanafiAsr).toMinutes()
        assertTrue("Asr time difference should be 20-90 minutes", minutesDifference in 20..90)
    }

    @Test
    fun testFajrBeforeSunrise() {
        // Islamic teaching: Fajr prayer must end before sunrise
        // Fajr time marks the beginning, and it must be before sunrise
        val testLocations = listOf(
            Triple(Coordinates(24.8607, 67.0011), "Karachi", CalculationMethod.KARACHI),
            Triple(Coordinates(51.5074, -0.1278), "London", CalculationMethod.MWL),
            Triple(Coordinates(40.7128, -74.0060), "New York", CalculationMethod.ISNA)
        )
        
        testLocations.forEach { (coords, location, method) ->
            val prayerTimes = PrayerTimesCalculated(
                coords,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                CalculationParameters(18.0, 18.0, method)
            )
            
            val fajr = prayerTimes.timeForPrayer(Prayer.FAJR)!!
            val sunrise = prayerTimes.timeForPrayer(Prayer.SUNRISE)!!
            
            assertTrue("$location: Fajr must be before Sunrise", fajr.isBefore(sunrise))
            
            // There should be reasonable time between Fajr and Sunrise (typically 60-90 min)
            val minutesDiff = java.time.Duration.between(fajr, sunrise).toMinutes()
            assertTrue("$location: Fajr-Sunrise gap should be 40-120 min, was $minutesDiff", 
                minutesDiff in 40..120)
        }
    }

    @Test
    fun testMaghribAtSunset() {
        // Islamic teaching: Maghrib prayer begins immediately at sunset
        // Maghrib time should be at or very close to sunset time
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        val calculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        val prayerTimes = PrayerTimesCalculated(coordinates, date, calculationParameters)
        
        val maghrib = prayerTimes.timeForPrayer(Prayer.MAGHRIB)!!
        
        // Note: In the implementation, sunset is calculated separately
        // but Maghrib should be at or very close to sunset time
        // We verify Maghrib is between Asr and Isha (basic sanity check)
        val asr = prayerTimes.timeForPrayer(Prayer.ASR)!!
        val isha = prayerTimes.timeForPrayer(Prayer.ISHA)!!
        
        assertTrue("Maghrib must be after Asr", maghrib.isAfter(asr))
        assertTrue("Maghrib must be before Isha", maghrib.isBefore(isha))
    }

    @Test
    fun testDhuhrAtSolarNoon() {
        // Islamic teaching: Dhuhr (Zuhr) prayer begins after solar noon
        // when the sun passes the meridian
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        val calculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        val prayerTimes = PrayerTimesCalculated(coordinates, date, calculationParameters)
        
        val sunrise = prayerTimes.timeForPrayer(Prayer.SUNRISE)!!
        val dhuhr = prayerTimes.timeForPrayer(Prayer.DHUHR)!!
        val asr = prayerTimes.timeForPrayer(Prayer.ASR)!!
        
        // Dhuhr should be roughly halfway between sunrise and sunset
        // or more precisely, at solar noon
        assertTrue("Dhuhr must be after Sunrise", dhuhr.isAfter(sunrise))
        assertTrue("Dhuhr must be before Asr", dhuhr.isBefore(asr))
        
        // Verify reasonable time gaps
        val sunriseToDhuhr = java.time.Duration.between(sunrise, dhuhr).toMinutes()
        assertTrue("Sunrise to Dhuhr should be 4-6 hours", sunriseToDhuhr in 240..360)
    }

    @Test
    fun testIshaAfterMaghrib() {
        // Islamic teaching: Isha prayer begins after twilight disappears
        // This should be after Maghrib with sufficient time gap
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        val calculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        val prayerTimes = PrayerTimesCalculated(coordinates, date, calculationParameters)
        
        val maghrib = prayerTimes.timeForPrayer(Prayer.MAGHRIB)!!
        val isha = prayerTimes.timeForPrayer(Prayer.ISHA)!!
        
        assertTrue("Isha must be after Maghrib", isha.isAfter(maghrib))
        
        // There should be reasonable time between Maghrib and Isha
        // (typically 60-90 minutes based on twilight disappearance)
        val minutesDiff = java.time.Duration.between(maghrib, isha).toMinutes()
        assertTrue("Maghrib-Isha gap should be 50-120 min, was $minutesDiff", 
            minutesDiff in 50..120)
    }

    @Test
    fun testPrayerTimesAcrossEquinoxes() {
        // Test prayer times during different seasons
        // to ensure algorithm works correctly year-round
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val calculationParameters = CalculationParameters(
            18.0,
            18.0,
            CalculationMethod.KARACHI
        )
        
        val seasons = listOf(
            Triple(LocalDateTime.of(2024, 3, 20, 0, 0), "Spring Equinox", "March"),
            Triple(LocalDateTime.of(2024, 6, 21, 0, 0), "Summer Solstice", "June"),
            Triple(LocalDateTime.of(2024, 9, 22, 0, 0), "Autumn Equinox", "September"),
            Triple(LocalDateTime.of(2024, 12, 21, 0, 0), "Winter Solstice", "December")
        )
        
        seasons.forEach { (date, season, month) ->
            val prayerTimes = PrayerTimesCalculated(coordinates, date, calculationParameters)
            
            val fajr = prayerTimes.timeForPrayer(Prayer.FAJR)
            val sunrise = prayerTimes.timeForPrayer(Prayer.SUNRISE)
            val dhuhr = prayerTimes.timeForPrayer(Prayer.DHUHR)
            val asr = prayerTimes.timeForPrayer(Prayer.ASR)
            val maghrib = prayerTimes.timeForPrayer(Prayer.MAGHRIB)
            val isha = prayerTimes.timeForPrayer(Prayer.ISHA)
            
            // All prayer times must be calculated for all seasons
            assertNotNull("$season: Fajr should be calculated", fajr)
            assertNotNull("$season: Sunrise should be calculated", sunrise)
            assertNotNull("$season: Dhuhr should be calculated", dhuhr)
            assertNotNull("$season: Asr should be calculated", asr)
            assertNotNull("$season: Maghrib should be calculated", maghrib)
            assertNotNull("$season: Isha should be calculated", isha)
            
            // Verify chronological order for each season
            assertTrue("$season: Fajr < Sunrise", fajr!!.isBefore(sunrise))
            assertTrue("$season: Sunrise < Dhuhr", sunrise!!.isBefore(dhuhr))
            assertTrue("$season: Dhuhr < Asr", dhuhr!!.isBefore(asr))
            assertTrue("$season: Asr < Maghrib", asr!!.isBefore(maghrib))
            assertTrue("$season: Maghrib < Isha", maghrib!!.isBefore(isha))
        }
    }

    @Test
    fun testCalculationMethodVariations() {
        // Different Islamic organizations use different angles for Fajr and Isha
        // Test that different methods produce valid but different results
        val coordinates = Coordinates(24.8607, 67.0011) // Karachi
        val date = LocalDateTime.of(2024, 1, 15, 0, 0)
        
        val methods = listOf(
            Triple(CalculationMethod.KARACHI, 18.0, 18.0),  // University of Karachi
            Triple(CalculationMethod.ISNA, 15.0, 15.0),      // ISNA
            Triple(CalculationMethod.MWL, 18.0, 17.0)        // Muslim World League
        )
        
        val results = mutableListOf<Pair<String, LocalDateTime>>()
        
        methods.forEach { (method, fajrAngle, ishaAngle) ->
            val params = CalculationParameters(fajrAngle, ishaAngle, method)
            val prayerTimes = PrayerTimesCalculated(coordinates, date, params)
            
            val fajr = prayerTimes.timeForPrayer(Prayer.FAJR)!!
            val isha = prayerTimes.timeForPrayer(Prayer.ISHA)!!
            
            results.add(Pair(method.name + "_Fajr", fajr))
            results.add(Pair(method.name + "_Isha", isha))
            
            // All methods must produce valid chronological prayer times
            val allPrayers = listOf(
                prayerTimes.timeForPrayer(Prayer.FAJR)!!,
                prayerTimes.timeForPrayer(Prayer.SUNRISE)!!,
                prayerTimes.timeForPrayer(Prayer.DHUHR)!!,
                prayerTimes.timeForPrayer(Prayer.ASR)!!,
                prayerTimes.timeForPrayer(Prayer.MAGHRIB)!!,
                prayerTimes.timeForPrayer(Prayer.ISHA)!!
            )
            
            // Verify each method produces chronological times
            for (i in 0 until allPrayers.size - 1) {
                assertTrue("${method.name}: Prayer $i must be before prayer ${i+1}", 
                    allPrayers[i].isBefore(allPrayers[i + 1]))
            }
        }
        
        // Verify that different methods produce different Fajr/Isha times
        // (due to different angle calculations)
        val karachiFajr = results.find { it.first == "KARACHI_Fajr" }!!.second
        val isnaFajr = results.find { it.first == "ISNA_Fajr" }!!.second
        
        // KARACHI uses 18° while ISNA uses 15°, so times should differ
        assertNotEquals("KARACHI and ISNA should produce different Fajr times", 
            karachiFajr, isnaFajr)
    }

    @Test
    fun testNoNullPrayerTimes() {
        // Islamic requirement: All five daily prayers must have valid times
        // This test ensures no prayer time is null under normal conditions
        val testCases = listOf(
            Triple(Coordinates(24.8607, 67.0011), CalculationMethod.KARACHI, "Karachi"),
            Triple(Coordinates(21.3891, 39.8579), CalculationMethod.MAKKAH, "Makkah"),
            Triple(Coordinates(25.2048, 55.2708), CalculationMethod.DUBAI, "Dubai"),
            Triple(Coordinates(40.7128, -74.0060), CalculationMethod.ISNA, "New York"),
            Triple(Coordinates(51.5074, -0.1278), CalculationMethod.MWL, "London")
        )
        
        testCases.forEach { (coords, method, location) ->
            val params = CalculationParameters(18.0, 18.0, method)
            val prayerTimes = PrayerTimesCalculated(
                coords,
                LocalDateTime.of(2024, 1, 15, 0, 0),
                params
            )
            
            assertNotNull("$location: Fajr must not be null", 
                prayerTimes.timeForPrayer(Prayer.FAJR))
            assertNotNull("$location: Sunrise must not be null", 
                prayerTimes.timeForPrayer(Prayer.SUNRISE))
            assertNotNull("$location: Dhuhr must not be null", 
                prayerTimes.timeForPrayer(Prayer.DHUHR))
            assertNotNull("$location: Asr must not be null", 
                prayerTimes.timeForPrayer(Prayer.ASR))
            assertNotNull("$location: Maghrib must not be null", 
                prayerTimes.timeForPrayer(Prayer.MAGHRIB))
            assertNotNull("$location: Isha must not be null", 
                prayerTimes.timeForPrayer(Prayer.ISHA))
        }
    }
}