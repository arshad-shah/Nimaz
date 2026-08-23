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

    /**
     * A PDF export was started from the screen.
     *
     * The screen used to log this itself, with the literal `"monthly_prayer_times"` — a live
     * instance of the drift §6.1 documents, where a month view of the timetable reported as a
     * feature of its own and never appeared in `Feature.PRAYER_TIMES`'s funnel. Routing it
     * through the ViewModel puts it on the seam and on the catalog constant at once.
     */
    data object ExportStarted : MonthlyPrayerTimesEvent

    /** A PDF export threw. Reported, never shown: the timetable on screen is still correct. */
    data class ExportFailed(val throwable: Throwable) : MonthlyPrayerTimesEvent

    /**
     * A month's PDF finished rendering, in [durationMs].
     *
     * Measured by the screen because that is where the work runs — a click handler rendering
     * thirty rows of a PDF on the caller's thread — and reported through the ViewModel because
     * that is where the [Telemetry] seam is. The screen never touches monitoring itself.
     */
    data class ExportCompleted(val durationMs: Long) : MonthlyPrayerTimesEvent
}
