package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.TasbihPresetEntity
import com.arshadshah.nimaz.data.local.database.entity.TasbihSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbihDao {
    // Preset operations
    @Query("SELECT * FROM tasbih_presets ORDER BY display_order ASC")
    fun getAllPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE is_custom = 0 ORDER BY display_order ASC")
    fun getDefaultPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE is_custom = 1 ORDER BY display_order ASC")
    fun getCustomPresets(): Flow<List<TasbihPresetEntity>>

    @Query("SELECT * FROM tasbih_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): TasbihPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TasbihPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<TasbihPresetEntity>)

    @Update
    suspend fun updatePreset(preset: TasbihPresetEntity)

    @Delete
    suspend fun deletePreset(preset: TasbihPresetEntity)

    @Query("DELETE FROM tasbih_presets WHERE id = :id AND is_custom = 1")
    suspend fun deleteCustomPreset(id: Long)

    // Session operations
    @Query("SELECT * FROM tasbih_sessions WHERE date = :date ORDER BY startedAt DESC")
    fun getSessionsForDate(date: Long): Flow<List<TasbihSessionEntity>>

    @Query("SELECT * FROM tasbih_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY startedAt DESC")
    fun getSessionsInRange(startDate: Long, endDate: Long): Flow<List<TasbihSessionEntity>>

    @Query("SELECT * FROM tasbih_sessions WHERE presetId = :presetId ORDER BY startedAt DESC")
    fun getSessionsForPreset(presetId: Long): Flow<List<TasbihSessionEntity>>

    @Query("SELECT * FROM tasbih_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TasbihSessionEntity?

    @Query("SELECT * FROM tasbih_sessions WHERE isCompleted = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): TasbihSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TasbihSessionEntity): Long

    @Update
    suspend fun updateSession(session: TasbihSessionEntity)

    @Delete
    suspend fun deleteSession(session: TasbihSessionEntity)

    @Query("UPDATE tasbih_sessions SET currentCount = :count, totalLaps = :laps WHERE id = :id")
    suspend fun updateSessionCount(id: Long, count: Int, laps: Int)

    @Query("UPDATE tasbih_sessions SET isCompleted = 1, completedAt = :completedAt, duration = :duration WHERE id = :id")
    suspend fun completeSession(id: Long, completedAt: Long, duration: Long)

    // Statistics
    @Query("SELECT SUM(currentCount + (totalLaps * targetCount)) FROM tasbih_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCountInRange(startDate: Long, endDate: Long): Int?

    @Query("SELECT COUNT(*) FROM tasbih_sessions WHERE isCompleted = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getCompletedSessionsInRange(startDate: Long, endDate: Long): Int

    @Query("SELECT presetId, SUM(currentCount + (totalLaps * targetCount)) as totalCount FROM tasbih_sessions WHERE presetId IS NOT NULL GROUP BY presetId ORDER BY totalCount DESC LIMIT :limit")
    suspend fun getMostUsedPresets(limit: Int): List<PresetUsageStat>

    @Query("""
        SELECT presetId,
               SUM(currentCount + (totalLaps * targetCount)) as totalCount,
               COUNT(*) as sessionsCount
        FROM tasbih_sessions
        WHERE presetId IS NOT NULL
        GROUP BY presetId
        ORDER BY totalCount DESC
        LIMIT :limit
    """)
    suspend fun getMostUsedPresetsWithSessions(limit: Int): List<PresetUsageWithSessions>

    @Query("SELECT COUNT(*) FROM tasbih_sessions WHERE presetId = :presetId")
    suspend fun getSessionsCountForPreset(presetId: Long): Int

    @Query("SELECT SUM(duration) FROM tasbih_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDurationInRange(startDate: Long, endDate: Long): Long?

    @Query("DELETE FROM tasbih_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM tasbih_presets WHERE is_custom = 1")
    suspend fun deleteCustomPresets()

    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllSessions()
        deleteCustomPresets()
    }
}

data class PresetUsageStat(
    val presetId: Long,
    val totalCount: Int
)

data class PresetUsageWithSessions(
    val presetId: Long,
    val totalCount: Int,
    val sessionsCount: Int
)
