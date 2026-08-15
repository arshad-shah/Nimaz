package com.arshadshah.nimaz.presentation.model

import com.arshadshah.nimaz.core.util.currentPrayerIndexAt
import com.arshadshah.nimaz.core.util.nextPrayerIndexAt
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import kotlin.time.Instant

/**
 * Display models shared across the presentation layer.
 *
 * These lived in `HomeViewModel.kt`. `PrayerTimeDisplay` alone was imported by **eight** files —
 * screens, components, and `PrayerTimesViewModel`, which reached into an unrelated feature's
 * ViewModel file for a type it renders. A shared presentation model parked inside one feature is
 * a layering leak rather than an organisation nit: every consumer ends up depending on Home.
 */

/**
 * A single dua surfaced on the home screen's "Today" section, picked to match
 * the current time of day (morning / evening / before sleep adhkar).
 */
data class DailyDua(
    val duaId: String,
    val title: String,
    val arabic: String,
    val translation: String,
    val source: String,
    val categoryLabel: String,
    val categoryIcon: String,
)

/**
 * One prayer row. Carries the prayer's **instant**, not a formatted string: the clock time is
 * rendered at the leaf so it honours `LocalUse24HourFormat` in the same frame the toggle flips,
 * and [isPassed]/[isCurrent]/[isNext] are re-derived at the leaf from the shared ticker via
 * [withClockState] rather than pushed once a second by a ViewModel loop.
 *
 * The three booleans default to `false`: a ViewModel publishes the facts (type, instant, status)
 * and the UI decides what they mean *now*.
 */
data class PrayerTimeDisplay(
    val type: PrayerType,
    val name: String,
    val timeAt: Instant,
    val isPassed: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false,
    val prayerStatus: PrayerStatus = PrayerStatus.NOT_PRAYED
)

/**
 * Re-derive the clock-dependent flags for [now]. Pure and cheap — call it from a composable that
 * reads `rememberNow()` so the list re-derives at the caller's tick resolution.
 *
 * Comparison is by instant, which fixes the long-standing bug where after Isha nothing highlighted
 * and before Fajr today's Isha rendered as "current" (see `core/util/PrayerClock.kt`).
 */
fun List<PrayerTimeDisplay>.withClockState(now: Instant): List<PrayerTimeDisplay> {
    if (isEmpty()) return this
    val sorted = sortedBy { it.timeAt }
    val instants = sorted.map { it.timeAt }
    val nextIndex = nextPrayerIndexAt(instants, now)
    val currentIndex = currentPrayerIndexAt(instants, now)
    return sorted.mapIndexed { index, display ->
        display.copy(
            isPassed = display.timeAt <= now,
            isCurrent = index == currentIndex,
            isNext = index == nextIndex
        )
    }
}
