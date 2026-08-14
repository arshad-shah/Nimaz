package com.arshadshah.nimaz.widget.hijricalendar

import kotlinx.serialization.Serializable

@Serializable
sealed interface HijriCalendarWidgetState {

    /**
     * Whether this state is a reading worth keeping on screen when a refresh fails — see
     * `refreshWidget`. The default state every widget starts on is `Success` with an empty
     * payload, so "is it Success" is not the question; carrying loaded values is.
     */
    val hasData: Boolean get() = false

    @Serializable
    data object Loading : HijriCalendarWidgetState

    @Serializable
    data class Success(val data: HijriCalendarData) : HijriCalendarWidgetState {
        override val hasData: Boolean get() = data.hijriMonthName.isNotEmpty()
    }

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
