package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.TasbihCategory
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.domain.model.TasbihStats
import kotlinx.coroutines.flow.Flow

interface TasbihRepository {
    // Preset operations
    fun getAllPresets(): Flow<List<TasbihPreset>>
    fun getDefaultPresets(): Flow<List<TasbihPreset>>
    fun getCustomPresets(): Flow<List<TasbihPreset>>
    fun getPresetsByCategory(category: TasbihCategory): Flow<List<TasbihPreset>>
    suspend fun getPresetById(id: Long): TasbihPreset?
    suspend fun insertPreset(preset: TasbihPreset): Long
    suspend fun updatePreset(preset: TasbihPreset)
    suspend fun deleteCustomPreset(id: Long)

    // Session operations
    fun getSessionsForDate(date: Long): Flow<List<TasbihSession>>
    fun getSessionsInRange(startDate: Long, endDate: Long): Flow<List<TasbihSession>>
    fun getSessionsForPreset(presetId: Long): Flow<List<TasbihSession>>
    suspend fun getSessionById(id: Long): TasbihSession?
    suspend fun getActiveSession(): TasbihSession?
    suspend fun insertSession(session: TasbihSession): Long
    suspend fun updateSession(session: TasbihSession)
    suspend fun deleteSession(session: TasbihSession)
    suspend fun updateSessionCount(id: Long, count: Int, laps: Int)
    suspend fun completeSession(id: Long, completedAt: Long, duration: Long)

    // Statistics
    suspend fun getTasbihStats(startDate: Long, endDate: Long): TasbihStats
    suspend fun getTotalCountInRange(startDate: Long, endDate: Long): Int
    suspend fun getCompletedSessionsInRange(startDate: Long, endDate: Long): Int

    // Data initialization
    suspend fun initializeDefaultPresets()
    suspend fun hasDefaultPresets(): Boolean
}
