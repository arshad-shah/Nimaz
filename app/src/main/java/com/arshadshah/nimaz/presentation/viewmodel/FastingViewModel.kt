package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.repository.FastingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class FastingTrackerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayRecord: FastRecord? = null,
    val isFastingToday: Boolean = false,
    val selectedFastType: FastType = FastType.VOLUNTARY,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class RamadanTrackerUiState(
    val ramadanRecords: List<FastRecord> = emptyList(),
    val fastedDays: Int = 0,
    val missedDays: Int = 0,
    val remainingDays: Int = 0,
    val currentDay: Int = 0,
    val isRamadan: Boolean = false,
    val isLoading: Boolean = true
)

data class FastingCalendarUiState(
    val records: List<FastRecord> = emptyList(),
    val selectedMonth: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val isLoading: Boolean = true
)

data class MakeupFastsUiState(
    val pendingMakeupFasts: List<MakeupFast> = emptyList(),
    val allMakeupFasts: List<MakeupFast> = emptyList(),
    val pendingCount: Int = 0,
    val totalFidyaPaid: Double = 0.0,
    val isLoading: Boolean = true
)

data class FastingStatsUiState(
    val stats: FastingStats? = null,
    val ramadanFastedCount: Int = 0,
    val voluntaryFastCount: Int = 0,
    val period: FastingStatsPeriod = FastingStatsPeriod.THIS_YEAR,
    val isLoading: Boolean = true
)

enum class FastingStatsPeriod {
    THIS_MONTH, THIS_YEAR, ALL_TIME
}

