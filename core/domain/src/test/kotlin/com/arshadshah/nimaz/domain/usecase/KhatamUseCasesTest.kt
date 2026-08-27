package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class KhatamUseCasesTest {

    private lateinit var repo: KhatamRepository
    private lateinit var useCases: KhatamUseCases

    private val now = System.currentTimeMillis()

    private fun makeKhatam(id: Long = 1L, status: KhatamStatus = KhatamStatus.ACTIVE) = Khatam(
        id = id, name = "My Khatam", notes = null, status = status,
        isActive = status == KhatamStatus.ACTIVE, dailyTarget = 20,
        deadline = null, reminderEnabled = false, reminderTime = null,
        totalAyahsRead = 0, createdAt = now, startedAt = now,
        completedAt = null, updatedAt = now
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = KhatamUseCases(
            createKhatam = CreateKhatamUseCase(repo),
            updateKhatam = UpdateKhatamUseCase(repo),
            observeActiveKhatam = ObserveActiveKhatamUseCase(repo),
            setActiveKhatam = SetActiveKhatamUseCase(repo),
            markAyahsRead = MarkAyahsReadUseCase(repo),
            observeReadAyahIds = ObserveReadAyahIdsUseCase(repo),
            observeJuzProgress = ObserveJuzProgressUseCase(repo),
            observeDailyLogs = ObserveDailyLogsUseCase(repo),
            observeKhatamDetail = ObserveKhatamDetailUseCase(repo),
            completeKhatam = CompleteKhatamUseCase(repo),
            abandonKhatam = AbandonKhatamUseCase(repo),
            reactivateKhatam = ReactivateKhatamUseCase(repo),
            deleteKhatam = DeleteKhatamUseCase(repo),
            observeAllKhatams = ObserveAllKhatamsUseCase(repo),
            observeInProgressKhatams = ObserveInProgressKhatamsUseCase(repo),
            observeCompletedKhatams = ObserveCompletedKhatamsUseCase(repo),
            observeAbandonedKhatams = ObserveAbandonedKhatamsUseCase(repo),
            observeKhatamById = ObserveKhatamByIdUseCase(repo),
            logDailyProgress = LogDailyProgressUseCase(repo),
            observeKhatamStats = ObserveKhatamStatsUseCase(repo),
            getNextUnreadPosition = GetNextUnreadPositionUseCase(repo),
            unmarkAyahRead = UnmarkAyahReadUseCase(repo),
            markSurahAsRead = MarkSurahAsReadUseCase(repo)
        )
    }

    @Test
    fun `createKhatam returns new id`() = runTest {
        val khatam = makeKhatam()
        coEvery { repo.createKhatam(khatam) } returns 1L

        val id = useCases.createKhatam(khatam)

        assertThat(id).isEqualTo(1L)
        coVerify { repo.createKhatam(khatam) }
    }

    @Test
    fun `observeActiveKhatam emits active khatam`() = runTest {
        val khatam = makeKhatam(id = 1L)
        every { repo.observeActiveKhatam() } returns flowOf(khatam)

        val result = useCases.observeActiveKhatam().first()

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
    }

    @Test
    fun `observeActiveKhatam emits null when none active`() = runTest {
        every { repo.observeActiveKhatam() } returns flowOf(null)

        val result = useCases.observeActiveKhatam().first()

        assertThat(result).isNull()
    }

    @Test
    fun `setActiveKhatam delegates to repo`() = runTest {
        useCases.setActiveKhatam(5L)
        coVerify { repo.setActiveKhatam(5L) }
    }

    @Test
    fun `markAyahsRead delegates with khatam id and ayah list`() = runTest {
        val ayahIds = listOf(1, 2, 3)
        useCases.markAyahsRead(1L, ayahIds)
        coVerify { repo.markAyahsRead(1L, ayahIds) }
    }

    @Test
    fun `completeKhatam delegates to repo`() = runTest {
        useCases.completeKhatam(1L)
        coVerify { repo.completeKhatam(1L) }
    }

    @Test
    fun `abandonKhatam delegates to repo`() = runTest {
        useCases.abandonKhatam(1L)
        coVerify { repo.abandonKhatam(1L) }
    }

    @Test
    fun `deleteKhatam delegates to repo`() = runTest {
        useCases.deleteKhatam(1L)
        coVerify { repo.deleteKhatam(1L) }
    }

    @Test
    fun `observeAllKhatams emits list of khatams`() = runTest {
        val khatams = listOf(makeKhatam(1L), makeKhatam(2L))
        every { repo.observeAllKhatams() } returns flowOf(khatams)

        val result = useCases.observeAllKhatams().first()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `observeKhatamStats emits stats`() = runTest {
        val stats = KhatamStats(
            totalKhatamsCompleted = 1, totalKhatamsActive = 1,
            totalAyahsReadAllTime = 6236, longestStreak = 10, currentStreak = 5
        )
        every { repo.observeKhatamStats() } returns flowOf(stats)

        val result = useCases.observeKhatamStats().first()

        assertThat(result.totalKhatamsCompleted).isEqualTo(1)
        assertThat(result.totalKhatamsActive).isEqualTo(1)
    }

    @Test
    fun `getNextUnreadPosition returns pair from repo`() = runTest {
        coEvery { repo.getNextUnreadPosition(1L) } returns Pair(2, 5)

        val result = useCases.getNextUnreadPosition(1L)

        assertThat(result).isEqualTo(Pair(2, 5))
    }
}
