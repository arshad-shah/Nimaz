package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
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

sealed interface PrayerTrackerEvent {
    data class SelectDate(val date: LocalDate) : PrayerTrackerEvent
    data class UpdatePrayerStatus(
        val prayerName: PrayerName,
        val status: PrayerStatus,
        val isJamaah: Boolean = false
    ) : PrayerTrackerEvent
    data class MarkPrayerPrayed(val prayerName: PrayerName, val isJamaah: Boolean = false) : PrayerTrackerEvent
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
    private val prayerRepository: PrayerRepository
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
    private var dateRecordsJob: kotlinx.coroutines.Job? = null

    init {
        loadCurrentLocation()
        loadToday()
        loadStats()
        loadQadaPrayers()
    }

    fun onEvent(event: PrayerTrackerEvent) {
        when (event) {
            is PrayerTrackerEvent.SelectDate -> selectDate(event.date)
            is PrayerTrackerEvent.UpdatePrayerStatus -> updatePrayerStatus(
                event.prayerName,
                event.status,
                event.isJamaah
            )
            is PrayerTrackerEvent.MarkPrayerPrayed -> markPrayerPrayed(event.prayerName, event.isJamaah)
            is PrayerTrackerEvent.MarkPrayerMissed -> markPrayerMissed(event.prayerName)
            is PrayerTrackerEvent.MarkQadaCompleted -> markQadaCompleted(event.record)
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
        viewModelScope.launch {
            prayerRepository.getCurrentLocation().collect { location ->
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

        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        // Cancel previous date's Flow collection before starting new one
        dateRecordsJob?.cancel()
        dateRecordsJob = viewModelScope.launch {
            // Room's reactive Flow ensures cross-screen sync: when HomeScreen or
            // PrayerTracker updates a prayer status via the repository, Room emits
            // the change to all active Flow collectors automatically.
            prayerRepository.getPrayerRecordsForDate(dateEpoch).collect { records ->
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
        val prayerTimes = prayerRepository.getPrayerTimesForDate(date, location)
        _trackerState.update { it.copy(prayerTimes = prayerTimes) }
    }

    private fun updatePrayerStatus(prayerName: PrayerName, status: PrayerStatus, isJamaah: Boolean) {
        val date = _trackerState.value.selectedDate
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val prayedAt = if (status == PrayerStatus.PRAYED || status == PrayerStatus.LATE) {
            System.currentTimeMillis()
        } else null

        viewModelScope.launch {
            prayerRepository.updatePrayerStatus(dateEpoch, prayerName, status, prayedAt, isJamaah)
            // Refresh stats after update
            loadStats()
        }
    }

    private fun markPrayerPrayed(prayerName: PrayerName, isJamaah: Boolean) {
        updatePrayerStatus(prayerName, PrayerStatus.PRAYED, isJamaah)
    }

    private fun markPrayerMissed(prayerName: PrayerName) {
        updatePrayerStatus(prayerName, PrayerStatus.MISSED, false)
    }

    private fun markQadaCompleted(record: PrayerRecord) {
        viewModelScope.launch {
            prayerRepository.updatePrayerStatus(
                record.date,
                record.prayerName,
                PrayerStatus.QADA, // Mark as QADA completed
                System.currentTimeMillis(),
                false
            )
            loadQadaPrayers()
            loadStats()
        }
    }

    private fun setStatsPeriod(period: StatsPeriod) {
        _statsState.update { it.copy(period = period, isLoading = true) }
        loadStats()
    }

    private fun loadStats() {
        val period = _statsState.value.period
        val now = LocalDate.now()

        val (startDate, endDate) = when (period) {
            StatsPeriod.WEEK -> now.minusDays(7) to now
            StatsPeriod.MONTH -> now.minusMonths(1) to now
            StatsPeriod.YEAR -> now.minusYears(1) to now
            StatsPeriod.ALL_TIME -> now.minusYears(10) to now
        }

        val startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val currentEpoch = now.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        // Always load monthly stats
        val monthStart = now.minusMonths(1)
        val monthStartEpoch = monthStart.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val monthEndEpoch = now.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            val stats = prayerRepository.getPrayerStats(startEpoch, endEpoch)
            val monthlyStats = prayerRepository.getPrayerStats(monthStartEpoch, monthEndEpoch)
            val currentStreak = prayerRepository.getCurrentStreak(currentEpoch)
            val longestStreak = prayerRepository.getLongestStreak()

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

    private fun loadQadaPrayers() {
        viewModelScope.launch {
            prayerRepository.getMissedPrayersRequiringQada().collect { missedPrayers ->
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

        val startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            prayerRepository.getPrayerRecordsInRange(startEpoch, endEpoch).collect { records ->
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
}
