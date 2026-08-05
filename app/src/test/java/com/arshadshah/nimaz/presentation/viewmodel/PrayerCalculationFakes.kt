package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.ResolvedLocation
import com.arshadshah.nimaz.domain.model.SunnahNightTimes
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/** A [PrayerCalculationSettings] with everything defaulted to London on MWL/Shafi. */
fun prayerCalculationSettings(
    latitude: Double = 51.5074,
    longitude: Double = -0.1278,
    name: String = "London",
    isFallback: Boolean = false,
    calculationMethod: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    asrCalculation: AsrCalculation = AsrCalculation.STANDARD,
    highLatitudeRule: HighLatitudeRule? = null,
    adjustments: Map<PrayerType, Int> = emptyMap(),
) = PrayerCalculationSettings(
    location = ResolvedLocation(latitude, longitude, name, isFallback),
    calculationMethod = calculationMethod,
    asrCalculation = asrCalculation,
    highLatitudeRule = highLatitudeRule,
    adjustments = adjustments,
)

/**
 * A [PrayerRepository] whose prayer-time methods run the **real** astronomy, under settings a test
 * controls.
 *
 * Faking the astronomy itself would be a mistake: `PrayerTimeCalculator` is pure and Android-free,
 * and a stub returning invented instants would let a wrong-day or wrong-order bug through
 * unnoticed — which is the class of bug these ViewModels have shipped. What the seam is for is
 * controlling the *settings*, which used to require mocking six preference flows per test and, in
 * one ViewModel, was not possible at all because the calculator was called with its defaults.
 *
 * Every other member throws: a test that reaches one has wandered outside what this double is for,
 * and should say so loudly rather than silently receive a relaxed mock's zero.
 */
class FakePrayerTimetableRepository(
    settings: PrayerCalculationSettings = prayerCalculationSettings(),
) : PrayerRepository by ThrowingPrayerRepository {

    private val calculator = PrayerTimeCalculator()
    private val settingsFlow = MutableStateFlow(settings)

    /** Change the settings mid-test; observers recompute exactly as they would in production. */
    fun setSettings(settings: PrayerCalculationSettings) {
        settingsFlow.value = settings
    }

    override fun observeCalculationSettings(): Flow<PrayerCalculationSettings> = settingsFlow

    override fun getDaySchedule(
        date: LocalDate,
        settings: PrayerCalculationSettings,
    ): List<PrayerTime> = calculator.getPrayerTimes(
        latitude = settings.location.latitude,
        longitude = settings.location.longitude,
        date = date,
        calculationMethod = settings.calculationMethod,
        asrCalculation = settings.asrCalculation,
        highLatitudeRule = settings.highLatitudeRule,
        adjustments = settings.adjustments,
    )

    override suspend fun getDaySchedule(date: LocalDate): List<PrayerTime> =
        getDaySchedule(date, settingsFlow.value)

    override fun getSunnahNightTimes(
        date: LocalDate,
        settings: PrayerCalculationSettings,
    ): SunnahNightTimes {
        val sunnah = calculator.getSunnahTimes(
            latitude = settings.location.latitude,
            longitude = settings.location.longitude,
            date = date,
            calculationMethod = settings.calculationMethod,
            asrCalculation = settings.asrCalculation,
            highLatitudeRule = settings.highLatitudeRule,
        )
        return SunnahNightTimes(sunnah.middleOfTheNight, sunnah.lastThirdOfTheNight)
    }

    override suspend fun getSunnahNightTimes(date: LocalDate): SunnahNightTimes =
        getSunnahNightTimes(date, settingsFlow.value)

    /**
     * A day with nothing tracked. `PrayerTimesViewModel` collects this to decorate each row with
     * its status, so it has to answer — but "which prayers were marked" is not what this double
     * is about, and a test that cares should use a real repository mock.
     */
    override fun getPrayerRecordsForDate(date: Long): Flow<List<PrayerRecord>> =
        MutableStateFlow(emptyList())
}

/** Delegation target: every member fails, so only the overridden ones are usable. */
private object ThrowingPrayerRepository : PrayerRepository by unusable()

private fun unusable(): PrayerRepository = java.lang.reflect.Proxy.newProxyInstance(
    PrayerRepository::class.java.classLoader,
    arrayOf(PrayerRepository::class.java),
) { _, method, _ ->
    error("FakePrayerTimetableRepository does not implement ${method.name}")
} as PrayerRepository
