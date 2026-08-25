package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.user.PresetUsageWithSessions
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
import com.arshadshah.nimaz.domain.model.DefaultTasbihPresets
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tasbih statistics and the default-preset repair.
 *
 * The statistics are `SUM()` aggregates, and SQLite's answer to `SUM` over no rows is **NULL**,
 * not 0. Every one of those nulls reaches a screen that renders a number, so a missing
 * `?: 0` is a crash on the stats screen for a user who has not counted anything this week —
 * exactly the user least likely to report it.
 *
 * `seedMissingDefaults` is the repair path for an install that lost a default preset (deleted,
 * or a corpus that shipped without one). It has to insert with `id = 0` so Room assigns fresh
 * ids: reusing the default's id would *replace* whatever custom preset happens to hold it.
 */
class TasbihRepositoryImplStatsTest {

    private lateinit var tasbihDao: TasbihDao
    private lateinit var sessionDao: TasbihSessionDao
    private lateinit var repository: TasbihRepositoryImpl

    @Before
    fun setUp() {
        tasbihDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        repository = TasbihRepositoryImpl(tasbihDao, sessionDao)
    }

    // ── aggregates over nothing ───────────────────────────────────────────────

    @Test
    fun `a week with no sessions reports zeroes rather than crashing the stats screen`() =
        runTest {
            // SUM() over no rows is NULL in SQLite, and the screen renders a number.
            coEvery { sessionDao.getTotalCountInRange(0L, 1L) } returns null
            coEvery { sessionDao.getTotalDurationInRange(0L, 1L) } returns null
            coEvery { sessionDao.getCompletedSessionsInRange(0L, 1L) } returns 0
            coEvery { sessionDao.getMostUsedPresetsWithSessions(any()) } returns emptyList()

            val stats = repository.getTasbihStats(0L, 1L)

            assertThat(stats.totalCount).isEqualTo(0)
            assertThat(stats.totalDuration).isEqualTo(0L)
            assertThat(stats.completedSessions).isEqualTo(0)
            assertThat(stats.mostUsedPresets).isEmpty()
            assertThat(stats.startDate).isEqualTo(0L)
            assertThat(stats.endDate).isEqualTo(1L)
        }

    @Test
    fun `a week with sessions reports the sums and the presets behind them`() = runTest {
        coEvery { sessionDao.getTotalCountInRange(0L, 1L) } returns 330
        coEvery { sessionDao.getTotalDurationInRange(0L, 1L) } returns 600_000L
        coEvery { sessionDao.getCompletedSessionsInRange(0L, 1L) } returns 10
        coEvery { sessionDao.getMostUsedPresetsWithSessions(5) } returns
            listOf(PresetUsageWithSessions(presetId = 1, totalCount = 330, sessionsCount = 10))
        coEvery { tasbihDao.getPresetById(1) } returns preset(1, "SubhanAllah")

        val stats = repository.getTasbihStats(0L, 1L)

        assertThat(stats.totalCount).isEqualTo(330)
        assertThat(stats.totalDuration).isEqualTo(600_000L)
        assertThat(stats.mostUsedPresets.single().presetName).isEqualTo("SubhanAllah")
        assertThat(stats.mostUsedPresets.single().sessionsCount).isEqualTo(10)
    }

    @Test
    fun `usage of a preset the user has since deleted is dropped, not shown nameless`() =
        runTest {
            coEvery { sessionDao.getMostUsedPresetsWithSessions(5) } returns
                listOf(PresetUsageWithSessions(presetId = 99, totalCount = 5, sessionsCount = 1))
            coEvery { tasbihDao.getPresetById(99) } returns null

            assertThat(repository.getTasbihStats(0L, 1L).mostUsedPresets).isEmpty()
        }

    @Test
    fun `the standalone total also survives a range with no sessions`() = runTest {
        coEvery { sessionDao.getTotalCountInRange(0L, 1L) } returns null
        coEvery { sessionDao.getCompletedSessionsInRange(0L, 1L) } returns 0

        assertThat(repository.getTotalCountInRange(0L, 1L)).isEqualTo(0)
        assertThat(repository.getCompletedSessionsInRange(0L, 1L)).isEqualTo(0)
    }

    // ── sessions ──────────────────────────────────────────────────────────────

    @Test
    fun `a session id that no longer exists opens nothing`() = runTest {
        coEvery { sessionDao.getSessionById(7) } returns null

        assertThat(repository.getSessionById(7)).isNull()
    }

