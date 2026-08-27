package com.arshadshah.nimaz.domain.usecase.calendar

import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The calendar's other grid: a *Hijri* month laid out day by day.
 *
 * `CalendarUseCasesTest` covers the Gregorian grid and the event projection. This is the view
 * that runs the other way — the reader is looking at Ramadan, not at March — and it has two
 * things the Gregorian one does not.
 *
 * **A Hijri month is 29 or 30 days, and which is not fixed.** The Umm al-Qura calendar decides
 * per month per year, so the grid's length has to come from the calculator rather than from a
 * constant. Hard-coding 30 gives Ramadan a day that does not exist; hard-coding 29 loses one.
 *
 * **Today is passed in, not read from a clock.** The same reason the Gregorian builder takes it:
 * a grid built at 23:59 must be rebuildable for the new day instead of highlighting yesterday
 * until the reader navigates away and back.
 */
class BuildHijriMonthUseCaseTest {

    private val eventsForDate = GetEventsForDateUseCase()
    private val build = BuildHijriMonthUseCase(eventsForDate)

    private fun event(id: String, hijriMonth: Int, hijriDay: Int) = IslamicEvent(
        id = id,
        nameArabic = "",
        nameEnglish = id,
        description = null,
        hijriMonth = hijriMonth,
        hijriDay = hijriDay,
        eventType = IslamicEventType.HISTORICAL,
        isHoliday = false,
        isFastingDay = false,
        isNightOfPower = false,
        gregorianDate = null,
        year = null,
        notes = null,
        priority = 0,
    )

    private val year = 1447

    @Test
    fun `the grid is as long as the month actually is`() {
        val ramadan = build(month = 9, year = year, events = emptyList(), today = LocalDate.MIN)

        assertThat(ramadan).hasSize(HijriDateCalculator.getDaysInHijriMonth(year, 9))
        assertThat(ramadan.size).isIn(listOf(29, 30))
    }

    @Test
    fun `the days run from the first to the last, with none missing`() {
        val month = build(month = 9, year = year, events = emptyList(), today = LocalDate.MIN)

        assertThat(month.map { it.hijriDate.day }).isEqualTo((1..month.size).toList())
    }

    @Test
    fun `every day carries the month and year it was built for`() {
        val month = build(month = 3, year = year, events = emptyList(), today = LocalDate.MIN)

        month.forEach {
            assertThat(it.hijriDate.month).isEqualTo(3)
            assertThat(it.hijriDate.year).isEqualTo(year)
            assertThat(it.isCurrentMonth).isTrue()
        }
    }

    @Test
    fun `each day knows its Gregorian date, and they run forward`() {
        val month = build(month = 9, year = year, events = emptyList(), today = LocalDate.MIN)

        assertThat(month.map { it.gregorianDate }).isInStrictOrder()
    }

    @Test
    fun `every month of the year builds a grid`() {
        // 29 or 30 is decided per month per year by the Umm al-Qura tables; asking for each one
        // is what catches a month the calculator cannot answer for.
        (1..12).forEach { month ->
            val days = build(month = month, year = year, events = emptyList(), today = LocalDate.MIN)
            assertThat(days).isNotEmpty()
        }
    }

    // ---- Today ----

    @Test
    fun `today is whatever the caller says it is`() {
        val month = build(month = 9, year = year, events = emptyList(), today = LocalDate.MIN)
        val theTenth = month[9].gregorianDate

        val rebuilt = build(month = 9, year = year, events = emptyList(), today = theTenth)

        assertThat(rebuilt.filter { it.isToday }.map { it.hijriDate.day }).containsExactly(10)
    }

    @Test
    fun `a month that does not contain today highlights nothing`() {
        val month = build(month = 9, year = year, events = emptyList(), today = LocalDate.MIN)

        assertThat(month.none { it.isToday }).isTrue()
    }

    // ---- Events ----

    @Test
    fun `an event lands on its own day and no other`() {
        val laylatAlQadr = event("laylat-al-qadr", hijriMonth = 9, hijriDay = 27)

        val month = build(month = 9, year = year, events = listOf(laylatAlQadr), today = LocalDate.MIN)

        assertThat(month.single { it.events.isNotEmpty() }.hijriDate.day).isEqualTo(27)
    }

    @Test
    fun `an event in another month does not appear in this one`() {
        val ashura = event("ashura", hijriMonth = 1, hijriDay = 10)

        val ramadan = build(month = 9, year = year, events = listOf(ashura), today = LocalDate.MIN)

        assertThat(ramadan.flatMap { it.events }).isEmpty()
    }

    @Test
    fun `two events on one day both land there`() {
        val days = build(
            month = 1,
            year = year,
            events = listOf(event("a", 1, 10), event("b", 1, 10)),
            today = LocalDate.MIN,
        )

        assertThat(days.first { it.hijriDate.day == 10 }.events.map { it.id })
            .containsExactly("a", "b")
    }

    @Test
    fun `a month with no events still builds every day`() {
        val month = build(month = 2, year = year, events = emptyList(), today = LocalDate.MIN)

        assertThat(month).isNotEmpty()
        assertThat(month.flatMap { it.events }).isEmpty()
    }
}
