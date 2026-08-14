package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats
import com.arshadshah.nimaz.domain.repository.TasbihRepository
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

class TasbihUseCasesTest {

    private lateinit var repo: TasbihRepository
    private lateinit var useCases: TasbihUseCases

    private val now = System.currentTimeMillis()

    private fun makePreset(id: Long = 1L) = TasbihPreset(
        id = id, name = "SubhanAllah", arabicText = "سبحان الله",
        transliteration = "SubhanAllah", translation = "Glory be to Allah",
        targetCount = 33, category = null, reference = null,
        isDefault = true, displayOrder = 1, createdAt = now, updatedAt = now
    )

    private fun makeSession(id: Long = 1L, presetId: Long = 1L) = TasbihSession(
        id = id, presetId = presetId, presetName = "SubhanAllah",
        date = now, currentCount = 10, targetCount = 33,
        totalLaps = 0, isCompleted = false, duration = null,
        startedAt = now, completedAt = null, note = null
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = TasbihUseCases(
            getDefaultPresets = GetDefaultPresetsUseCase(repo),
            getCustomPresets = GetCustomPresetsUseCase(repo),
            getPresetById = GetPresetByIdUseCase(repo),
            insertPreset = InsertPresetUseCase(repo),
            updatePreset = UpdatePresetUseCase(repo),
            deleteCustomPreset = DeleteCustomPresetUseCase(repo),
            seedMissingDefaults = SeedMissingDefaultsUseCase(repo),
            getSessionsForDate = GetSessionsForDateUseCase(repo),
            getSessionsInRange = GetSessionsInRangeUseCase(repo),
            getActiveSession = GetActiveSessionUseCase(repo),
            getSessionById = GetSessionByIdUseCase(repo),
            insertSession = InsertSessionUseCase(repo),
            updateSessionCount = UpdateSessionCountUseCase(repo),
            completeSession = CompleteSessionUseCase(repo),
            getTasbihStats = GetTasbihStatsUseCase(repo),
            getTotalCountInRange = GetTotalCountInRangeUseCase(repo),
            getCompletedSessionsInRange = GetCompletedSessionsInRangeUseCase(repo)
        )
    }

    @Test
    fun `getDefaultPresets returns flow from repo`() = runTest {
        val preset = makePreset()
        every { repo.getDefaultPresets() } returns flowOf(listOf(preset))

        val result = useCases.getDefaultPresets().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("SubhanAllah")
    }

    @Test
    fun `getPresetById returns preset by id`() = runTest {
        val preset = makePreset(id = 5L)
        coEvery { repo.getPresetById(5L) } returns preset

        val result = useCases.getPresetById(5L)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(5L)
    }

    @Test
    fun `getPresetById returns null when not found`() = runTest {
        coEvery { repo.getPresetById(999L) } returns null

        assertThat(useCases.getPresetById(999L)).isNull()
    }

    @Test
    fun `insertPreset delegates to repo and returns id`() = runTest {
        val preset = makePreset()
        coEvery { repo.insertPreset(preset) } returns 42L

        val id = useCases.insertPreset(preset)

        assertThat(id).isEqualTo(42L)
        coVerify { repo.insertPreset(preset) }
    }

    @Test
    fun `deleteCustomPreset delegates to repo`() = runTest {
        useCases.deleteCustomPreset(3L)
        coVerify { repo.deleteCustomPreset(3L) }
    }

    @Test
    fun `getSessionsForDate returns sessions for given date`() = runTest {
        val session = makeSession()
        every { repo.getSessionsForDate(now) } returns flowOf(listOf(session))

        val result = useCases.getSessionsForDate(now).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getActiveSession returns null when no active session`() = runTest {
        coEvery { repo.getActiveSession() } returns null

        assertThat(useCases.getActiveSession()).isNull()
    }

    @Test
    fun `insertSession returns new session id`() = runTest {
        val session = makeSession()
        coEvery { repo.insertSession(session) } returns 10L

        val id = useCases.insertSession(session)

        assertThat(id).isEqualTo(10L)
    }

    @Test
    fun `updateSessionCount delegates to repo`() = runTest {
        useCases.updateSessionCount(1L, 20, 0)
        coVerify { repo.updateSessionCount(1L, 20, 0) }
    }

    @Test
    fun `completeSession delegates to repo`() = runTest {
        useCases.completeSession(1L, now, 5000L)
        coVerify { repo.completeSession(1L, now, 5000L) }
    }

    @Test
    fun `getTasbihStats returns stats from repo`() = runTest {
        val stats = TasbihStats(
            totalCount = 99, completedSessions = 3,
            totalDuration = 60000L, mostUsedPresets = emptyList(),
            startDate = now, endDate = now + 1000
        )
        coEvery { repo.getTasbihStats(any(), any()) } returns stats

        val result = useCases.getTasbihStats(now, now + 1000)

        assertThat(result.totalCount).isEqualTo(99)
        assertThat(result.completedSessions).isEqualTo(3)
    }

    @Test
    fun `getTotalCountInRange returns count from repo`() = runTest {
        coEvery { repo.getTotalCountInRange(any(), any()) } returns 500

        val count = useCases.getTotalCountInRange(now, now + 1000)

        assertThat(count).isEqualTo(500)
    }
}
