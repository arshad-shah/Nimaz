package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.presentation.components.organisms.MoonPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Drives the dedicated Prayer Times screen: a day pager over the prayer
 * schedule with a living-sky hero and per-day tracking.
 *
 * The sky always reflects "now" (so it stays alive while you browse), while the
 * list and day-info reflect the [PrayerTimesUiState.selectedDate]. Tracking
 * toggles apply to today and past days only (future prayers can't be prayed).
 */
data class PrayerTimesUiState(
    val locationName: String = "Location not set",
    /**
     * True when [locationName] and the times below it come from [FallbackLocation] rather than
     * anywhere the reader chose — onboarding can be skipped and the permission denied, so the
     * header must not assert a city they have never been to.
     */
    val isUsingFallbackLocation: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val prayers: List<PrayerTimeDisplay> = emptyList(),
    /** Tomorrow's Fajr, so the UI can wrap the countdown once today's Isha has passed. */
    val tomorrowFajrAt: kotlin.time.Instant? = null,
    // Living-sky inputs
    val moonFraction: Float = 0.5f,     // moon phase for the selected date
    val sunriseFraction: Float = 0.27f, // sunrise as a fraction of the day (sun arc)
    val sunsetFraction: Float = 0.80f,  // sunset (Maghrib) as a fraction of the day
    // Day-info card — instants, formatted at the leaf so the 12/24h toggle is a recomposition
    val sunriseAt: kotlin.time.Instant? = null,
    val sunsetAt: kotlin.time.Instant? = null,
    val daylight: String = "",
    val methodLabel: String = "",
    // +1 when moving to a later day, -1 to an earlier day (drives slide direction)
    val navDirection: Int = 0,
)

sealed interface PrayerTimesEvent {
    data object PreviousDay : PrayerTimesEvent
    data object NextDay : PrayerTimesEvent
    data object GoToToday : PrayerTimesEvent
    data class SelectDate(val date: LocalDate) : PrayerTimesEvent
    data class TogglePrayer(val type: PrayerType) : PrayerTimesEvent
}

@HiltViewModel
class PrayerTimesViewModel @Inject constructor(
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val prayerUseCases: PrayerUseCases,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PrayerTimesUiState())
    val state: StateFlow<PrayerTimesUiState> = _state.asStateFlow()

    // Cached location + calculation settings (mirrors MonthlyPrayerTimesViewModel).
    private var latitude = 0.0
    private var longitude = 0.0
    private var calcMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
    private var asrCalc = AsrCalculation.STANDARD
    private var highLatRule: HighLatitudeRule? = null
    private var adjustments = mapOf<PrayerType, Int>()
    private var settingsReady = false

    // Cached prayer instants for the selected day, and the tracking statuses.
    private var dayTimes: List<PrayerTime> = emptyList()
    private var statuses: Map<PrayerName, PrayerStatus> = emptyMap()
    private var statusJob: Job? = null

    private val tz get() = TimeZone.currentSystemDefault()

    init {
        observeSettings()
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.PreviousDay -> {
                AppAnalytics.logFeatureUsed(
                    AppAnalytics.Feature.PRAYER_TIMES,
                    "previous_day"
                )
                changeDay(-1)
            }
            PrayerTimesEvent.NextDay -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.PRAYER_TIMES, "next_day")
                changeDay(1)
            }
            PrayerTimesEvent.GoToToday -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.PRAYER_TIMES, "go_to_today")
                selectDate(LocalDate.now())
            }
            is PrayerTimesEvent.SelectDate -> selectDate(event.date)
            is PrayerTimesEvent.TogglePrayer -> {
                AppAnalytics.logFeatureUsed(
                    AppAnalytics.Feature.PRAYER_TIMES,
                    "toggle_prayer"
                )
                togglePrayer(event.type)
            }
        }
    }

    private fun changeDay(delta: Long) {
        val target = _state.value.selectedDate.plusDays(delta)
        selectDate(target)
    }

    private fun selectDate(date: LocalDate) {
        val current = _state.value.selectedDate
        if (date == current) return
        val dir = if (date.isAfter(current)) 1 else -1
        _state.update { it.copy(selectedDate = date, navDirection = dir) }
        recomputeDay()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.latitude,
                settingsRepository.longitude,
                settingsRepository.locationName,
            ) { lat, lng, name -> Triple(lat, lng, name) }
                .combine(
                    combine(
                        settingsRepository.calculationMethod,
                        settingsRepository.asrCalculation,
                        settingsRepository.highLatitudeRule,
                    ) { calc, asr, high -> Triple(calc, asr, high) },
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
                            PrayerType.ASR to asr,
                        )
                    }.combine(
                        combine(
                            settingsRepository.maghribAdjustment,
                            settingsRepository.ishaAdjustment,
                        ) { maghrib, isha ->
                            mapOf(PrayerType.MAGHRIB to maghrib, PrayerType.ISHA to isha)
                        },
                    ) { first, second -> first + second },
                ) { (location, calcSettings), adj -> Triple(location, calcSettings, adj) }
                .collect { (location, calcSettings, adj) ->
                    val (lat, lng, name) = location
                    val (calcStr, asrStr, highStr) = calcSettings

                    val resolved = resolveLocation(lat, lng, name)
                    latitude = resolved.latitude
                    longitude = resolved.longitude
                    // The domain's own parsers, in place of `valueOf` in a swallowing `try` plus a
                    // hand-written "hanafi" comparison. `valueOf` throws on every persisted alias
                    // the app itself writes — "MWL", "ISNA", "MAKKAH" — and the catch then
                    // substituted Muslim World League, so a user on ISNA silently got MWL prayer
                    // times, forever, with no signal. `fromString` knows the aliases.
                    calcMethod = CalculationMethod.fromString(calcStr)
                    asrCalc = AsrCalculation.fromString(asrStr)
                    highLatRule = HighLatitudeRule.fromString(highStr)
                    adjustments = adj
                    settingsReady = true

                    _state.update {
                        it.copy(
                            locationName = resolved.name,
                            isUsingFallbackLocation = resolved.isFallback
                        )
                    }
                    recomputeDay()
                }
        }
    }

    /** Recompute the cached prayer instants + day-info for the selected date. */
    private fun recomputeDay() {
        if (!settingsReady) return
        val date = _state.value.selectedDate

        dayTimes = prayerTimeCalculator.getPrayerTimes(
            latitude = latitude,
            longitude = longitude,
            date = date,
            calculationMethod = calcMethod,
            asrCalculation = asrCalc,
            highLatitudeRule = highLatRule,
            adjustments = adjustments,
        )

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
                methodLabel = "${calcMethod.shortName()} · ${asrCalc.shortName()}",
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
        val date = _state.value.selectedDate
        val today = LocalDate.now()
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

        // Only today can wrap past Isha, and only today needs tomorrow's Fajr.
        val tomorrowFajr = if (isToday) {
            prayerTimeCalculator.getPrayerTimes(
                latitude = latitude, longitude = longitude, date = today.plusDays(1),
                calculationMethod = calcMethod, asrCalculation = asrCalc,
                highLatitudeRule = highLatRule, adjustments = adjustments,
            ).firstOrNull { it.type == PrayerType.FAJR }?.time
        } else null

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
        val date = _state.value.selectedDate
        if (date.isAfter(LocalDate.now())) return // can't track future prayers
        viewModelScope.launch {
            val dateKey = date.toEpochDay() * 86_400_000L
            val name = PrayerName.valueOf(type.name)
            val current = statuses[name] ?: PrayerStatus.NOT_PRAYED
            val newStatus =
                if (current == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED else PrayerStatus.PRAYED
            val prayedAt =
                if (newStatus == PrayerStatus.PRAYED) Instant.now().toEpochMilli() else null
            prayerUseCases.updatePrayerStatus(dateKey, name, newStatus, prayedAt, false)
            // getPrayerRecordsForDate re-emits → applyTick refreshes the UI.
        }
    }

    // No `use24HourFormat` mirror: times are formatted at the leaf from LocalUse24HourFormat, so
    // toggling the preference no longer forces a full day recompute.

    private fun formatCountdown(totalSeconds: Long): String {
        val s = if (totalSeconds < 0) 0 else totalSeconds
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun daysFromToday(date: LocalDate, today: LocalDate): String {
        val diff = date.toEpochDay() - today.toEpochDay()
        return when {
            diff == 0L -> "Today"
            diff == 1L -> "Tomorrow"
            diff == -1L -> "Yesterday"
            diff > 0 -> "in ${diff} days"
            else -> "${abs(diff)} days ago"
        }
    }


}