    @Test
    fun `a session reads back with the counter it holds`() = runTest {
        coEvery { sessionDao.getSessionById(7) } returns session(7)

        val read = repository.getSessionById(7)!!

        assertThat(read.currentCount).isEqualTo(33)
        assertThat(read.totalLaps).isEqualTo(1)
        assertThat(read.presetName).isEqualTo("SubhanAllah")
    }

    @Test
    fun `a device with no counter open has no active session`() = runTest {
        coEvery { sessionDao.getActiveSession() } returns null

        assertThat(repository.getActiveSession()).isNull()
    }

    @Test
    fun `an open counter is the active session`() = runTest {
        coEvery { sessionDao.getActiveSession() } returns session(7).copy(isCompleted = false)

        assertThat(repository.getActiveSession()!!.isCompleted).isFalse()
    }

    // ── the default presets ───────────────────────────────────────────────────

    @Test
    fun `an install that already has every default is left alone`() = runTest {
        coEvery { tasbihDao.getAllPresetNames() } returns
            DefaultTasbihPresets.allDefaults.map { it.name }

        repository.seedMissingDefaults()

        coVerify(exactly = 0) { tasbihDao.insertPresets(any()) }
    }

    @Test
    fun `a missing default is re-inserted under a fresh id, never the default's own`() =
        runTest {
            val missingName = DefaultTasbihPresets.allDefaults.first().name
            coEvery { tasbihDao.getAllPresetNames() } returns
                DefaultTasbihPresets.allDefaults.drop(1).map { it.name }
            val inserted = mutableListOf<List<TasbihPresetEntity>>()
            coEvery { tasbihDao.insertPresets(capture(inserted)) } returns Unit

            repository.seedMissingDefaults()

            assertThat(inserted.single().map { it.name }).containsExactly(missingName)
            // Reusing the default's id would REPLACE whatever custom preset holds it.
            assertThat(inserted.single().map { it.id }.toSet()).containsExactly(0L)
        }

    @Test
    fun `a fresh install gets every default seeded`() = runTest {
        val inserted = mutableListOf<List<TasbihPresetEntity>>()
        coEvery { tasbihDao.insertPresets(capture(inserted)) } returns Unit

        repository.initializeDefaultPresets()

        assertThat(inserted.single()).hasSize(DefaultTasbihPresets.allDefaults.size)
        assertThat(inserted.single().first().isCustom).isEqualTo(0)
    }

    @Test
    fun `an install with no default presets reports so`() = runTest {
        every { tasbihDao.getDefaultPresets() } returns flowOf(emptyList())
        assertThat(repository.hasDefaultPresets()).isFalse()

        every { tasbihDao.getDefaultPresets() } returns flowOf(listOf(preset(1, "SubhanAllah")))
        assertThat(repository.hasDefaultPresets()).isTrue()
    }

    @Test
    fun `a preset with no optional text writes empty strings rather than nulls`() = runTest {
        val inserted = mutableListOf<TasbihPresetEntity>()
        coEvery { tasbihDao.insertPreset(capture(inserted)) } returns 1L

        repository.insertPreset(
            DefaultTasbihPresets.allDefaults.first().copy(
                id = 0,
                arabicText = null,
                transliteration = null,
                translation = null,
                category = null,
                isDefault = false,
            )
        )

        // The columns are NOT NULL; a screen renders "" as nothing, which is the intent.
        assertThat(inserted.single().arabic).isEmpty()
        assertThat(inserted.single().transliteration).isEmpty()
        assertThat(inserted.single().translation).isEmpty()
        assertThat(inserted.single().category).isNull()
        // Custom, because the user made it.
        assertThat(inserted.single().isCustom).isEqualTo(1)
    }

    @Test
    fun `a preset's category is stored in the lowercase form the table uses`() = runTest {
        val inserted = mutableListOf<TasbihPresetEntity>()
        coEvery { tasbihDao.insertPreset(capture(inserted)) } returns 1L

        repository.insertPreset(
            DefaultTasbihPresets.allDefaults.first()
                .copy(id = 0, category = TasbihCategory.AFTER_PRAYER)
        )

        assertThat(inserted.single().category).isEqualTo("after_prayer")
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private fun preset(id: Long, name: String) = TasbihPresetEntity(
        id = id, name = name, arabic = "سبحان الله", transliteration = name,
        translation = "Glory be to Allah", targetCount = 33, isCustom = 0, displayOrder = 1,
        updatedAt = 1L,
    )

    private fun session(id: Long) = TasbihSessionEntity(
        id = id, presetId = 1, presetName = "SubhanAllah", date = 1L, currentCount = 33,
        targetCount = 33, totalLaps = 1, isCompleted = true, duration = 60_000L,
        startedAt = 1L, completedAt = 2L, note = null, updatedAt = 2L,
    )
}
