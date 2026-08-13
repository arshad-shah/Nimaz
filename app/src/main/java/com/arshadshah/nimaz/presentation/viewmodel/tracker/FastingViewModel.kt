package com.arshadshah.nimaz.presentation.viewmodel.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.domain.repository.settings.HijriSettings
import com.arshadshah.nimaz.domain.usecase.fasting.CountUnloggedRamadanDaysUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetDaysUntilAyyamAlBeedUseCase
import com.arshadshah.nimaz.domain.usecase.fasting.GetRamadanCountdownUseCase
import com.arshadshah.nimaz.core.monitoring.launchBestEffort
import com.arshadshah.nimaz.domain.repository.settings.ZakatSettings
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.usecase.FastingUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class FastingStatsPeriod {
    THIS_MONTH, THIS_YEAR, ALL_TIME
}

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val fastingUseCases: FastingUseCases,
    private val prayerUseCases: PrayerUseCases,
    private val todayProvider: TodayProvider,
    private val daysUntilAyyamAlBeed: GetDaysUntilAyyamAlBeedUseCase,
    private val ramadanCountdown: GetRamadanCountdownUseCase,
    private val countUnloggedRamadanDays: CountUnloggedRamadanDaysUseCase,
    // The offset the user set to match their local moon sighting. The seam, not the whole
    // SettingsRepository — the same argument ZakatSettings settled for the currency (#436).
    private val hijriSettings: HijriSettings,
    // Fidya is money, and the one currency setting the app has is the zakat one. The seam, not
    // the whole SettingsRepository (#436).
    private val zakatSettings: ZakatSettings,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _trackerState =
        MutableStateFlow(FastingTrackerUiState(selectedDate = todayProvider.today()))
    val trackerState: StateFlow<FastingTrackerUiState> = _trackerState.asStateFlow()

    private val _ramadanState = MutableStateFlow(RamadanTrackerUiState())
    val ramadanState: StateFlow<RamadanTrackerUiState> = _ramadanState.asStateFlow()

    private val _calendarState = MutableStateFlow(
        todayProvider.today().let {
            FastingCalendarUiState(selectedMonth = it.monthValue, selectedYear = it.year)
        }
    )
    val calendarState: StateFlow<FastingCalendarUiState> = _calendarState.asStateFlow()

    private val _makeupState = MutableStateFlow(MakeupFastsUiState())
    val makeupState: StateFlow<MakeupFastsUiState> = _makeupState.asStateFlow()

    private val _statsState = MutableStateFlow(FastingStatsUiState())
    val statsState: StateFlow<FastingStatsUiState> = _statsState.asStateFlow()

    private val _sheetState =
        MutableStateFlow(FastManagementSheetState(date = todayProvider.today()))
    val sheetState: StateFlow<FastManagementSheetState> = _sheetState.asStateFlow()

    private var calendarJob: Job? = null
    private var ramadanJob: Job? = null
    private var makeupPendingJob: Job? = null
    private var makeupAllJob: Job? = null

    // No `use24HourFormat` mirror: suhoor/iftar are instants, formatted at the leaf.

    private companion object {
        const val DOMAIN = "fasting"
    }

    init {
        loadToday()
        loadRamadan()
        observeLocationAndLoadPrayerTimes()
        loadCalendarMonth()
        loadMakeupFasts()
        loadStats()
    }

    /**
     * Suhoor and iftar, recomputed whenever *any* calculation setting changes.
     *
     * This used to watch latitude, longitude and the clock format, and then call
     * `prayerTimeCalculator.getPrayerTimes(lat, lng)` — **taking all four calculation defaults**.
     * Fast Tracker therefore showed Muslim World League, Shafi, no high-latitude rule and no
     * adjustments no matter what the user had set, while Home honoured all four: the same Fajr,
     * two different times, in one app. Nothing in the type system objected, because every one of
     * those four arguments has a default.
     *
     * Observing the settings snapshot fixes both halves — the values are the user's, and a
     * change to any of them (not just to the location) now recomputes.
     */
    private fun observeLocationAndLoadPrayerTimes() {
        launchSafely(telemetry, DOMAIN, "observe_location_and_load_prayer_times") {
            prayerUseCases.observeCalculationSettings().collect { settings ->
                loadPrayerTimes(settings)
            }
        }
    }

    private fun loadPrayerTimes(settings: PrayerCalculationSettings) {
        try {
            val prayerTimes = prayerUseCases.getDaySchedule(todayProvider.today(), settings)

            val fajrPrayer = prayerTimes.find { it.type == PrayerType.FAJR }
            val maghribPrayer = prayerTimes.find { it.type == PrayerType.MAGHRIB }

            if (fajrPrayer != null && maghribPrayer != null) {
                _trackerState.update {
                    it.copy(
                        suhoorAt = fajrPrayer.time,
                        iftarAt = maghribPrayer.time,
                    )
                }
            }
        } catch (e: Exception) {
            telemetry.failure(DOMAIN, "load_prayer_times", e)
            // Keep default placeholder times
        }
    }

    fun onEvent(event: FastingEvent) {
        when (event) {
            is FastingEvent.SelectDate -> selectDate(event.date)
            is FastingEvent.SetFastType -> _trackerState.update { it.copy(selectedFastType = event.fastType) }
            is FastingEvent.SelectMonth -> selectMonth(event.month, event.year)
            is FastingEvent.CompleteMakeupFast -> {
                telemetry.featureUsed(DOMAIN, "complete_makeup")
                // Status/type only — never the user's exemption reason or note text.
                completeMakeupFast(event.makeupFastId)
            }
            is FastingEvent.PayFidya -> {
                telemetry.featureUsed(DOMAIN, "pay_fidya")
                payFidya(event.makeupFastId, event.amount)
            }
            is FastingEvent.SetStatsPeriod -> setStatsPeriod(event.period)
            FastingEvent.LoadToday -> loadToday()
            FastingEvent.LoadRamadan -> loadRamadan()
            FastingEvent.LoadMakeupFasts -> loadMakeupFasts()
            FastingEvent.LoadStats -> loadStats()
            FastingEvent.ToggleTodayFast -> toggleTodayFast()
            is FastingEvent.OpenFastSheet -> openFastSheet(event.date)
            FastingEvent.DismissFastSheet -> _sheetState.update { it.copy(isVisible = false) }
            is FastingEvent.SaveFastForDate -> {
                telemetry.fastTracked(
                    "save_for_date",
                    event.fastType.name
                )
                saveFastForDate(
                    event.date,
                    event.status,
                    event.fastType,
                    event.exemptionReason,
                    event.note
                )
            }

            is FastingEvent.DeleteFastRecord -> {
                telemetry.fastTracked("delete")
                deleteFastRecord(event.date)
            }
            is FastingEvent.UpdateMakeupFast -> updateMakeupFastRecord(event.makeupFast)
        }
    }

    private fun loadToday() {
        selectDate(todayProvider.today())
    }

    private fun selectDate(date: LocalDate) {
        _trackerState.update { it.copy(selectedDate = date, isLoading = true) }

        val dateEpoch = date.toUtcMidnightMillis()

        launchSafely(
            telemetry,
            DOMAIN,
            "select_date",
            onFailure = { _trackerState.update { it.copy(isLoading = false) } },
        ) {
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

        launchSafely(telemetry, DOMAIN, "start_fast") {
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

    private fun breakFast(date: LocalDate) {
        val dateEpoch = date.toUtcMidnightMillis()

        launchSafely(telemetry, DOMAIN, "break_fast") {
            fastingUseCases.updateFastStatus(dateEpoch, FastStatus.NOT_FASTED)
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
                    launchSafely(telemetry, DOMAIN, "toggle_today_fast") {
                        fastingUseCases.updateFastStatus(dateEpoch, FastStatus.FASTED)
                        selectDate(date)
                        loadStats()
                    }
                }

                // Exhaustive on purpose: these two are managed in the day sheet, where
                // the reason lives, and the screen disables the toggle for them via
                // canToggleToday. Reaching here means the UI let through a tap it should
                // not have, so it is recorded rather than silently dropped.
                FastStatus.EXEMPTED,
                FastStatus.MAKEUP_DUE ->
                    telemetry.featureUsed(DOMAIN, "toggle_blocked_${currentRecord.status.name.lowercase()}")
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

        calendarJob = launchSafely(telemetry, DOMAIN, "load_calendar_month") {
            fastingUseCases.getFastRecordsInRange(startEpoch, endEpoch).collect { records ->
                _calendarState.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    private fun loadRamadan() {
        ramadanJob?.cancel()
        ramadanJob = launchSafely(telemetry, DOMAIN, "load_ramadan") {
            // The offset the user set to match their local moon sighting. Read once per load
            // rather than collected: this whole block re-runs when the day does.
            val offset = hijriSettings.hijriDayOffset.first()
            val ayyamDays = daysUntilAyyamAlBeed(offset)
            val countdown = ramadanCountdown(offset)
            _ramadanState.update {
                it.copy(
                    daysUntilAyyamAlBeed = ayyamDays,
                    daysUntilRamadan = countdown.daysAway,
                    ramadanStartsOn = countdown.startsOn,
                )
            }

            val today = todayProvider.today()
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
                            unloggedDays = countUnloggedRamadanDays(currentDay, records),
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
        makeupPendingJob = launchSafely(telemetry, DOMAIN, "load_makeup_fasts") {
            fastingUseCases.getPendingMakeupFasts().collect { pending ->
                _makeupState.update {
                    it.copy(
                        pendingMakeupFasts = pending,
                        pendingCount = pending.size
                    )
                }
            }
        }
        makeupAllJob = launchSafely(telemetry, DOMAIN, "load_makeup_fasts") {
            fastingUseCases.getAllMakeupFasts().collect { all ->
                _makeupState.update { it.copy(allMakeupFasts = all) }
            }
        }
        launchSafely(telemetry, DOMAIN, "load_makeup_fasts") {
            val totalFidya = fastingUseCases.getTotalFidyaPaid()
            _makeupState.update { it.copy(totalFidyaPaid = totalFidya, isLoading = false) }
        }
        // Collected, not read once: the currency can change while this screen is on the
        // back stack, and a fidya figure labelled with the old symbol is worse than none.
        launchBestEffort(telemetry, DOMAIN, "observe_currency") {
            zakatSettings.zakatCurrency.collect { code ->
                _makeupState.update { it.copy(currency = code) }
            }
        }
    }

    private fun completeMakeupFast(makeupFastId: Long) {
        launchSafely(telemetry, DOMAIN, "complete_makeup_fast") {
            fastingUseCases.markMakeupFastCompleted(makeupFastId, System.currentTimeMillis())
            loadMakeupFasts()
            loadStats()
        }
    }

    private fun payFidya(makeupFastId: Long, amount: Double) {
        launchSafely(telemetry, DOMAIN, "pay_fidya") {
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
        val now = todayProvider.today()

        val (startDate, endDate) = when (period) {
            FastingStatsPeriod.THIS_MONTH -> now.withDayOfMonth(1) to now
            FastingStatsPeriod.THIS_YEAR -> now.withDayOfYear(1) to now
            FastingStatsPeriod.ALL_TIME -> now.minusYears(10) to now
        }

        val startEpoch = startDate.toUtcMidnightMillis()
        val endEpoch = endDate.plusDays(1).toUtcMidnightMillis()

        launchSafely(telemetry, DOMAIN, "load_stats") {
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

        launchSafely(telemetry, DOMAIN, "open_fast_sheet") {
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

        launchSafely(telemetry, DOMAIN, "save_fast_for_date") {
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

        launchSafely(telemetry, DOMAIN, "delete_fast_record") {
            fastingUseCases.deleteFastRecordByDate(dateEpoch)
            _sheetState.update { it.copy(isVisible = false) }
            selectDate(date)
            loadCalendarMonth()
            loadStats()
            loadRamadan()
            // saveFastForDate auto-creates a makeup fast for a missed or exempted
            // Ramadan day. Deleting the record left that row behind for ever and the
            // pending count stale on screen, because this path never reloaded it.
            loadMakeupFasts()
        }
    }

    private fun updateMakeupFastRecord(makeupFast: MakeupFast) {
        launchSafely(telemetry, DOMAIN, "update_makeup_fast_record") {
            fastingUseCases.updateMakeupFast(makeupFast)
            loadMakeupFasts()
        }
    }
}
