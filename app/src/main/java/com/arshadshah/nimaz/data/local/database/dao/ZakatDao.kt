package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZakatDao {
    @Query("SELECT * FROM zakat_history ORDER BY calculatedAt DESC")
    fun getAllHistory(): Flow<List<ZakatHistoryEntity>>

    @Insert
    suspend fun insertCalculation(entry: ZakatHistoryEntity): Long

    @Query("UPDATE zakat_history SET isPaid = 1, paidAt = :paidAt WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidAt: Long)

    @Query("SELECT SUM(zakatDue) FROM zakat_history WHERE isPaid = 1")
    suspend fun getTotalPaid(): Double?

    @Query("DELETE FROM zakat_history WHERE id = :id")
    suspend fun deleteCalculation(id: Long)

    @Query("SELECT * FROM zakat_history ORDER BY calculatedAt DESC")
    suspend fun getAllHistorySync(): List<ZakatHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculations(entries: List<ZakatHistoryEntity>)

    @Query("DELETE FROM zakat_history")
    suspend fun deleteAllUserData()
}
