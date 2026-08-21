package com.arshadshah.nimaz.domain.calendar

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HijriDateCalculatorOffsetTest {

    @Test
    fun `today with zero offset equals today of current date`() {
        val zero = HijriDateCalculator.today(0)
        val fromDate = HijriDateCalculator.toHijri(LocalDate.now())
        assertThat(zero).isEqualTo(fromDate)
    }

    @Test
    fun `positive offset advances the hijri day relative to zero`() {
        val zero = HijriDateCalculator.today(0)
        val plusOne = HijriDateCalculator.today(1)
        // +1 day is either the next day in the same month, or day 1 of the next month
        assertThat(plusOne).isNotEqualTo(zero)
        assertThat(plusOne).isEqualTo(HijriDateCalculator.toHijri(LocalDate.now().plusDays(1)))
    }

    @Test
    fun `negative offset matches yesterday`() {
        assertThat(HijriDateCalculator.today(-1))
            .isEqualTo(HijriDateCalculator.toHijri(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `default arg is zero offset`() {
        assertThat(HijriDateCalculator.today()).isEqualTo(HijriDateCalculator.today(0))
    }
}
