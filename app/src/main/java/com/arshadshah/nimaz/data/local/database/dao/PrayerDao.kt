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

    @Query("SELECT * FROM prayer_records WHERE date = :date ORDER BY scheduledTime ASC")
    suspend fun getPrayerRecordsForDateSync(date: Long): List<PrayerRecordEntity>

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

    // Perfect days queries - days where all 5 prayers (excluding sunrise) were completed
    @Query("""
        SELECT date FROM prayer_records
        WHERE status IN ('prayed', 'late')
        AND prayerName != 'sunrise'
        GROUP BY date
        HAVING COUNT(DISTINCT prayerName) = 5
        ORDER BY date DESC
    """)
    suspend fun getPerfectDays(): List<Long>

    // Count perfect days in range
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT date FROM prayer_records
            WHERE status IN ('prayed', 'late')
            AND prayerName != 'sunrise'
            AND date BETWEEN :startDate AND :endDate
            GROUP BY date
            HAVING COUNT(DISTINCT prayerName) = 5
        )
    """)
    suspend fun getPerfectDaysCount(startDate: Long, endDate: Long): Int

    // Mark past pending/not_prayed prayers as missed (for dates before today)
    @Query("""
        UPDATE prayer_records
        SET status = 'missed', updatedAt = :timestamp
        WHERE date < :todayDate
        AND status IN ('pending', 'not_prayed')
        AND prayerName != 'sunrise'
    """)
    suspend fun markPastPrayersAsMissed(todayDate: Long, timestamp: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM prayer_records")
    suspend fun deleteAllUserData()
}

data class PrayerStatCount(
    val prayerName: String,
    val count: Int
)
