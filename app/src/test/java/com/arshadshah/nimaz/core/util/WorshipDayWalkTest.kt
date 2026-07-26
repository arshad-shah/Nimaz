package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Walks a synthetic day hour by hour and asserts which worship reminder should
 * be surfaced at each step.
 *
 * ## Why this test exists
 *
 * The deployed app showed **Evening Adhkar all morning** (and Morning Adhkar
 * all evening). Root cause: occurrences were instants gated on
 * `triggerAt.isAfter(now)`, so the moment a trigger passed, that type's next
 * occurrence jumped ~24h out and fell off the resolver's 14h near-window —
 * leaving the *opposite* card as the only candidate. The same mechanism hid
 * Iftar at Maghrib and Tahajjud when the last third began.
 *
 * The previous suite couldn't catch this because it sampled single instants —
 * and one test (`filters out reminders beyond the near window` in
 * [NextWorshipResolverTest]) actually asserted the buggy behaviour. A day-walk
 * makes the selection *trajectory* the thing under test, so an inversion
 * anywhere in the day fails loudly with the full timeline in the message.
 *
 * ## Level under test
 *
 * This drives [WorshipReminderCalculator.nextOccurrence] plus a replica of
 * [NextWorshipResolver]'s selection (filter + ranking) with fixed inputs.
 * The resolver's DataStore/PrayerTimeCalculator plumbing is covered by
 * [NextWorshipResolverTest]; duplicating the *selection* here keeps this test
 * pure-JVM and millisecond-fast. If selection logic changes in the resolver,
 * update [select] to match — the duplication is deliberate and small.
 *
 * Fixture (same synthetic day as [WorshipReminderCalculatorTest]):
 * Fajr 05:00 · Sunrise 06:30 · Dhuhr 12:30 · Asr 15:45 · Maghrib 18:30 ·
 * Isha 20:00 · last third 02:40 next morning. Adhkar offset 30m → morning
 * trigger 05:30, evening trigger 16:15.
 */
class WorshipDayWalkTest {

    private val calc = WorshipReminderCalculator()

    private fun timesForDay(d: LocalDate) = DayWorshipTimes(
        fajr = d.atTime(5, 0),
        sunrise = d.atTime(6, 30),
        dhuhr = d.atTime(12, 30),
        asr = d.atTime(15, 45),
        maghrib = d.atTime(18, 30),
        isha = d.atTime(20, 0),
        lastThirdOfNight = d.plusDays(1).atTime(2, 40),
    )

    /** Non-Ramadan so the fasting/taraweeh types stay out of the walk. */
    private fun nonRamadan(d: LocalDate) = HijriDayInfo(month = 3, day = ((d.dayOfMonth - 1) % 30) + 1)

    private val nearWindow: Duration = Duration.ofHours(14)

    /**
     * Replica of `NextWorshipResolver.nearest()`'s selection over fixed inputs:
     * active-window occurrences always pass the filter and outrank countdowns;
     * otherwise nearest `triggerAt` within the 14h `eventAt` window wins.
     */
    private fun select(
        enabled: List<WorshipReminderType>,
        now: LocalDateTime,
    ): WorshipReminderOccurrence? = enabled
        .mapNotNull { type ->
            calc.nextOccurrence(
                type, now, type.defaultOffsetMinutes,
                ::timesForDay, ::nonRamadan, maxSearchDays = 2,
            )
        }
        .filter { it.isActiveAt(now) || Duration.between(now, it.eventAt) <= nearWindow }
        .minWithOrNull(compareBy({ if (it.isActiveAt(now)) 0 else 1 }, { it.triggerAt }))

    /** One expected step of the walk. */
    private data class Step(
        val hour: Int,
        val minute: Int = 0,
        val expected: WorshipReminderType?,
        val expectActive: Boolean = false,
        val why: String,
    )

    private fun runWalk(day: LocalDate, enabled: List<WorshipReminderType>, steps: List<Step>) {
        val failures = StringBuilder()
        for (s in steps) {
            val now = day.atTime(s.hour, s.minute)
            val got = select(enabled, now)
            val typeOk = got?.type == s.expected
            val activeOk = !s.expectActive || (got?.isActiveAt(now) == true)
            if (!typeOk || !activeOk) {
                failures.append(
                    "  %02d:%02d  expected=%s%s  got=%s%s   (%s)\n".format(
                        s.hour, s.minute,
                        s.expected ?: "nothing", if (s.expectActive) " [ACTIVE]" else "",
                        got?.type ?: "nothing",
                        if (got?.isActiveAt(now) == true) " [ACTIVE]" else "",
                        s.why,
                    )
                )
            }
        }
        assertWithMessage("Day-walk mismatches:\n$failures").that(failures.isEmpty()).isTrue()
    }

