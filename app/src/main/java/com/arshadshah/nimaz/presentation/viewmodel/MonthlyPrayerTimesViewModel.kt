package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * One row of the month table. Times are **instants**; the clock format is applied at the leaf from
 * `LocalUse24HourFormat`, so flipping the 12/24-hour toggle is a recomposition rather than a full
 * month recompute.
 */
data class DayPrayerTimes(
    val date: LocalDate,
    val fajr: kotlin.time.Instant?,
    val sunrise: kotlin.time.Instant?,
    val dhuhr: kotlin.time.Instant?,
    val asr: kotlin.time.Instant?,
    val maghrib: kotlin.time.Instant?,
    val isha: kotlin.time.Instant?,
    /** Fasting length (Fajr → Maghrib) in minutes; null if unavailable. */
    val fastMinutes: Int? = null
)

data class MonthlyPrayerTimesUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val dayPrayerTimes: List<DayPrayerTimes> = emptyList(),
    val locationName: String = "Location not set",
    val methodLabel: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ramadanHijriYear: Int? = null,
    val isLoading: Boolean = true,
    val expandedDay: LocalDate? = LocalDate.now()
)

sealed interface MonthlyPrayerTimesEvent {
    data object NextMonth : MonthlyPrayerTimesEvent
    data object PreviousMonth : MonthlyPrayerTimesEvent
    data class ToggleDayExpanded(val date: LocalDate) : MonthlyPrayerTimesEvent
}

