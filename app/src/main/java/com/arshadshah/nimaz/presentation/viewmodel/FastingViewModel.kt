package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.domain.usecase.FastingUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class FastingTrackerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayRecord: FastRecord? = null,
    val isFastingToday: Boolean = false,
    val selectedFastType: FastType = FastType.VOLUNTARY,
    val suhoorTime: String = "--:-- AM",
    val iftarTime: String = "--:-- PM",
    val timeUntilIftar: String = "",
    val timeUntilSuhoor: String = "",
    val isSuhoorTime: Boolean = false,
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

data class FastManagementSheetState(
    val isVisible: Boolean = false,
    val date: LocalDate = LocalDate.now(),
    val existingRecord: FastRecord? = null,
    val selectedStatus: FastStatus = FastStatus.FASTED,
    val selectedFastType: FastType = FastType.VOLUNTARY,
    val selectedExemptionReason: ExemptionReason? = null,
    val note: String = ""
)

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
    data class OpenFastSheet(val date: LocalDate) : FastingEvent
    data object DismissFastSheet : FastingEvent
    data class SaveFastForDate(
        val date: LocalDate,
        val status: FastStatus,
        val fastType: FastType,
        val exemptionReason: ExemptionReason?,
        val note: String
    ) : FastingEvent

    data class DeleteFastRecord(val date: LocalDate) : FastingEvent
    data class UpdateMakeupFast(val makeupFast: MakeupFast) : FastingEvent
    data class LogRecommendedFast(val date: LocalDate, val fastType: FastType) : FastingEvent
}

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val fastingUseCases: FastingUseCases,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val settingsRepository: SettingsRepository
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

    private val _sheetState = MutableStateFlow(FastManagementSheetState())
    val sheetState: StateFlow<FastManagementSheetState> = _sheetState.asStateFlow()

    private var calendarJob: Job? = null
    private var ramadanJob: Job? = null
    private var makeupPendingJob: Job? = null
    private var makeupAllJob: Job? = null

    private var use24HourFormat: Boolean = false

    companion object {
        // Default location: Dublin, Ireland (fallback)
        private const val DEFAULT_LATITUDE = 53.3498
        private const val DEFAULT_LONGITUDE = -6.2603
    }

    init {
        loadToday()
        loadRamadan()
        observeLocationAndLoadPrayerTimes()
        loadCalendarMonth()
        loadMakeupFasts()
        loadStats()
    }

    private fun observeLocationAndLoadPrayerTimes() {
        viewModelScope.launch {
            combine(
                settingsRepository.latitude,
                settingsRepository.longitude,
                settingsRepository.use24HourFormat
            ) { lat, lng, h24 -> Triple(lat, lng, h24) }
                .collect { (lat, lng, h24) ->
                    use24HourFormat = h24
                    val latitude = if (lat != 0.0) lat else DEFAULT_LATITUDE
                    val longitude = if (lng != 0.0) lng else DEFAULT_LONGITUDE
                    loadPrayerTimes(latitude, longitude)
                }
        }
    }

    private fun loadPrayerTimes(latitude: Double, longitude: Double) {
        try {
            val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude)
            val timeZone = TimeZone.currentSystemDefault()

            val fajrPrayer = prayerTimes.find { it.type.name == "FAJR" }
            val maghribPrayer = prayerTimes.find { it.type.name == "MAGHRIB" }

            if (fajrPrayer != null && maghribPrayer != null) {
                val fajrLocalTime = fajrPrayer.time.toLocalDateTime(timeZone)
                val maghribLocalTime = maghribPrayer.time.toLocalDateTime(timeZone)

                val suhoorTimeStr = formatTime(fajrLocalTime.hour, fajrLocalTime.minute)
                val iftarTimeStr = formatTime(maghribLocalTime.hour, maghribLocalTime.minute)

                val now = LocalDateTime.now()
                val fajrJavaTime = LocalDateTime.of(
                    now.toLocalDate(),
                    java.time.LocalTime.of(fajrLocalTime.hour, fajrLocalTime.minute)
                )
                val maghribJavaTime = LocalDateTime.of(
                    now.toLocalDate(),
                    java.time.LocalTime.of(maghribLocalTime.hour, maghribLocalTime.minute)
                )

                val (countdown, isSuhoor) = calculateCountdown(now, fajrJavaTime, maghribJavaTime)

                _trackerState.update {
                    it.copy(
                        suhoorTime = suhoorTimeStr,
                        iftarTime = iftarTimeStr,
                        timeUntilIftar = if (!isSuhoor) countdown else "",
                        timeUntilSuhoor = if (isSuhoor) countdown else "",
                        isSuhoorTime = isSuhoor
                    )
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppAnalytics.logError("fasting", "load_prayer_times", e.message)
            // Keep default placeholder times
        }
    }

    private fun formatTime(hour: Int, minute: Int): String =
        formatClockTime(hour, minute, use24HourFormat)

    private fun calculateCountdown(
        now: LocalDateTime,
        fajr: LocalDateTime,
        maghrib: LocalDateTime
    ): Pair<String, Boolean> {
        return when {
            now.isBefore(fajr) -> {
                // Before Fajr - count down to Suhoor end
                val duration = Duration.between(now, fajr)
                formatDuration(duration) to true
            }

            now.isBefore(maghrib) -> {
                // After Fajr, before Maghrib - count down to Iftar
                val duration = Duration.between(now, maghrib)
                formatDuration(duration) to false
            }

            else -> {
                // After Maghrib - fasting period completed
                "Completed" to false
            }
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return "${hours}h ${minutes}m remaining"
    }

    fun onEvent(event: FastingEvent) {
        when (event) {
            is FastingEvent.StartFast -> AppAnalytics.logFastTracked("start", event.fastType.name)
            is FastingEvent.CompleteFast -> AppAnalytics.logFastTracked("complete")
            is FastingEvent.BreakFast -> AppAnalytics.logFastTracked("break")
            is FastingEvent.MissFast -> AppAnalytics.logFastTracked("miss")
            is FastingEvent.LogRecommendedFast -> AppAnalytics.logFastTracked(
                "recommended",
                event.fastType.name
            )

            is FastingEvent.PayFidya -> AppAnalytics.logFeatureUsed("fasting", "pay_fidya")
            is FastingEvent.AddMakeupFast -> AppAnalytics.logFeatureUsed("fasting", "add_makeup")
            is FastingEvent.CompleteMakeupFast ->
                AppAnalytics.logFeatureUsed("fasting", "complete_makeup")
            // Status/type only — never the user's exemption reason or note text.
            is FastingEvent.SaveFastForDate -> AppAnalytics.logFastTracked(
                "save_for_date",
                event.fastType.name
            )

            is FastingEvent.DeleteFastRecord -> AppAnalytics.logFastTracked("delete")
            else -> {}
        }
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
            is FastingEvent.OpenFastSheet -> openFastSheet(event.date)
            FastingEvent.DismissFastSheet -> _sheetState.update { it.copy(isVisible = false) }
            is FastingEvent.SaveFastForDate -> saveFastForDate(
                event.date,
                event.status,
                event.fastType,
                event.exemptionReason,
                event.note
            )

            is FastingEvent.DeleteFastRecord -> deleteFastRecord(event.date)
            is FastingEvent.UpdateMakeupFast -> updateMakeupFastRecord(event.makeupFast)
            is FastingEvent.LogRecommendedFast -> startFast(event.date, event.fastType)
        }
    }

    private fun loadToday() {
        selectDate(LocalDate.now())
    }

    private fun selectDate(date: LocalDate) {
        _trackerState.update { it.copy(selectedDate = date, isLoading = true) }

        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            val record = fastingUseCases.getFastRecordForDate(dateEpoch)
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
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hijri = HijriDateCalculator.toHijri(date)
            val record = FastRecord(
                id = 0,
                date = dateEpoch,
                hijriDate = hijri.formattedShort(),
                hijriMonth = hijri.month,
                hijriYear = hijri.year,
                fastType = fastType,
                status = FastStatus.FASTED,
                exemptionReason = null,
                suhoorTime = null,
                iftarTime = null,
                note = null,
                createdAt = now,
                updatedAt = now
            )
            fastingUseCases.insertFastRecord(record)
            selectDate(date)
            loadStats()
        }
    }

    private fun completeFast(date: LocalDate) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            fastingUseCases.updateFastStatus(dateEpoch, FastStatus.FASTED)
            selectDate(date)
            loadStats()
        }
    }

    private fun breakFast(date: LocalDate) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            fastingUseCases.updateFastStatus(dateEpoch, FastStatus.NOT_FASTED)
            selectDate(date)
            loadStats()
        }
    }

    private fun missFast(date: LocalDate, reason: String?) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            val existingRecord = fastingUseCases.getFastRecordForDate(dateEpoch)
            if (existingRecord != null) {
                fastingUseCases.updateFastStatus(dateEpoch, FastStatus.NOT_FASTED)
            } else {
                val now = System.currentTimeMillis()
                val hijri = HijriDateCalculator.toHijri(date)
                val record = FastRecord(
                    id = 0,
                    date = dateEpoch,
                    hijriDate = hijri.formattedShort(),
                    hijriMonth = hijri.month,
                    hijriYear = hijri.year,
                    fastType = FastType.RAMADAN,
                    status = FastStatus.NOT_FASTED,
                    exemptionReason = null,
                    suhoorTime = null,
                    iftarTime = null,
                    note = reason,
                    createdAt = now,
                    updatedAt = now
                )
                fastingUseCases.insertFastRecord(record)
            }
            selectDate(date)
            loadStats()
        }
    }

    private fun toggleTodayFast() {
        val date = _trackerState.value.selectedDate
        val currentRecord = _trackerState.value.todayRecord

        if (currentRecord == null) {
            startFast(date, _trackerState.value.selectedFastType)
        } else {
            when (currentRecord.status) {
                FastStatus.FASTED -> breakFast(date)
                FastStatus.NOT_FASTED -> {
                    val dateEpoch = date.toUtcMidnightMillis()
                    viewModelScope.launch {
                        fastingUseCases.updateFastStatus(dateEpoch, FastStatus.FASTED)
                        selectDate(date)
                        loadStats()
                    }
                }

                else -> {}
            }
        }
    }

    private fun selectMonth(month: Int, year: Int) {
        _calendarState.update {
            it.copy(
                selectedMonth = month,
                selectedYear = year,
                isLoading = true
            )
        }
        loadCalendarMonth()
    }

    private fun loadCalendarMonth() {
        calendarJob?.cancel()

        val month = _calendarState.value.selectedMonth
        val year = _calendarState.value.selectedYear

        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.plusMonths(1).minusDays(1)

        val startEpoch = startDate.toUtcMidnightMillis()
        val endEpoch = endDate.plusDays(1).toUtcMidnightMillis()

        calendarJob = viewModelScope.launch {
            fastingUseCases.getFastRecordsInRange(startEpoch, endEpoch).collect { records ->
                _calendarState.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    private fun loadRamadan() {
        ramadanJob?.cancel()
        ramadanJob = viewModelScope.launch {
            val today = LocalDate.now()
            val hijriToday = HijriDateCalculator.toHijri(today)
            val isCurrentlyRamadan = hijriToday.month == 9

            if (isCurrentlyRamadan) {
                val currentDay = hijriToday.day
                val daysInRamadan = HijriDateCalculator.getDaysInHijriMonth(hijriToday.year, 9)
                val ramadanStart = HijriDateCalculator.getFirstDayOfRamadan(hijriToday.year)
                val ramadanEnd = HijriDateCalculator.getLastDayOfRamadan(hijriToday.year)

                val startEpoch = ramadanStart.toUtcMidnightMillis()
                val endEpoch =
                    ramadanEnd.plusDays(1).toUtcMidnightMillis()

                fastingUseCases.getFastRecordsInRange(startEpoch, endEpoch).collect { records ->
                    val fasted = records.count { it.status == FastStatus.FASTED }
                    val missed = records.count { it.status == FastStatus.NOT_FASTED }

                    _ramadanState.update {
                        it.copy(
                            ramadanRecords = records,
                            fastedDays = fasted,
                            missedDays = missed,
                            remainingDays = daysInRamadan - currentDay,
                            currentDay = currentDay,
                            isRamadan = true,
                            isLoading = false
                        )
                    }
                }
            } else {
                _ramadanState.update {
                    it.copy(
                        isRamadan = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadMakeupFasts() {
        makeupPendingJob?.cancel()
        makeupAllJob?.cancel()
        makeupPendingJob = viewModelScope.launch {
            fastingUseCases.getPendingMakeupFasts().collect { pending ->
                _makeupState.update {
                    it.copy(
                        pendingMakeupFasts = pending,
                        pendingCount = pending.size
                    )
                }
            }
        }
        makeupAllJob = viewModelScope.launch {
            fastingUseCases.getAllMakeupFasts().collect { all ->
                _makeupState.update { it.copy(allMakeupFasts = all) }
            }
        }
        viewModelScope.launch {
            val totalFidya = fastingUseCases.getTotalFidyaPaid()
            _makeupState.update { it.copy(totalFidyaPaid = totalFidya, isLoading = false) }
        }
    }

    private fun addMakeupFast(makeupFast: MakeupFast) {
        viewModelScope.launch {
            fastingUseCases.insertMakeupFast(makeupFast)
            loadMakeupFasts()
        }
    }

    private fun completeMakeupFast(makeupFastId: Long) {
        viewModelScope.launch {
            fastingUseCases.markMakeupFastCompleted(makeupFastId, System.currentTimeMillis())
            loadMakeupFasts()
            loadStats()
        }
    }

    private fun payFidya(makeupFastId: Long, amount: Double) {
        viewModelScope.launch {
            fastingUseCases.markFidyaPaid(makeupFastId, amount)
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

        val startEpoch = startDate.toUtcMidnightMillis()
        val endEpoch = endDate.plusDays(1).toUtcMidnightMillis()

        viewModelScope.launch {
            val stats = fastingUseCases.getFastingStats(startEpoch, endEpoch)
            val ramadanCount = fastingUseCases.getRamadanFastedCount()
            val voluntaryCount = fastingUseCases.getVoluntaryFastCount()

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

    private fun openFastSheet(date: LocalDate) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            val existingRecord = fastingUseCases.getFastRecordForDate(dateEpoch)
            val hijri = HijriDateCalculator.toHijri(date)
            val isRamadan = hijri.month == 9

            _sheetState.update {
                FastManagementSheetState(
                    isVisible = true,
                    date = date,
                    existingRecord = existingRecord,
                    selectedStatus = existingRecord?.status ?: FastStatus.FASTED,
                    selectedFastType = existingRecord?.fastType
                        ?: if (isRamadan) FastType.RAMADAN else FastType.VOLUNTARY,
                    selectedExemptionReason = existingRecord?.exemptionReason,
                    note = existingRecord?.note ?: ""
                )
            }
        }
    }

    private fun saveFastForDate(
        date: LocalDate,
        status: FastStatus,
        fastType: FastType,
        exemptionReason: ExemptionReason?,
        note: String
    ) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            val existingRecord = fastingUseCases.getFastRecordForDate(dateEpoch)
            val now = System.currentTimeMillis()
            val hijri = HijriDateCalculator.toHijri(date)

            val record = FastRecord(
                id = existingRecord?.id ?: 0,
                date = dateEpoch,
                hijriDate = hijri.formattedShort(),
                hijriMonth = hijri.month,
                hijriYear = hijri.year,
                fastType = fastType,
                status = status,
                exemptionReason = if (status == FastStatus.EXEMPTED) exemptionReason else null,
                suhoorTime = existingRecord?.suhoorTime,
                iftarTime = existingRecord?.iftarTime,
                note = note.ifBlank { null },
                createdAt = existingRecord?.createdAt ?: now,
                updatedAt = now
            )

            if (existingRecord != null) {
                fastingUseCases.updateFastRecord(record)
            } else {
                fastingUseCases.insertFastRecord(record)
            }

            // Auto-create makeup fast for missed/exempted Ramadan days
            if (fastType == FastType.RAMADAN &&
                (status == FastStatus.NOT_FASTED || status == FastStatus.EXEMPTED)
            ) {
                val existingMakeupCount = fastingUseCases.getMakeupFastCountForDate(dateEpoch)
                if (existingMakeupCount == 0) {
                    val makeupFast = MakeupFast(
                        id = 0,
                        originalDate = dateEpoch,
                        originalHijriDate = hijri.formattedShort(),
                        reason = exemptionReason?.displayName() ?: "Missed Ramadan fast",
                        status = MakeupFastStatus.PENDING,
                        completedDate = null,
                        fidyaAmount = null,
                        note = note.ifBlank { null },
                        createdAt = now,
                        updatedAt = now
                    )
                    fastingUseCases.insertMakeupFast(makeupFast)
                }
            }

            _sheetState.update { it.copy(isVisible = false) }
            selectDate(date)
            loadCalendarMonth()
            loadStats()
            loadRamadan()
            loadMakeupFasts()
        }
    }

    private fun deleteFastRecord(date: LocalDate) {
        val dateEpoch = date.toUtcMidnightMillis()

        viewModelScope.launch {
            fastingUseCases.deleteFastRecordByDate(dateEpoch)
            _sheetState.update { it.copy(isVisible = false) }
            selectDate(date)
            loadCalendarMonth()
            loadStats()
            loadRamadan()
        }
    }

    private fun updateMakeupFastRecord(makeupFast: MakeupFast) {
        viewModelScope.launch {
            fastingUseCases.updateMakeupFast(makeupFast)
            loadMakeupFasts()
        }
    }
}
