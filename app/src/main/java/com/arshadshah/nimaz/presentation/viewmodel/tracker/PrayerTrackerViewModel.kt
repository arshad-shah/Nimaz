package com.arshadshah.nimaz.presentation.viewmodel.tracker

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
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

@HiltViewModel
class PrayerTrackerViewModel @Inject constructor(
    private val prayerUseCases: PrayerUseCases,
    private val todayProvider: TodayProvider,
    private val telemetry: Telemetry
) : ViewModel() {

    private val _trackerState =
        MutableStateFlow(PrayerTrackerUiState(selectedDate = todayProvider.today()))
    val trackerState: StateFlow<PrayerTrackerUiState> = _trackerState.asStateFlow()

    private val _statsState = MutableStateFlow(PrayerStatsUiState())
    val statsState: StateFlow<PrayerStatsUiState> = _statsState.asStateFlow()

    private val _qadaState = MutableStateFlow(QadaPrayersUiState())
    val qadaState: StateFlow<QadaPrayersUiState> = _qadaState.asStateFlow()

    private val _historyState = MutableStateFlow(
        todayProvider.today().let { today ->
            PrayerHistoryUiState(startDate = today.minusDays(30), endDate = today)
        }
    )
    val historyState: StateFlow<PrayerHistoryUiState> = _historyState.asStateFlow()

    /**
     * The latest calculation-settings snapshot, held so [selectDate] can compute a day's
     * schedule without re-collecting the flow. Null only before the first emission.
     */
    private var calculationSettings: PrayerCalculationSettings? = null
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
        observeCalculationSettings()
        loadToday()
        loadStats()
        loadQadaPrayers()
    }

    fun onEvent(event: PrayerTrackerEvent) {
        when (event) {
            is PrayerTrackerEvent.SelectDate -> {
                telemetry.featureUsed(DOMAIN, "select_date")
                selectDate(event.date)
            }

            is PrayerTrackerEvent.SetPrayerStatus -> {
                telemetry.prayerTracked(
                    event.prayerName.name,
                    event.status?.name ?: "cleared",
                    false
                )
                setPrayerStatus(event.prayerName, event.status)
            }

            is PrayerTrackerEvent.ConfirmUnrecordedAsMissed -> {
                telemetry.featureUsed(DOMAIN, "confirm_unrecorded_missed")
                confirmUnrecordedAsMissed(event.from, event.to)
            }

            is PrayerTrackerEvent.MarkQadaCompleted -> {
                telemetry.featureUsed(DOMAIN, "qada_completed")
                markQadaCompleted(event.record)
            }

            is PrayerTrackerEvent.SetStatsPeriod -> {
                telemetry.featureUsed(DOMAIN, "set_stats_period")
                setStatsPeriod(event.period)
            }

            is PrayerTrackerEvent.LoadHistory -> {
                telemetry.featureUsed(DOMAIN, "load_history")
                loadHistory(event.startDate, event.endDate)
            }

            PrayerTrackerEvent.LoadToday -> loadToday()
            PrayerTrackerEvent.LoadStats -> loadStats()
            PrayerTrackerEvent.LoadQadaPrayers -> loadQadaPrayers()
            PrayerTrackerEvent.NavigateToPreviousDay -> navigateToPreviousDay()
            PrayerTrackerEvent.NavigateToNextDay -> navigateToNextDay()
        }
    }

    /**
     * The schedule the tracker measures the day against, from the settings every other
     * prayer-time surface reads.
     *
     * This used to observe `getCurrentLocation()` — the `locations` table, `isCurrentLocation =
     * 1` — and pass the row to `getPrayerTimesForDate(date, location)`. That row is written by
     * exactly one path, searching for a place and picking it; detecting by GPS, finishing
     * onboarding and the home screen's own picker all write the **preference store** and nothing
     * else. So for a user whose location came from GPS the table was empty, `currentLocation`
     * stayed null, and the tracker alone had no times at all while every other screen had them —
     * which is what made the day card announce "Day complete" over five rows correctly reading
     * UPCOMING.
     *
     * It also fixes a quieter divergence. `getPrayerTimesForDate(date, location)` takes its
     * method and school from the *row's* own columns and applies no per-prayer adjustments, so
     * even a user with a row could see the tracker disagree with Home about when Asr was.
     * `FastingViewModel` had the same defect and was moved to this same snapshot; this was the
     * last surface still on the old path.
     */
    private fun observeCalculationSettings() {
        // Started once from `init`, so it needs no handle (§4.1).
        launchSafely(telemetry, DOMAIN, "observe_calculation_settings") {
            prayerUseCases.observeCalculationSettings().collect { settings ->
                calculationSettings = settings
                // Not just today's: a change of method has to move the day being *looked at*.
                loadPrayerTimes(_trackerState.value.selectedDate, settings)
            }
        }
    }

    private fun loadToday() {
        selectDate(todayProvider.today())
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

        // The settings arrive asynchronously; until the first emission there is nothing to
        // compute with, and the observer above will fill this day in when they land.
        calculationSettings?.let { settings ->
            loadPrayerTimes(date, settings)
        }
    }

    private fun loadPrayerTimes(date: LocalDate, settings: PrayerCalculationSettings) {
        val prayerTimes = prayerUseCases.getPrayerTimesForDate(date, settings)
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

    private fun setPrayerStatus(prayerName: PrayerName, status: PrayerStatus?) {
        // Clearing is a write of NOT_PRAYED rather than a delete. The display derivation treats
        // NOT_PRAYED as absence, so the row reads back as "not recorded" — and a status the user
        // withdrew leaving no row at all would be indistinguishable from one never touched,
        // which is a distinction sync and the widget both care about.
        updatePrayerStatus(prayerName, status ?: PrayerStatus.NOT_PRAYED, isJamaah = false)
    }

    private fun confirmUnrecordedAsMissed(from: LocalDate, to: LocalDate) {
        launchSafely(telemetry, DOMAIN, "confirm_unrecorded_missed") {
            prayerUseCases.markUnrecordedAsMissed(
                from.toUtcMidnightMillis(),
                to.toUtcMidnightMillis(),
            )
            // No reload. The qada list, the stats and the selected day are all Room-backed
            // observers of this table, so the write re-emits to every one of them.
        }
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
        val now = todayProvider.today()

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
        if (!nextDay.isAfter(todayProvider.today())) {
            selectDate(nextDay)
        }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.PRAYER_TRACKER
    }
}
