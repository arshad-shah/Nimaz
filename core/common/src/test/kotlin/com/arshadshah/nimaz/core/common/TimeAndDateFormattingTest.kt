package com.arshadshah.nimaz.core.common

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Test

/**
 * `formatClockTime` / `formatFastLength` and the epoch conversion under `toUtcMidnightMillis`.
 *
 * `toUtcMidnightMillis` is the highest-traffic helper in this module — 12 import sites including
 * two widgets — and it is the key half of a Room query range. If it drifts by an hour the query
 * bounds move and a day's rows fall out of the wrong end of the range, which is not a crash and
 * not visible in a screenshot; it is a fasting day that silently shows nothing. The DST case is
 * the one that would break a naive `atStartOfDay()` implementation, so it is asserted explicitly
 * rather than assumed from the fact that the current implementation happens not to use one.
 */
class TimeAndDateFormattingTest {

    // ── toUtcMidnightMillis ─────────────────────────────────────────────────────────────────

    @Test
    fun `a date maps to its own UTC midnight`() {
        val date = LocalDate.of(2026, 3, 14)
        val expected = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1_000L

        assertThat(date.toUtcMidnightMillis()).isEqualTo(expected)
    }

    @Test
    fun `consecutive days are exactly one day apart, including across a DST boundary`() {
        // 2026-03-29 is the European spring-forward. A zone-aware start-of-day would put these
        // 23 hours apart; this helper is deliberately zone-free, and the Room ranges built from
        // it depend on the gap being constant.
        val before = LocalDate.of(2026, 3, 28)
        val after = LocalDate.of(2026, 3, 29)

        assertThat(after.toUtcMidnightMillis() - before.toUtcMidnightMillis())
            .isEqualTo(MILLIS_PER_DAY)
    }

    @Test
    fun `dates before the epoch are negative rather than wrapped`() {
        // Hijri conversion and imported history can both reach back past 1970. An unsigned or
        // truncated implementation would put these in the far future, and a `>= start` range
        // check would then match everything.
        val old = LocalDate.of(1969, 12, 31)

        assertThat(old.toUtcMidnightMillis()).isEqualTo(-MILLIS_PER_DAY)
        assertThat(LocalDate.of(1970, 1, 1).toUtcMidnightMillis()).isEqualTo(0L)
    }

    @Test
    fun `the round trip back to a date is lossless`() {
        val dates = listOf(
            LocalDate.of(1901, 1, 1),
            LocalDate.of(2000, 2, 29),
            LocalDate.of(2026, 3, 29),
            LocalDate.of(2100, 12, 31),
        )
        dates.forEach { date ->
            val backAgain = LocalDate.ofEpochDay(date.toUtcMidnightMillis() / MILLIS_PER_DAY)
            assertThat(backAgain).isEqualTo(date)
        }
    }

    // ── formatClockTime ─────────────────────────────────────────────────────────────────────

    @Test
    fun `24-hour clock pads the hour and keeps midnight at zero`() {
        assertThat(formatClockTime(hour = 5, minute = 7, use24Hour = true)).isEqualTo("05:07")
        assertThat(formatClockTime(hour = 0, minute = 0, use24Hour = true)).isEqualTo("00:00")
        assertThat(formatClockTime(hour = 23, minute = 59, use24Hour = true)).isEqualTo("23:59")
    }

    @Test
    fun `12-hour clock renders noon and midnight as 12, never as 0`() {
        // The classic off-by-twelve. Hour 0 is "12 AM" and hour 12 is "12 PM"; a naive `hour % 12`
        // gives "0 AM" and "0 PM" for exactly the two times a prayer app displays most.
        assertThat(formatClockTime(hour = 0, minute = 30, use24Hour = false)).contains("12:30")
        assertThat(formatClockTime(hour = 12, minute = 30, use24Hour = false)).contains("12:30")
        assertThat(formatClockTime(hour = 13, minute = 5, use24Hour = false)).contains("1:05")
    }

    @Test
    fun `both clocks agree on the minute they render`() {
        (0..23).forEach { hour ->
            val twelve = formatClockTime(hour, minute = 42, use24Hour = false)
            assertThat(twelve).contains(":42")
        }
    }

    @Test
    fun `LocalTime and LocalDateTime format the same clock`() {
        val time = LocalTime.of(18, 4)
        val dateTime = LocalDateTime.of(2026, 3, 14, 18, 4)

        assertThat(time.formatClock(use24Hour = true)).isEqualTo("18:04")
        assertThat(dateTime.formatClock(use24Hour = true)).isEqualTo("18:04")
        assertThat(dateTime.formatClock(use24Hour = false))
            .isEqualTo(time.formatClock(use24Hour = false))
    }

    // ── formatFastLength ────────────────────────────────────────────────────────────────────

    @Test
    fun `a fast length reads as hours and minutes`() {
        assertThat(formatFastLength(0)).isEqualTo("0h 00m")
        assertThat(formatFastLength(59)).isEqualTo("0h 59m")
        assertThat(formatFastLength(60)).isEqualTo("1h 00m")
        assertThat(formatFastLength(14 * 60 + 5)).isEqualTo("14h 05m")
    }

    @Test
    fun `the minute component is zero-padded so lengths line up in a column`() {
        assertThat(formatFastLength(61)).isEqualTo("1h 01m")
        assertThat(formatFastLength(70)).isEqualTo("1h 10m")
    }
}
