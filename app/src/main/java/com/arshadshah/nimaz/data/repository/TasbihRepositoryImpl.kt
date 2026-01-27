package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import com.arshadshah.nimaz.domain.model.DefaultTasbihPresets
import com.arshadshah.nimaz.domain.model.PresetUsage
import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats
import com.arshadshah.nimaz.domain.repository.TasbihRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasbihRepositoryImpl @Inject constructor(
    private val tasbihDao: TasbihDao
) : TasbihRepository {

    override fun getAllPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDefaultPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getDefaultPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCustomPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getCustomPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPresetsByCategory(category: TasbihCategory): Flow<List<TasbihPreset>> {
        // No category column in database, filter in memory
        return tasbihDao.getAllPresets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getPresetById(id: Long): TasbihPreset? {
        return tasbihDao.getPresetById(id)?.toDomain()
    }

    override suspend fun insertPreset(preset: TasbihPreset): Long {
        return tasbihDao.insertPreset(preset.toEntity())
    }

    override suspend fun updatePreset(preset: TasbihPreset) {
        tasbihDao.updatePreset(preset.toEntity())
    }

    override suspend fun deleteCustomPreset(id: Long) {
        tasbihDao.deleteCustomPreset(id)
    }

    override fun getSessionsForDate(date: Long): Flow<List<TasbihSession>> {
        return tasbihDao.getSessionsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSessionsInRange(startDate: Long, endDate: Long): Flow<List<TasbihSession>> {
        return tasbihDao.getSessionsInRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSessionsForPreset(presetId: Long): Flow<List<TasbihSession>> {
        return tasbihDao.getSessionsForPreset(presetId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: Long): TasbihSession? {
        return tasbihDao.getSessionById(id)?.toDomain()
    }

    override suspend fun getActiveSession(): TasbihSession? {
        return tasbihDao.getActiveSession()?.toDomain()
    }

    override suspend fun insertSession(session: TasbihSession): Long {
        return tasbihDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: TasbihSession) {
        tasbihDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(session: TasbihSession) {
        tasbihDao.deleteSession(session.toEntity())
    }

    override suspend fun updateSessionCount(id: Long, count: Int, laps: Int) {
        tasbihDao.updateSessionCount(id, count, laps)
    }

    override suspend fun completeSession(id: Long, completedAt: Long, duration: Long) {
        tasbihDao.completeSession(id, completedAt, duration)
    }

    override suspend fun getTasbihStats(startDate: Long, endDate: Long): TasbihStats {
        val totalCount = tasbihDao.getTotalCountInRange(startDate, endDate) ?: 0
        val completedSessions = tasbihDao.getCompletedSessionsInRange(startDate, endDate)
        val totalDuration = tasbihDao.getTotalDurationInRange(startDate, endDate) ?: 0L
        val mostUsedPresets = tasbihDao.getMostUsedPresetsWithSessions(5).mapNotNull { stat ->
            val preset = tasbihDao.getPresetById(stat.presetId)
            preset?.let {
                PresetUsage(
                    presetId = it.id,
                    presetName = it.name,
                    totalCount = stat.totalCount,
                    sessionsCount = stat.sessionsCount
                )
            }
        }

        return TasbihStats(
            totalCount = totalCount,
            completedSessions = completedSessions,
            totalDuration = totalDuration,
            mostUsedPresets = mostUsedPresets,
            startDate = startDate,
            endDate = endDate
        )
    }

    override suspend fun getTotalCountInRange(startDate: Long, endDate: Long): Int {
        return tasbihDao.getTotalCountInRange(startDate, endDate) ?: 0
    }

    override suspend fun getCompletedSessionsInRange(startDate: Long, endDate: Long): Int {
        return tasbihDao.getCompletedSessionsInRange(startDate, endDate)
    }

    override suspend fun initializeDefaultPresets() {
        val entities = DefaultTasbihPresets.allDefaults.map { it.toEntity() }
        tasbihDao.insertPresets(entities)
    }

    override suspend fun hasDefaultPresets(): Boolean {
        return tasbihDao.getDefaultPresets().first().isNotEmpty()
    }

    // Mapping functions
    private fun TasbihPresetEntity.toDomain(): TasbihPreset {
        return TasbihPreset(
            id = id,
            name = name,
            arabicText = arabic,
            transliteration = transliteration,
            translation = translation,
            targetCount = targetCount,
            category = null, // No category in database
            reference = null,
            isDefault = isCustom == 0,
            displayOrder = displayOrder,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun TasbihPreset.toEntity(): TasbihPresetEntity {
        return TasbihPresetEntity(
            id = id,
            name = name,
            arabic = arabicText ?: "",
            transliteration = transliteration ?: "",
            translation = translation ?: "",
            targetCount = targetCount,
            isCustom = if (isDefault) 0 else 1,
            displayOrder = displayOrder
        )
    }

    private fun TasbihSessionEntity.toDomain(): TasbihSession {
        return TasbihSession(
            id = id,
            presetId = presetId,
            presetName = presetName,
            date = date,
            currentCount = currentCount,
            targetCount = targetCount,
            totalLaps = totalLaps,
            isCompleted = isCompleted,
            duration = duration,
            startedAt = startedAt,
            completedAt = completedAt,
            note = note
        )
    }

    private fun TasbihSession.toEntity(): TasbihSessionEntity {
        return TasbihSessionEntity(
            id = id,
            presetId = presetId,
            presetName = presetName,
            date = date,
            currentCount = currentCount,
            targetCount = targetCount,
            totalLaps = totalLaps,
            isCompleted = isCompleted,
            duration = duration,
            startedAt = startedAt,
            completedAt = completedAt,
            note = note
        )
    }
}
