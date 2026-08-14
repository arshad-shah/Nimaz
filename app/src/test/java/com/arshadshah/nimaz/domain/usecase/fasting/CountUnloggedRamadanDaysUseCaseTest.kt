package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class CountUnloggedRamadanDaysUseCaseTest {

    private val oneDayMs = 24L * 60 * 60 * 1000

    private fun useCase(today: LocalDate) =
        CountUnloggedRamadanDaysUseCase(FakeTodayProvider(today))

    private fun recordForDate(date: LocalDate): FastRecord {
        val epochDay = date.toEpochDay()
        return FastRecord(
            id = 0, date = epochDay * oneDayMs, hijriDate = "1/9/1446",
            hijriMonth = 9, hijriYear = 1446,
            fastType = FastType.RAMADAN, status = FastStatus.FASTED,
            exemptionReason = null, suhoorTime = null, iftarTime = null,
            note = null, createdAt = epochDay * oneDayMs, updatedAt = epochDay * oneDayMs
        )
    }

    @Test
    fun `currentDay of 1 means 0 elapsed days so count is 0`() {
        val today = LocalDate.of(2025, 3, 1)
        val uc = useCase(today)

        val result = uc.invoke(currentDay = 1, records = emptyList())

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `all days logged returns 0 unlogged`() {
        val today = LocalDate.of(2025, 3, 3) // 3rd day of Ramadan
        val uc = useCase(today)

        // Days 1 and 2 are before today
        val yesterday = today.minusDays(1)
        val dayBefore = today.minusDays(2)
        val records = listOf(recordForDate(yesterday), recordForDate(dayBefore))

        val result = uc.invoke(currentDay = 3, records = records)

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `no days logged returns count equal to elapsed days`() {
        val today = LocalDate.of(2025, 3, 5) // day 5
        val uc = useCase(today)

        // currentDay = 5 means 4 days have elapsed
        val result = uc.invoke(currentDay = 5, records = emptyList())

        assertThat(result).isEqualTo(4)
    }

    @Test
    fun `partial logging returns correct unlogged count`() {
        val today = LocalDate.of(2025, 3, 5) // day 5
        val uc = useCase(today)

        // 4 days elapsed; log only 2
        val records = listOf(
            recordForDate(today.minusDays(1)),
            recordForDate(today.minusDays(3))
        )

        val result = uc.invoke(currentDay = 5, records = records)

        // 4 elapsed - 2 logged = 2 unlogged (coerced >= 0)
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `result is never negative`() {
        val today = LocalDate.of(2025, 3, 2) // day 2
        val uc = useCase(today)

        // Overlogged: more records than elapsed days (e.g., duplicate records)
        val records = listOf(
            recordForDate(today.minusDays(1)),
            recordForDate(today.minusDays(1)) // duplicate
        )

        val result = uc.invoke(currentDay = 2, records = records)

        assertThat(result).isAtLeast(0)
    }
}
