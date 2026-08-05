package com.arshadshah.nimaz.presentation.viewmodel.prayer

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Test
import kotlin.time.Instant

/**
 * The fast length is **elapsed time between two instants**, not the difference of two wall clocks.
 *
 * The month table computed it as
 * `(maghrib.hour*60 + maghrib.minute) - (fajr.hour*60 + fajr.minute)`, which is wrong twice: it
 * floors both operands before subtracting, so seconds vanish; and wall-clock fields do not know
 * about a DST transition falling between the two.
 *
 * This suite is arithmetic over `Instant`, so it needs none of the app — which is the point. The
 * old form could not be tested at this level at all, because it needed a `LocalDateTime` and
 * therefore a timezone, and the timezone was the bug.
 */
class FastLengthTest {

    /** What the ViewModel now does. */
    private fun fastMinutes(fajr: Instant, maghrib: Instant): Int =
        (maghrib - fajr).inWholeMinutes.toInt()

    /** What it used to do, kept here so the difference is demonstrable rather than asserted. */
    private fun legacyFastMinutes(fajr: Instant, maghrib: Instant, zone: TimeZone): Int {
        val f = fajr.toLocalDateTime(zone)
        val m = maghrib.toLocalDateTime(zone)
        var mins = (m.hour * 60 + m.minute) - (f.hour * 60 + f.minute)
        if (mins < 0) mins += 24 * 60
        return mins
    }

    /**
     * The issue's own example: Fajr 05:00:59, Maghrib 20:00:01. True elapsed is 899 minutes and
     * 2 seconds, so a whole-minute answer is 899. The old form reported 900 — it floored 05:00:59
     * to 05:00 and 20:00:01 to 20:00 and subtracted those.
     */
    @Test
    fun `seconds are not discarded on both sides before subtracting`() {
        val fajr = Instant.parse("2026-03-14T05:00:59Z")
        val maghrib = Instant.parse("2026-03-14T20:00:01Z")

        assertThat(fastMinutes(fajr, maghrib)).isEqualTo(899)
        assertThat(legacyFastMinutes(fajr, maghrib, TimeZone.UTC)).isEqualTo(900)
    }

    /**
     * A DST transition between Fajr and Maghrib.
     *
     * London springs forward at 01:00 UTC on 29 March 2026, so a fast that starts before it and
     * ends after it spans one hour less wall-clock than elapsed time. Reading hour/minute fields
     * reports the wall-clock difference; the instants report the truth.
     */
    @Test
    fun `a DST transition between fajr and maghrib does not change the fast length`() {
        val london = TimeZone.of("Europe/London")
        val fajr = Instant.parse("2026-03-29T00:30:00Z")
        val maghrib = Instant.parse("2026-03-29T18:30:00Z")

        // 18 hours of real elapsed time, whatever the clocks did in between.
        assertThat(fastMinutes(fajr, maghrib)).isEqualTo(18 * 60)
        // The old form sees 01:30 → 19:30 local and reports an hour more.
        assertThat(legacyFastMinutes(fajr, maghrib, london)).isEqualTo(18 * 60 + 60)
    }

    /** An ordinary day is unchanged — the fix must not move the common case. */
    @Test
    fun `an ordinary day is unaffected`() {
        val fajr = Instant.parse("2026-06-14T03:00:00Z")
        val maghrib = Instant.parse("2026-06-14T20:15:00Z")

        assertThat(fastMinutes(fajr, maghrib)).isEqualTo(17 * 60 + 15)
        assertThat(legacyFastMinutes(fajr, maghrib, TimeZone.UTC))
            .isEqualTo(fastMinutes(fajr, maghrib))
    }
}
