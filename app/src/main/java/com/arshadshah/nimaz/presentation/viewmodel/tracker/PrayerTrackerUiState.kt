package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerTimes
import java.time.LocalDate

/**
 * [selectedDate] has no default on purpose. It read `LocalDate.now()` as a data-class default,
 * which is evaluated once when the state object is constructed and never again — the same
 * frozen-"today" shape as the rollover bugs in #363. Requiring it makes the ViewModel anchor
 * the date through `TodayProvider`, which a test can fake and a midnight can move.
 */
data class PrayerTrackerUiState(
    val selectedDate: LocalDate,
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

/** [startDate]/[endDate] are anchored by the ViewModel — see [PrayerTrackerUiState]. */
data class PrayerHistoryUiState(
    val records: List<PrayerRecord> = emptyList(),
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isLoading: Boolean = true
)
