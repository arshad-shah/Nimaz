package com.arshadshah.nimaz.presentation.viewmodel.prayer

import java.time.LocalDate

sealed interface MonthlyPrayerTimesEvent {
    data object NextMonth : MonthlyPrayerTimesEvent
    data object PreviousMonth : MonthlyPrayerTimesEvent
    data class ToggleDayExpanded(val date: LocalDate) : MonthlyPrayerTimesEvent

    /** Compute the Ramadan timetable for sharing. The result lands in `ramadanExport`. */
    data object PrepareRamadanExport : MonthlyPrayerTimesEvent

    /** The share sheet has consumed `ramadanExport`; clear it so it cannot fire twice. */
    data object RamadanExportConsumed : MonthlyPrayerTimesEvent
}
