package com.arshadshah.nimaz.presentation.viewmodel.home

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.presentation.components.organisms.WorshipCardUi
import com.arshadshah.nimaz.core.time.TodayProvider
import kotlin.time.Instant
import com.arshadshah.nimaz.presentation.model.DailyDua
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.model.withClockState
import com.arshadshah.nimaz.presentation.viewmodel.UiError

data class HomeUiState(
    // `currentDate` used to live here, set once at construction and read by no screen
    // (`grep state.currentDate` in `screens/` returned nothing). It was the field that was
    // *supposed* to detect a date rollover, and being inert is why nothing did. The rollover
    // now comes from `TodayProvider.todayChanges`, so the dead field is gone rather than
    // quietly kept and still unread.
    val hijriDate: String = "",
    val prayerTimes: List<PrayerTimeDisplay> = emptyList(),
    // Tomorrow's Fajr, so the UI can wrap the "next prayer" countdown past Isha without the
    // ViewModel having to know what time it is. Which prayer is next — and the countdown to it —
    // is derived at the leaf from [prayerTimes] + this, via `withClockState`/`nextPrayerAt`.
    val tomorrowFajrAt: Instant? = null,
    val locationName: String = "Location not set",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val fastingToday: Boolean = false,
    val dailyHadith: String? = null,
    val dailyHadithReference: String? = null,
    val dailyHadithId: String? = null,
    val dailyHadithGrade: String? = null,
    // Today's sunrise/sunset as fractions of the day — anchor the living sky's
    // sun arc to the real sun instead of fixed clock times. These vary with the
    // day's prayer times, not with "now", so they stay pushed state.
    val sunriseFraction: Float = 0.27f,
    val sunsetFraction: Float = 0.80f,
    val dailyDua: DailyDua? = null,
    val isFriday: Boolean = false,
    // Today's Dhuhr when it is Friday, else null. "Has it passed" and the countdown to it are
    // derived at the leaf, so no per-second string lives here.
    val jumuahAt: Instant? = null,
    val isLoading: Boolean = true,
    /**
     * Set when the day's prayer times could not be calculated.
     *
     * Scoped to the prayer card by the screen, not to the dashboard: the rest of the home
     * screen — the worship card, the daily hadith, the tracker — is built by independent
     * loaders, and one failing loader must not take the others down with it.
     */
    val error: UiError? = null,
    // Permission states
    val hasNotificationPermission: Boolean = true,
    val hasLocationPermission: Boolean = true,
    val isBatteryOptimized: Boolean = false,
    // Local calendar occasions merged with any pushed CELEBRATION announcement,
    // rendered as cards in the Home events carousel (after the Jumu'ah card).
    val celebrationCards: List<HomeEventCard> = emptyList(),
    // The single nearest upcoming *enabled* extended worship reminder, rendered as the
    // "Next Worship" card in the events carousel. Null when nothing is enabled/near.
    val worshipCard: WorshipCardUi? = null,
)

/**
 * The FCM engagement banner's slice of Home state. [announcement] is null when
 * there is nothing active (nothing received, dismissed, expired or outside the
 * version window). [showCta] is true only when the announcement carries a CTA
 * label AND its route resolves (allowlisted key or https URL) — otherwise the
 * banner renders without a button.
 */
data class AnnouncementUiState(
    val announcement: Announcement? = null,
    val showCta: Boolean = false,
)
