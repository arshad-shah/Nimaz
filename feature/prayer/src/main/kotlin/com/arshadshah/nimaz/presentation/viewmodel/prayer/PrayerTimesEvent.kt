package com.arshadshah.nimaz.presentation.viewmodel.prayer

import java.time.LocalDate

/**
 * Everything the Prayer Times screen can ask for — all of it navigation between days.
 *
 * **Nothing here writes.** The screen answers *when* a prayer is; the prayer tracker answers what
 * the reader did about it. An event that records a prayer belongs there, and
 * `PrayerTimesViewModelTest.no event writes a prayer record` fails if one is added here.
 */
sealed interface PrayerTimesEvent {
    data object PreviousDay : PrayerTimesEvent
    data object NextDay : PrayerTimesEvent
    data object GoToToday : PrayerTimesEvent
    data class SelectDate(val date: LocalDate) : PrayerTimesEvent
}
