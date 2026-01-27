package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_records WHERE date = :date ORDER BY scheduledTime ASC")
    fun getPrayerRecordsForDate(date: Long): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, scheduledTime ASC")
    fun getPrayerRecordsInRange(startDate: Long, endDate: Long): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE date = :date AND prayerName = :prayerName LIMIT 1")
    suspend fun getPrayerRecord(date: Long, prayerName: String): PrayerRecordEntity?

    @Query("SELECT * FROM prayer_records WHERE status = :status ORDER BY date DESC, scheduledTime ASC")
    fun getPrayerRecordsByStatus(status: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE status = 'missed' AND isQadaFor IS NULL ORDER BY date DESC")
    fun getMissedPrayersRequiringQada(): Flow<List<PrayerRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRecord(record: PrayerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRecords(records: List<PrayerRecordEntity>)

    @Update
    suspend fun updatePrayerRecord(record: PrayerRecordEntity)

    @Query("UPDATE prayer_records SET status = :status, prayedAt = :prayedAt, isJamaah = :isJamaah, updatedAt = :timestamp WHERE date = :date AND prayerName = :prayerName")
    suspend fun updatePrayerStatus(
        date: Long,
        prayerName: String,
        status: String,
        prayedAt: Long?,
        isJamaah: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    // Statistics queries
    @Query("SELECT COUNT(*) FROM prayer_records WHERE status = 'prayed' AND date BETWEEN :startDate AND :endDate")
    suspend fun getPrayedCountInRange(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM prayer_records WHERE status = 'missed' AND date BETWEEN :startDate AND :endDate")
    suspend fun getMissedCountInRange(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM prayer_records WHERE isJamaah = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getJamaahCountInRange(startDate: Long, endDate: Long): Int

    @Query("SELECT prayerName, COUNT(*) as count FROM prayer_records WHERE status = 'prayed' AND date BETWEEN :startDate AND :endDate GROUP BY prayerName")
    suspend fun getPrayedCountByPrayer(startDate: Long, endDate: Long): List<PrayerStatCount>

    @Query("SELECT prayerName, COUNT(*) as count FROM prayer_records WHERE status = 'missed' AND date BETWEEN :startDate AND :endDate GROUP BY prayerName")
    suspend fun getMissedCountByPrayer(startDate: Long, endDate: Long): List<PrayerStatCount>

    // Streak calculation
    @Query("""
        SELECT COUNT(DISTINCT date) FROM prayer_records
        WHERE status = 'prayed'
        AND date <= :currentDate
        AND date >= (
            SELECT COALESCE(MAX(date), 0) FROM prayer_records
            WHERE status = 'missed' AND date <= :currentDate
        )
    """)
    suspend fun getCurrentStreak(currentDate: Long): Int

    @Query("""
        SELECT MAX(streak) FROM (
            SELECT COUNT(*) as streak FROM prayer_records
            WHERE status = 'prayed'
            GROUP BY date
        )
    """)
    suspend fun getLongestStreak(): Int?
}

data class PrayerStatCount(
    val prayerName: String,
    val count: Int
)