    // ── The regression that shipped ─────────────────────────────────────────

    @Test
    fun `adhkar day-walk - morning card in the morning, evening card in the evening`() {
        val day = LocalDate.of(2026, 3, 10)
        val adhkar = listOf(WorshipReminderType.ADHKAR_MORNING, WorshipReminderType.ADHKAR_EVENING)
        // NOTE on window ends: this walk assumes the accommodating close
        // (morning → Dhuhr, evening → Isha). If the sunrise/maghrib decision
        // from patch 02 (§2c) goes the other way, adjust the 06:00–12:00 and
        // 18:00–19:00 expectations — deliberately, not by weakening the test.
        runWalk(
            day, adhkar,
            listOf(
                Step(4, expected = WorshipReminderType.ADHKAR_MORNING,
                    why = "pre-Fajr: morning trigger 05:30 is the nearest upcoming"),
                Step(5, 45, expected = WorshipReminderType.ADHKAR_MORNING, expectActive = true,
                    why = "trigger passed, window open — the production bug flipped this to EVENING"),
                Step(7, expected = WorshipReminderType.ADHKAR_MORNING, expectActive = true,
                    why = "mid-morning: window still open; must NOT show evening card"),
                Step(11, expected = WorshipReminderType.ADHKAR_MORNING, expectActive = true,
                    why = "late morning, still before Dhuhr close"),
                Step(13, expected = WorshipReminderType.ADHKAR_EVENING,
                    why = "morning window closed at Dhuhr; evening trigger 16:15 upcoming"),
                Step(15, expected = WorshipReminderType.ADHKAR_EVENING,
                    why = "afternoon countdown to evening adhkar"),
                Step(17, expected = WorshipReminderType.ADHKAR_EVENING, expectActive = true,
                    why = "evening trigger passed, window open until Isha"),
                Step(19, expected = WorshipReminderType.ADHKAR_EVENING, expectActive = true,
                    why = "post-Maghrib, pre-Isha — mirror of the morning bug"),
                Step(21, expected = WorshipReminderType.ADHKAR_MORNING,
                    why = "evening window closed at Isha; tomorrow 05:30 trigger, event Fajr 05:00 within 14h"),
                Step(23, expected = WorshipReminderType.ADHKAR_MORNING,
                    why = "late night: countdown to tomorrow's morning adhkar"),
            ),
        )
    }

    // ── The same mechanism on the night types ───────────────────────────────

    @Test
    fun `tahajjud day-walk - card survives the start of the last third`() {
        val day = LocalDate.of(2026, 3, 10)
        val only = listOf(WorshipReminderType.TAHAJJUD)
        runWalk(
            day, only,
            listOf(
                Step(21, expected = WorshipReminderType.TAHAJJUD,
                    why = "evening: countdown to tonight's last third (02:40)"),
            ),
        )
        // Cross midnight: walk continues on the 11th.
        runWalk(
            day.plusDays(1), only,
            listOf(
                Step(1, expected = WorshipReminderType.TAHAJJUD,
                    why = "after midnight, before the last third — same night"),
                Step(3, expected = WorshipReminderType.TAHAJJUD, expectActive = true,
                    why = "02:40 passed: last third is HAPPENING — pre-fix this card vanished"),
                Step(4, 30, expected = WorshipReminderType.TAHAJJUD, expectActive = true,
                    why = "still the last third, just before Fajr window close"),
            ),
        )
    }

    @Test
    fun `iftar day-walk - card flips to active at maghrib instead of vanishing`() {
        val day = LocalDate.of(2026, 3, 10)
        val ramadan = { d: LocalDate -> HijriDayInfo(month = 9, day = d.dayOfMonth) }
        fun selectRamadan(now: LocalDateTime) = listOf(WorshipReminderType.IFTAR)
            .mapNotNull {
                calc.nextOccurrence(it, now, it.defaultOffsetMinutes, ::timesForDay, ramadan, maxSearchDays = 2)
            }
            .filter { it.isActiveAt(now) || Duration.between(now, it.eventAt) <= nearWindow }
            .minWithOrNull(compareBy({ if (it.isActiveAt(now)) 0 else 1 }, { it.triggerAt }))

        val atMaghrib = day.atTime(18, 45) // 15 min after Maghrib 18:30
        val occ = selectRamadan(atMaghrib)
        assertWithMessage(
            "Iftar must be surfaced as ACTIVE just after Maghrib — the moment the fast is broken " +
                "is exactly when the pre-fix card vanished"
        ).that(occ?.isActiveAt(atMaghrib) == true).isTrue()

        val afterIsha = day.atTime(20, 30)
        assertWithMessage("After Isha, iftar's window is closed; tomorrow's is ~22h out → nothing")
            .that(selectRamadan(afterIsha)).isNull()
    }
}
