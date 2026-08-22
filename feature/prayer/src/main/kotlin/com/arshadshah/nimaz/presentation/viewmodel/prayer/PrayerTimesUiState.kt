package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import java.time.LocalDate

/**
 * Drives the dedicated Prayer Times screen: a day pager over the prayer
 * schedule with a living-sky hero and per-day tracking.
 *
 * The sky always reflects "now" (so it stays alive while you browse), while the
 * list and day-info reflect the [PrayerTimesUiState.selectedDate]. Tracking
 * toggles apply to today and past days only (future prayers can't be prayed).
 */
data class PrayerTimesUiState(
    val locationName: String = "Location not set",
    /**
     * True when [locationName] and the times below it come from [FallbackLocation] rather than
     * anywhere the reader chose — onboarding can be skipped and the permission denied, so the
     * header must not assert a city they have never been to.
     */
    val isUsingFallbackLocation: Boolean = false,
    /**
     * Null until the ViewModel anchors it, rather than a `LocalDate.now()` data-class default.
     *
     * A default is evaluated when the instance is constructed, so it froze at whatever day the
     * screen was opened and nothing re-evaluated it. The ViewModel sets it from `TodayProvider`
     * and re-anchors at midnight; making it nullable is what stops the old shape being written
     * back by habit.
     */
    val selectedDate: LocalDate? = null,
    val isToday: Boolean = true,
    val prayers: List<PrayerTimeDisplay> = emptyList(),
    /** Tomorrow's Fajr, so the UI can wrap the countdown once today's Isha has passed. */
    val tomorrowFajrAt: kotlin.time.Instant? = null,
    // Living-sky inputs
    val moonFraction: Float = 0.5f,     // moon phase for the selected date
    val sunriseFraction: Float = 0.27f, // sunrise as a fraction of the day (sun arc)
    val sunsetFraction: Float = 0.80f,  // sunset (Maghrib) as a fraction of the day
    // Day-info card — instants, formatted at the leaf so the 12/24h toggle is a recomposition
    val sunriseAt: kotlin.time.Instant? = null,
    val sunsetAt: kotlin.time.Instant? = null,
    val daylight: String = "",
    val methodLabel: String = "",
    // +1 when moving to a later day, -1 to an earlier day (drives slide direction)
)
