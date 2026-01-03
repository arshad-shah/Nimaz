package com.arshadshah.nimaz.data.local.dao
import androidx.room.*
import com.arshadshah.nimaz.data.local.models.KhatamSession

@Dao
interface KhatamSessionDao {
    @Query("SELECT * FROM khatam_sessions WHERE isCompleted = 0 ORDER BY startDate DESC LIMIT 1")
    suspend fun getActiveKhatam(): KhatamSession?

    @Query("SELECT * FROM khatam_sessions ORDER BY startDate DESC")
    suspend fun getAllKhatamSessions(): List<KhatamSession>

    @Query("SELECT * FROM khatam_sessions WHERE isCompleted = 1 ORDER BY completionDate DESC")
    suspend fun getCompletedKhatams(): List<KhatamSession>

    @Query("SELECT * FROM khatam_sessions WHERE id = :id")
    suspend fun getKhatamById(id: Long): KhatamSession?

    @Insert
    suspend fun insertKhatam(khatam: KhatamSession): Long

    @Update
    suspend fun updateKhatam(khatam: KhatamSession)

    @Delete
    suspend fun deleteKhatam(khatam: KhatamSession)

    @Query("UPDATE khatam_sessions SET isActive = 0 WHERE id = :id")
    suspend fun pauseKhatam(id: Long)

    @Query("UPDATE khatam_sessions SET isActive = 1 WHERE id = :id")
    suspend fun resumeKhatam(id: Long)

    @Query("UPDATE khatam_sessions SET isCompleted = 1, completionDate = :completionDate, isActive = 0 WHERE id = :id")
    suspend fun completeKhatam(id: Long, completionDate: String)

    @Query("UPDATE khatam_sessions SET currentSurah = :surah, currentAya = :aya, totalAyasRead = :totalRead WHERE id = :id")
    suspend fun updateKhatamProgress(id: Long, surah: Int, aya: Int, totalRead: Int)
}