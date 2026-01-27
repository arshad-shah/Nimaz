package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import kotlinx.coroutines.flow.Flow

interface FastingRepository {
    // Fast records
    suspend fun getFastRecordForDate(date: Long): FastRecord?
    fun getFastRecordsInRange(startDate: Long, endDate: Long): Flow<List<FastRecord>>
    fun getFastRecordsByHijriMonth(hijriMonth: Int): Flow<List<FastRecord>>
    fun getFastRecordsByType(fastType: FastType): Flow<List<FastRecord>>
    fun getFastRecordsByStatus(status: FastStatus): Flow<List<FastRecord>>

    // Fast record operations
    suspend fun insertFastRecord(record: FastRecord)
    suspend fun insertFastRecords(records: List<FastRecord>)
    suspend fun updateFastRecord(record: FastRecord)
    suspend fun updateFastStatus(date: Long, status: FastStatus)

    // Makeup fast operations
    fun getPendingMakeupFasts(): Flow<List<MakeupFast>>
    fun getAllMakeupFasts(): Flow<List<MakeupFast>>
    suspend fun getMakeupFastById(id: Long): MakeupFast?
    fun getPendingMakeupFastCount(): Flow<Int>
    suspend fun insertMakeupFast(makeupFast: MakeupFast)
    suspend fun updateMakeupFast(makeupFast: MakeupFast)
    suspend fun markMakeupFastCompleted(id: Long, completedDate: Long)
    suspend fun markFidyaPaid(id: Long, amount: Double)

    // Statistics
    suspend fun getFastingStats(startDate: Long, endDate: Long): FastingStats
    suspend fun getRamadanFastedCount(): Int
    suspend fun getVoluntaryFastCount(): Int
    suspend fun getTotalFidyaPaid(): Double
}
