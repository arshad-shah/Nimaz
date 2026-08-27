package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.common.mapItems
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.user.PresetUsageStat
import com.arshadshah.nimaz.data.local.user.PresetUsageWithSessions
import com.arshadshah.nimaz.data.local.user.TasbihSessionDao
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Presets come from the content database, sessions from the user's own.
 *
 * `tasbih_presets` holds both the presets we ship and the ones a person creates, which makes
 * it the one table in the app that is genuinely both; the sessions have no such ambiguity and
 * moved out wholesale. Composing the two here is the repository doing its job.
 */
class TasbihRepositoryImpl @Inject constructor(
    private val tasbihDao: TasbihDao,
    private val sessionDao: TasbihSessionDao,
) : TasbihRepository {

    override fun getAllPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getAllPresets().mapItems { it.toDomain() }
    }

    override fun getDefaultPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getDefaultPresets().mapItems { it.toDomain() }
    }

    override fun getCustomPresets(): Flow<List<TasbihPreset>> {
        return tasbihDao.getCustomPresets().mapItems { it.toDomain() }
    }

    override fun getPresetsByCategory(category: TasbihCategory): Flow<List<TasbihPreset>> {
        // No category column in database, filter in memory
        return tasbihDao.getAllPresets().mapItems { it.toDomain() }
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
        return sessionDao.getSessionsForDate(date).mapItems { it.toDomain() }
    }

    override fun getSessionsInRange(startDate: Long, endDate: Long): Flow<List<TasbihSession>> {
        return sessionDao.getSessionsInRange(startDate, endDate).mapItems { it.toDomain() }
    }

    override fun getSessionsForPreset(presetId: Long): Flow<List<TasbihSession>> {
        return sessionDao.getSessionsForPreset(presetId).mapItems { it.toDomain() }
    }

    override suspend fun getSessionById(id: Long): TasbihSession? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    override suspend fun getActiveSession(): TasbihSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun insertSession(session: TasbihSession): Long {
        return sessionDao.insertSession(session.toEntity())
    }

    override suspend fun updateSession(session: TasbihSession) {
        sessionDao.updateSession(session.toEntity())
    }

    override suspend fun deleteSession(session: TasbihSession) {
        sessionDao.deleteSession(session.toEntity())
    }

    override suspend fun updateSessionCount(id: Long, count: Int, laps: Int) {
        sessionDao.updateSessionCount(id, count, laps)
    }

    override suspend fun completeSession(id: Long, completedAt: Long, duration: Long) {
        sessionDao.completeSession(id, completedAt, duration)
    }

    override suspend fun getTasbihStats(startDate: Long, endDate: Long): TasbihStats {
        val totalCount = sessionDao.getTotalCountInRange(startDate, endDate) ?: 0
        val completedSessions = sessionDao.getCompletedSessionsInRange(startDate, endDate)
        val totalDuration = sessionDao.getTotalDurationInRange(startDate, endDate) ?: 0L
        val mostUsedPresets = sessionDao.getMostUsedPresetsWithSessions(5).mapNotNull { stat ->
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
        return sessionDao.getTotalCountInRange(startDate, endDate) ?: 0
    }

    override suspend fun getCompletedSessionsInRange(startDate: Long, endDate: Long): Int {
        return sessionDao.getCompletedSessionsInRange(startDate, endDate)
    }

    override suspend fun initializeDefaultPresets() {
        val entities = DefaultTasbihPresets.allDefaults.map { it.toEntity() }
        tasbihDao.insertPresets(entities)
    }

    override suspend fun seedMissingDefaults() {
        val existingNames = tasbihDao.getAllPresetNames().toSet()
        val missing = DefaultTasbihPresets.allDefaults.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) {
            // id = 0 so Room assigns fresh ids and never replaces an existing row.
            tasbihDao.insertPresets(missing.map { it.toEntity().copy(id = 0) })
        }
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
            category = TasbihCategory.fromString(category),
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
            displayOrder = displayOrder,
            category = category?.name?.lowercase()
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
