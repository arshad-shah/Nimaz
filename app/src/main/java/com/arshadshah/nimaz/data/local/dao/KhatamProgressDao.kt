package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.KhatamProgress

@Dao
interface KhatamProgressDao {
    @Query("SELECT * FROM khatam_progress WHERE khatamId = :khatamId ORDER BY timestamp DESC")
    suspend fun getProgressForKhatam(khatamId: Long): List<KhatamProgress>

    @Query("SELECT * FROM khatam_progress WHERE khatamId = :khatamId AND dateRead = :date")
    suspend fun getProgressForDate(khatamId: Long, date: String): List<KhatamProgress>

    @Query("SELECT COUNT(*) FROM khatam_progress WHERE khatamId = :khatamId AND dateRead = :date")
    suspend fun getAyasReadToday(khatamId: Long, date: String): Int

    @Insert
    suspend fun insertProgress(progress: KhatamProgress)

    @Delete
    suspend fun deleteProgress(progress: KhatamProgress)

    @Query("DELETE FROM khatam_progress WHERE khatamId = :khatamId")
    suspend fun deleteAllProgressForKhatam(khatamId: Long)

    @Query("SELECT AVG(sessionDuration) FROM khatam_progress WHERE khatamId = :khatamId AND sessionDuration > 0")
    suspend fun getAverageSessionDuration(khatamId: Long): Double?

    @Query("SELECT COUNT(DISTINCT dateRead) FROM khatam_progress WHERE khatamId = :khatamId")
    suspend fun getActiveDaysCount(khatamId: Long): Int
}
