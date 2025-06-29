package com.arshadshah.nimaz.data.local.dao

import androidx.room.*
import com.arshadshah.nimaz.data.local.models.ReadingProgress

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM ReadingProgress WHERE surahNumber = :surahNumber")
    suspend fun getProgressForSurah(surahNumber: Int): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: ReadingProgress)

    @Query("SELECT * FROM ReadingProgress ORDER BY lastReadDate DESC LIMIT 5")
    suspend fun getRecentlyRead(): List<ReadingProgress>

    @Query("SELECT * FROM ReadingProgress ORDER BY completionPercentage DESC")
    suspend fun getAllProgressOrderedByCompletion(): List<ReadingProgress>

    @Delete
    suspend fun deleteProgress(progress: ReadingProgress)

    //clearAllReadingProgress
    @Query("DELETE FROM ReadingProgress")
    suspend fun clearAllReadingProgress()
}