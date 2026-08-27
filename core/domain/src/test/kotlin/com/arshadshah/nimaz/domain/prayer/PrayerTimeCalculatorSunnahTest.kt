package com.arshadshah.nimaz.domain.prayer

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.Location
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The night window, and the settings a *saved location* carries into the calculation.
 *
 * `PrayerTimeCalculatorTest` covers the five daily prayers from loose coordinates. Two paths it
 * does not reach, and both are ones a user configures and then trusts:
 *
 * **The night window.** Middle-of-the-night and last-third are what the night-worship screen is
 * built on. They must fall between Maghrib and the next Fajr and in that order — a last third
 * that starts before the middle is not a subtle error, it is a screen telling someone to pray
 * tahajjud at dusk.
 *
 * **A location's own overrides.** `calculatePrayerTimes(date, location)` reads the method, the
 * madhab and the high-latitude rule off the `Location` row rather than from arguments, which is a
 * second copy of the parameter assembly `getPrayerTimes` does from its arguments. Two copies of
 * the same mapping is exactly where one of them gets left behind: the Fast Tracker shipping with
 * the calculator's four defaults instead of the user's settings is the same shape of bug, recorded
 * in `ObservePrayerCalculationSettingsUseCase`'s KDoc.
 */
class PrayerTimeCalculatorSunnahTest {

    private val calculator = PrayerTimeCalculator()

    private val dublinLat = 53.3498
    private val dublinLng = -6.2603
    private val makkahLat = 21.4225
    private val makkahLng = 39.8262

    private val summer = LocalDate.of(2025, 6, 15)

    private fun location(
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asr: AsrCalculation = AsrCalculation.STANDARD,
        highLatitude: HighLatitudeRule? = null,
    ) = Location(
        id = 1,
        name = "Dublin",
        latitude = dublinLat,
        longitude = dublinLng,
        timezone = "Europe/Dublin",
        country = "Ireland",
        city = "Dublin",
        isCurrentLocation = true,
        isFavorite = false,
        calculationMethod = method,
        asrCalculation = asr,
        highLatitudeRule = highLatitude,
        fajrAngle = null,
        ishaAngle = null,
    )

    private fun makkah(method: CalculationMethod) = location(method = method).copy(
        name = "Makkah",
        latitude = makkahLat,
        longitude = makkahLng,
        timezone = "Asia/Riyadh",
        country = "Saudi Arabia",
        city = "Makkah",
    )

    // ---- The night window ----

    @Test
    fun `the middle of the night comes before the last third of it`() {
        val night = calculator.getSunnahTimes(makkahLat, makkahLng, summer)

        assertThat(night.middleOfTheNight).isLessThan(night.lastThirdOfTheNight)
    }

    @Test
    fun `the night window falls inside the night it belongs to`() {
        val night = calculator.getSunnahTimes(makkahLat, makkahLng, summer)
        val nextNight = calculator.getSunnahTimes(makkahLat, makkahLng, summer.plusDays(1))

        // One night's window is wholly before the next night's, and the gap between them is
        // about a day — the check that catches a window computed off the wrong date entirely.
        assertThat(night.lastThirdOfTheNight).isLessThan(nextNight.middleOfTheNight)
    }

    @Test
    fun `the night window can be asked for without naming any settings`() {
        // The defaults path: every caller that has coordinates and nothing else.
        val night = calculator.getSunnahTimes(dublinLat, dublinLng)

        assertThat(night.middleOfTheNight).isLessThan(night.lastThirdOfTheNight)
    }

    @Test
    fun `the madhab does not move the night window`() {
        // Asr is the only prayer the madhab changes; a night window that moved with it would
        // mean the parameter assembly is writing the wrong field.
        val standard = calculator.getSunnahTimes(
            makkahLat, makkahLng, summer, asrCalculation = AsrCalculation.STANDARD,
        )
        val hanafi = calculator.getSunnahTimes(
            makkahLat, makkahLng, summer, asrCalculation = AsrCalculation.HANAFI,
        )

        assertThat(standard.middleOfTheNight).isEqualTo(hanafi.middleOfTheNight)
        assertThat(standard.lastThirdOfTheNight).isEqualTo(hanafi.lastThirdOfTheNight)
    }

    @Test
    fun `the high latitude rule reaches the night window`() {
        // Not "differs from the default" — the default already *is* one of the three. What a
        // rule that never reached the parameters would produce is three identical answers.
        val windows = HighLatitudeRule.entries.map {
            calculator.getSunnahTimes(dublinLat, dublinLng, summer, highLatitudeRule = it)
        }

        assertThat(windows.map { it.lastThirdOfTheNight }.toSet().size).isGreaterThan(1)
    }

    @Test
    fun `every high latitude rule produces a usable night window`() {
        HighLatitudeRule.entries.forEach { rule ->
            val night = calculator.getSunnahTimes(
                dublinLat, dublinLng, summer, highLatitudeRule = rule,
            )
            assertThat(night.middleOfTheNight).isLessThan(night.lastThirdOfTheNight)
        }
    }

    // ---- What a saved location carries into the calculation ----

    @Test
    fun `a location's madhab reaches Asr`() {
        val standard = calculator.calculatePrayerTimes(summer, location(asr = AsrCalculation.STANDARD))
        val hanafi = calculator.calculatePrayerTimes(summer, location(asr = AsrCalculation.HANAFI))

        assertThat(hanafi.asr).isGreaterThan(standard.asr)
    }

    @Test
    fun `a location's calculation method reaches Fajr`() {
        // Makkah at the equinox: a moderate latitude, so the 18-degree and 19.5-degree Fajr
        // angles are not both swallowed by a high-latitude fallback.
        val equinox = LocalDate.of(2025, 3, 20)
        val mwl = calculator.calculatePrayerTimes(
            equinox, makkah(method = CalculationMethod.MUSLIM_WORLD_LEAGUE),
        )
        val egyptian = calculator.calculatePrayerTimes(
            equinox, makkah(method = CalculationMethod.EGYPTIAN),
        )

        assertThat(mwl.fajr).isNotEqualTo(egyptian.fajr)
    }

    @Test
    fun `a location's high latitude rule reaches the times`() {
        val plain = calculator.calculatePrayerTimes(summer, location(highLatitude = null))
        val ruled = calculator.calculatePrayerTimes(
            summer, location(highLatitude = HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
        )

        assertThat(ruled.isha).isNotEqualTo(plain.isha)
    }

    @Test
    fun `every high latitude rule on a location produces an ordered day`() {
        HighLatitudeRule.entries.forEach { rule ->
            val times = calculator.calculatePrayerTimes(summer, location(highLatitude = rule))

            assertThat(times.fajr).isLessThan(times.sunrise)
            assertThat(times.sunrise).isLessThan(times.dhuhr)
            assertThat(times.dhuhr).isLessThan(times.asr)
            assertThat(times.asr).isLessThan(times.maghrib)
            assertThat(times.maghrib).isLessThan(times.isha)
        }
    }

    @Test
    fun `every calculation method on a location produces an ordered day`() {
        CalculationMethod.entries.forEach { method ->
            val times = calculator.calculatePrayerTimes(summer, location(method = method))

            assertThat(times.fajr).isLessThan(times.sunrise)
            assertThat(times.maghrib).isLessThan(times.isha)
        }
    }
}
