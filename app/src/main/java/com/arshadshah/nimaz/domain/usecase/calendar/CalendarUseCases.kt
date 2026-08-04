package com.arshadshah.nimaz.domain.usecase.calendar

import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.CalendarMonth
import com.arshadshah.nimaz.domain.model.HijriDate
import com.arshadshah.nimaz.domain.model.IslamicEvent
import java.time.LocalDate
import javax.inject.Inject

/**
 * The calendar's arithmetic, extracted from `CalendarViewModel`.
 *
 * These were 75 lines of pure functions over domain models, `private` inside the ViewModel and
 * reachable only through it with mocked use cases. That is why two of the sharpest bugs in the
 * app shipped and stayed: every Islamic event was projected into the **current** Hijri year
 * only, so during the last weeks of a Hijri year the "upcoming events" list silently dropped
 * Islamic New Year and Ashura — precisely the events that were upcoming — and the month grid
 * dropped every Muharram event from the Gregorian month that straddles the year boundary.
 *
 * Nothing here reads a repository: the events are passed in, and so is today. That is the
 * point — each function is a value in, a value out, testable without a ViewModel or a clock.
 */
data class CalendarUseCases(
    val buildGregorianMonth: BuildCalendarMonthUseCase,
    val buildHijriMonth: BuildHijriMonthUseCase,
    val eventsForMonth: GetEventsForMonthUseCase,
    val upcomingEvents: GetUpcomingEventsUseCase,
    val eventsForDate: GetEventsForDateUseCase,
)

/**
 * Projects a recurring Islamic event onto the Gregorian calendar.
 *
 * An [IslamicEvent] carries only a Hijri month and day — it recurs every Hijri year — so
 * turning it into a date always means choosing a year. Choosing *this* Hijri year and
 * stopping is the bug both D4 and D5 describe, and the reason this lives in one object is
 * that two implementations of one projection drift apart silently.
 */
internal object IslamicEventProjection {

    /** The event's date in [hijriYear], or null if the event is malformed. */
    fun inYear(event: IslamicEvent, hijriYear: Int): LocalDate? {
        if (event.hijriMonth !in 1..12 || event.hijriDay < 1) return null
        return runCatching {
            HijriDateCalculator.toGregorian(event.hijriDay, event.hijriMonth, hijriYear)
        }.getOrNull()
    }

    /**
     * The event's next occurrence on or after [onOrAfter].
     *
     * Tries the Hijri year containing [onOrAfter] and then the one after it, and takes the
     * first projection that has not already passed. Without the second attempt, an event in
     * Muharram looks ~355 days *behind* a today in Dhul-Hijjah and is filtered out as past.
     */
    fun nextOccurrence(event: IslamicEvent, onOrAfter: LocalDate): LocalDate? {
        val hijriYear = HijriDateCalculator.toHijri(onOrAfter).year
        return (hijriYear..hijriYear + 1)
            .mapNotNull { year -> inYear(event, year) }
            .firstOrNull { !it.isBefore(onOrAfter) }
    }

    /**
     * The event's occurrence inside the Gregorian window `[from, to]`, or null.
     *
     * A Gregorian month can span two Hijri years, so both are tried — taking the year from
     * the first day alone is what dropped every Muharram event from the month that contains
     * 1 Muharram.
     */
    fun within(event: IslamicEvent, from: LocalDate, to: LocalDate): LocalDate? {
        val firstHijriYear = HijriDateCalculator.toHijri(from).year
        val lastHijriYear = HijriDateCalculator.toHijri(to).year
        return (firstHijriYear..lastHijriYear)
            .mapNotNull { year -> inYear(event, year) }
            .firstOrNull { !it.isBefore(from) && !it.isAfter(to) }
    }
}

/** The events that fall on a given Hijri day, in any year — the grid's per-day markers. */
class GetEventsForDateUseCase @Inject constructor() {
    operator fun invoke(events: List<IslamicEvent>, hijriDate: HijriDate): List<IslamicEvent> =
        events.filter { it.hijriMonth == hijriDate.month && it.hijriDay == hijriDate.day }
}

/** Builds one Gregorian month's grid, with each day's Hijri date and events. */
class BuildCalendarMonthUseCase @Inject constructor(
    private val eventsForDate: GetEventsForDateUseCase,
) {
    operator fun invoke(
        month: Int,
        year: Int,
        events: List<IslamicEvent>,
        today: LocalDate,
    ): CalendarMonth {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)

        val days = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastDay) }
            .map { date ->
                val hijriDate = HijriDateCalculator.toHijri(date).let {
                    HijriDate(day = it.day, month = it.month, year = it.year)
                }
                CalendarDay(
                    gregorianDate = date,
                    hijriDate = hijriDate,
                    // Passed in rather than read from the system clock, so a grid built at
                    // 23:59 can be rebuilt for the new day instead of highlighting yesterday
                    // until the user navigates away and back.
                    isToday = date == today,
                    isCurrentMonth = true,
                    events = eventsForDate(events, hijriDate),
                )
            }
            .toList()

        val firstHijri = days.firstOrNull()?.hijriDate
        return CalendarMonth(
            hijriMonth = firstHijri?.month ?: 1,
            hijriYear = firstHijri?.year ?: HijriDateCalculator.toHijri(firstDay).year,
            days = days,
            events = days.flatMap { it.events }.distinctBy { it.id },
        )
    }
}

/** Builds one Hijri month's days. */
class BuildHijriMonthUseCase @Inject constructor(
    private val eventsForDate: GetEventsForDateUseCase,
) {
    operator fun invoke(
        month: Int,
        year: Int,
        events: List<IslamicEvent>,
        today: LocalDate,
    ): List<CalendarDay> =
        (1..HijriDateCalculator.getDaysInHijriMonth(year, month)).map { day ->
            val hijriDate = HijriDate(day = day, month = month, year = year)
            val gregorianDate = HijriDateCalculator.toGregorian(day, month, year)
            CalendarDay(
                gregorianDate = gregorianDate,
                hijriDate = hijriDate,
                isToday = gregorianDate == today,
                isCurrentMonth = true,
                events = eventsForDate(events, hijriDate),
            )
        }
}

/** The events landing inside a Gregorian month, projected into whichever Hijri year fits. */
class GetEventsForMonthUseCase @Inject constructor() {
    operator fun invoke(month: Int, year: Int, events: List<IslamicEvent>): List<IslamicEvent> {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.plusMonths(1).minusDays(1)
        return events.mapNotNull { event ->
            IslamicEventProjection.within(event, firstDay, lastDay)
                ?.let { event.copy(gregorianDate = it) }
        }.sortedBy { it.gregorianDate }
    }
}

/** The events occurring in the [window] starting today, soonest first. */
class GetUpcomingEventsUseCase @Inject constructor() {
    operator fun invoke(
        events: List<IslamicEvent>,
        today: LocalDate,
        window: java.time.Period = DEFAULT_WINDOW,
    ): List<IslamicEvent> {
        val until = today.plus(window)
        return events.mapNotNull { event ->
            IslamicEventProjection.nextOccurrence(event, today)
                ?.takeIf { !it.isAfter(until) }
                ?.let { event.copy(gregorianDate = it) }
        }.sortedBy { it.gregorianDate }
    }

    private companion object {
        val DEFAULT_WINDOW: java.time.Period = java.time.Period.ofMonths(3)
    }
}
