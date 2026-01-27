package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingDao {
    // Fast record operations
    @Query("SELECT * FROM fast_records WHERE date = :date LIMIT 1")
    suspend fun getFastRecordForDate(date: Long): FastRecordEntity?

    @Query("SELECT * FROM fast_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getFastRecordsInRange(startDate: Long, endDate: Long): Flow<List<FastRecordEntity>>

    @Query("SELECT * FROM fast_records WHERE hijriMonth = :hijriMonth ORDER BY date ASC")
    fun getFastRecordsByHijriMonth(hijriMonth: Int): Flow<List<FastRecordEntity>>

    @Query("SELECT * FROM fast_records WHERE fastType = :fastType ORDER BY date DESC")
    fun getFastRecordsByType(fastType: String): Flow<List<FastRecordEntity>>

    @Query("SELECT * FROM fast_records WHERE status = :status ORDER BY date DESC")
    fun getFastRecordsByStatus(status: String): Flow<List<FastRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFastRecord(record: FastRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFastRecords(records: List<FastRecordEntity>)

    @Update
    suspend fun updateFastRecord(record: FastRecordEntity)

    @Query("UPDATE fast_records SET status = :status, updatedAt = :timestamp WHERE date = :date")
    suspend fun updateFastStatus(date: Long, status: String, timestamp: Long = System.currentTimeMillis())

    // Makeup fast operations
    @Query("SELECT * FROM makeup_fasts WHERE status = 'pending' ORDER BY originalDate ASC")
    fun getPendingMakeupFasts(): Flow<List<MakeupFastEntity>>

    @Query("SELECT * FROM makeup_fasts ORDER BY originalDate DESC")
    fun getAllMakeupFasts(): Flow<List<MakeupFastEntity>>

    @Query("SELECT * FROM makeup_fasts WHERE id = :id")
    suspend fun getMakeupFastById(id: Long): MakeupFastEntity?

    @Query("SELECT COUNT(*) FROM makeup_fasts WHERE status = 'pending'")
    fun getPendingMakeupFastCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMakeupFast(makeupFast: MakeupFastEntity)

    @Update
    suspend fun updateMakeupFast(makeupFast: MakeupFastEntity)

    @Query("UPDATE makeup_fasts SET status = 'completed', completedDate = :completedDate, updatedAt = :timestamp WHERE id = :id")
    suspend fun markMakeupFastCompleted(id: Long, completedDate: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE makeup_fasts SET status = 'fidya_paid', fidyaAmount = :amount, updatedAt = :timestamp WHERE id = :id")
    suspend fun markFidyaPaid(id: Long, amount: Double, timestamp: Long = System.currentTimeMillis())

    // Statistics
    @Query("SELECT COUNT(*) FROM fast_records WHERE status = 'fasted' AND date BETWEEN :startDate AND :endDate")
    suspend fun getFastedCountInRange(startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(*) FROM fast_records WHERE status = 'fasted' AND fastType = 'ramadan' AND hijriMonth = 9")
    suspend fun getRamadanFastedCount(): Int

    @Query("SELECT COUNT(*) FROM fast_records WHERE fastType = 'voluntary' AND status = 'fasted'")
    suspend fun getVoluntaryFastCount(): Int

    @Query("SELECT SUM(fidyaAmount) FROM makeup_fasts WHERE status = 'fidya_paid'")
    suspend fun getTotalFidyaPaid(): Double?

    // Streak calculation - get consecutive fasted days ending today or yesterday
    @Query("""
        SELECT * FROM fast_records
        WHERE status = 'fasted'
        AND date <= :todayTimestamp
        ORDER BY date DESC
    """)
    suspend fun getRecentFastedRecords(todayTimestamp: Long): List<FastRecordEntity>
}
