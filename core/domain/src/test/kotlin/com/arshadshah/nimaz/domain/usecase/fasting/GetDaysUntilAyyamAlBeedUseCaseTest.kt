package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The countdown to the white days, which had no test because it had nowhere to live.
 *
 * It was `private fun calculateAyyamAlBeedDays(today: LocalDate)` inside a 1,779-line screen
 * file, so nothing could reach it; one of its two callers passed `LocalDate.now()`. These are
 * the cases that were unreachable before the move (audit §5.1).
 */
class GetDaysUntilAyyamAlBeedUseCaseTest {

    private fun daysOn(date: LocalDate, offset: Int = 0): Int =
        GetDaysUntilAyyamAlBeedUseCase(FakeTodayProvider(date))(offset)

    /** A Gregorian date whose Hijri day is [hijriDay], found by walking forward from a seed. */
    private fun gregorianForHijriDay(hijriDay: Int): LocalDate {
        var date = LocalDate.of(2026, 1, 1)
        repeat(400) {
            if (HijriDateCalculator.toHijri(date).day == hijriDay) return date
            date = date.plusDays(1)
        }
        error("no date with Hijri day $hijriDay in range")
    }

    @Test
    fun `the white days themselves count zero`() {
        for (day in 13..15) {
            assertThat(daysOn(gregorianForHijriDay(day))).isEqualTo(0)
        }
    }

    @Test
    fun `before the 13th it counts forward within the same month`() {
        assertThat(daysOn(gregorianForHijriDay(1))).isEqualTo(12)
        assertThat(daysOn(gregorianForHijriDay(12))).isEqualTo(1)
    }

    @Test
    fun `after the 15th it runs out the month and on to the next 13th`() {
        val date = gregorianForHijriDay(20)
        val hijri = HijriDateCalculator.toHijri(date)
        val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(hijri.year, hijri.month)

        // Not a hard-coded number: the length of a Hijri month varies, which is the whole
        // reason this branch cannot just add a constant.
        assertThat(daysOn(date)).isEqualTo((daysInMonth - 20) + 13)
    }

    @Test
    fun `the count is never negative and never longer than a lunar month`() {
        var date = LocalDate.of(2026, 1, 1)
        repeat(365) {
            assertThat(daysOn(date)).isAtLeast(0)
            assertThat(daysOn(date)).isAtMost(30)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `the hijri day offset shifts the answer, which is registry Open #10`() {
        // A user who shifted their Hijri date by a day to match a local moon sighting used to
        // see event matching honour that and this countdown ignore it.
        val dayTwelve = gregorianForHijriDay(12)

        assertThat(daysOn(dayTwelve, offset = 0)).isEqualTo(1)
        // +1 day makes it the 13th: the white days have started.
        assertThat(daysOn(dayTwelve, offset = 1)).isEqualTo(0)
        // -1 day makes it the 11th, so two days to go.
        assertThat(daysOn(dayTwelve, offset = -1)).isEqualTo(2)
    }

    @Test
    fun `the clock is a seam, so a screen open across midnight is not stuck on yesterday`() {
        val eve = gregorianForHijriDay(12)

        // Same use case, two days: the answer moves because the provider does, which is what
        // `LocalDate.now()` at composition could not express.
        assertThat(daysOn(eve)).isEqualTo(1)
        assertThat(daysOn(eve.plusDays(1))).isEqualTo(0)
    }
}
