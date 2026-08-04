package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class PrayerTrackerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val prayerRecords: List<PrayerRecord> = emptyList(),
    val prayerTimes: PrayerTimes? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PrayerStatsUiState(
    val stats: PrayerStats? = null,
    val monthlyStats: PrayerStats? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val period: StatsPeriod = StatsPeriod.WEEK,
    val isLoading: Boolean = true
)

data class QadaPrayersUiState(
    val missedPrayers: List<PrayerRecord> = emptyList(),
    val groupedByMonth: Map<String, List<PrayerRecord>> = emptyMap(),
    val totalMissed: Int = 0,
    val isLoading: Boolean = true
)

data class PrayerHistoryUiState(
    val records: List<PrayerRecord> = emptyList(),
    val startDate: LocalDate = LocalDate.now().minusDays(30),
    val endDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true
)

enum class StatsPeriod {
    WEEK, MONTH, YEAR, ALL_TIME
}

/**
 * The **inclusive** first and last day a [StatsPeriod] covers, ending today.
 *
 * Every window used to start a day too early: `WEEK` ran `today-7 .. today` and the caller
 * then made the end exclusive with `plusDays(1)`, so the query spanned eight days. A user
 * who prayed all five prayers for seven days but missed three on the eighth day back read
 * "37/40" under a chip labelled *Week*. The end is still made exclusive by the caller, so a
 * window of `n` days starts `n - 1` days back.
 *
 * `ALL_TIME` is a floor rather than a window — nothing is counted out of it — so it keeps its
 * ten-year reach.
 */
internal fun statsWindow(period: StatsPeriod, today: LocalDate): Pair<LocalDate, LocalDate> =
    when (period) {
        StatsPeriod.WEEK -> today.minusDays(6)
        StatsPeriod.MONTH -> today.minusMonths(1).plusDays(1)
        StatsPeriod.YEAR -> today.minusYears(1).plusDays(1)
        StatsPeriod.ALL_TIME -> today.minusYears(10)
    } to today

sealed interface PrayerTrackerEvent {
    data class SelectDate(val date: LocalDate) : PrayerTrackerEvent
    data class UpdatePrayerStatus(
        val prayerName: PrayerName,
        val status: PrayerStatus,
        val isJamaah: Boolean = false
    ) : PrayerTrackerEvent

    data class MarkPrayerPrayed(val prayerName: PrayerName, val isJamaah: Boolean = false) :
        PrayerTrackerEvent

    data class MarkPrayerMissed(val prayerName: PrayerName) : PrayerTrackerEvent
    data class MarkQadaCompleted(val record: PrayerRecord) : PrayerTrackerEvent
    data class SetStatsPeriod(val period: StatsPeriod) : PrayerTrackerEvent
    data class LoadHistory(val startDate: LocalDate, val endDate: LocalDate) : PrayerTrackerEvent
    data object LoadToday : PrayerTrackerEvent
    data object LoadStats : PrayerTrackerEvent
    data object LoadQadaPrayers : PrayerTrackerEvent
    data object NavigateToPreviousDay : PrayerTrackerEvent
    data object NavigateToNextDay : PrayerTrackerEvent
}

