package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.presentation.components.organisms.MoonPhase
import dagger.hilt.android.lifecycle.HiltViewModel
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

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
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val prayers: List<PrayerTimeDisplay> = emptyList(),
    val timeUntilNext: String = "",
    val nextPrayerName: String = "",
    // Living-sky inputs
    val timeOfDay: Float = 0.5f,        // minute-quantised fraction of the day
    val moonFraction: Float = 0.5f,     // moon phase for the selected date
    val skyTimeLabel: String = "",
    val skyStatusLabel: String = "",
    // Day-info card
    val sunrise: String = "",
    val sunset: String = "",
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
    private val prayerRepository: PrayerRepository,
    private val preferencesDataStore: PreferencesDataStore,
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
        startTicker()
    }

    fun onEvent(event: PrayerTimesEvent) {
        when (event) {
            PrayerTimesEvent.PreviousDay -> changeDay(-1)
            PrayerTimesEvent.NextDay -> changeDay(1)
            PrayerTimesEvent.GoToToday -> selectDate(LocalDate.now())
            is PrayerTimesEvent.SelectDate -> selectDate(event.date)
            is PrayerTimesEvent.TogglePrayer -> togglePrayer(event.type)
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
                preferencesDataStore.latitude,
                preferencesDataStore.longitude,
                preferencesDataStore.locationName,
            ) { lat, lng, name -> Triple(lat, lng, name) }
                .combine(
                    combine(
                        preferencesDataStore.calculationMethod,
                        preferencesDataStore.asrCalculation,
                        preferencesDataStore.highLatitudeRule,
                    ) { calc, asr, high -> Triple(calc, asr, high) },
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
                            PrayerType.ASR to asr,
                        )
                    }.combine(
                        combine(
                            preferencesDataStore.maghribAdjustment,
                            preferencesDataStore.ishaAdjustment,
                        ) { maghrib, isha ->
                            mapOf(PrayerType.MAGHRIB to maghrib, PrayerType.ISHA to isha)
                        },
                    ) { first, second -> first + second },
                ) { (location, calcSettings), adj -> Triple(location, calcSettings, adj) }
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
                    asrCalc =
                        if (asrStr.lowercase() == "hanafi") AsrCalculation.HANAFI else AsrCalculation.STANDARD
                    highLatRule = try {
                        HighLatitudeRule.valueOf(highStr)
                    } catch (_: Exception) {
                        null
                    }
                    adjustments = adj
                    settingsReady = true

                    _state.update { it.copy(locationName = if (name.isNotBlank()) name else "Dublin, Ireland") }
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
        val sunriseStr = sunriseInstant?.let {
            formatClock12(
                it.toLocalDateTime(tz).hour,
                it.toLocalDateTime(tz).minute
            )
        } ?: "--:--"
        val sunsetStr = maghribInstant?.let {
            formatClock12(
                it.toLocalDateTime(tz).hour,
                it.toLocalDateTime(tz).minute
            )
        } ?: "--:--"
        val daylightStr = if (sunriseInstant != null && maghribInstant != null) {
            val mins = (maghribInstant - sunriseInstant).inWholeMinutes
            "${mins / 60}h ${mins % 60}m"
        } else ""
        val moon = MoonPhase.fractionForEpochMillis(
            date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )

        _state.update {
            it.copy(
                sunrise = sunriseStr,
                sunset = sunsetStr,
                daylight = daylightStr,
                methodLabel = "${prettyMethod(calcMethod)} · ${prettyAsr(asrCalc)}",
                moonFraction = moon,
            )
        }

        observeStatuses(date)
        applyTick()
    }

    private fun observeStatuses(date: LocalDate) {
        statusJob?.cancel()
        val dateKey = date.toEpochDay() * 86_400_000L
        statusJob = viewModelScope.launch {
            prayerRepository.getPrayerRecordsForDate(dateKey).collect { records ->
                statuses = records.associate { it.prayerName to it.status }
                applyTick()
            }
        }
    }

    /** Rebuild the display list + countdown + sky labels against the clock. */
    private fun applyTick() {
        if (dayTimes.isEmpty()) return
        val date = _state.value.selectedDate
        val today = LocalDate.now()
        val isToday = date == today
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val nowTime = now.toLocalTime()
        val nowMillis = Instant.now().toEpochMilli()

        var displays = dayTimes.map { pt ->
            val local =
                Instant.ofEpochMilli(pt.time.toEpochMilliseconds()).atZone(zone).toLocalDateTime()
            val isPassed = isToday && local.toLocalTime().isBefore(nowTime)
            PrayerTimeDisplay(
                type = pt.type,
                name = pt.type.displayName,
                time = formatClock12(local.hour, local.minute),
                isPassed = isPassed,
                isCurrent = false,
                isNext = false,
                prayerStatus = statuses[PrayerName.valueOf(pt.type.name)]
                    ?: PrayerStatus.NOT_PRAYED,
            )
        }

        var nextName = ""
        var countdown = ""
        if (isToday) {
            val nextIndex = displays.indexOfFirst { !it.isPassed }
            val currentIndex = if (nextIndex > 0) nextIndex - 1 else displays.lastIndex
            displays = displays.mapIndexed { i, d ->
                d.copy(isCurrent = i == currentIndex, isNext = i == nextIndex)
            }
            if (nextIndex >= 0) {
                nextName = displays[nextIndex].type.displayName
                val nextInstant = dayTimes.firstOrNull { it.type == displays[nextIndex].type }?.time
                if (nextInstant != null) countdown =
                    formatCountdown((nextInstant.toEpochMilliseconds() - nowMillis) / 1000)
            } else {
                // All of today's prayers have passed — count down to tomorrow's Fajr.
                nextName = PrayerType.FAJR.displayName
                val tomorrow = prayerTimeCalculator.getPrayerTimes(
                    latitude = latitude, longitude = longitude, date = today.plusDays(1),
                    calculationMethod = calcMethod, asrCalculation = asrCalc,
                    highLatitudeRule = highLatRule, adjustments = adjustments,
                )
                val fajr = tomorrow.firstOrNull { it.type == PrayerType.FAJR }?.time
                if (fajr != null) countdown =
                    formatCountdown((fajr.toEpochMilliseconds() - nowMillis) / 1000)
            }
        }

        // Sky reflects "now" (quantised to the minute so the baked scene only
        // rebuilds once a minute, not every countdown tick).
        val minuteOfDay = now.hour * 60 + now.minute
        val timeOfDay = minuteOfDay / 1440f

        val skyTimeLabel = if (isToday) {
            formatClock12(now.hour, now.minute)
        } else {
            date.format(DATE_FMT)
        }
        val skyStatusLabel = if (isToday) {
            if (countdown.isNotEmpty()) "$nextName in $countdown" else nextName
        } else {
            val rel = daysFromToday(date, today)
            "$rel · ${_state.value.sunrise} — ${_state.value.sunset}"
        }

        _state.update {
            it.copy(
                isToday = isToday,
                prayers = displays,
                nextPrayerName = nextName,
                timeUntilNext = countdown,
                timeOfDay = timeOfDay,
                skyTimeLabel = skyTimeLabel,
                skyStatusLabel = skyStatusLabel,
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
            prayerRepository.updatePrayerStatus(dateKey, name, newStatus, prayedAt, false)
            // getPrayerRecordsForDate re-emits → applyTick refreshes the UI.
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                applyTick()
                kotlinx.coroutines.delay(1_000)
            }
        }
    }

    // ── formatting helpers ──────────────────────────────────────────────
    private fun formatClock12(hour: Int, minute: Int): String {
        val h = if (hour % 12 == 0) 12 else hour % 12
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }

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

    private fun prettyMethod(m: CalculationMethod): String = when (m) {
        CalculationMethod.MUSLIM_WORLD_LEAGUE -> "MWL"
        CalculationMethod.EGYPTIAN -> "Egyptian"
        CalculationMethod.KARACHI -> "Karachi"
        CalculationMethod.UMM_AL_QURA -> "Umm al-Qura"
        CalculationMethod.DUBAI -> "Dubai"
        CalculationMethod.MOON_SIGHTING_COMMITTEE -> "Moonsighting"
        CalculationMethod.NORTH_AMERICA -> "ISNA"
        CalculationMethod.KUWAIT -> "Kuwait"
        CalculationMethod.QATAR -> "Qatar"
        CalculationMethod.SINGAPORE -> "Singapore"
        CalculationMethod.TURKEY -> "Turkey"
    }

    private fun prettyAsr(a: AsrCalculation): String = when (a) {
        AsrCalculation.HANAFI -> "Hanafi"
        AsrCalculation.STANDARD -> "Standard"
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    }
}
