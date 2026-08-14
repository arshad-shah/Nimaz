package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class MoreUseCasesTest {

    // ── GetHijriTodayUseCase ──────────────────────────────────────────────────

    @Test
    fun `GetHijriTodayUseCase converts a known Gregorian date with zero offset`() {
        val useCase = GetHijriTodayUseCase()
        // 2026-08-14 (Gregorian) should map to a valid Hijri date
        val result = useCase(LocalDate.of(2026, 8, 14), offsetDays = 0)
        // The year should be in the 14xx range
        assertThat(result.year).isGreaterThan(1400)
        assertThat(result.year).isLessThan(1500)
    }

    @Test
    fun `GetHijriTodayUseCase applies positive offset by adding days`() {
        val useCase = GetHijriTodayUseCase()
        val base = LocalDate.of(2026, 8, 14)
        val withoutOffset = useCase(base, 0)
        val withOffset = useCase(base, 1)
        // Adding one day to the Gregorian date advances the Hijri date
        val withoutOffsetFromPlusOne = HijriDateCalculator.toHijri(base.plusDays(1))
        assertThat(withOffset).isEqualTo(withoutOffsetFromPlusOne)
    }

    @Test
    fun `GetHijriTodayUseCase applies negative offset by subtracting days`() {
        val useCase = GetHijriTodayUseCase()
        val base = LocalDate.of(2026, 8, 14)
        val withOffset = useCase(base, -1)
        val expected = HijriDateCalculator.toHijri(base.minusDays(1))
        assertThat(withOffset).isEqualTo(expected)
    }

    // ── ObserveKhatamRowProgressUseCase ───────────────────────────────────────

    @Test
    fun `ObserveKhatamRowProgressUseCase emits null when no active khatam`() = runTest {
        val repository = mockk<KhatamRepository>()
        every { repository.observeActiveKhatam() } returns flowOf(null)

        val useCase = ObserveKhatamRowProgressUseCase(repository)
        val result = useCase().first()
        assertThat(result).isNull()
    }

    @Test
    fun `ObserveKhatamRowProgressUseCase reports first incomplete juz`() = runTest {
        val khatam = Khatam(
            id = 1L,
            name = "Test",
            status = KhatamStatus.ACTIVE,
            isActive = true,
            dailyTarget = 20,
            totalAyahsRead = 200,
            startedAt = System.currentTimeMillis() - 10 * 86_400_000L, // 10 days ago
        )
        val juzProgress = (1..30).map { juz ->
            JuzProgressInfo(
                juzNumber = juz,
                totalAyahs = 207,
                readAyahs = if (juz <= 1) 207 else 0, // juz 1 complete, rest not
            )
        }

        val repository = mockk<KhatamRepository>()
        every { repository.observeActiveKhatam() } returns flowOf(khatam)
        every { repository.observeJuzProgress(1L) } returns flowOf(juzProgress)

        val useCase = ObserveKhatamRowProgressUseCase(repository)
        val result = useCase().first()

        assertThat(result).isNotNull()
        // First incomplete juz is 2
        assertThat(result!!.juz).isEqualTo(2)
    }
}
