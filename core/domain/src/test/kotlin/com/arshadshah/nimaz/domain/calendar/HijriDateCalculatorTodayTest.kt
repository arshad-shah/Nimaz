package com.arshadshah.nimaz.domain.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The four calculator functions that ask about *today*.
 *
 * They read `LocalDate.now()` rather than taking a date, so they cannot be pinned to a fixed
 * answer — but the interesting failures here are not "wrong day", they are shape failures that
 * hold whatever day the suite runs on:
 *
 * - a Ramadan countdown that goes negative, or past a year, has picked the wrong Hijri year —
 *   the same off-by-a-year that made "upcoming events" drop Islamic New Year and Ashura during
 *   the last weeks of a Hijri year;
 * - "days remaining in Ramadan" and "is it Ramadan" are two answers to one question, and they
 *   must never disagree;
 * - the upcoming-events list must respect its limit and be sorted, or the calendar's "next up"
 *   card names something that is not next.
 *
 * Anything that needs a fixed today is tested through `CalendarUseCases`, which takes one.
 */
class HijriDateCalculatorTodayTest {

    @Test
    fun `the countdown to Ramadan is a number of days within one year`() {
        val days = HijriDateCalculator.daysUntilNextRamadan()

        // Negative means the target year was chosen behind today; over a year means it was
        // chosen too far ahead. Both are the same off-by-a-year, in opposite directions.
        assertThat(days).isAtLeast(0)
        assertThat(days).isLessThan(355)
    }

    @Test
    fun `days remaining in Ramadan is either a real count or the not-in-Ramadan sentinel`() {
        val remaining = HijriDateCalculator.daysRemainingInRamadan()

        if (remaining != -1) {
            assertThat(remaining).isAtLeast(1)
            assertThat(remaining).isAtMost(30)
        }
    }

    @Test
    fun `the two Ramadan questions never disagree`() {
        val inRamadan = HijriDateCalculator.isTodayRamadan()
        val remaining = HijriDateCalculator.daysRemainingInRamadan()

        assertThat(remaining != -1).isEqualTo(inRamadan)
    }

    @Test
    fun `the countdown is zero exactly when Ramadan has begun today`() {
        val today = HijriDateCalculator.toHijri(LocalDate.now())

        if (today.month == 9 && today.day == 1) {
            assertThat(HijriDateCalculator.daysUntilNextRamadan()).isEqualTo(0)
        } else {
            assertThat(HijriDateCalculator.daysUntilNextRamadan()).isNotEqualTo(0)
        }
    }

    // ---- Upcoming events ----

    @Test
    fun `upcoming events default to five`() {
        assertThat(HijriDateCalculator.getUpcomingEvents()).hasSize(5)
    }

    @Test
    fun `a caller that wants more or fewer gets them`() {
        assertThat(HijriDateCalculator.getUpcomingEvents(limit = 2)).hasSize(2)
        assertThat(HijriDateCalculator.getUpcomingEvents(limit = 10)).hasSize(10)
    }

    @Test
    fun `upcoming events come back soonest first`() {
        val events = HijriDateCalculator.getUpcomingEvents(limit = 10)

        val ordered = events.map { it.year * 10_000 + it.month * 100 + it.day }
        assertThat(ordered).isInOrder()
    }

    @Test
    fun `nothing already past is called upcoming`() {
        val today = HijriDateCalculator.toHijri(LocalDate.now())

        HijriDateCalculator.getUpcomingEvents(limit = 10).forEach { event ->
            val notBehind = when {
                event.year > today.year -> true
                event.year < today.year -> false
                event.month > today.month -> true
                event.month < today.month -> false
                else -> event.day >= today.day
            }
            assertThat(notBehind).isTrue()
        }
    }

    @Test
    fun `the list reaches into next year rather than running out`() {
        // Thirteen events a year, so asking for more than thirteen only works if the year after
        // is consulted too — which is the same fix as the projection bug in `CalendarUseCases`.
        assertThat(HijriDateCalculator.getUpcomingEvents(limit = 20)).hasSize(20)
    }

    @Test
    fun `asking for none gives none`() {
        assertThat(HijriDateCalculator.getUpcomingEvents(limit = 0)).isEmpty()
    }
}
