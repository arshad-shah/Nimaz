package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Khatam use cases. Each use case is a thin delegation onto
 * [KhatamRepository]; these tests guard the wiring (correct repository method,
 * correct arguments, return value passed straight through) since a mis-wired
 * use case would silently call the wrong operation.
 */
class KhatamUseCasesTest {

    private lateinit var repository: KhatamRepository

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `CreateKhatam passes the khatam through and returns the new id`() = runTest {
        val khatam = Khatam(name = "Ramadan")
        coEvery { repository.createKhatam(khatam) } returns 7L

        val id = CreateKhatamUseCase(repository)(khatam)

        assertThat(id).isEqualTo(7L)
        coVerify { repository.createKhatam(khatam) }
    }

    @Test
    fun `GetActiveKhatam returns the repository value`() = runTest {
        val active = Khatam(id = 3, name = "Active")
        coEvery { repository.getActiveKhatam() } returns active

        assertThat(GetActiveKhatamUseCase(repository)()).isEqualTo(active)
    }

    @Test
    fun `ObserveActiveKhatam exposes the repository flow`() = runTest {
        val active = Khatam(id = 3, name = "Active")
        every { repository.observeActiveKhatam() } returns flowOf(active)

        assertThat(ObserveActiveKhatamUseCase(repository)().first()).isEqualTo(active)
    }

    @Test
    fun `SetActiveKhatam delegates with the khatam id`() = runTest {
        SetActiveKhatamUseCase(repository)(11L)
        coVerify { repository.setActiveKhatam(11L) }
    }

    @Test
    fun `MarkAyahsRead forwards the khatam id and ayah ids`() = runTest {
        val ayahs = listOf(1, 2, 3)
        MarkAyahsReadUseCase(repository)(5L, ayahs)
        coVerify { repository.markAyahsRead(5L, ayahs) }
    }

    @Test
    fun `GetReadAyahIds returns the repository set`() = runTest {
        coEvery { repository.getReadAyahIds(5L) } returns setOf(1, 2, 3)
        assertThat(GetReadAyahIdsUseCase(repository)(5L)).containsExactly(1, 2, 3)
    }

    @Test
    fun `UnmarkAyahRead and MarkSurahAsRead delegate with their arguments`() = runTest {
        UnmarkAyahReadUseCase(repository)(5L, 42)
        MarkSurahAsReadUseCase(repository)(5L, 2)

        coVerify { repository.unmarkAyahRead(5L, 42) }
        coVerify { repository.markSurahAsRead(5L, 2) }
    }

    @Test
    fun `GetNextUnreadPosition returns the repository pair`() = runTest {
        coEvery { repository.getNextUnreadPosition(5L) } returns (2 to 5)
        assertThat(GetNextUnreadPositionUseCase(repository)(5L)).isEqualTo(2 to 5)
    }

    @Test
    fun `LogDailyProgress forwards id, date and ayahs read`() = runTest {
        LogDailyProgressUseCase(repository)(5L, 1000L, 20)
        coVerify { repository.logDailyProgress(5L, 1000L, 20) }
    }

    @Test
    fun `GetKhatamStats returns the repository stats`() = runTest {
        val stats = KhatamStats(
            totalKhatamsCompleted = 2, totalKhatamsActive = 1,
            totalAyahsReadAllTime = 12_472, longestStreak = 30, currentStreak = 5
        )
        coEvery { repository.getKhatamStats() } returns stats

        assertThat(GetKhatamStatsUseCase(repository)()).isEqualTo(stats)
    }

    @Test
    fun `lifecycle use cases delegate to the matching repository operation`() = runTest {
        CompleteKhatamUseCase(repository)(1L)
        AbandonKhatamUseCase(repository)(2L)
        ReactivateKhatamUseCase(repository)(3L)
        DeleteKhatamUseCase(repository)(4L)

        coVerify { repository.completeKhatam(1L) }
        coVerify { repository.abandonKhatam(2L) }
        coVerify { repository.reactivateKhatam(3L) }
        coVerify { repository.deleteKhatam(4L) }
    }

    @Test
    fun `list observers expose the repository flows`() = runTest {
        val all = listOf(Khatam(id = 1, name = "A"), Khatam(id = 2, name = "B"))
        every { repository.observeAllKhatams() } returns flowOf(all)
        every { repository.observeInProgressKhatams() } returns flowOf(all)

        assertThat(ObserveAllKhatamsUseCase(repository)().first()).isEqualTo(all)
        assertThat(ObserveInProgressKhatamsUseCase(repository)().first()).isEqualTo(all)
    }
}
