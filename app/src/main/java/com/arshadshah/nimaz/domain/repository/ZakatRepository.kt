package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import kotlinx.coroutines.flow.Flow

interface ZakatRepository {
    fun getAllHistory(): Flow<List<ZakatHistoryEntity>>
    suspend fun insertCalculation(entry: ZakatHistoryEntity): Long
    suspend fun markAsPaid(id: Long, paidAt: Long)
    suspend fun getTotalPaid(): Double
}
