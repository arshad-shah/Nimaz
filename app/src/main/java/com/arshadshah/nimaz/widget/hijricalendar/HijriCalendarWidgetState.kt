package com.arshadshah.nimaz.widget.hijricalendar

import kotlinx.serialization.Serializable

@Serializable
sealed interface HijriCalendarWidgetState {

    @Serializable
    data object Loading : HijriCalendarWidgetState

    @Serializable
    data class Success(val data: HijriCalendarData) : HijriCalendarWidgetState

    @Serializable
    data class Error(val message: String?) : HijriCalendarWidgetState
}

@Serializable
data class HijriCalendarData(
    val hijriMonth: Int = 1,
    val hijriMonthName: String = "",
    val hijriYear: Int = 1446,
    val gregorianDate: String = "",
    val daysInMonth: Int = 30,
    val firstDayOfWeekOffset: Int = 0,
    val todayHijriDay: Int = 1,
    val events: List<HijriCalendarEventData> = emptyList()
)

@Serializable
data class HijriCalendarEventData(
    val name: String = "",
    val nameArabic: String = "",
    val type: String = ""
)
