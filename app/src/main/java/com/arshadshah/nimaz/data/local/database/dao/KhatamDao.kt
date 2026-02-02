package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.KhatamAyahEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import kotlinx.coroutines.flow.Flow

data class NextUnreadResult(
    val surahId: Int,
    val numberInSurah: Int
)

@Dao
interface KhatamDao {

    // ---- Khatam CRUD ----

    @Insert
    suspend fun insertKhatam(khatam: KhatamEntity): Long

    @Update
    suspend fun updateKhatam(khatam: KhatamEntity)

    @Query("DELETE FROM khatams WHERE id = :khatamId")
    suspend fun deleteKhatam(khatamId: Long)

    @Query("SELECT * FROM khatams WHERE id = :khatamId")
    suspend fun getKhatamById(khatamId: Long): KhatamEntity?

    @Query("SELECT * FROM khatams WHERE id = :khatamId")
    fun observeKhatamById(khatamId: Long): Flow<KhatamEntity?>

    @Query("SELECT * FROM khatams WHERE is_active = 1 LIMIT 1")
    fun observeActiveKhatam(): Flow<KhatamEntity?>

    @Query("SELECT * FROM khatams WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveKhatam(): KhatamEntity?

    @Query("SELECT * FROM khatams WHERE status = 'active' ORDER BY updated_at DESC")
    fun observeInProgressKhatams(): Flow<List<KhatamEntity>>

    @Query("SELECT * FROM khatams WHERE status = 'completed' ORDER BY completed_at DESC")
    fun observeCompletedKhatams(): Flow<List<KhatamEntity>>

    @Query("SELECT * FROM khatams WHERE status = 'abandoned' ORDER BY updated_at DESC")
    fun observeAbandonedKhatams(): Flow<List<KhatamEntity>>

    @Query("SELECT * FROM khatams ORDER BY updated_at DESC")
    fun observeAllKhatams(): Flow<List<KhatamEntity>>

    // ---- Active khatam management ----

    @Query("UPDATE khatams SET is_active = 0, updated_at = :timestamp WHERE is_active = 1")
    suspend fun deactivateAllKhatams(timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE khatams SET is_active = 1, started_at = COALESCE(started_at, :timestamp), updated_at = :timestamp WHERE id = :khatamId")
    suspend fun activateKhatam(khatamId: Long, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun setActiveKhatam(khatamId: Long) {
        deactivateAllKhatams()
        activateKhatam(khatamId)
    }

    // ---- Ayah tracking ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAyahs(ayahs: List<KhatamAyahEntity>)

    @Query("SELECT ayah_id FROM khatam_ayahs WHERE khatam_id = :khatamId")
    suspend fun getReadAyahIds(khatamId: Long): List<Int>

    @Query("SELECT ayah_id FROM khatam_ayahs WHERE khatam_id = :khatamId")
    fun observeReadAyahIds(khatamId: Long): Flow<List<Int>>

    @Query("SELECT COUNT(*) FROM khatam_ayahs WHERE khatam_id = :khatamId")
    suspend fun getReadAyahCount(khatamId: Long): Int

    @Query("SELECT COUNT(*) FROM khatam_ayahs WHERE khatam_id = :khatamId")
    fun observeReadAyahCount(khatamId: Long): Flow<Int>

    @Transaction
    suspend fun markAyahsRead(khatamId: Long, ayahIds: List<Int>) {
        val entities = ayahIds.map { ayahId ->
            KhatamAyahEntity(khatamId = khatamId, ayahId = ayahId)
        }
        insertAyahs(entities)
        val count = getReadAyahCount(khatamId)
        updateTotalAyahsRead(khatamId, count)
    }

    @Query("UPDATE khatams SET total_ayahs_read = :count, updated_at = :timestamp WHERE id = :khatamId")
    suspend fun updateTotalAyahsRead(khatamId: Long, count: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM khatam_ayahs WHERE khatam_id = :khatamId AND ayah_id = :ayahId")
    suspend fun unmarkAyahRead(khatamId: Long, ayahId: Int)

    @Query("UPDATE khatams SET total_ayahs_read = (SELECT COUNT(*) FROM khatam_ayahs WHERE khatam_id = :khatamId), updated_at = :timestamp WHERE id = :khatamId")
    suspend fun recalculateTotalAyahsRead(khatamId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT a.surah_id AS surahId, a.number_in_surah AS numberInSurah FROM ayahs a
        LEFT JOIN khatam_ayahs ka ON a.id = ka.ayah_id AND ka.khatam_id = :khatamId
        WHERE ka.ayah_id IS NULL ORDER BY a.id ASC LIMIT 1
    """)
    suspend fun getNextUnreadAyah(khatamId: Long): NextUnreadResult?

    @Transaction
    suspend fun markSurahAsRead(khatamId: Long, surahNumber: Int) {
        val ayahIds = getAyahIdsForSurah(surahNumber)
        if (ayahIds.isNotEmpty()) {
            markAyahsRead(khatamId, ayahIds)
        }
    }

    @Query("SELECT id FROM ayahs WHERE surah_id = :surahNumber ORDER BY id ASC")
    suspend fun getAyahIdsForSurah(surahNumber: Int): List<Int>

    // ---- Juz/Surah progress ----

    // Ayah IDs are sequential 1-6236. Juz boundaries are known.
    // We compute per-juz progress in the repository layer.

    @Query("""
        SELECT ka.ayah_id FROM khatam_ayahs ka
        WHERE ka.khatam_id = :khatamId
        AND ka.ayah_id BETWEEN :startAyahId AND :endAyahId
    """)
    suspend fun getReadAyahIdsInRange(khatamId: Long, startAyahId: Int, endAyahId: Int): List<Int>

    // ---- Daily log ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: KhatamDailyLogEntity)

    @Query("SELECT * FROM khatam_daily_log WHERE khatam_id = :khatamId ORDER BY date DESC")
    fun observeDailyLogs(khatamId: Long): Flow<List<KhatamDailyLogEntity>>

    @Query("SELECT * FROM khatam_daily_log WHERE khatam_id = :khatamId AND date = :date")
    suspend fun getDailyLog(khatamId: Long, date: Long): KhatamDailyLogEntity?

    // ---- Completion ----

    @Query("UPDATE khatams SET status = 'completed', completed_at = :timestamp, updated_at = :timestamp, is_active = 0 WHERE id = :khatamId")
    suspend fun completeKhatam(khatamId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE khatams SET status = 'abandoned', updated_at = :timestamp, is_active = 0 WHERE id = :khatamId")
    suspend fun abandonKhatam(khatamId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE khatams SET status = 'active', updated_at = :timestamp WHERE id = :khatamId")
    suspend fun reactivateKhatam(khatamId: Long, timestamp: Long = System.currentTimeMillis())
}
