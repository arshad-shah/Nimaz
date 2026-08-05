package com.arshadshah.nimaz.presentation.viewmodel.prayer

import java.time.LocalDate
import java.time.YearMonth
import com.arshadshah.nimaz.domain.model.DayPrayerTimes

data class MonthlyPrayerTimesUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val dayPrayerTimes: List<DayPrayerTimes> = emptyList(),
    val locationName: String = "Location not set",
    val methodLabel: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val ramadanHijriYear: Int? = null,
    val isLoading: Boolean = true,
    val expandedDay: LocalDate? = LocalDate.now()
)
