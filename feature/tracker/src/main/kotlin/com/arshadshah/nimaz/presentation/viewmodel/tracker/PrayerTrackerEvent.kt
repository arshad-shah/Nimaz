package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import java.time.LocalDate

sealed interface PrayerTrackerEvent {
    data class SelectDate(val date: LocalDate) : PrayerTrackerEvent

    /**
     * Set — or clear — one prayer's status on the selected date.
     *
     * Replaces the old prayed/missed pair, which between them could express only two of the four
     * states the app already stored and displayed: `LATE` and `QADA` were renderable but not
     * settable from anywhere in the UI.
     *
     * @param status the user's assertion, or `null` to withdraw it. Clearing writes
     *   [PrayerStatus.NOT_PRAYED], which the display derivation reads as "not recorded".
     */
    data class SetPrayerStatus(
        val prayerName: PrayerName,
        val status: PrayerStatus?,
    ) : PrayerTrackerEvent

    /**
     * Confirm every unrecorded prayer in an inclusive date range as missed.
     *
     * The only path into the qada list. Raised by the tracker's review banner.
     */
    data class ConfirmUnrecordedAsMissed(
        val from: LocalDate,
        val to: LocalDate,
    ) : PrayerTrackerEvent

    data class MarkQadaCompleted(val record: PrayerRecord) : PrayerTrackerEvent
    data class SetStatsPeriod(val period: StatsPeriod) : PrayerTrackerEvent
    data class LoadHistory(val startDate: LocalDate, val endDate: LocalDate) : PrayerTrackerEvent
    data object LoadToday : PrayerTrackerEvent
    data object LoadStats : PrayerTrackerEvent
    data object LoadQadaPrayers : PrayerTrackerEvent
    data object NavigateToPreviousDay : PrayerTrackerEvent
    data object NavigateToNextDay : PrayerTrackerEvent
}
