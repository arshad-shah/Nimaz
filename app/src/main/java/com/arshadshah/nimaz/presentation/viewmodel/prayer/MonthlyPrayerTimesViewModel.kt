package com.arshadshah.nimaz.presentation.viewmodel.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import com.arshadshah.nimaz.domain.model.DayPrayerTimes

@HiltViewModel
class MonthlyPrayerTimesViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyPrayerTimesUiState())
    val state: StateFlow<MonthlyPrayerTimesUiState> = _state.asStateFlow()

    /** The user's calculation settings, mirrored so a month can be built without suspending. */
    private var settings: PrayerCalculationSettings? = null

    // No `use24HourFormat` mirror: the month table formats its times at the leaf.

    init {
        observeSettings()
    }

    fun onEvent(event: MonthlyPrayerTimesEvent) {
        when (event) {
            MonthlyPrayerTimesEvent.NextMonth -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "next_month")
                _state.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
                calculateMonth()
            }

            MonthlyPrayerTimesEvent.PreviousMonth -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "previous_month")
                _state.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
                calculateMonth()
            }

            is MonthlyPrayerTimesEvent.ToggleDayExpanded -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "toggle_day_expanded")
                _state.update {
                    it.copy(
                        expandedDay = if (it.expandedDay == event.date) null else event.date
                    )
                }
            }
        }
    }

    /**
     * One flow instead of thirteen.
     *
     * This assembled the settings from six preference flows through three nested `combine`s and
     * parsed three of them itself — the same block, near-identically, as `PrayerTimesViewModel`
     * and `HomeViewModel`. The parsing now happens once in the data layer, and this observes the
     * result.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            prayerUseCases.observeCalculationSettings().collect { resolved ->
                settings = resolved
                _state.update {
                    it.copy(
                        locationName = resolved.location.name.ifBlank { DEFAULT_LOCATION_NAME },
                        methodLabel = "${resolved.calculationMethod.shortName()} · " +
                                resolved.asrCalculation.shortName(),
                        latitude = resolved.location.latitude,
                        longitude = resolved.location.longitude,
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
        val prayerTimes = settings
            ?.let { prayerUseCases.getDaySchedule(date, it) }
            .orEmpty()
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


    private companion object {
        /** Shown until the user's location resolves; the same fallback the calculation uses. */
        const val DEFAULT_LOCATION_NAME = "Dublin, Ireland"
    }
}
