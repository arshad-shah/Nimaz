package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Pure JVM tests for [WorshipReminderCalculator]. No Android — all inputs are lambdas.
 *
 * A synthetic day: Fajr 05:00, Sunrise 06:30, Dhuhr 12:30, Asr 15:45, Maghrib 18:30, Isha 20:00,
 * and last-third-of-night at 02:40 the *following* morning (matching adhan2 semantics).
 */
class WorshipReminderCalculatorTest {

    private val calc = WorshipReminderCalculator()

    private fun timesForDay(d: LocalDate) = DayWorshipTimes(
        fajr = d.atTime(5, 0),
        sunrise = d.atTime(6, 30),
        dhuhr = d.atTime(12, 30),
        asr = d.atTime(15, 45),
        maghrib = d.atTime(18, 30),
        isha = d.atTime(20, 0),
        lastThirdOfNight = d.plusDays(1).atTime(2, 40)
    )

    /** Non-Ramadan Hijri: Rabi al-Awwal (month 3), day tracks the Gregorian day-of-month. */
    private fun nonRamadan(d: LocalDate) = HijriDayInfo(month = 3, day = ((d.dayOfMonth - 1) % 30) + 1)

    /** Ramadan (month 9), day = Gregorian day-of-month (good enough for gating tests). */
    private fun ramadan(d: LocalDate) = HijriDayInfo(month = 9, day = d.dayOfMonth)

    private fun next(
        type: WorshipReminderType,
        now: LocalDateTime,
        offset: Int = type.defaultOffsetMinutes,
        hijri: (LocalDate) -> HijriDayInfo? = ::nonRamadan
    ) = calc.nextOccurrence(type, now, offset, ::timesForDay, hijri)

    // ── Tahajjud: last third, self-correcting across midnight ────────────

