package com.arshadshah.nimaz.core.common

import kotlin.time.Instant

/**
 * Pure, clock-derived views over a day's prayer instants.
 *
 * ## Why this exists
 *
 * "Which prayer is next", "has this one passed" and "how far through the day are we" are not
 * *state* — they are continuously varying functions of `now`. Modelling them as `StateFlow` fields
 * forced a 1 Hz ViewModel loop to push a whole new UI state every second, which is what coupled the
 * countdown to the expensive recompute path.
 *
 * These helpers take the cached instants plus a `now` and return the derived view, so the
 * derivation can happen at the leaf (from the shared ticker) at whatever resolution the caller
 * needs. They are pure and `Instant`-based, so they are exhaustively unit-testable.
 *
 * ## The bug this also fixes
 *
 * The previous derivation compared **times of day**, not instants:
 *
 * ```kotlin
 * isPassed = prayerLocalTime.time < localTime.time   // LocalTime — the date is dropped
 * ```
 *
 * After Isha, `indexOfFirst { !isPassed }` returned `-1`, so no row highlighted while the header
 * counted down to tomorrow's Fajr; before Fajr, the current-prayer index fell through to
 * `lastIndex`, so today's not-yet-happened Isha rendered as "current". Comparing instants — as
 * below — removes both.
 */

/**
 * Index of the first prayer in [sorted] (ascending by instant) that has not yet started, or `-1`
 * once every prayer of the day has passed.
 */
fun nextPrayerIndexAt(sorted: List<Instant>, now: Instant): Int =
    sorted.indexOfFirst { it > now }

/**
 * Index of the prayer currently in effect: the last one that has started. Returns `-1` before the
 * day's first prayer — deliberately, so a caller can distinguish "before Fajr" from "in Fajr"
 * instead of wrapping around to Isha the way the old time-of-day comparison did.
 */
fun currentPrayerIndexAt(sorted: List<Instant>, now: Instant): Int {
    val next = nextPrayerIndexAt(sorted, now)
    return when {
        next < 0 -> sorted.lastIndex   // everything has passed → the last one is in effect
        else -> next - 1               // -1 before the first prayer
    }
}

/**
 * Fractional position of [now] along the day's timeline, 0f at the first instant and 1f at the
 * last, interpolated *within* the current interval so the value advances smoothly rather than
 * stepping at each prayer.
 *
 * Returns 0f for fewer than two instants rather than dividing by zero. [instants] must be sorted
 * ascending.
 */
fun prayerTimelineProgressAt(instants: List<Instant>, now: Instant): Float {
    if (instants.size < 2) return 0f
    if (now <= instants.first()) return 0f
    if (now >= instants.last()) return 1f
    for (k in 0 until instants.size - 1) {
        val start = instants[k]
        val end = instants[k + 1]
        if (now in start..<end) {
            val span = (end - start).inWholeSeconds.toFloat()
            if (span <= 0f) return (k.toFloat() / (instants.size - 1)).coerceIn(0f, 1f)
            val frac = (now - start).inWholeSeconds.toFloat() / span
            return ((k + frac) / (instants.size - 1)).coerceIn(0f, 1f)
        }
    }
    return 1f
}
