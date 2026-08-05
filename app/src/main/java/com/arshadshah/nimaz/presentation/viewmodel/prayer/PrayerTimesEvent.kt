package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.domain.model.PrayerType
import java.time.LocalDate

sealed interface PrayerTimesEvent {
    data object PreviousDay : PrayerTimesEvent
    data object NextDay : PrayerTimesEvent
    data object GoToToday : PrayerTimesEvent
    data class SelectDate(val date: LocalDate) : PrayerTimesEvent
    data class TogglePrayer(val type: PrayerType) : PrayerTimesEvent
}
