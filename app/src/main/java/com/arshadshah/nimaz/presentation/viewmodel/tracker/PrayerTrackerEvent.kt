package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import java.time.LocalDate

sealed interface PrayerTrackerEvent {
    data class SelectDate(val date: LocalDate) : PrayerTrackerEvent

    data class MarkPrayerPrayed(val prayerName: PrayerName, val isJamaah: Boolean = false) :
        PrayerTrackerEvent

    data class MarkPrayerMissed(val prayerName: PrayerName) : PrayerTrackerEvent
    data class MarkQadaCompleted(val record: PrayerRecord) : PrayerTrackerEvent
    data class SetStatsPeriod(val period: StatsPeriod) : PrayerTrackerEvent
    data class LoadHistory(val startDate: LocalDate, val endDate: LocalDate) : PrayerTrackerEvent
    data object LoadToday : PrayerTrackerEvent
    data object LoadStats : PrayerTrackerEvent
    data object LoadQadaPrayers : PrayerTrackerEvent
    data object NavigateToPreviousDay : PrayerTrackerEvent
    data object NavigateToNextDay : PrayerTrackerEvent
}