    @Test
    fun `tahajjud in the evening targets tonight's last third (tomorrow 02_40)`() {
        val now = LocalDate.of(2026, 3, 10).atTime(21, 0)
        val occ = next(WorshipReminderType.TAHAJJUD, now)!!
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(2, 40))
    }

    @Test
    fun `tahajjud just after midnight still targets this same night, not next`() {
        // 00:30 on the 11th — the last third computed from the 10th's night is 02:40 today.
        val now = LocalDate.of(2026, 3, 11).atTime(0, 30)
        val occ = next(WorshipReminderType.TAHAJJUD, now)!!
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(2, 40))
    }

    @Test
    fun `tahajjud during the last third is still tonight's occurrence, active`() {
        // 03:00 is inside the last third (02:40 → Fajr 05:00): the window model keeps *this* night's
        // occurrence live and active rather than jumping to the next night (which the pre-window gate
        // did, hiding the card exactly when Tahajjud is happening).
        val now = LocalDate.of(2026, 3, 11).atTime(3, 0)
        val occ = next(WorshipReminderType.TAHAJJUD, now)!!
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(2, 40))
        assertThat(occ.isActiveAt(now)).isTrue()
    }

    @Test
    fun `tahajjud after the window closes rolls to the following night`() {
        // 06:00 is past Fajr (05:00) — tonight's window has closed, so it rolls forward.
        val now = LocalDate.of(2026, 3, 11).atTime(6, 0)
        val occ = next(WorshipReminderType.TAHAJJUD, now)!!
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 12).atTime(2, 40))
        assertThat(occ.isActiveAt(now)).isFalse()
    }

    // ── Witr: after-Isha (default) vs before-Fajr mode (#309) ────────────

    @Test
    fun `witr default mode fires after isha`() {
        val now = LocalDate.of(2026, 3, 10).atTime(19, 0)
        val occ = next(WorshipReminderType.WITR, now, offset = 30)!!
        // Isha 20:00 + 30m = 20:30 the same evening.
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 10).atTime(20, 30))
    }

    @Test
    fun `witr before-fajr mode fires fajr minus offset`() {
        val now = LocalDate.of(2026, 3, 10).atTime(21, 0)
        val occ = calc.nextOccurrence(
            WorshipReminderType.WITR, now, 30, ::timesForDay,
            hijriFor = ::nonRamadan, witrBeforeFajr = true
        )!!
        // Next Fajr 05:00 − 30m = 04:30 tomorrow morning.
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(4, 30))
    }

    // ── Offsets: Suhoor (before Fajr), Iftar (at Maghrib), Taraweeh (after Isha) ──

    @Test
    fun `suhoor fires 30 min before fajr and only in ramadan`() {
        val now = LocalDate.of(2026, 3, 10).atTime(23, 0)
        assertThat(next(WorshipReminderType.SUHOOR, now, hijri = ::nonRamadan)).isNull()
        val occ = next(WorshipReminderType.SUHOOR, now, hijri = ::ramadan)!!
        assertThat(occ.triggerAt.toLocalTime()).isEqualTo(LocalTime.of(4, 30))
        assertThat(occ.eventAt.toLocalTime()).isEqualTo(LocalTime.of(5, 0))
    }

    @Test
    fun `iftar fires at maghrib in ramadan`() {
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0)
        val occ = next(WorshipReminderType.IFTAR, now, hijri = ::ramadan)!!
        assertThat(occ.triggerAt.toLocalTime()).isEqualTo(LocalTime.of(18, 30))
    }

    @Test
    fun `taraweeh fires 15 min after isha in ramadan`() {
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0)
        val occ = next(WorshipReminderType.TARAWEEH, now, hijri = ::ramadan)!!
        assertThat(occ.triggerAt.toLocalTime()).isEqualTo(LocalTime.of(20, 15))
    }

    // ── Laylatul Qadr: odd nights of the last ten only ───────────────────

    @Test
    fun `laylatul qadr fires on odd last-ten nights only`() {
        // Ramadan day 27 (odd, in last ten) → fires; day 26 → skipped.
        val on27 = LocalDate.of(2026, 3, 27).atTime(12, 0)
        assertThat(next(WorshipReminderType.LAYLATUL_QADR, on27, hijri = ::ramadan)).isNotNull()

        val calc26 = calc.nextOccurrence(
            WorshipReminderType.LAYLATUL_QADR,
            LocalDate.of(2026, 3, 26).atTime(21, 0),
            0, ::timesForDay,
            hijriFor = { d -> HijriDayInfo(9, d.dayOfMonth) }
        )
        // From the eve of the 26th the next odd night is the 27th.
        assertThat(calc26!!.triggerAt.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 27))
    }

    // ── Adhkar: fixed offsets from Fajr / Asr ────────────────────────────

    @Test
    fun `morning and evening adhkar fire after fajr and asr`() {
        val now = LocalDate.of(2026, 3, 10).atTime(3, 0)
        assertThat(next(WorshipReminderType.ADHKAR_MORNING, now)!!.triggerAt.toLocalTime())
            .isEqualTo(LocalTime.of(5, 30))
        assertThat(next(WorshipReminderType.ADHKAR_EVENING, now)!!.triggerAt.toLocalTime())
            .isEqualTo(LocalTime.of(16, 15))
    }

    // ── Eve-of reminders: Mon/Thu, White Days, Arafah/Ashura ─────────────

    @Test
    fun `monday-thursday fast fires the evening before, tagged with the target day`() {
        // 2026-03-08 is a Sunday → its eve targets Monday the 9th.
        val sundayEve = LocalDate.of(2026, 3, 8).atTime(19, 0)
        val occ = next(WorshipReminderType.MONDAY_THURSDAY_FAST, sundayEve)!!
        assertThat(occ.triggerAt.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 8))
        assertThat(occ.subKey).isEqualTo("monday")
    }

    @Test
    fun `white days fast fires on the eve of the 13th hijri`() {
        // Search from a day well before; hijri day = Gregorian day-of-month, so eve of the 13th
        // is the 12th.
        val now = LocalDate.of(2026, 3, 1).atTime(12, 0)
        val occ = calc.nextOccurrence(
            WorshipReminderType.WHITE_DAYS_FAST, now, 0, ::timesForDay, ::nonRamadan
        )!!
        assertThat(occ.triggerAt.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 12))
    }

    @Test
    fun `arafah fast fires on the eve of 9 dhul-hijjah tagged arafah`() {
        val now = LocalDate.of(2026, 3, 1).atTime(12, 0)
        val occ = calc.nextOccurrence(
            WorshipReminderType.ARAFAH_ASHURA_FAST, now, 0, ::timesForDay,
            hijriFor = { d -> HijriDayInfo(month = 12, day = ((d.dayOfMonth - 1) % 30) + 1) }
        )!!
        assertThat(occ.subKey).isEqualTo("arafah")
        assertThat(occ.triggerAt.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 8)) // eve of the 9th
    }

    // ── requireFutureTrigger: the scheduler must never re-arm a past trigger ──

    @Test
    fun `requireFutureTrigger skips an active occurrence and rolls to the next future trigger`() {
        // 03:00 is inside tonight's Tahajjud window (02:40 → Fajr 05:00). The Home card wants this
        // active occurrence (default gate); the scheduler must NOT — arming its past 02:40 trigger
        // would fire immediately. With requireFutureTrigger it rolls to tomorrow night's 02:40.
        val now = LocalDate.of(2026, 3, 11).atTime(3, 0)

        val forCard = calc.nextOccurrence(
            WorshipReminderType.TAHAJJUD, now, 0, ::timesForDay, ::nonRamadan
        )!!
        assertThat(forCard.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(2, 40))
        assertThat(forCard.isActiveAt(now)).isTrue()

        val forScheduler = calc.nextOccurrence(
            WorshipReminderType.TAHAJJUD, now, 0, ::timesForDay, ::nonRamadan,
            requireFutureTrigger = true
        )!!
        assertThat(forScheduler.triggerAt).isEqualTo(LocalDate.of(2026, 3, 12).atTime(2, 40))
        assertThat(forScheduler.triggerAt.isAfter(now)).isTrue()
    }

    @Test
    fun `requireFutureTrigger rolls a fired-but-still-open adhkar to tomorrow`() {
        // 06:00: morning adhkar (Fajr 05:00 + 30m = 05:30) already fired, but its window
        // (Fajr → Dhuhr 12:30) is still open. The scheduler must arm tomorrow's 05:30, not
        // re-fire today's past trigger.
        val now = LocalDate.of(2026, 3, 10).atTime(6, 0)

        val forScheduler = calc.nextOccurrence(
            WorshipReminderType.ADHKAR_MORNING, now, 30, ::timesForDay, ::nonRamadan,
            requireFutureTrigger = true
        )!!
        assertThat(forScheduler.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(5, 30))
    }

    @Test
    fun `requireFutureTrigger keeps a genuinely upcoming trigger unchanged`() {
        // Before the event, both gates agree — the scheduler still arms tonight's occurrence.
        val now = LocalDate.of(2026, 3, 10).atTime(21, 0)
        val occ = calc.nextOccurrence(
            WorshipReminderType.TAHAJJUD, now, 0, ::timesForDay, ::nonRamadan,
            requireFutureTrigger = true
        )!!
        assertThat(occ.triggerAt).isEqualTo(LocalDate.of(2026, 3, 11).atTime(2, 40))
    }

    @Test
    fun `disabled or non-matching types return null`() {
        // A non-Ramadan day yields no Ramadan reminders.
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0)
        assertThat(next(WorshipReminderType.TARAWEEH, now, hijri = ::nonRamadan)).isNull()
    }
}
