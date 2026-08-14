package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class GetRamadanCountdownUseCaseTest {

    private fun useCase(today: LocalDate) = GetRamadanCountdownUseCase(
        FakeTodayProvider(today)
    )

    @Test
    fun `countdown has non-negative daysAway`() {
        // Any date should produce a non-negative count
        val result = useCase(LocalDate.of(2025, 1, 1)).invoke()

        assertThat(result.daysAway).isAtLeast(0)
    }

    @Test
    fun `startsOn is a valid date in the future or present`() {
        val today = LocalDate.of(2025, 1, 1)
        val result = useCase(today).invoke()

        assertThat(result.startsOn).isAtLeast(today)
    }

    @Test
    fun `when today is well before Ramadan daysAway is positive`() {
        // Use a Gregorian date known to be before Ramadan (e.g. early January 2025)
        val today = LocalDate.of(2025, 1, 1)
        val result = useCase(today).invoke()

        assertThat(result.daysAway).isGreaterThan(0)
    }

    @Test
    fun `applying positive offsetDays brings today forward`() {
        val today = LocalDate.of(2025, 1, 1)
        val uc = useCase(today)

        val withoutOffset = uc.invoke(0)
        val withOffset = uc.invoke(1)

        // Moving today forward by 1 day reduces the countdown by 0 or 1
        assertThat(withOffset.daysAway).isAtMost(withoutOffset.daysAway)
    }

    @Test
    fun `applying negative offsetDays brings today backward`() {
        val today = LocalDate.of(2025, 3, 1)
        val uc = useCase(today)

        val withoutOffset = uc.invoke(0)
        val withOffset = uc.invoke(-1)

        // Moving today back by 1 day increases or maintains the countdown
        assertThat(withOffset.daysAway).isAtLeast(withoutOffset.daysAway)
    }

    @Test
    fun `FakeTodayProvider can be advanced`() {
        val provider = FakeTodayProvider(LocalDate.of(2025, 1, 1))
        val uc = GetRamadanCountdownUseCase(provider)

        val initialCountdown = uc.invoke().daysAway

        provider.now = LocalDate.of(2025, 2, 1)
        val laterCountdown = uc.invoke().daysAway

        // After advancing a month, there should be fewer days until Ramadan
        assertThat(laterCountdown).isAtMost(initialCountdown)
    }
}
