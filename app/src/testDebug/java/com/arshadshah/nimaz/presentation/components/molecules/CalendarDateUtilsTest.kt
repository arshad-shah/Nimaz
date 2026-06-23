package com.arshadshah.nimaz.presentation.components.molecules

import com.arshadshah.nimaz.presentation.components.molecules.calendar.buildCalendarDays
import com.arshadshah.nimaz.presentation.components.molecules.calendar.formatDefault
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class CalendarDateUtilsTest {

    @Test
    fun `month starting on sunday produces five weeks`() {
        // June 2025 starts on Sunday (no leading padding, 30 days -> 5 weeks).
        val month = YearMonth.of(2025, 6)
        assertThat(month.atDay(1).dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        val days = buildCalendarDays(month)
        assertThat(days).hasSize(35)
        assertThat(days.first()).isEqualTo(LocalDate.of(2025, 6, 1))
    }

    @Test
    fun `grid always has a multiple of seven cells`() {
        for (m in 1..12) {
            val days = buildCalendarDays(YearMonth.of(2026, m))
            assertThat(days.size % 7).isEqualTo(0)
        }
    }

    @Test
    fun `grid size is clamped between thirty five and forty two`() {
        for (m in 1..12) {
            val size = buildCalendarDays(YearMonth.of(2026, m)).size
            assertThat(size).isAtLeast(35)
            assertThat(size).isAtMost(42)
        }
    }

    @Test
    fun `thirty one day month starting late wraps to six weeks`() {
        // August 2025 has 31 days and starts on Friday -> needs 6 weeks.
        val month = YearMonth.of(2025, 8)
        assertThat(month.atDay(1).dayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
        val days = buildCalendarDays(month)
        assertThat(days).hasSize(42)
        // Last day of the month must be present (not silently truncated).
        assertThat(days).contains(LocalDate.of(2025, 8, 31))
    }

    @Test
    fun `leading padding pulls in days from the previous month`() {
        // July 2026 starts on Wednesday -> three padding days before the 1st.
        val month = YearMonth.of(2026, 7)
        val days = buildCalendarDays(month)
        assertThat(days.first()).isEqualTo(LocalDate.of(2026, 6, 28))
        assertThat(days).contains(LocalDate.of(2026, 7, 1))
    }

    @Test
    fun `format default produces month year title`() {
        assertThat(YearMonth.of(2026, 1).formatDefault()).isEqualTo("January 2026")
        assertThat(YearMonth.of(2026, 12).formatDefault()).isEqualTo("December 2026")
    }
}
