package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.ZakatDefaults
import java.time.LocalDate

/**
 * [selectedDate] has no default on purpose — a `LocalDate.now()` data-class default is
 * evaluated once at construction and never again, which is the frozen-"today" shape behind
 * the rollover bugs in #363. The ViewModel anchors it through `TodayProvider`.
 */
data class FastingTrackerUiState(
    val selectedDate: LocalDate,
    val todayRecord: FastRecord? = null,
    val isFastingToday: Boolean = false,
    val selectedFastType: FastType = FastType.VOLUNTARY,
    /** Today's Fajr / Maghrib as instants — formatted and counted down at the leaf. */
    val suhoorAt: kotlin.time.Instant? = null,
    val iftarAt: kotlin.time.Instant? = null,
    val isSuhoorTime: Boolean = false,
    /**
     * The record for [selectedDate] — `null` when that day has not been logged.
     *
     * Distinct from [todayRecord], which is always today's. Before the redesign the two were the
     * same field under today's name, so "the selected day" was only ever a relabelled today.
     */
    val selectedRecord: FastRecord? = null,
    /**
     * Records for the Monday–Sunday containing [selectedDate].
     *
     * Not derivable from `FastingCalendarUiState.records`: that query covers one calendar month,
     * and a week straddling a month boundary is half missing from it.
     */
    val weekRecords: List<FastRecord> = emptyList(),
    /**
     * [selectedDate]'s **own** Fajr / Maghrib.
     *
     * `PrayerUseCases.getDaySchedule` always took a date; only this ViewModel's hardcoded
     * `todayProvider.today()` kept the screen pinned to today's window.
     */
    val selectedSuhoorAt: kotlin.time.Instant? = null,
    val selectedIftarAt: kotlin.time.Instant? = null,
    /** Whether [selectedDate] is today — decides whether the window draws a "now" marker. */
    val isSelectedToday: Boolean = true,
    val isLoading: Boolean = true,
) {
    /**
     * Whether the one-tap "fasting today" control can act on the selected day.
     *
     * `FastStatus` has four values, and only `FASTED`/`NOT_FASTED` are two ends of a
     * toggle. `EXEMPTED` and `MAKEUP_DUE` are considered states recorded in the day
     * sheet — with a reason attached — so a single tap must not silently overwrite
     * them. The toggle renders disabled for those, with [toggleBlockedReason] saying
     * why, rather than looking live and doing nothing (which is what it used to do).
     */
    val canToggleToday: Boolean
        get() = todayRecord == null ||
                todayRecord.status == FastStatus.FASTED ||
                todayRecord.status == FastStatus.NOT_FASTED

    /** The status blocking the toggle, or null when it is actionable. */
    val toggleBlockedReason: FastStatus?
        get() = if (canToggleToday) null else todayRecord?.status
}

data class RamadanTrackerUiState(
    val ramadanRecords: List<FastRecord> = emptyList(),
    val fastedDays: Int = 0,
    val missedDays: Int = 0,
    val remainingDays: Int = 0,
    val currentDay: Int = 0,
    val isRamadan: Boolean = false,
    /**
     * Days until the next Ayyām al-Bīḍ — 0 while they are in progress.
     *
     * Computed here rather than in the screen, which used to call a private
     * `calculateAyyamAlBeedDays(LocalDate.now())`: that read the clock at composition, so a
     * screen open across midnight counted down to yesterday's answer, and it ignored the
     * user's `hijriDayOffset` while everything else honoured it (registry Open #10).
     */
    val daysUntilAyyamAlBeed: Int = 0,
    /**
     * Days until the next Ramadan, and the day it starts.
     *
     * Here for the same reason as [daysUntilAyyamAlBeed]: `RamadanCountdownCard` used to call
     * `HijriDateCalculator.daysUntilNextRamadan()` at composition, and the screen called it a
     * second time just to decide whether to show the card (#492).
     */
    val daysUntilRamadan: Int = 0,
    val ramadanStartsOn: LocalDate? = null,
    /**
     * Days of this Ramadan gone by with no fast record at all — not the same as [missedDays],
     * which counts days explicitly recorded as not fasted.
     */
    val unloggedDays: Int = 0,
    val isLoading: Boolean = true
)

/** The month grid is anchored by the ViewModel — see [FastingTrackerUiState]. */
data class FastingCalendarUiState(
    val records: List<FastRecord> = emptyList(),
    val selectedMonth: Int,
    val selectedYear: Int,
    val isLoading: Boolean = true
)

data class MakeupFastsUiState(
    val pendingMakeupFasts: List<MakeupFast> = emptyList(),
    val allMakeupFasts: List<MakeupFast> = emptyList(),
    val pendingCount: Int = 0,
    val totalFidyaPaid: Double = 0.0,
    /**
     * The currency [totalFidyaPaid] is denominated in.
     *
     * Fidya is money, and the app has exactly one currency setting — the zakat one. Carrying it
     * here rather than letting the screen assume a default is the difference between a euro user
     * reading "€24.00" and reading "$24.00" against the same number.
     */
    val currency: String = ZakatDefaults.CURRENCY,
    val isLoading: Boolean = true
)

data class FastingStatsUiState(
    val stats: FastingStats? = null,
    val ramadanFastedCount: Int = 0,
    val voluntaryFastCount: Int = 0,
    val period: FastingStatsPeriod = FastingStatsPeriod.THIS_YEAR,
    val isLoading: Boolean = true
)