@HiltViewModel
class PrayerTrackerViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val telemetry: Telemetry
) : ViewModel() {

    private val _trackerState = MutableStateFlow(PrayerTrackerUiState())
    val trackerState: StateFlow<PrayerTrackerUiState> = _trackerState.asStateFlow()

    private val _statsState = MutableStateFlow(PrayerStatsUiState())
    val statsState: StateFlow<PrayerStatsUiState> = _statsState.asStateFlow()

    private val _qadaState = MutableStateFlow(QadaPrayersUiState())
    val qadaState: StateFlow<QadaPrayersUiState> = _qadaState.asStateFlow()

    private val _historyState = MutableStateFlow(PrayerHistoryUiState())
    val historyState: StateFlow<PrayerHistoryUiState> = _historyState.asStateFlow()

    private var currentLocation: Location? = null
    private var dateRecordsJob: Job? = null

    // The history range is re-requested every time the user changes period. Like
    // `dateRecordsJob` above, this collects a Room flow that never completes, so without a
    // handle each range left a collector alive on `_historyState` and an earlier range could
    // redraw the chart under a later one. (AP-7.1b.)
    private var historyJob: Job? = null

    // The same hazard twice more, both missed when the two above were fixed — because neither
    // takes a parameter, so the sweep that found those read them as one-shot observers.
    // `loadStats` is re-entered from `init`, `SetStatsPeriod` and `LoadStats` (it takes its
    // period from `_statsState`), and `loadQadaPrayers` from `init` and `LoadQadaPrayers`.
    // Un-handled, `loadQadaPrayers` left a collector per call and `loadStats` a read in
    // flight per call, the slowest of which won.
    private var statsJob: Job? = null
    private var qadaJob: Job? = null

    init {
        loadCurrentLocation()
        loadToday()
        loadStats()
        loadQadaPrayers()
    }

    fun onEvent(event: PrayerTrackerEvent) {
        when (event) {
            is PrayerTrackerEvent.SelectDate -> selectDate(event.date)
            is PrayerTrackerEvent.UpdatePrayerStatus -> {
                telemetry.prayerTracked(
                        event.prayerName.name,
                        event.status.name,
                        event.isJamaah
                    )
                updatePrayerStatus(
                    event.prayerName,
                    event.status,
                    event.isJamaah
                )
            }

            is PrayerTrackerEvent.MarkPrayerPrayed -> {
                telemetry.prayerTracked(
                        event.prayerName.name,
                        PrayerStatus.PRAYED.name,
                        event.isJamaah
                    )
                markPrayerPrayed(
                    event.prayerName,
                    event.isJamaah
                )
            }

            is PrayerTrackerEvent.MarkPrayerMissed -> {
                telemetry.prayerTracked(event.prayerName.name, PrayerStatus.MISSED.name)
                markPrayerMissed(event.prayerName)
            }
            is PrayerTrackerEvent.MarkQadaCompleted -> {
                telemetry.featureUsed(DOMAIN, "qada_completed")
                markQadaCompleted(event.record)
            }
            is PrayerTrackerEvent.SetStatsPeriod -> setStatsPeriod(event.period)
            is PrayerTrackerEvent.LoadHistory -> loadHistory(event.startDate, event.endDate)
            PrayerTrackerEvent.LoadToday -> loadToday()
            PrayerTrackerEvent.LoadStats -> loadStats()
            PrayerTrackerEvent.LoadQadaPrayers -> loadQadaPrayers()
            PrayerTrackerEvent.NavigateToPreviousDay -> navigateToPreviousDay()
            PrayerTrackerEvent.NavigateToNextDay -> navigateToNextDay()
        }
    }

    private fun loadCurrentLocation() {
        // Started once from `init`, so it needs no handle (§4.1).
        launchSafely(telemetry, DOMAIN, "observe_location") {
            prayerUseCases.getCurrentLocation().collect { location ->
                currentLocation = location
                // Reload prayer times if we have a location
                location?.let {
                    loadPrayerTimes(_trackerState.value.selectedDate, it)
                }
            }
        }
    }

    private fun loadToday() {
        selectDate(LocalDate.now())
    }

    private fun selectDate(date: LocalDate) {
        _trackerState.update { it.copy(selectedDate = date, isLoading = true) }

        val dateEpoch = date.toUtcMidnightMillis()

        // Cancel previous date's Flow collection before starting new one
        dateRecordsJob?.cancel()
        dateRecordsJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_date_records",
            onFailure = { _trackerState.update { it.copy(isLoading = false) } }
        ) {
            // Room's reactive Flow ensures cross-screen sync: when HomeScreen or
            // PrayerTracker updates a prayer status via the repository, Room emits
            // the change to all active Flow collectors automatically.
            prayerUseCases.getPrayerRecordsForDate(dateEpoch).collect { records ->
                _trackerState.update {
                    it.copy(prayerRecords = records, isLoading = false)
                }
            }
        }

        // Load prayer times if we have location
        currentLocation?.let { location ->
            loadPrayerTimes(date, location)
        }
    }

    private fun loadPrayerTimes(date: LocalDate, location: Location) {
        val prayerTimes = prayerUseCases.getPrayerTimesForDate(date, location)
        _trackerState.update { it.copy(prayerTimes = prayerTimes) }
    }

    private fun updatePrayerStatus(
        prayerName: PrayerName,
        status: PrayerStatus,
        isJamaah: Boolean
    ) {
        val date = _trackerState.value.selectedDate
        val dateEpoch = date.toUtcMidnightMillis()
        val prayedAt = if (status == PrayerStatus.PRAYED || status == PrayerStatus.LATE) {
            System.currentTimeMillis()
        } else null

        launchSafely(telemetry, DOMAIN, "update_status") {
            prayerUseCases.updatePrayerStatus(dateEpoch, prayerName, status, prayedAt, isJamaah)
            // No `loadStats()` here. The stats collector observes the same table, so Room
            // re-emits to it on this write. Re-reading imperatively is what put a second
            // load in flight and let a period change race it.
        }
    }

    private fun markPrayerPrayed(prayerName: PrayerName, isJamaah: Boolean) {
        updatePrayerStatus(prayerName, PrayerStatus.PRAYED, isJamaah)
    }

    private fun markPrayerMissed(prayerName: PrayerName) {
        updatePrayerStatus(prayerName, PrayerStatus.MISSED, false)
    }

    private fun markQadaCompleted(record: PrayerRecord) {
        launchSafely(telemetry, DOMAIN, "complete_qada") {
            prayerUseCases.updatePrayerStatus(
                record.date,
                record.prayerName,
                PrayerStatus.QADA, // Mark as QADA completed
                System.currentTimeMillis(),
                false
            )
            // The qada list and the stats are both Room-backed observers of this table;
            // the write re-emits to both. Re-calling the loaders here is what left three
            // live collectors on the missed-prayer list after two completions.
        }
    }

    private fun setStatsPeriod(period: StatsPeriod) {
        _statsState.update { it.copy(period = period, isLoading = true) }
        loadStats()
    }

    private fun loadStats() {
        val period = _statsState.value.period
        val now = LocalDate.now()

        val (startDate, endDate) = statsWindow(period, now)
        val startEpoch = startDate.toUtcMidnightMillis()
        val endEpoch = endDate.plusDays(1).toUtcMidnightMillis()
        val currentEpoch = now.toUtcMidnightMillis()

        // The month summary is shown alongside every period, so it is always read.
        val (monthStart, monthEnd) = statsWindow(StatsPeriod.MONTH, now)
        val monthStartEpoch = monthStart.toUtcMidnightMillis()
        val monthEndEpoch = monthEnd.plusDays(1).toUtcMidnightMillis()

        statsJob?.cancel()
        statsJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_stats",
            onFailure = { _statsState.update { it.copy(isLoading = false) } }
        ) {
            // The four reads below are one-shot, but every number they produce is a view of
            // `prayer_records` — a table Home, the widget and this screen all write. Read
            // once and they freeze: marking Fajr on the Home card while the tracker sat in
            // the back stack left the streak card showing the streak from before the tap.
            // Collecting the records for the same window turns them into a subscription;
            // Room re-emits on any write to the table, so the numbers follow it.
            //
            // `collectLatest` cancels a recompute a newer emission has overtaken, so a burst
            // of writes costs one set of reads rather than one per emission.
            prayerUseCases.getPrayerRecordsInRange(startEpoch, endEpoch).collectLatest {
                val stats = prayerUseCases.getPrayerStats(startEpoch, endEpoch)
                val monthlyStats = prayerUseCases.getPrayerStats(monthStartEpoch, monthEndEpoch)
                val currentStreak = prayerUseCases.getCurrentStreak(currentEpoch)
                val longestStreak = prayerUseCases.getLongestStreak()

                // Cancelling the previous job stops an abandoned period at its next
                // suspension point — and after the last read there isn't one, so a load
                // that finished just as the period changed could still write. The captured
                // period is checked against the live one so it cannot.
                if (_statsState.value.period != period) return@collectLatest

                _statsState.update {
                    it.copy(
                        stats = stats,
                        monthlyStats = monthlyStats,
                        currentStreak = currentStreak,
                        longestStreak = longestStreak,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadQadaPrayers() {
        qadaJob?.cancel()
        qadaJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_qada",
            onFailure = { _qadaState.update { it.copy(isLoading = false) } }
        ) {
            prayerUseCases.getMissedPrayersRequiringQada().collect { missedPrayers ->
                val grouped = missedPrayers.groupBy { record ->
                    val date = LocalDate.ofEpochDay(record.date / (24 * 60 * 60 * 1000))
                    "${date.month.name} ${date.year}"
                }

                _qadaState.update {
                    it.copy(
                        missedPrayers = missedPrayers,
                        groupedByMonth = grouped,
                        totalMissed = missedPrayers.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadHistory(startDate: LocalDate, endDate: LocalDate) {
        _historyState.update { it.copy(startDate = startDate, endDate = endDate, isLoading = true) }

        val startEpoch = startDate.toUtcMidnightMillis()
        val endEpoch = endDate.plusDays(1).toUtcMidnightMillis()

        historyJob?.cancel()
        historyJob = launchSafely(
            telemetry,
            DOMAIN,
            "load_history",
            onFailure = { _historyState.update { it.copy(isLoading = false) } }
        ) {
            prayerUseCases.getPrayerRecordsInRange(startEpoch, endEpoch).collect { records ->
                _historyState.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    private fun navigateToPreviousDay() {
        selectDate(_trackerState.value.selectedDate.minusDays(1))
    }

    private fun navigateToNextDay() {
        val nextDay = _trackerState.value.selectedDate.plusDays(1)
        if (!nextDay.isAfter(LocalDate.now())) {
            selectDate(nextDay)
        }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.PRAYER_TRACKER
    }
}
