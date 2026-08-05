package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
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
    val isLoading: Boolean = true,
    val error: String? = null
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
    val isLoading: Boolean = true
)

data class FastingStatsUiState(
    val stats: FastingStats? = null,
    val ramadanFastedCount: Int = 0,
    val voluntaryFastCount: Int = 0,
    val period: FastingStatsPeriod = FastingStatsPeriod.THIS_YEAR,
    val isLoading: Boolean = true
)

/**
 * The day sheet. [date] is always supplied by the event that opens the sheet; the
 * `LocalDate.now()` default it used to carry was never the date the sheet showed.
 */
data class FastManagementSheetState(
    val isVisible: Boolean = false,
    val date: LocalDate,
    val existingRecord: FastRecord? = null,
    val selectedStatus: FastStatus = FastStatus.FASTED,
    val selectedFastType: FastType = FastType.VOLUNTARY,
    val selectedExemptionReason: ExemptionReason? = null,
    val note: String = ""
)
