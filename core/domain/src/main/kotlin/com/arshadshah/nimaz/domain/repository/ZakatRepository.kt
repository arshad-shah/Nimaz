package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import kotlinx.coroutines.flow.Flow

interface ZakatRepository {
    fun getAllHistory(): Flow<List<ZakatHistoryEntry>>
    suspend fun insertCalculation(entry: ZakatHistoryEntry): Long
    suspend fun markAsPaid(id: Long, paidAt: Long)
    suspend fun getTotalPaid(): Double
    suspend fun deleteCalculation(id: Long)
}