@HiltViewModel
class MonthlyPrayerTimesViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyPrayerTimesUiState())
    val state: StateFlow<MonthlyPrayerTimesUiState> = _state.asStateFlow()

    // Cached settings
    private var latitude = 0.0
    private var longitude = 0.0
    private var calcMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
    private var asrCalc = AsrCalculation.STANDARD
    private var highLatRule: HighLatitudeRule? = null
    private var adjustments = mapOf<PrayerType, Int>()

    // No `use24HourFormat` mirror: the month table formats its times at the leaf.

    init {
        observeSettings()
    }

    fun onEvent(event: MonthlyPrayerTimesEvent) {
        when (event) {
            MonthlyPrayerTimesEvent.NextMonth -> {
                AppAnalytics.logFeatureUsed("monthly_prayer_times", "next_month")
                _state.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
                calculateMonth()
            }

            MonthlyPrayerTimesEvent.PreviousMonth -> {
                AppAnalytics.logFeatureUsed("monthly_prayer_times", "previous_month")
                _state.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
                calculateMonth()
            }

            is MonthlyPrayerTimesEvent.ToggleDayExpanded -> {
                _state.update {
                    it.copy(
                        expandedDay = if (it.expandedDay == event.date) null else event.date
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.latitude,
                settingsRepository.longitude,
                settingsRepository.locationName
            ) { lat, lng, name -> Triple(lat, lng, name) }
                .combine(
                    combine(
                        settingsRepository.calculationMethod,
                        settingsRepository.asrCalculation,
                        settingsRepository.highLatitudeRule
                    ) { calc, asr, high -> Triple(calc, asr, high) }
                ) { location, calcSettings -> Pair(location, calcSettings) }
                .combine(
                    combine(
                        settingsRepository.fajrAdjustment,
                        settingsRepository.sunriseAdjustment,
                        settingsRepository.dhuhrAdjustment,
                        settingsRepository.asrAdjustment,
                    ) { fajr, sunrise, dhuhr, asr ->
                        mapOf(
                            PrayerType.FAJR to fajr,
                            PrayerType.SUNRISE to sunrise,
                            PrayerType.DHUHR to dhuhr,
                            PrayerType.ASR to asr
                        )
                    }.combine(
                        combine(
                            settingsRepository.maghribAdjustment,
                            settingsRepository.ishaAdjustment
                        ) { maghrib, isha ->
                            mapOf(
                                PrayerType.MAGHRIB to maghrib,
                                PrayerType.ISHA to isha
                            )
                        }
                    ) { first, second -> first + second }
                ) { (location, calcSettings), adj ->
                    Triple(location, calcSettings, adj)
                }
                .collect { (location, calcSettings, adj) ->
                    val (lat, lng, name) = location
                    val (calcStr, asrStr, highStr) = calcSettings

                    latitude = if (lat != 0.0) lat else 53.3498
                    longitude = if (lng != 0.0) lng else -6.2603
                    calcMethod = try {
                        CalculationMethod.valueOf(calcStr)
                    } catch (_: Exception) {
                        CalculationMethod.MUSLIM_WORLD_LEAGUE
                    }
                    asrCalc = when (asrStr.lowercase()) {
                        "hanafi" -> AsrCalculation.HANAFI
                        else -> AsrCalculation.STANDARD
                    }
                    highLatRule = try {
                        HighLatitudeRule.valueOf(highStr)
                    } catch (_: Exception) {
                        null
                    }
                    adjustments = adj

                    _state.update {
                        it.copy(
                            locationName = if (name.isNotBlank()) name else "Dublin, Ireland",
                            methodLabel = "${calcMethod.shortName()} · ${asrCalc.shortName()}",
                            latitude = latitude,
                            longitude = longitude,
                        )
                    }
                    calculateMonth()
                }
        }
    }

    private fun calculateMonth() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val month = _state.value.currentMonth
            val days = (1..month.lengthOfMonth()).map { dayTimesFor(month.atDay(it)) }
            val ramadanYear = days
                .map { HijriDateCalculator.toHijri(it.date) }
                .firstOrNull { it.month == 9 }
                ?.year
            _state.update {
                it.copy(dayPrayerTimes = days, ramadanHijriYear = ramadanYear, isLoading = false)
            }
        }
    }

    /** Full Hijri Ramadan (day 1 → last) for the Ramadan year currently in view. */
    fun ramadanDays(): List<DayPrayerTimes> {
        val year = _state.value.ramadanHijriYear ?: return emptyList()
        val start = HijriDateCalculator.getFirstDayOfRamadan(year)
        val end = HijriDateCalculator.getLastDayOfRamadan(year)
        val out = mutableListOf<DayPrayerTimes>()
        var date = start
        while (!date.isAfter(end)) {
            out.add(dayTimesFor(date))
            date = date.plusDays(1)
        }
        return out
    }

    private fun dayTimesFor(date: LocalDate): DayPrayerTimes {
        val prayerTimes = prayerTimeCalculator.getPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            date = date,
            calculationMethod = calcMethod,
            asrCalculation = asrCalc,
            highLatitudeRule = highLatRule,
            adjustments = adjustments,
        )
        val timesMap = prayerTimes.associate { it.type to it.time }
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        fun fmt(type: PrayerType): kotlin.time.Instant? = timesMap[type]
        // Fasting length (Fajr → Maghrib), computed from the raw times so it is
        // independent of the display clock format / locale.
        val fajrLocal = timesMap[PrayerType.FAJR]?.toLocalDateTime(tz)
        val maghribLocal = timesMap[PrayerType.MAGHRIB]?.toLocalDateTime(tz)
        val fastMinutes = if (fajrLocal != null && maghribLocal != null) {
            var mins = (maghribLocal.hour * 60 + maghribLocal.minute) -
                    (fajrLocal.hour * 60 + fajrLocal.minute)
            if (mins < 0) mins += 24 * 60
            mins
        } else {
            null
        }
        return DayPrayerTimes(
            date = date,
            fajr = fmt(PrayerType.FAJR),
            sunrise = fmt(PrayerType.SUNRISE),
            dhuhr = fmt(PrayerType.DHUHR),
            asr = fmt(PrayerType.ASR),
            maghrib = fmt(PrayerType.MAGHRIB),
            isha = fmt(PrayerType.ISHA),
            fastMinutes = fastMinutes,
        )
    }


}
