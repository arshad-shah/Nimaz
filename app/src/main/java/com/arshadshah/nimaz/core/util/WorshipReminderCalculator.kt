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
 *
 * ## Occurrences are windows, not instants
 *
 * Each occurrence carries a `[windowStart, windowEnd)` span: between `eventAt` and `windowEnd` the
 * reminder is *active* (happening now), not spent. This is what stops a card vanishing — and the
 * resolver surfacing the opposite card — the moment an event begins (see [WorshipReminderOccurrence]).
 * Night-worship windows close at the **next** day's Fajr (`timesFor(day.plusDays(1))?.fajr`).
 *
 * **Religious-content decision — adhkar window close (accommodating view).** Morning adhkar closes
 * at **Dhuhr** and evening adhkar at **Isha** (rather than the strict sunrise/Maghrib bounds). This
 * keeps the "now" card useful for the wider window most users expect. The choice is deliberate and
 * flagged for review; a per-type `worshipReminderMode` preference (as Witr already has) is the
 * natural place to expose the strict alternative later.
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
        witrBeforeFajr: Boolean = false,
        /**
         * Scheduler-only gate. When true, only occurrences whose [WorshipReminderOccurrence.triggerAt]
         * is strictly in the future are accepted — a currently-*active* occurrence (event begun,
         * window still open, trigger already passed) is skipped and the scan rolls forward to the
         * next future trigger.
         *
         * This is what the alarm scheduler must use. Arming `setExactAndAllowWhileIdle` at a past
         * instant makes Android fire it *immediately*, so accepting an active-but-past occurrence
         * (the default, which the Home card wants) re-posts the notification on every reschedule —
         * i.e. every time the app is opened during the event's window. The Home card resolver keeps
         * the default so a "happening now" card stays visible.
         */
        requireFutureTrigger: Boolean = false
    ): WorshipReminderOccurrence? {
        val startDay = now.toLocalDate().minusDays(1)
        var day = startDay
        val end = startDay.plusDays(maxSearchDays + 1)
        var best: WorshipReminderOccurrence? = null

        while (day.isBefore(end)) {
            val occ = triggerForDay(type, day, offsetMinutes, timesFor, hijriFor, witrBeforeFajr)
            // Accept an occurrence that is still *live* — upcoming (future trigger) OR
            // currently active (its event has begun but its window is still open). The
            // old gate (`triggerAt.isAfter(now)`) treated an occurrence as spent the
            // instant its trigger passed, which dropped today's occurrence the moment
            // its event began and let the resolver's near-window surface the opposite
            // card for the rest of the day. The backward scan already starts at
            // `now.minusDays(1)`, so today's (or last night's) live occurrence is
            // visited — only the acceptance test rejected it.
            //
            // The scheduler ([requireFutureTrigger]) instead demands a strictly-future
            // trigger so it never arms a past alarm (which fires immediately, re-posting
            // the notification on every app-open during the active window).
            if (occ != null) {
                val accepted =
                    if (requireFutureTrigger) occ.triggerAt.isAfter(now) else occ.isLiveAt(now)
                if (accepted) {
                    if (best == null || occ.triggerAt.isBefore(best.triggerAt)) best = occ
                    // Daily reminders resolve on the first future day found; keep scanning only for
                    // the sparse eve-of ones where an earlier day might still yield a later-in-window
                    // hit already covered above. Break once we have the earliest daily hit.
                    if (!isEveOf(type)) break
                }
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

        fun occ(
            trigger: LocalDateTime,
            event: LocalDateTime,
            sub: String? = null,
            windowStart: LocalDateTime? = null,
            windowEnd: LocalDateTime? = null
        ) = WorshipReminderOccurrence(type, trigger, event, sub, windowStart, windowEnd)

        // Night-worship windows run from Isha to the *next* day's Fajr. `t` is one
        // Gregorian day's times, so `t.fajr` is the morning *before* this day's Isha —
        // a night window must therefore close at the following day's Fajr, not `t.fajr`
        // and never `t.lastThirdOfNight` (which equals Tahajjud's `eventAt` and would
        // zero-length the very window that matters most). Null (times unavailable)
        // leaves `windowEnd` null → instantaneous, never inventing a span.
        fun nextFajr(): LocalDateTime? = timesFor(day.plusDays(1))?.fajr

        return when (type) {
            WorshipReminderType.TAHAJJUD ->
                occ(
                    t.lastThirdOfNight, t.lastThirdOfNight,
                    windowStart = t.isha, windowEnd = nextFajr()
                )

            // Two modes (#309): after Isha (default) → Isha + offset; or before Fajr → Fajr − offset.
            // eventAt is the next Fajr either way so the card can show "Fajr in …".
            //
            // DELIBERATE DEVIATION from patch 02's window table: Witr is left *instantaneous*
            // (no windowEnd). Its `eventAt` is the same calendar day's Fajr (the morning *before*
            // that day's Isha) — the pre-existing eventAt/triggerAt inconsistency the brief scopes
            // OUT of this fix. A night window (isha → nextFajr) laid over that decoupled eventAt
            // would mark an already-spent morning occurrence "active" all evening, surfacing the
            // wrong Witr. Windowing Witr correctly requires first fixing its eventAt — tracked as
            // follow-up. Until then it keeps its pre-window behaviour (a pre-fajr lead-up reminder).
            WorshipReminderType.WITR ->
                if (witrBeforeFajr) occ(t.fajr.minusMinutes(offsetMinutes.toLong()), t.fajr)
                else occ(t.isha.plusMinutes(offsetMinutes.toLong()), t.fajr)

            WorshipReminderType.SUHOOR -> {
                if (hijri?.isRamadan != true) return null
                // `t` here is already the event-morning's row, so Fajr is the hard stop.
                occ(
                    t.fajr.minusMinutes(offsetMinutes.toLong()), t.fajr,
                    windowStart = t.lastThirdOfNight, windowEnd = t.fajr
                )
            }

            WorshipReminderType.IFTAR -> {
                if (hijri?.isRamadan != true) return null
                occ(
                    t.maghrib.minusMinutes(offsetMinutes.toLong()), t.maghrib,
                    windowStart = t.asr, windowEnd = t.isha
                )
            }

            WorshipReminderType.TARAWEEH -> {
                if (hijri?.isRamadan != true) return null
                occ(
                    t.isha.plusMinutes(offsetMinutes.toLong()), t.isha,
                    windowStart = t.isha, windowEnd = nextFajr()
                )
            }

            WorshipReminderType.LAYLATUL_QADR -> {
                if (hijri?.isRamadan != true) return null
                if (hijri.day !in ODD_LAST_TEN) return null
                occ(
                    t.isha.plusMinutes(30), t.isha,
                    windowStart = t.isha, windowEnd = nextFajr()
                )
            }

            // Adhkar close-time is a religious-content decision (see class KDoc): the
            // accommodating view is used — morning closes at Dhuhr, evening at Isha.
            WorshipReminderType.ADHKAR_MORNING ->
                occ(
                    t.fajr.plusMinutes(offsetMinutes.toLong()), t.fajr,
                    windowStart = t.fajr, windowEnd = t.dhuhr
                )

            WorshipReminderType.ADHKAR_EVENING ->
                occ(
                    t.asr.plusMinutes(offsetMinutes.toLong()), t.asr,
                    windowStart = t.asr, windowEnd = t.isha
                )

            // Eve-of reminders fire the evening (after Isha) *before* the target day.
            WorshipReminderType.MONDAY_THURSDAY_FAST -> {
                val target = day.plusDays(1).dayOfWeek
                val sub = when (target) {
                    DayOfWeek.MONDAY -> "monday"
                    DayOfWeek.THURSDAY -> "thursday"
                    else -> return null
                }
                occ(t.isha.plusMinutes(15), t.isha, sub, windowStart = t.isha, windowEnd = nextFajr())
            }

            WorshipReminderType.WHITE_DAYS_FAST -> {
                val next = hijriFor(day.plusDays(1)) ?: return null
                if (next.day != 13) return null
                occ(t.isha.plusMinutes(15), t.isha, windowStart = t.isha, windowEnd = nextFajr())
            }

            WorshipReminderType.ARAFAH_ASHURA_FAST -> {
                val next = hijriFor(day.plusDays(1)) ?: return null
                val sub = when {
                    next.month == 12 && next.day == 9 -> "arafah"
                    next.month == 1 && next.day == 10 -> "ashura"
                    else -> return null
                }
                occ(t.isha.plusMinutes(15), t.isha, sub, windowStart = t.isha, windowEnd = nextFajr())
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
