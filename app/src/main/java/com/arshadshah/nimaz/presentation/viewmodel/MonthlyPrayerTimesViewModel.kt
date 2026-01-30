package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class DayPrayerTimes(
    val date: LocalDate,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

data class MonthlyPrayerTimesUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val dayPrayerTimes: List<DayPrayerTimes> = emptyList(),
    val locationName: String = "Location not set",
    val isLoading: Boolean = true,
    val expandedDay: LocalDate? = null
)

sealed interface MonthlyPrayerTimesEvent {
    data object NextMonth : MonthlyPrayerTimesEvent
    data object PreviousMonth : MonthlyPrayerTimesEvent
    data class ToggleDayExpanded(val date: LocalDate) : MonthlyPrayerTimesEvent
}

@HiltViewModel
class MonthlyPrayerTimesViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val preferencesDataStore: PreferencesDataStore
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

    init {
        observeSettings()
    }

    fun onEvent(event: MonthlyPrayerTimesEvent) {
        when (event) {
            MonthlyPrayerTimesEvent.NextMonth -> {
                _state.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
                calculateMonth()
            }
            MonthlyPrayerTimesEvent.PreviousMonth -> {
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
                preferencesDataStore.latitude,
                preferencesDataStore.longitude,
                preferencesDataStore.locationName
            ) { lat, lng, name -> Triple(lat, lng, name) }
                .combine(
                    combine(
                        preferencesDataStore.calculationMethod,
                        preferencesDataStore.asrCalculation,
                        preferencesDataStore.highLatitudeRule
                    ) { calc, asr, high -> Triple(calc, asr, high) }
                ) { location, calcSettings -> Pair(location, calcSettings) }
                .combine(
                    combine(
                        preferencesDataStore.fajrAdjustment,
                        preferencesDataStore.sunriseAdjustment,
                        preferencesDataStore.dhuhrAdjustment,
                        preferencesDataStore.asrAdjustment,
                    ) { fajr, sunrise, dhuhr, asr ->
                        mapOf(
                            PrayerType.FAJR to fajr,
                            PrayerType.SUNRISE to sunrise,
                            PrayerType.DHUHR to dhuhr,
                            PrayerType.ASR to asr
                        )
                    }.combine(
                        combine(
                            preferencesDataStore.maghribAdjustment,
                            preferencesDataStore.ishaAdjustment
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
                    calcMethod = try { CalculationMethod.valueOf(calcStr) } catch (_: Exception) { CalculationMethod.MUSLIM_WORLD_LEAGUE }
                    asrCalc = when (asrStr.lowercase()) {
                        "hanafi" -> AsrCalculation.HANAFI
                        else -> AsrCalculation.STANDARD
                    }
                    highLatRule = try { HighLatitudeRule.valueOf(highStr) } catch (_: Exception) { null }
                    adjustments = adj

                    _state.update {
                        it.copy(locationName = if (name.isNotBlank()) name else "Dublin, Ireland")
                    }
                    calculateMonth()
                }
        }
    }

    private fun calculateMonth() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val month = _state.value.currentMonth
            val days = mutableListOf<DayPrayerTimes>()

            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                val prayerTimes = prayerTimeCalculator.getPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    date = date,
                    calculationMethod = calcMethod,
                    asrCalculation = asrCalc,
                    highLatitudeRule = highLatRule,
                    adjustments = adjustments
                )

                val timesMap = prayerTimes.associate { it.type to it.time }
                val tz = kotlinx.datetime.TimeZone.currentSystemDefault()

                fun formatTime(type: PrayerType): String {
                    val instant = timesMap[type] ?: return "--:--"
                    val local = instant.toLocalDateTime(tz)
                    val h = if (local.hour > 12) local.hour - 12 else if (local.hour == 0) 12 else local.hour
                    val amPm = if (local.hour >= 12) "PM" else "AM"
                    return String.format("%d:%02d %s", h, local.minute, amPm)
                }

                days.add(
                    DayPrayerTimes(
                        date = date,
                        fajr = formatTime(PrayerType.FAJR),
                        sunrise = formatTime(PrayerType.SUNRISE),
                        dhuhr = formatTime(PrayerType.DHUHR),
                        asr = formatTime(PrayerType.ASR),
                        maghrib = formatTime(PrayerType.MAGHRIB),
                        isha = formatTime(PrayerType.ISHA)
                    )
                )
            }

            _state.update { it.copy(dayPrayerTimes = days, isLoading = false) }
        }
    }
}
