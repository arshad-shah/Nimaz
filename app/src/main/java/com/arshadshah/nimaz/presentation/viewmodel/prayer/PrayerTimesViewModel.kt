package com.arshadshah.nimaz.presentation.viewmodel.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.di.DefaultDispatcher
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.presentation.components.organisms.MoonPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val settingsRepository: SettingsRepository,
    private val todayProvider: TodayProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(PrayerTimesUiState())
    val state: StateFlow<PrayerTimesUiState> = _state.asStateFlow()

    /** The user's calculation settings, mirrored so a recompute never has to suspend. */
    private var settings: PrayerCalculationSettings? = null

    // Cached prayer instants for the selected day, and the tracking statuses.
    private var dayTimes: List<PrayerTime> = emptyList()
    private var statuses: Map<PrayerName, PrayerStatus> = emptyMap()
    private var statusJob: Job? = null

    /** The in-flight day recompute, cancelled and replaced so a fast pager cannot stack them. */
    private var dayJob: Job? = null

    /** Tomorrow's Fajr for the after-Isha wrap, computed with the day rather than per publish. */
    private var tomorrowFajr: kotlin.time.Instant? = null

    private val tz get() = TimeZone.currentSystemDefault()

    init {
        observeToday()
        observeSettings()
    }

    /**
     * Follow the day across midnight — but only while the user is still on today.
     *
     * `selectedDate` was captured once, as a data-class default reading `LocalDate.now()`, and
     * nothing re-evaluated it. Leaving the Prayer Times screen open across 00:00 — a plausible
     * state for an app whose whole point is Fajr — left it showing yesterday, with
     * `tomorrowFajrAt` pointing at yesterday's tomorrow. Worse, there was **no way back**: the
     * "Today" chip only renders `if (!state.isToday)`, and `isToday` kept its last published
     * value of `true`, so the affordance to return never appeared. `NimazClock`'s shared ticker
     * drives the leaf UI and never pokes the ViewModel.
     *
     * Someone who has paged to another day is left where they are; re-anchoring under them
     * would be its own bug.
     */
    private fun observeToday() {
        viewModelScope.launch {
            todayProvider.todayChanges.collect { today ->
                val selected = _state.value.selectedDate
                if (selected == null || selected == today.minusDays(1)) {
                    selectDate(today)
                }
            }
        }
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.PreviousDay -> {
                telemetry.featureUsed(
                    AppAnalytics.Feature.PRAYER_TIMES,
                    "previous_day"
                )
                changeDay(-1)
            }
            PrayerTimesEvent.NextDay -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "next_day")
                changeDay(1)
            }
            PrayerTimesEvent.GoToToday -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "go_to_today")
                selectDate(LocalDate.now())
            }
            is PrayerTimesEvent.SelectDate -> {
                telemetry.featureUsed(AppAnalytics.Feature.PRAYER_TIMES, "select_date")
                selectDate(event.date)
            }
            // The `toggle_prayer` feature event moved into `togglePrayer`, past its two
            // guards, and became `prayerTracked`. Logged here it counted taps on Sunrise and
            // on future days, neither of which changes anything.
            is PrayerTimesEvent.TogglePrayer -> togglePrayer(event.type)
        }
    }

    private fun changeDay(delta: Long) {
        val target = (_state.value.selectedDate ?: todayProvider.today()).plusDays(delta)
        selectDate(target)
    }

    private fun selectDate(date: LocalDate) {
        val current = _state.value.selectedDate
        if (date == current) return
        _state.update { it.copy(selectedDate = date) }
        recomputeDay()
    }

    /**
     * One flow instead of thirteen.
     *
     * Four nested `combine`s over six preference flows, plus three `fromString` calls, appeared
     * here, in `MonthlyPrayerTimesViewModel` and in `HomeViewModel` in near-identical form — and
     * a fifth ViewModel, `FastingViewModel`, skipped the whole block and silently used defaults.
     * The parsing now lives once in the data layer; this observes its result.
     */
    private fun observeSettings() {
        viewModelScope.launch {
            prayerUseCases.observeCalculationSettings().collect { resolved ->
                settings = resolved
                pinCalculationInputs(resolved)

                _state.update {
                    it.copy(
                        locationName = resolved.location.name,
                        isUsingFallbackLocation = resolved.location.isFallback,
                    )
                }
                recomputeDay()
            }
        }
    }

    /** Recompute the cached prayer instants + day-info for the selected date. */
    /**
     * Recompute the cached prayer instants + day-info for the selected date, **off the main
     * thread**.
     *
     * This was called synchronously from the settings `collect` (which runs on
     * `Dispatchers.Main.immediate`) and from `selectDate`, so a day's solar geometry for six
     * prayers ran on the UI thread on every settings emission and every date change.
     */
    private fun recomputeDay() {
        val resolved = settings ?: return
        val date = _state.value.selectedDate ?: return
        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            recompute(date, resolved)
        }
    }

    private suspend fun recompute(date: LocalDate, resolved: PrayerCalculationSettings) {
        val computed = withContext(defaultDispatcher) {
            val day = prayerUseCases.getDaySchedule(date, resolved)
            // Tomorrow's Fajr is only needed for today's after-Isha wrap, and it is computed
            // **here** rather than in `publishDisplays`. It used to run there, which meant a
            // second full day of astronomy on every publish — including every Room re-emission
            // of the tracker statuses, so toggling one prayer recomputed a whole day's solar
            // geometry on the UI thread.
            val tomorrow = if (date == todayProvider.today()) {
                prayerUseCases.getDaySchedule(date.plusDays(1), resolved)
                    .firstOrNull { it.type == PrayerType.FAJR }?.time
            } else null
            day to tomorrow
        }
        dayTimes = computed.first
        tomorrowFajr = computed.second

        // Day-info: sunrise/sunset/daylight + method label + moon phase.
        val byType = dayTimes.associate { it.type to it.time }
        val sunriseInstant = byType[PrayerType.SUNRISE]
        val maghribInstant = byType[PrayerType.MAGHRIB]
        val daylightStr = if (sunriseInstant != null && maghribInstant != null) {
            val mins = (maghribInstant - sunriseInstant).inWholeMinutes
            "${mins / 60}h ${mins % 60}m"
        } else ""
        // Day-fractions for the living sky's sun arc — anchors the scene to the
        // real sunrise/sunset for this location & date instead of fixed clock anchors.
        val sunriseFraction = sunriseInstant?.let {
            val l = it.toLocalDateTime(tz); (l.hour * 60 + l.minute) / 1440f
        } ?: 0.27f
        val sunsetFraction = maghribInstant?.let {
            val l = it.toLocalDateTime(tz); (l.hour * 60 + l.minute) / 1440f
        } ?: 0.80f
        val moon = MoonPhase.fractionForEpochMillis(
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )

        _state.update {
            it.copy(
                sunriseAt = sunriseInstant,
                sunsetAt = maghribInstant,
                daylight = daylightStr,
                methodLabel = "${resolved.calculationMethod.shortName()} · " +
                        resolved.asrCalculation.shortName(),
                moonFraction = moon,
                sunriseFraction = sunriseFraction,
                sunsetFraction = sunsetFraction,
            )
        }

        observeStatuses(date)
        publishDisplays()
    }

    private fun observeStatuses(date: LocalDate) {
        statusJob?.cancel()
        val dateKey = date.toEpochDay() * 86_400_000L
        statusJob = viewModelScope.launch {
            prayerUseCases.getPrayerRecordsForDate(dateKey).collect { records ->
                statuses = records.associate { it.prayerName to it.status }
                publishDisplays()
            }
        }
    }

    /**
     * Publish the day's prayer facts. **Free of "now"** — which prayer is next/current/passed, the
     * countdown and the sky labels are all derived at the leaf from these instants plus the shared
     * ticker, so this runs on input changes rather than once a second.
     */
    private fun publishDisplays() {
        if (dayTimes.isEmpty()) return
        val date = _state.value.selectedDate ?: return
        val today = todayProvider.today()
        val isToday = date == today

        val displays = dayTimes
            .sortedBy { it.time }
            .map { pt ->
                PrayerTimeDisplay(
                    type = pt.type,
                    name = pt.type.displayName,
                    timeAt = pt.time,
                    prayerStatus = statuses[PrayerName.valueOf(pt.type.name)]
                        ?: PrayerStatus.NOT_PRAYED,
                )
            }

        _state.update {
            it.copy(
                isToday = isToday,
                prayers = displays,
                tomorrowFajrAt = tomorrowFajr,
            )
        }
    }

    private fun togglePrayer(type: PrayerType) {
        if (type == PrayerType.SUNRISE) return
        val date = _state.value.selectedDate ?: return
        if (date.isAfter(todayProvider.today())) return // can't track future prayers
        viewModelScope.launch {
            val dateKey = date.toEpochDay() * 86_400_000L
            val name = PrayerName.valueOf(type.name)
            val current = statuses[name] ?: PrayerStatus.NOT_PRAYED
            val newStatus =
                if (current == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED else PrayerStatus.PRAYED
            val prayedAt =
                if (newStatus == PrayerStatus.PRAYED) Instant.now().toEpochMilli() else null
            prayerUseCases.updatePrayerStatus(dateKey, name, newStatus, prayedAt, false)
            // The third place a prayer is tracked, alongside the tracker and Home. #359 names
            // only two; this one recorded a generic `toggle_prayer` and is the reason a
            // dashboard built on `prayer_tracked` would still have under-counted after fixing
            // the other two.
            telemetry.prayerTracked(name.name, newStatus.name, isJamaah = false)
            // getPrayerRecordsForDate re-emits → applyTick refreshes the UI.
        }
    }

    // No `use24HourFormat` mirror: times are formatted at the leaf from LocalUse24HourFormat, so
    // toggling the preference no longer forces a full day recompute.


    /**
     * Pin the inputs a prayer-time crash needs to be reproducible.
     *
     * `CrashReporter.setCustomKey` was used nowhere in the ViewModel layer, so a crash in the
     * calculator arrived with a stack trace and nothing else — and "Fajr is wrong" is a class
     * of report that is unanswerable without the method, the school and roughly where the user
     * is. The latitude is **rounded to a whole degree**: a degree is enough to tell a
     * high-latitude failure from an equatorial one, and not enough to locate anyone.
     */
    private fun pinCalculationInputs(settings: PrayerCalculationSettings) {
        telemetry.customKey("calc_method", settings.calculationMethod.name)
        telemetry.customKey("asr_calculation", settings.asrCalculation.name)
        telemetry.customKey("high_latitude_rule", settings.highLatitudeRule?.name ?: "none")
        telemetry.customKey(
            "latitude_rounded",
            settings.location.latitude.roundToInt().toString(),
        )
    }
}
