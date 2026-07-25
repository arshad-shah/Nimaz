package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.WorshipReminderOccurrence
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Per-day inputs the calculator needs. Wall-clock [LocalDateTime]s in the user's zone.
 *
 * [lastThirdOfNight] is the start of the last third of *this day's* night (maghrib → next fajr),
 * so it lands in the early hours of the following morning — matching adhan2 `SunnahTimes`.
 */
data class DayWorshipTimes(
    val fajr: LocalDateTime,
    val sunrise: LocalDateTime,
    val dhuhr: LocalDateTime,
    val asr: LocalDateTime,
    val maghrib: LocalDateTime,
    val isha: LocalDateTime,
    val lastThirdOfNight: LocalDateTime
)

/** Minimal Hijri view for gating: month (1..12) + day (1..30). */
data class HijriDayInfo(val month: Int, val day: Int) {
    val isRamadan: Boolean get() = month == 9
}

/**
 * Pure, stateless computation of the **next upcoming** [WorshipReminderOccurrence] for a given
 * [WorshipReminderType], searching forward from `now`. Android-free: all date-dependent data is
 * supplied via lambdas so this is exhaustively unit-testable on the JVM.
 *
 * "Next upcoming" (rather than "today's") is deliberate: it self-corrects the midnight-reschedule
 * edge case where Tahajjud's last-third for *tonight* was computed from *yesterday's* maghrib. By
 * scanning `[now-1day .. now+window]` and taking the earliest future trigger, both the evening
 * schedule and the 00:01 reschedule arm the same correct instant.
 */
class WorshipReminderCalculator {

    /** How far ahead to search for eve-of reminders (white days, Arafah/Ashura, Mon/Thu). */
    private val forwardSearchDays = 40L

    fun nextOccurrence(
        type: WorshipReminderType,
        now: LocalDateTime,
        offsetMinutes: Int,
        timesFor: (LocalDate) -> DayWorshipTimes?,
        hijriFor: (LocalDate) -> HijriDayInfo?,
        maxSearchDays: Long = forwardSearchDays,
        /** Witr only: fire before Fajr (Fajr − offset) instead of after Isha (Isha + offset). */
        witrBeforeFajr: Boolean = false
    ): WorshipReminderOccurrence? {
        val startDay = now.toLocalDate().minusDays(1)
        var day = startDay
        val end = startDay.plusDays(maxSearchDays + 1)
        var best: WorshipReminderOccurrence? = null

        while (day.isBefore(end)) {
            val occ = triggerForDay(type, day, offsetMinutes, timesFor, hijriFor, witrBeforeFajr)
            if (occ != null && occ.triggerAt.isAfter(now)) {
                if (best == null || occ.triggerAt.isBefore(best.triggerAt)) best = occ
                // Daily reminders resolve on the first future day found; keep scanning only for
                // the sparse eve-of ones where an earlier day might still yield a later-in-window
                // hit already covered above. Break once we have the earliest daily hit.
                if (!isEveOf(type)) break
            }
            day = day.plusDays(1)
        }
        return best
    }

    private fun isEveOf(type: WorshipReminderType): Boolean = when (type) {
        WorshipReminderType.MONDAY_THURSDAY_FAST,
        WorshipReminderType.WHITE_DAYS_FAST,
        WorshipReminderType.ARAFAH_ASHURA_FAST -> true
        else -> false
    }

    /**
     * The trigger for [type] anchored on calendar [day], or null if the type does not fire that
     * day (wrong Hijri date, not Ramadan, not an eligible day of week, …).
     */
    private fun triggerForDay(
        type: WorshipReminderType,
        day: LocalDate,
        offsetMinutes: Int,
        timesFor: (LocalDate) -> DayWorshipTimes?,
        hijriFor: (LocalDate) -> HijriDayInfo?,
        witrBeforeFajr: Boolean = false
    ): WorshipReminderOccurrence? {
        val t = timesFor(day) ?: return null
        val hijri = hijriFor(day)

        fun occ(trigger: LocalDateTime, event: LocalDateTime, sub: String? = null) =
            WorshipReminderOccurrence(type, trigger, event, sub)

        return when (type) {
            WorshipReminderType.TAHAJJUD ->
                occ(t.lastThirdOfNight, t.lastThirdOfNight)

            // Two modes (#309): after Isha (default) → Isha + offset; or before Fajr → Fajr − offset.
            // eventAt is the next Fajr either way so the card can show "Fajr in …".
            WorshipReminderType.WITR ->
                if (witrBeforeFajr) occ(t.fajr.minusMinutes(offsetMinutes.toLong()), t.fajr)
                else occ(t.isha.plusMinutes(offsetMinutes.toLong()), t.fajr)

            WorshipReminderType.SUHOOR -> {
                if (hijri?.isRamadan != true) return null
                occ(t.fajr.minusMinutes(offsetMinutes.toLong()), t.fajr)
            }

            WorshipReminderType.IFTAR -> {
                if (hijri?.isRamadan != true) return null
                occ(t.maghrib.minusMinutes(offsetMinutes.toLong()), t.maghrib)
            }

            WorshipReminderType.TARAWEEH -> {
                if (hijri?.isRamadan != true) return null
                occ(t.isha.plusMinutes(offsetMinutes.toLong()), t.isha)
            }

            WorshipReminderType.LAYLATUL_QADR -> {
                if (hijri?.isRamadan != true) return null
                if (hijri.day !in ODD_LAST_TEN) return null
                occ(t.isha.plusMinutes(30), t.isha)
            }

            WorshipReminderType.ADHKAR_MORNING ->
                occ(t.fajr.plusMinutes(offsetMinutes.toLong()), t.fajr)

            WorshipReminderType.ADHKAR_EVENING ->
                occ(t.asr.plusMinutes(offsetMinutes.toLong()), t.asr)

            // Eve-of reminders fire the evening (after Isha) *before* the target day.
            WorshipReminderType.MONDAY_THURSDAY_FAST -> {
                val target = day.plusDays(1).dayOfWeek
                val sub = when (target) {
                    DayOfWeek.MONDAY -> "monday"
                    DayOfWeek.THURSDAY -> "thursday"
                    else -> return null
                }
                occ(t.isha.plusMinutes(15), t.isha, sub)
            }

            WorshipReminderType.WHITE_DAYS_FAST -> {
                val next = hijriFor(day.plusDays(1)) ?: return null
                if (next.day != 13) return null
                occ(t.isha.plusMinutes(15), t.isha)
            }

            WorshipReminderType.ARAFAH_ASHURA_FAST -> {
                val next = hijriFor(day.plusDays(1)) ?: return null
                val sub = when {
                    next.month == 12 && next.day == 9 -> "arafah"
                    next.month == 1 && next.day == 10 -> "ashura"
                    else -> return null
                }
                occ(t.isha.plusMinutes(15), t.isha, sub)
            }
        }
    }

    companion object {
        /** Odd nights of the last ten of Ramadan on which Laylatul Qadr is sought. */
        val ODD_LAST_TEN = setOf(21, 23, 25, 27, 29)

        /** Witr timing modes (pref value for `worshipReminderMode("witr", …)`). */
        const val WITR_MODE_AFTER_ISHA = "after_isha"
        const val WITR_MODE_BEFORE_FAJR = "before_fajr"
    }
}