sealed interface FastingEvent {
    data class SelectDate(val date: LocalDate) : FastingEvent
    data class StartFast(val date: LocalDate, val fastType: FastType) : FastingEvent
    data class CompleteFast(val date: LocalDate) : FastingEvent
    data class BreakFast(val date: LocalDate) : FastingEvent
    data class MissFast(val date: LocalDate, val reason: String?) : FastingEvent
    data class SetFastType(val fastType: FastType) : FastingEvent
    data class SelectMonth(val month: Int, val year: Int) : FastingEvent
    data class AddMakeupFast(val makeupFast: MakeupFast) : FastingEvent
    data class CompleteMakeupFast(val makeupFastId: Long) : FastingEvent
    data class PayFidya(val makeupFastId: Long, val amount: Double) : FastingEvent
    data class SetStatsPeriod(val period: FastingStatsPeriod) : FastingEvent
    data object LoadToday : FastingEvent
    data object LoadRamadan : FastingEvent
    data object LoadMakeupFasts : FastingEvent
    data object LoadStats : FastingEvent
    data object ToggleTodayFast : FastingEvent
}

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val fastingRepository: FastingRepository
) : ViewModel() {

    private val _trackerState = MutableStateFlow(FastingTrackerUiState())
    val trackerState: StateFlow<FastingTrackerUiState> = _trackerState.asStateFlow()

    private val _ramadanState = MutableStateFlow(RamadanTrackerUiState())
    val ramadanState: StateFlow<RamadanTrackerUiState> = _ramadanState.asStateFlow()

    private val _calendarState = MutableStateFlow(FastingCalendarUiState())
    val calendarState: StateFlow<FastingCalendarUiState> = _calendarState.asStateFlow()

    private val _makeupState = MutableStateFlow(MakeupFastsUiState())
    val makeupState: StateFlow<MakeupFastsUiState> = _makeupState.asStateFlow()

    private val _statsState = MutableStateFlow(FastingStatsUiState())
    val statsState: StateFlow<FastingStatsUiState> = _statsState.asStateFlow()

    init {
        loadToday()
        loadCalendarMonth()
        loadMakeupFasts()
        loadStats()
    }

    fun onEvent(event: FastingEvent) {
        when (event) {
            is FastingEvent.SelectDate -> selectDate(event.date)
            is FastingEvent.StartFast -> startFast(event.date, event.fastType)
            is FastingEvent.CompleteFast -> completeFast(event.date)
            is FastingEvent.BreakFast -> breakFast(event.date)
            is FastingEvent.MissFast -> missFast(event.date, event.reason)
            is FastingEvent.SetFastType -> _trackerState.update { it.copy(selectedFastType = event.fastType) }
            is FastingEvent.SelectMonth -> selectMonth(event.month, event.year)
            is FastingEvent.AddMakeupFast -> addMakeupFast(event.makeupFast)
            is FastingEvent.CompleteMakeupFast -> completeMakeupFast(event.makeupFastId)
            is FastingEvent.PayFidya -> payFidya(event.makeupFastId, event.amount)
            is FastingEvent.SetStatsPeriod -> setStatsPeriod(event.period)
            FastingEvent.LoadToday -> loadToday()
            FastingEvent.LoadRamadan -> loadRamadan()
            FastingEvent.LoadMakeupFasts -> loadMakeupFasts()
            FastingEvent.LoadStats -> loadStats()
            FastingEvent.ToggleTodayFast -> toggleTodayFast()
        }
    }

    private fun loadToday() {
        selectDate(LocalDate.now())
    }

    private fun selectDate(date: LocalDate) {
        _trackerState.update { it.copy(selectedDate = date, isLoading = true) }

        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            val record = fastingRepository.getFastRecordForDate(dateEpoch)
            _trackerState.update {
                it.copy(
                    todayRecord = record,
                    isFastingToday = record?.status == FastStatus.FASTED,
                    isLoading = false
                )
            }
        }
    }

    private fun startFast(date: LocalDate, fastType: FastType) {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val record = FastRecord(
                id = 0,
                date = dateEpoch,
                hijriDate = null, // Would be calculated
                hijriMonth = null,
                hijriYear = null,
                fastType = fastType,
                status = FastStatus.FASTED,
                exemptionReason = null,
                suhoorTime = null,
                iftarTime = null,
                note = null,
                createdAt = now,
                updatedAt = now
            )
            fastingRepository.insertFastRecord(record)
            selectDate(date)
            loadStats()
        }
    }

    private fun completeFast(date: LocalDate) {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            fastingRepository.updateFastStatus(dateEpoch, FastStatus.FASTED)
            selectDate(date)
            loadStats()
        }
    }

    private fun breakFast(date: LocalDate) {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            fastingRepository.updateFastStatus(dateEpoch, FastStatus.NOT_FASTED)
            selectDate(date)
            loadStats()
        }
    }

    private fun missFast(date: LocalDate, reason: String?) {
        val dateEpoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            val existingRecord = fastingRepository.getFastRecordForDate(dateEpoch)
            if (existingRecord != null) {
                fastingRepository.updateFastStatus(dateEpoch, FastStatus.NOT_FASTED)
            } else {
                val now = System.currentTimeMillis()
                val record = FastRecord(
                    id = 0,
                    date = dateEpoch,
                    hijriDate = null,
                    hijriMonth = null,
                    hijriYear = null,
                    fastType = FastType.RAMADAN,
                    status = FastStatus.NOT_FASTED,
                    exemptionReason = null,
                    suhoorTime = null,
                    iftarTime = null,
                    note = reason,
                    createdAt = now,
                    updatedAt = now
                )
                fastingRepository.insertFastRecord(record)
            }
            selectDate(date)
            loadStats()
        }
    }

    private fun toggleTodayFast() {
        val today = LocalDate.now()
        val currentRecord = _trackerState.value.todayRecord

        if (currentRecord == null) {
            startFast(today, _trackerState.value.selectedFastType)
        } else {
            when (currentRecord.status) {
                FastStatus.FASTED -> breakFast(today) // Toggle off
                FastStatus.NOT_FASTED -> startFast(today, _trackerState.value.selectedFastType)
                else -> {} // Do nothing for other statuses
            }
        }
    }

    private fun selectMonth(month: Int, year: Int) {
        _calendarState.update { it.copy(selectedMonth = month, selectedYear = year, isLoading = true) }
        loadCalendarMonth()
    }

    private fun loadCalendarMonth() {
        val month = _calendarState.value.selectedMonth
        val year = _calendarState.value.selectedYear

        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)

        val startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            fastingRepository.getFastRecordsInRange(startEpoch, endEpoch).collect { records ->
                _calendarState.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    private fun loadRamadan() {
        // This would need Hijri calendar support to determine Ramadan dates
        // For now, loading records by Hijri month 9 (Ramadan)
        viewModelScope.launch {
            fastingRepository.getFastRecordsByHijriMonth(9).collect { records ->
                val fasted = records.count { it.status == FastStatus.FASTED }
                val missed = records.count { it.status == FastStatus.NOT_FASTED }

                _ramadanState.update {
                    it.copy(
                        ramadanRecords = records,
                        fastedDays = fasted,
                        missedDays = missed,
                        remainingDays = 30 - records.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadMakeupFasts() {
        viewModelScope.launch {
            fastingRepository.getPendingMakeupFasts().collect { pending ->
                _makeupState.update { it.copy(pendingMakeupFasts = pending, pendingCount = pending.size) }
            }
        }
        viewModelScope.launch {
            fastingRepository.getAllMakeupFasts().collect { all ->
                _makeupState.update { it.copy(allMakeupFasts = all) }
            }
        }
        viewModelScope.launch {
            val totalFidya = fastingRepository.getTotalFidyaPaid()
            _makeupState.update { it.copy(totalFidyaPaid = totalFidya, isLoading = false) }
        }
    }

    private fun addMakeupFast(makeupFast: MakeupFast) {
        viewModelScope.launch {
            fastingRepository.insertMakeupFast(makeupFast)
            loadMakeupFasts()
        }
    }

    private fun completeMakeupFast(makeupFastId: Long) {
        viewModelScope.launch {
            fastingRepository.markMakeupFastCompleted(makeupFastId, System.currentTimeMillis())
            loadMakeupFasts()
            loadStats()
        }
    }

    private fun payFidya(makeupFastId: Long, amount: Double) {
        viewModelScope.launch {
            fastingRepository.markFidyaPaid(makeupFastId, amount)
            loadMakeupFasts()
        }
    }

    private fun setStatsPeriod(period: FastingStatsPeriod) {
        _statsState.update { it.copy(period = period, isLoading = true) }
        loadStats()
    }

    private fun loadStats() {
        val period = _statsState.value.period
        val now = LocalDate.now()

        val (startDate, endDate) = when (period) {
            FastingStatsPeriod.THIS_MONTH -> now.withDayOfMonth(1) to now
            FastingStatsPeriod.THIS_YEAR -> now.withDayOfYear(1) to now
            FastingStatsPeriod.ALL_TIME -> now.minusYears(10) to now
        }

        val startEpoch = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endEpoch = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000

        viewModelScope.launch {
            val stats = fastingRepository.getFastingStats(startEpoch, endEpoch)
            val ramadanCount = fastingRepository.getRamadanFastedCount()
            val voluntaryCount = fastingRepository.getVoluntaryFastCount()

            _statsState.update {
                it.copy(
                    stats = stats,
                    ramadanFastedCount = ramadanCount,
                    voluntaryFastCount = voluntaryCount,
                    isLoading = false
                )
            }
        }
    }
}
