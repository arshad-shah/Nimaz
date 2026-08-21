package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * [CalendarMonth.displayedMonth] is the single source of truth for "which month
 * is on screen". Both the calendar header title and the day grid derive from it.
 *
 * They previously came from different places — the title from `currentMonth`,
 * the grid from `selectedDate` — so paging the calendar moved the title while
 * the grid stayed on today. These tests pin the property the fix relies on.
 */
class CalendarMonthDisplayedMonthTest {

    private fun monthOf(first: LocalDate, length: Int) = CalendarMonth(
        hijriMonth = 1,
        hijriYear = 1447,
        days = (0 until length).map { offset ->
            val date = first.plusDays(offset.toLong())
            CalendarDay(
                gregorianDate = date,
                hijriDate = HijriDate(1, 1, 1447),
                events = emptyList(),
                isToday = false,
                isCurrentMonth = true
            )
        },
        events = emptyList()
    )

    @Test
    fun `reports the month its days belong to`() {
        val month = monthOf(LocalDate.of(2026, 3, 1), 31)
        assertThat(month.displayedMonth).isEqualTo(YearMonth.of(2026, 3))
    }

    @Test
    fun `is independent of today`() {
        // The original bug was the grid tracking the current date rather than
        // the loaded month, so a month far from today must still report itself.
        val month = monthOf(LocalDate.of(2019, 11, 1), 30)
        assertThat(month.displayedMonth).isEqualTo(YearMonth.of(2019, 11))
        assertThat(month.displayedMonth).isNotEqualTo(YearMonth.from(LocalDate.now()))
    }

    @Test
    fun `handles a december page rolling into the next year`() {
        val month = monthOf(LocalDate.of(2026, 12, 1), 31)
        assertThat(month.displayedMonth).isEqualTo(YearMonth.of(2026, 12))
        assertThat(month.displayedMonth?.plusMonths(1)).isEqualTo(YearMonth.of(2027, 1))
    }

    @Test
    fun `returns null when the month has no days`() {
        val empty = CalendarMonth(
            hijriMonth = 1,
            hijriYear = 1447,
            days = emptyList(),
            events = emptyList()
        )
        // The screen falls back to the selected date in this case rather than
        // rendering an arbitrary month.
        assertThat(empty.displayedMonth).isNull()
    }
}
