package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
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

class TasbihRepositoryImplTest {

    private lateinit var tasbihDao: TasbihDao
    private lateinit var sessionDao: TasbihSessionDao
    private lateinit var repository: TasbihRepositoryImpl

    private val now = System.currentTimeMillis()

    private fun makePresetEntity(id: Long = 1L, isCustom: Int = 0) = TasbihPresetEntity(
        id = id, name = "SubhanAllah", arabic = "سبحان الله",
        transliteration = "SubhanAllah", translation = "Glory be to Allah",
        targetCount = 33, isCustom = isCustom, displayOrder = 1,
        category = null, updatedAt = now
    )

    private fun makeSessionEntity(id: Long = 1L, presetId: Long? = 1L) = TasbihSessionEntity(
        id = id, presetId = presetId, presetName = "SubhanAllah",
        date = now, currentCount = 10, targetCount = 33, totalLaps = 0,
        isCompleted = false, duration = null, startedAt = now,
        completedAt = null, note = null
    )

    @Before
    fun setUp() {
        tasbihDao = mockk(relaxed = true)
        sessionDao = mockk(relaxed = true)
        repository = TasbihRepositoryImpl(tasbihDao, sessionDao)
    }

    @Test
    fun `getDefaultPresets returns flow of mapped presets`() = runTest {
        every { tasbihDao.getDefaultPresets() } returns flowOf(
            listOf(makePresetEntity(1L, isCustom = 0), makePresetEntity(2L, isCustom = 0))
        )

        val result = repository.getDefaultPresets().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("SubhanAllah")
        assertThat(result[0].isDefault).isTrue()
    }

    @Test
    fun `getCustomPresets returns flow of custom presets`() = runTest {
        every { tasbihDao.getCustomPresets() } returns flowOf(
            listOf(makePresetEntity(5L, isCustom = 1))
        )

        val result = repository.getCustomPresets().first()

        assertThat(result).hasSize(1)
        assertThat(result[0].isDefault).isFalse()
    }

    @Test
    fun `getPresetById returns mapped preset`() = runTest {
        coEvery { tasbihDao.getPresetById(1L) } returns makePresetEntity(1L)

        val result = repository.getPresetById(1L)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(1L)
        assertThat(result.targetCount).isEqualTo(33)
    }

    @Test
    fun `getPresetById returns null when not found`() = runTest {
        coEvery { tasbihDao.getPresetById(any()) } returns null

        assertThat(repository.getPresetById(999L)).isNull()
    }

    @Test
    fun `insertPreset converts domain to entity and returns id`() = runTest {
        coEvery { tasbihDao.insertPreset(any()) } returns 10L

        val preset = TasbihPreset(
            id = 0L, name = "Alhamdulillah", arabicText = "الحمد لله",
            transliteration = "Alhamdulillah", translation = "Praise be to Allah",
            targetCount = 33, category = null, reference = null,
            isDefault = false, displayOrder = 1, createdAt = now, updatedAt = now
        )
        val id = repository.insertPreset(preset)

        assertThat(id).isEqualTo(10L)
        coVerify { tasbihDao.insertPreset(any()) }
    }

    @Test
    fun `deleteCustomPreset delegates to dao`() = runTest {
        repository.deleteCustomPreset(5L)
        coVerify { tasbihDao.deleteCustomPreset(5L) }
    }

    @Test
    fun `getSessionsForDate returns flow of mapped sessions`() = runTest {
        every { sessionDao.getSessionsForDate(now) } returns flowOf(listOf(makeSessionEntity()))

        val result = repository.getSessionsForDate(now).first()

        assertThat(result).hasSize(1)
        assertThat(result[0].currentCount).isEqualTo(10)
    }

    @Test
    fun `getActiveSession returns null when no active session`() = runTest {
        coEvery { sessionDao.getActiveSession() } returns null

        assertThat(repository.getActiveSession()).isNull()
    }

    @Test
    fun `insertSession returns new session id`() = runTest {
        coEvery { sessionDao.insertSession(any()) } returns 7L

        val session = TasbihSession(
            id = 0L, presetId = 1L, presetName = "SubhanAllah",
            date = now, currentCount = 0, targetCount = 33, totalLaps = 0,
            isCompleted = false, duration = null, startedAt = now,
            completedAt = null, note = null
        )
        val id = repository.insertSession(session)

        assertThat(id).isEqualTo(7L)
    }

    @Test
    fun `updateSessionCount delegates to session dao`() = runTest {
        repository.updateSessionCount(1L, 25, 0)
        coVerify { sessionDao.updateSessionCount(1L, 25, 0) }
    }

    @Test
    fun `completeSession delegates to session dao`() = runTest {
        repository.completeSession(1L, now, 5000L)
        coVerify { sessionDao.completeSession(1L, now, 5000L) }
    }

    @Test
    fun `preset entity with isCustom 0 maps to isDefault true`() = runTest {
        every { tasbihDao.getDefaultPresets() } returns flowOf(listOf(makePresetEntity(isCustom = 0)))

        val result = repository.getDefaultPresets().first()

        assertThat(result[0].isDefault).isTrue()
    }

    @Test
    fun `preset entity with isCustom 1 maps to isDefault false`() = runTest {
        every { tasbihDao.getCustomPresets() } returns flowOf(listOf(makePresetEntity(isCustom = 1)))

        val result = repository.getCustomPresets().first()

        assertThat(result[0].isDefault).isFalse()
    }
}
