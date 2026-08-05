package com.arshadshah.nimaz.presentation.viewmodel.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.di.DefaultDispatcher
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import com.arshadshah.nimaz.domain.model.DayPrayerTimes

@HiltViewModel
class MonthlyPrayerTimesViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val todayProvider: TodayProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(MonthlyPrayerTimesUiState())
    val state: StateFlow<MonthlyPrayerTimesUiState> = _state.asStateFlow()

    /** The user's calculation settings, mirrored so a month can be built without suspending. */
    private var settings: PrayerCalculationSettings? = null

    /** The in-flight month build, cancelled and replaced so a fast pager cannot stack them. */
    private var monthJob: Job? = null

    // No `use24HourFormat` mirror: the month table formats its times at the leaf.

    init {
        observeToday()
        observeSettings()
    }

    /**
     * Anchor the grid to the current month, and re-anchor when the day rolls over.
     *
     * `currentMonth` and `expandedDay` were data-class defaults reading `YearMonth.now()` /
     * `LocalDate.now()`, evaluated once when the state was constructed — so a screen left open
     * across a month boundary went on showing the previous month with no way to know, and
     * `expandedDay` highlighted a row that no longer meant today.
     *
     * The re-anchor only follows the clock while the user is *on* the current month: someone
     * who has paged forward to Ramadan should not be yanked back at midnight.
     */
    private fun observeToday() {
        launchSafely(telemetry, AppAnalytics.Feature.PRAYER_TIMES, "observe_today") {
            todayProvider.todayChanges.collect { today ->
                val month = YearMonth.from(today)
                val wasOnCurrentMonth = _state.value.currentMonth?.let { it == month.minusMonths(1) }
                    ?: true
                _state.update {
                    it.copy(
                        currentMonth = if (it.currentMonth == null || wasOnCurrentMonth) {
                            month
                        } else {
                            it.currentMonth
                        },
                        expandedDay = today,
                    )
                }
                calculateMonth()
            }
        }
    }

    fun onEvent(event: MonthlyPrayerTimesEvent) {
        when (event) {
            MonthlyPrayerTimesEvent.NextMonth -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "next_month")
                _state.update { it.copy(currentMonth = it.currentMonth?.plusMonths(1)) }
                calculateMonth()
            }

            MonthlyPrayerTimesEvent.PreviousMonth -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "previous_month")
                _state.update { it.copy(currentMonth = it.currentMonth?.minusMonths(1)) }
                calculateMonth()
            }

            MonthlyPrayerTimesEvent.PrepareRamadanExport -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "export_ramadan")
                prepareRamadanExport()
            }

            MonthlyPrayerTimesEvent.RamadanExportConsumed ->
                _state.update { it.copy(ramadanExport = null) }

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
        launchSafely(telemetry, AppAnalytics.Feature.PRAYER_TIMES, "observe_settings") {
            prayerUseCases.observeCalculationSettings().collect { resolved ->
                settings = resolved
                _state.update {
                    it.copy(
                        // The header names the place the maths used, or says it is a default —
                        // never a third thing. It used to read `if (name.isNotBlank()) name else
                        // "Dublin, Ireland"`, a hardcoded English string beside a `resolveLocation`
                        // that had already decided the coordinates. A user who set coordinates
                        // manually (lat/lng set, display name blank) got a correct timetable under
                        // a foreign city's name — and a foreign city above your prayer times is a
                        // good reason to conclude the times are wrong.
                        locationName = resolved.location.name.takeIf { name -> name.isNotBlank() },
                        isUsingFallbackLocation = resolved.location.isFallback,
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

    /**
     * Build the visible month **off the main thread**.
     *
     * This ran with no dispatcher — so on `Main` — and does 28–31 passes of solar geometry for
     * six prayers each plus 28–31 Hijri conversions, on every settings emission and every month
     * tap. The injected default dispatcher is where that belongs; the two `_state` writes stay on the
     * collector's context, which is what `withContext` gives back.
     */
    private fun calculateMonth() {
        val month = _state.value.currentMonth ?: return
        monthJob?.cancel()
        monthJob = launchSafely(
            telemetry,
            AppAnalytics.Feature.PRAYER_TIMES,
            "calculate_month",
            onFailure = { _state.update { it.copy(isLoading = false) } },
        ) {
            _state.update { it.copy(isLoading = true) }
            val (days, ramadanYear) = withContext(defaultDispatcher) {
                val computed = (1..month.lengthOfMonth()).map { dayTimesFor(month.atDay(it)) }
                computed to computed
                    .map { HijriDateCalculator.toHijri(it.date) }
                    .firstOrNull { it.month == 9 }
                    ?.year
            }
            _state.update {
                it.copy(dayPrayerTimes = days, ramadanHijriYear = ramadanYear, isLoading = false)
            }
        }
    }

    /**
     * Compute the full Hijri Ramadan (day 1 → last) for the year currently in view, and publish
     * it for the share sheet to pick up.
     *
     * This was a **public, synchronous, non-suspending** function computing ~30 more days of
     * prayer times, called straight from a click handler — so the share button ran a month of
     * astronomy inside composition's event callback, on the UI thread. On a low-end device that
     * is a visible freeze and a candidate ANR.
     *
     * It is now an event whose result lands in state, which fixes the UDF violation at the same
     * time: the screen dispatches and renders, rather than calling into the ViewModel for a
     * value.
     */
    private fun prepareRamadanExport() {
        val year = _state.value.ramadanHijriYear ?: return
        launchSafely(telemetry, AppAnalytics.Feature.PRAYER_TIMES, "prepare_ramadan_export") {
            val rows = withContext(defaultDispatcher) {
                val start = HijriDateCalculator.getFirstDayOfRamadan(year)
                val end = HijriDateCalculator.getLastDayOfRamadan(year)
                buildList {
                    var date = start
                    while (!date.isAfter(end)) {
                        add(dayTimesFor(date))
                        date = date.plusDays(1)
                    }
                }
            }
            _state.update { it.copy(ramadanExport = rows) }
        }
    }

    private fun dayTimesFor(date: LocalDate): DayPrayerTimes {
        val prayerTimes = settings
            ?.let { prayerUseCases.getDaySchedule(date, it) }
            .orEmpty()
        val timesMap = prayerTimes.associate { it.type to it.time }
        fun fmt(type: PrayerType): kotlin.time.Instant? = timesMap[type]
        // Fasting length (Fajr → Maghrib) as **elapsed time**, from the instants themselves.
        //
        // It used to convert both to local wall-clock and subtract hour/minute fields, which is
        // wrong twice. Seconds were discarded — a floor of the difference of two floors, so
        // 05:00:59 → 20:00:01 reported 900 minutes against a true 899. And wall-clock fields do
        // not know about DST: a zone whose transition falls between Fajr and Maghrib (Lord Howe
        // shifts 30 minutes) reported a fast half an hour out. Instants have neither problem,
        // and the timezone conversion this needed disappears with it.
        val fajr = timesMap[PrayerType.FAJR]
        val maghrib = timesMap[PrayerType.MAGHRIB]
        val fastMinutes = if (fajr != null && maghrib != null) {
            (maghrib - fajr).inWholeMinutes.toInt()
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
