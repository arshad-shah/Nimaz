package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PrayerTimeCalculatorTest {

    private lateinit var calculator: PrayerTimeCalculator

    // Dublin, Ireland
    private val dublinLat = 53.3498
    private val dublinLng = -6.2603

    // Makkah, Saudi Arabia
    private val makkahLat = 21.4225
    private val makkahLng = 39.8262

    private val testDate = LocalDate.of(2025, 6, 15) // Summer - longer days

    @Before
    fun setUp() {
        calculator = PrayerTimeCalculator()
    }

    // ── Basic prayer time generation ────────────────────────────────

    @Test
    fun `getPrayerTimes returns exactly 6 prayer times`() {
        val times = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
        assertThat(times).hasSize(6)
    }

    @Test
    fun `getPrayerTimes returns all prayer types in order`() {
        val times = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
        val types = times.map { it.type }
        assertThat(types).containsExactly(
            PrayerType.FAJR, PrayerType.SUNRISE, PrayerType.DHUHR,
            PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA
        ).inOrder()
    }

    @Test
    fun `getPrayerTimes times are in chronological order`() {
        val times = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
        val millis = times.map { it.time.toEpochMilliseconds() }
        for (i in 0 until millis.size - 1) {
            assertThat(millis[i]).isLessThan(millis[i + 1])
        }
    }

    // ── Known location sanity checks ────────────────────────────────

    @Test
    fun `Makkah prayer times are reasonable for summer`() {
        val times = calculator.getPrayerTimes(
            makkahLat, makkahLng, testDate,
            calculationMethod = CalculationMethod.UMM_AL_QURA
        )
        val timeZone = TimeZone.of("Asia/Riyadh")

        val fajr = times.first { it.type == PrayerType.FAJR }
        val fajrHour = fajr.time.toLocalDateTime(timeZone).hour
        // Fajr in Makkah in June should be roughly between 3:30-4:30 AM
        assertThat(fajrHour).isIn(3..4)

        val maghrib = times.first { it.type == PrayerType.MAGHRIB }
        val maghribHour = maghrib.time.toLocalDateTime(timeZone).hour
        // Maghrib in Makkah in June should be roughly between 7:00-7:30 PM
        assertThat(maghribHour).isIn(18..19)
    }

    @Test
    fun `Dublin prayer times are reasonable for summer`() {
        val times = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
        val timeZone = TimeZone.of("Europe/Dublin")

        val fajr = times.first { it.type == PrayerType.FAJR }
        val fajrHour = fajr.time.toLocalDateTime(timeZone).hour
        // Fajr in Dublin in June is very early due to high latitude
        assertThat(fajrHour).isIn(1..4)

        val dhuhr = times.first { it.type == PrayerType.DHUHR }
        val dhuhrHour = dhuhr.time.toLocalDateTime(timeZone).hour
        // Dhuhr should be around noon-1 PM
        assertThat(dhuhrHour).isIn(12..14)
    }

    // ── Calculation methods produce different results ────────────────

    @Test
    fun `different calculation methods produce different Fajr times`() {
        // Use a moderate latitude and equinox date to avoid high-latitude edge cases
        val equinoxDate = LocalDate.of(2025, 3, 20)
        val mwl = calculator.getPrayerTimes(
            makkahLat, makkahLng, equinoxDate,
            calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
        ).first { it.type == PrayerType.FAJR }

        val egyptian = calculator.getPrayerTimes(
            makkahLat, makkahLng, equinoxDate,
            calculationMethod = CalculationMethod.EGYPTIAN
        ).first { it.type == PrayerType.FAJR }

        // MWL (18°) and Egyptian (19.5°) use different Fajr angles
        assertThat(mwl.time).isNotEqualTo(egyptian.time)
    }

    // ── Asr calculation ─────────────────────────────────────────────

    @Test
    fun `Hanafi Asr is later than Standard Asr`() {
        val standard = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            asrCalculation = AsrCalculation.STANDARD
        ).first { it.type == PrayerType.ASR }

        val hanafi = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            asrCalculation = AsrCalculation.HANAFI
        ).first { it.type == PrayerType.ASR }

        // Hanafi Asr (2x shadow length) is always later than Standard (1x)
        assertThat(hanafi.time.toEpochMilliseconds())
            .isGreaterThan(standard.time.toEpochMilliseconds())
    }

    // ── High latitude rule ──────────────────────────────────────────

    @Test
    fun `high latitude rule does not crash and produces valid times`() {
        val middleNight = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        assertThat(middleNight).hasSize(6)
        middleNight.forEach { assertThat(it.time).isNotNull() }
    }

    // ── Time adjustments ────────────────────────────────────────────

    @Test
    fun `positive time adjustment delays prayer time`() {
        val baseTime = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
            .first { it.type == PrayerType.FAJR }

        val adjustedTimes = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            adjustments = mapOf(PrayerType.FAJR to 5) // +5 minutes
        )
        val adjustedFajr = adjustedTimes.first { it.type == PrayerType.FAJR }

        val diffMs = adjustedFajr.time.toEpochMilliseconds() - baseTime.time.toEpochMilliseconds()
        assertThat(diffMs).isEqualTo(5 * 60_000L)
    }

    @Test
    fun `negative time adjustment advances prayer time`() {
        val baseTime = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
            .first { it.type == PrayerType.MAGHRIB }

        val adjustedTimes = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            adjustments = mapOf(PrayerType.MAGHRIB to -3) // -3 minutes
        )
        val adjustedMaghrib = adjustedTimes.first { it.type == PrayerType.MAGHRIB }

        val diffMs = adjustedMaghrib.time.toEpochMilliseconds() - baseTime.time.toEpochMilliseconds()
        assertThat(diffMs).isEqualTo(-3 * 60_000L)
    }

    @Test
    fun `adjustment only affects specified prayer`() {
        val baseTimes = calculator.getPrayerTimes(dublinLat, dublinLng, testDate)
        val adjustedTimes = calculator.getPrayerTimes(
            dublinLat, dublinLng, testDate,
            adjustments = mapOf(PrayerType.FAJR to 10)
        )

        // Dhuhr should be unchanged
        val baseDhuhr = baseTimes.first { it.type == PrayerType.DHUHR }.time
        val adjustedDhuhr = adjustedTimes.first { it.type == PrayerType.DHUHR }.time
        assertThat(baseDhuhr).isEqualTo(adjustedDhuhr)
    }

    // ── calculatePrayerTimes (Location-based) ───────────────────────

    @Test
    fun `calculatePrayerTimes returns valid PrayerTimes object`() {
        val location = createDublinLocation()

        val prayerTimes = calculator.calculatePrayerTimes(testDate, location)
        assertThat(prayerTimes.date).isEqualTo(testDate)
        assertThat(prayerTimes.location).isEqualTo(location)
        assertThat(prayerTimes.fajr).isNotNull()
        assertThat(prayerTimes.sunrise).isNotNull()
        assertThat(prayerTimes.dhuhr).isNotNull()
        assertThat(prayerTimes.asr).isNotNull()
        assertThat(prayerTimes.maghrib).isNotNull()
        assertThat(prayerTimes.isha).isNotNull()
    }

    @Test
    fun `calculatePrayerTimes times are in chronological order`() {
        val location = Location(
            id = 1, name = "Makkah",
            latitude = makkahLat, longitude = makkahLng,
            timezone = "Asia/Riyadh",
            country = "Saudi Arabia", city = "Makkah",
            isCurrentLocation = false, isFavorite = false,
            calculationMethod = CalculationMethod.UMM_AL_QURA,
            asrCalculation = AsrCalculation.STANDARD,
            highLatitudeRule = null, fajrAngle = null, ishaAngle = null
        )

        val pt = calculator.calculatePrayerTimes(testDate, location)
        assertThat(pt.fajr).isLessThan(pt.sunrise)
        assertThat(pt.sunrise).isLessThan(pt.dhuhr)
        assertThat(pt.dhuhr).isLessThan(pt.asr)
        assertThat(pt.asr).isLessThan(pt.maghrib)
        assertThat(pt.maghrib).isLessThan(pt.isha)
    }

    // ── calculatePrayerTimesForRange ────────────────────────────────

    @Test
    fun `calculatePrayerTimesForRange returns one entry per day`() {
        val location = createDublinLocation()

        val start = LocalDate.of(2025, 3, 1)
        val end = LocalDate.of(2025, 3, 7)
        val range = calculator.calculatePrayerTimesForRange(start, end, location)

        assertThat(range).hasSize(7)
        assertThat(range.first().date).isEqualTo(start)
        assertThat(range.last().date).isEqualTo(end)
    }

    @Test
    fun `calculatePrayerTimesForRange single day returns one entry`() {
        val location = createDublinLocation()

        val date = LocalDate.of(2025, 3, 1)
        val range = calculator.calculatePrayerTimesForRange(date, date, location)
        assertThat(range).hasSize(1)
    }

    // ── Winter vs Summer ────────────────────────────────────────────

    @Test
    fun `summer Fajr is earlier than winter Fajr in Dublin`() {
        val summerDate = LocalDate.of(2025, 6, 21)
        val winterDate = LocalDate.of(2025, 12, 21)
        val timeZone = TimeZone.of("Europe/Dublin")

        val summerFajr = calculator.getPrayerTimes(dublinLat, dublinLng, summerDate)
            .first { it.type == PrayerType.FAJR }
        val winterFajr = calculator.getPrayerTimes(dublinLat, dublinLng, winterDate)
            .first { it.type == PrayerType.FAJR }

        val summerHour = summerFajr.time.toLocalDateTime(timeZone).hour
        val winterHour = winterFajr.time.toLocalDateTime(timeZone).hour

        // Summer Fajr should be earlier (smaller hour) than winter
        assertThat(summerHour).isLessThan(winterHour)
    }

    @Test
    fun `summer Maghrib is later than winter Maghrib in Dublin`() {
        val summerDate = LocalDate.of(2025, 6, 21)
        val winterDate = LocalDate.of(2025, 12, 21)
        val timeZone = TimeZone.of("Europe/Dublin")

        val summerMaghrib = calculator.getPrayerTimes(dublinLat, dublinLng, summerDate)
            .first { it.type == PrayerType.MAGHRIB }
        val winterMaghrib = calculator.getPrayerTimes(dublinLat, dublinLng, winterDate)
            .first { it.type == PrayerType.MAGHRIB }

        val summerHour = summerMaghrib.time.toLocalDateTime(timeZone).hour
        val winterHour = winterMaghrib.time.toLocalDateTime(timeZone).hour

        // Summer Maghrib should be later (larger hour) than winter
        assertThat(summerHour).isGreaterThan(winterHour)
    }

    // ── All calculation methods work without error ───────────────────

    @Test
    fun `all calculation methods produce valid results`() {
        for (method in CalculationMethod.entries) {
            val times = calculator.getPrayerTimes(
                makkahLat, makkahLng, testDate,
                calculationMethod = method
            )
            assertThat(times).hasSize(6)
            times.forEach { assertThat(it.time).isNotNull() }
        }
    }

    // ── Helper ──────────────────────────────────────────────────────

    private fun createDublinLocation() = Location(
        id = 1, name = "Dublin",
        latitude = dublinLat, longitude = dublinLng,
        timezone = "Europe/Dublin",
        country = "Ireland", city = "Dublin",
        isCurrentLocation = true, isFavorite = false,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null, fajrAngle = null, ishaAngle = null
    )
}
