package com.arshadshah.nimaz.core.common

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Test

/**
 * The countdown decomposition and the proximity classification that drives card treatment.
 *
 * Both are pure, both are read every second by whatever is on screen, and neither had a test.
 * The interesting cases are the boundaries — the clamp at zero, the thresholds between
 * treatments, and the window that separates "happening now" from "over" — because each is a
 * `>` that could as easily have been a `>=` and no reviewer would notice.
 */
class CountdownFormattingTest {

    // ── countdownOf ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `a duration decomposes into hours, minutes and seconds`() {
        val parts = countdownOf(3.hours + 25.minutes + 9.seconds)

        assertThat(parts.hours).isEqualTo(3)
        assertThat(parts.minutes).isEqualTo(25)
        assertThat(parts.seconds).isEqualTo(9)
        assertThat(parts.elapsed).isFalse()
    }

    @Test
    fun `hours are not wrapped at 24`() {
        // A Ramadan countdown is measured in weeks. Truncating to a day would show "2 hours".
        assertThat(countdownOf(3.days + 2.hours).hours).isEqualTo(74)
    }

    @Test
    fun `an elapsed or exactly-due countdown clamps to zero rather than going negative`() {
        // The documented reason: `isPassed` and the render happen at different points in a
        // frame, so an instant can elapse between them. Without the clamp that shows as "-1s".
        assertThat(countdownOf(Duration.ZERO)).isEqualTo(CountdownParts.ZERO)
        assertThat(countdownOf((-5).seconds)).isEqualTo(CountdownParts.ZERO)
        assertThat(countdownOf((-3).hours)).isEqualTo(CountdownParts.ZERO)
        assertThat(countdownOf((-1).hours).elapsed).isTrue()
    }

    @Test
    fun `a sub-second remainder is already zero, not rounded up to one second`() {
        // inWholeSeconds truncates, so 900ms is 0 total seconds and takes the elapsed branch.
        // Worth pinning: the alternative reading — "there is still time, show 0h 0m 0s but not
        // elapsed" — is a different UI state.
        assertThat(countdownOf(900.milliseconds)).isEqualTo(CountdownParts.ZERO)
    }

    @Test
    fun `countdownTo is the gap between two instants`() {
        val now = Instant.parse("2026-03-14T12:00:00Z")
        val target = Instant.parse("2026-03-14T13:30:45Z")

        val parts = countdownTo(target, now)

        assertThat(parts.hours).isEqualTo(1)
        assertThat(parts.minutes).isEqualTo(30)
        assertThat(parts.seconds).isEqualTo(45)
    }

    // ── proximityOf ─────────────────────────────────────────────────────────────────────────

    private val now = Instant.parse("2026-03-14T12:00:00Z")

    @Test
    fun `proximity steps through the treatments as the event approaches`() {
        assertThat(proximityOf(now + 5.hours, now)).isEqualTo(EventProximity.DISTANT)
        assertThat(proximityOf(now + 1.hours, now)).isEqualTo(EventProximity.APPROACHING)
        assertThat(proximityOf(now + 5.minutes, now)).isEqualTo(EventProximity.IMMINENT)
    }

    @Test
    fun `each threshold is exclusive, so landing exactly on one takes the nearer treatment`() {
        // `until > THRESHOLD`, not `>=`. At exactly two hours the card is already APPROACHING
        // rather than DISTANT, and at exactly fifteen minutes it is IMMINENT.
        assertThat(proximityOf(now + 2.hours, now)).isEqualTo(EventProximity.APPROACHING)
        assertThat(proximityOf(now + 2.hours + 1.seconds, now)).isEqualTo(EventProximity.DISTANT)
        assertThat(proximityOf(now + 15.minutes, now)).isEqualTo(EventProximity.IMMINENT)
        assertThat(proximityOf(now + 15.minutes + 1.seconds, now))
            .isEqualTo(EventProximity.APPROACHING)
    }

    @Test
    fun `an event with an open window is active, not passed`() {
        // Iftar has happened but the window runs to Isha: the card should still be on screen.
        val iftar = now - 10.minutes
        assertThat(proximityOf(iftar, now, windowEnd = now + 1.hours))
            .isEqualTo(EventProximity.ACTIVE)
    }

    @Test
    fun `the window closing is what makes an event passed`() {
        val iftar = now - 2.hours
        assertThat(proximityOf(iftar, now, windowEnd = now - 1.hours))
            .isEqualTo(EventProximity.PASSED)
        // windowEnd == now is closed: `now < windowEnd` is strict.
        assertThat(proximityOf(iftar, now, windowEnd = now)).isEqualTo(EventProximity.PASSED)
    }

    @Test
    fun `an instant with no window goes straight from imminent to passed`() {
        assertThat(proximityOf(now + 1.seconds, now)).isEqualTo(EventProximity.IMMINENT)
        assertThat(proximityOf(now, now)).isEqualTo(EventProximity.PASSED)
    }

    @Test
    fun `isBefore covers exactly the three not-yet-happened treatments`() {
        val before = EventProximity.entries.filter { it.isBefore }
        assertThat(before).containsExactly(
            EventProximity.DISTANT, EventProximity.APPROACHING, EventProximity.IMMINENT,
        )
    }

    // ── progressToward ──────────────────────────────────────────────────────────────────────

    @Test
    fun `progress runs from zero at the start of the window to one at the event`() {
        val from = now
        val event = now + 4.hours

        assertThat(progressToward(event, from, from)).isEqualTo(0f)
        assertThat(progressToward(event, from, event)).isEqualTo(1f)
        assertThat(progressToward(event, from, from + 1.hours)).isWithin(0.001f).of(0.25f)
    }

    @Test
    fun `progress clamps rather than overshooting once the event has passed`() {
        val from = now
        val event = now + 1.hours
        assertThat(progressToward(event, from, event + 5.hours)).isEqualTo(1f)
        assertThat(progressToward(event, from, from - 5.hours)).isEqualTo(0f)
    }

    @Test
    fun `a degenerate or inverted window is zero rather than a division by zero`() {
        assertThat(progressToward(now, now, now)).isEqualTo(0f)
        assertThat(progressToward(now - 1.hours, now, now)).isEqualTo(0f)
    }
}
