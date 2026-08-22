package com.arshadshah.nimaz.data.local.user

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

/**
 * Counting sessions — the user's, so they live in the user's database.
 *
 * `tasbih_presets` stays in the content database with [TasbihDao], because it holds both the
 * presets we ship and the ones a person creates. That is the one genuinely mixed table in the
 * app and splitting it needs a decision about a shipped preset the user has edited; the
 * sessions have no such ambiguity.
 */
@Dao
interface TasbihSessionDao {
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
    @Query(
        """
        SELECT presetId,
               SUM(currentCount + (totalLaps * targetCount)) as totalCount,
               COUNT(*) as sessionsCount
        FROM tasbih_sessions
        WHERE presetId IS NOT NULL
        GROUP BY presetId
        ORDER BY totalCount DESC
        LIMIT :limit
    """
    )
    suspend fun getMostUsedPresetsWithSessions(limit: Int): List<PresetUsageWithSessions>
    @Query("SELECT COUNT(*) FROM tasbih_sessions WHERE presetId = :presetId")
    suspend fun getSessionsCountForPreset(presetId: Long): Int
    @Query("SELECT SUM(duration) FROM tasbih_sessions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDurationInRange(startDate: Long, endDate: Long): Long?
    @Query("SELECT * FROM tasbih_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessionsSync(): List<TasbihSessionEntity>
    @Query("DELETE FROM tasbih_sessions")
    suspend fun deleteAllSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TasbihSessionEntity): Long

    @Update
    suspend fun updateSession(session: TasbihSessionEntity)

    @Delete
    suspend fun deleteSession(session: TasbihSessionEntity)
}

/** How much a preset has been used, summed over the user's sessions. */
data class PresetUsageStat(
    val presetId: Long,
    val totalCount: Int,
)

/** The same, with the number of sessions that made it up. */
data class PresetUsageWithSessions(
    val presetId: Long,
    val totalCount: Int,
    val sessionsCount: Int,
)
