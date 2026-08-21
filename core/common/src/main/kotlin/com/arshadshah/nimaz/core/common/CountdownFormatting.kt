package com.arshadshah.nimaz.core.common

import com.arshadshah.nimaz.domain.worship.DayWorshipTimes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Canonical countdown decomposition.
 *
 * This is the *arithmetic* half of countdown display. It is pure, has no
 * `Context`, and produces numbers — never strings — so it is unit-testable and
 * cannot hardcode English unit suffixes. Rendering lives in
 * `presentation/components/atoms/NimazCountdown.kt`, which turns these numbers
 * into localized text via string resources.
 *
 * This replaces six copy-pasted formatters:
 *   - `HomeViewModel.formatCountdown`
 *   - `HomeViewModel.renderWorshipCard` (its own inline h/m variant)
 *   - `PrayerTimesViewModel.formatCountdown`
 *   - `FastingViewModel` ("${hours}h ${minutes}m remaining")
 *   - `WidgetsScreen` (inline)
 *   - `widget/core/WidgetFormatters.formatWidgetCountdown`
 *
 * …and removes the need for `CountdownTimer` to re-parse a formatted string
 * back into digits.
 */
data class CountdownParts(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    /** True once the target instant has been reached; all fields are then zero. */
    val elapsed: Boolean,
) {
    val totalSeconds: Long get() = hours * 3600 + minutes * 60 + seconds

    /** The coarsest unit that is non-zero — drives which units a renderer shows. */
    val leadUnit: CountdownUnit
        get() = when {
            hours > 0 -> CountdownUnit.HOURS
            minutes > 0 -> CountdownUnit.MINUTES
            else -> CountdownUnit.SECONDS
        }

    companion object {
        val ZERO = CountdownParts(0, 0, 0, elapsed = true)
    }
}

enum class CountdownUnit { HOURS, MINUTES, SECONDS }

/**
 * Decompose [remaining] into h/m/s, clamped at zero.
 *
 * Clamping matters: the `isPassed` check and the countdown render happen at
 * different points in a frame, so an instant can elapse between them. Without
 * the clamp that renders as a negative time.
 */
fun countdownOf(remaining: Duration): CountdownParts {
    val total = remaining.inWholeSeconds
    if (total <= 0L) return CountdownParts.ZERO
    return CountdownParts(
        hours = total / 3600,
        minutes = (total % 3600) / 60,
        seconds = total % 60,
        elapsed = false,
    )
}

/** Decompose the gap between [now] and [target]. */
fun countdownTo(target: Instant, now: Instant): CountdownParts = countdownOf(target - now)

/**
 * How close an event is, used to drive *treatment* rather than just the number.
 *
 * The thresholds are deliberately coarse — they exist so a card can change its
 * visual weight as an event approaches, not to be precise. A renderer should
 * pick its own tick resolution from [tickResolution] so a card counting whole
 * hours does not recompose every second.
 */
enum class EventProximity {
    /** More than [APPROACHING_THRESHOLD] away — render quietly. */
    DISTANT,

    /** Within a couple of hours — the countdown becomes the focal element. */
    APPROACHING,

    /** Within a quarter hour — show seconds, saturate the accent. */
    IMMINENT,

    /** The event instant has arrived and its window is still open. */
    ACTIVE,

    /** The window has closed; the card should be replaced or dropped. */
    PASSED,
    ;

    val isBefore: Boolean get() = this == DISTANT || this == APPROACHING || this == IMMINENT

    companion object {
        val IMMINENT_THRESHOLD: Duration = 15.minutes
        val APPROACHING_THRESHOLD: Duration = 2.hours
    }
}

/**
 * Classify [eventAt] relative to [now].
 *
 * [windowEnd] is the absolute instant the event's window closes — e.g. next Fajr
 * for Tahajjud, Isha for Iftar/evening adhkar. Between [eventAt] and [windowEnd]
 * the event is [EventProximity.ACTIVE] (happening now). Pass `null` for an
 * instant with no window, which then goes straight from [EventProximity.IMMINENT]
 * to [EventProximity.PASSED].
 *
 * An absolute [windowEnd] (rather than an `activeWindow: Duration`) is what the
 * worship calculator naturally produces from `DayWorshipTimes`, and it avoids a
 * subtraction that can drift across DST.
 */
fun proximityOf(
    eventAt: Instant,
    now: Instant,
    windowEnd: Instant? = null,
): EventProximity {
    val until = eventAt - now
    return when {
        until > EventProximity.APPROACHING_THRESHOLD -> EventProximity.DISTANT
        until > EventProximity.IMMINENT_THRESHOLD -> EventProximity.APPROACHING
        until > Duration.ZERO -> EventProximity.IMMINENT
        windowEnd != null && now < windowEnd -> EventProximity.ACTIVE
        else -> EventProximity.PASSED
    }
}

/**
 * Fractional progress from [from] to [eventAt] at [now], clamped to 0f..1f.
 *
 * Drives the arc around a card's icon so proximity is glanceable without
 * reading digits. Returns 0f for a degenerate or inverted window rather than
 * dividing by zero.
 */
fun progressToward(eventAt: Instant, from: Instant, now: Instant): Float {
    val span = (eventAt - from).inWholeSeconds
    if (span <= 0L) return 0f
    val done = (now - from).inWholeSeconds
    return (done.toFloat() / span.toFloat()).coerceIn(0f, 1f)
}
