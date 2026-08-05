package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerTimes
import java.time.LocalDate

data class PrayerTrackerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val prayerRecords: List<PrayerRecord> = emptyList(),
    val prayerTimes: PrayerTimes? = null,
    val isLoading: Boolean = true,
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
