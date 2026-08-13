package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The two Ramadan calculations that had no test because they had nowhere to live.
 *
 * Both were inside private composables in `FastTrackerScreen.kt` — the countdown read
 * `HijriDateCalculator.daysUntilNextRamadan()` at composition, the unlogged-days count read
 * `LocalDate.now()` — so nothing could reach either, and both went stale on a screen left open
 * across midnight. Issue #492 moves those cards to the shared component layer, which is what
 * made the impurity worth fixing rather than carrying: a composable that reads the clock is a
 * defect every future caller inherits.
 */
class RamadanCalculationsTest {

    // ── the countdown ─────────────────────────────────────────────────────────

    private fun countdownOn(date: LocalDate, offset: Int = 0) =
        GetRamadanCountdownUseCase(FakeTodayProvider(date))(offset)

    /** A Gregorian date whose Hijri month is [hijriMonth], found by walking forward. */
    private fun gregorianInHijriMonth(hijriMonth: Int, day: Int = 1): LocalDate {
        var date = LocalDate.of(2026, 1, 1)
        repeat(800) {
            val hijri = HijriDateCalculator.toHijri(date)
            if (hijri.month == hijriMonth && hijri.day == day) return date
            date = date.plusDays(1)
        }
        error("no date in Hijri month $hijriMonth day $day in range")
    }

    @Test
    fun `the countdown lands on the first day of Ramadan`() {
        val inShaban = gregorianInHijriMonth(hijriMonth = 8, day = 1)

        val countdown = countdownOn(inShaban)

        assertThat(HijriDateCalculator.toHijri(countdown.startsOn).month).isEqualTo(9)
        assertThat(HijriDateCalculator.toHijri(countdown.startsOn).day).isEqualTo(1)
        assertThat(inShaban.plusDays(countdown.daysAway.toLong()))
            .isEqualTo(countdown.startsOn)
    }

    @Test
    fun `during Ramadan the countdown is zero rather than eleven months`() {
        // Month 9 counts as "this year": treating it as past would make the card claim the
        // next Ramadan is 350 days away while the user is fasting it.
        val inRamadan = gregorianInHijriMonth(hijriMonth = 9, day = 10)

        assertThat(countdownOn(inRamadan).daysAway).isEqualTo(0)
    }

    @Test
    fun `after Ramadan the countdown rolls to next year`() {
        val inShawwal = gregorianInHijriMonth(hijriMonth = 10, day = 5)

        val countdown = countdownOn(inShawwal)

        assertThat(countdown.startsOn).isGreaterThan(inShawwal)
        assertThat(HijriDateCalculator.toHijri(countdown.startsOn).month).isEqualTo(9)
        // Roughly a lunar year out, and certainly not a small number.
        assertThat(countdown.daysAway).isGreaterThan(300)
    }

    @Test
    fun `the count is never negative, on any day of the year`() {
        var date = LocalDate.of(2026, 1, 1)
        repeat(400) {
            assertThat(countdownOn(date).daysAway).isAtLeast(0)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `the hijri day offset shifts the answer, which is registry Open #10`() {
        // A user who shifted their Hijri date to match a local moon sighting saw every other
        // date honour it and this countdown ignore it.
        val inShaban = gregorianInHijriMonth(hijriMonth = 8, day = 20)

        val plain = countdownOn(inShaban).daysAway
        assertThat(countdownOn(inShaban, offset = 1).daysAway).isEqualTo(plain - 1)
    }

    @Test
    fun `the clock is a seam, so a card open across midnight is not stuck on yesterday`() {
        val eve = gregorianInHijriMonth(hijriMonth = 8, day = 20)

        assertThat(countdownOn(eve.plusDays(1)).daysAway)
            .isEqualTo(countdownOn(eve).daysAway - 1)
    }

    // ── unlogged days ─────────────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 3, 15)

    private fun unloggedOn(currentDay: Int, records: List<FastRecord>) =
        CountUnloggedRamadanDaysUseCase(FakeTodayProvider(today))(currentDay, records)

    private fun recordOn(date: LocalDate) = FastRecord(
        id = 0,
        date = date.toEpochDay() * 24 * 60 * 60 * 1000,
        hijriDate = "",
        hijriMonth = 9,
        hijriYear = 1447,
        fastType = FastType.RAMADAN,
        status = FastStatus.FASTED,
        exemptionReason = null,
        suhoorTime = null,
        iftarTime = null,
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun `the first day of Ramadan owes nothing, because today is still in progress`() {
        assertThat(unloggedOn(currentDay = 1, records = emptyList())).isEqualTo(0)
    }

    @Test
    fun `days gone by with no record at all are what the card counts`() {
        // Day 5, so four days have elapsed; two of them were logged.
        val logged = listOf(recordOn(today.minusDays(1)), recordOn(today.minusDays(2)))

        assertThat(unloggedOn(currentDay = 5, records = logged)).isEqualTo(2)
    }

    @Test
    fun `today's own record does not count towards days gone by`() {
        // Logging today must not make the card claim a day is unaccounted for elsewhere.
        val logged = listOf(recordOn(today))

        assertThat(unloggedOn(currentDay = 3, records = logged)).isEqualTo(2)
    }

    @Test
    fun `more records than elapsed days never reads as negative`() {
        val logged = (1..6).map { recordOn(today.minusDays(it.toLong())) }

        assertThat(unloggedOn(currentDay = 3, records = logged)).isEqualTo(0)
    }
}
