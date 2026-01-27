package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.domain.repository.FastingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FastingRepositoryImpl @Inject constructor(
    private val fastingDao: FastingDao
) : FastingRepository {

    override suspend fun getFastRecordForDate(date: Long): FastRecord? {
        return fastingDao.getFastRecordForDate(date)?.toDomain()
    }

    override fun getFastRecordsInRange(startDate: Long, endDate: Long): Flow<List<FastRecord>> {
        return fastingDao.getFastRecordsInRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFastRecordsByHijriMonth(hijriMonth: Int): Flow<List<FastRecord>> {
        return fastingDao.getFastRecordsByHijriMonth(hijriMonth).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFastRecordsByType(fastType: FastType): Flow<List<FastRecord>> {
        return fastingDao.getFastRecordsByType(fastType.name.lowercase()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFastRecordsByStatus(status: FastStatus): Flow<List<FastRecord>> {
        return fastingDao.getFastRecordsByStatus(status.name.lowercase()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertFastRecord(record: FastRecord) {
        fastingDao.insertFastRecord(record.toEntity())
    }

    override suspend fun insertFastRecords(records: List<FastRecord>) {
        fastingDao.insertFastRecords(records.map { it.toEntity() })
    }

    override suspend fun updateFastRecord(record: FastRecord) {
        fastingDao.updateFastRecord(record.toEntity())
    }

    override suspend fun updateFastStatus(date: Long, status: FastStatus) {
        fastingDao.updateFastStatus(date, status.name.lowercase())
    }

    override fun getPendingMakeupFasts(): Flow<List<MakeupFast>> {
        return fastingDao.getPendingMakeupFasts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllMakeupFasts(): Flow<List<MakeupFast>> {
        return fastingDao.getAllMakeupFasts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMakeupFastById(id: Long): MakeupFast? {
        return fastingDao.getMakeupFastById(id)?.toDomain()
    }

    override fun getPendingMakeupFastCount(): Flow<Int> {
        return fastingDao.getPendingMakeupFastCount()
    }

    override suspend fun insertMakeupFast(makeupFast: MakeupFast) {
        fastingDao.insertMakeupFast(makeupFast.toEntity())
    }

    override suspend fun updateMakeupFast(makeupFast: MakeupFast) {
        fastingDao.updateMakeupFast(makeupFast.toEntity())
    }

    override suspend fun markMakeupFastCompleted(id: Long, completedDate: Long) {
        fastingDao.markMakeupFastCompleted(id, completedDate)
    }

    override suspend fun markFidyaPaid(id: Long, amount: Double) {
        fastingDao.markFidyaPaid(id, amount)
    }

    override suspend fun getFastingStats(startDate: Long, endDate: Long): FastingStats {
        val currentStreak = calculateCurrentStreak()
        return FastingStats(
            totalFasted = fastingDao.getFastedCountInRange(startDate, endDate),
            ramadanFasted = fastingDao.getRamadanFastedCount(),
            voluntaryFasted = fastingDao.getVoluntaryFastCount(),
            pendingMakeupCount = 0, // Will be calculated from flow
            totalFidyaPaid = fastingDao.getTotalFidyaPaid() ?: 0.0,
            currentStreak = currentStreak,
            startDate = startDate,
            endDate = endDate
        )
    }

    /**
     * Calculate the current fasting streak (consecutive days fasted).
     * A streak is counted from today or yesterday backwards.
     */
    private suspend fun calculateCurrentStreak(): Int {
        val today = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val todayStart = (today / oneDayMillis) * oneDayMillis

        val recentRecords = fastingDao.getRecentFastedRecords(todayStart + oneDayMillis)
        if (recentRecords.isEmpty()) return 0

        // Sort by date descending to process from most recent
        val sortedRecords = recentRecords.sortedByDescending { it.date }

        // Check if the most recent fast is today or yesterday (to allow for ongoing streak)
        val mostRecentDate = sortedRecords.first().date
        val daysSinceMostRecent = (todayStart - mostRecentDate) / oneDayMillis

        // If the most recent fast is more than 1 day ago, no current streak
        if (daysSinceMostRecent > 1) return 0

        var streak = 0
        var expectedDate = mostRecentDate

        for (record in sortedRecords) {
            val recordDateStart = (record.date / oneDayMillis) * oneDayMillis
            val expectedDateStart = (expectedDate / oneDayMillis) * oneDayMillis

            if (recordDateStart == expectedDateStart) {
                streak++
                expectedDate -= oneDayMillis
            } else if (recordDateStart < expectedDateStart) {
                // Gap in streak, stop counting
                break
            }
        }

        return streak
    }

    override suspend fun getRamadanFastedCount(): Int {
        return fastingDao.getRamadanFastedCount()
    }

    override suspend fun getVoluntaryFastCount(): Int {
        return fastingDao.getVoluntaryFastCount()
    }

    override suspend fun getTotalFidyaPaid(): Double {
        return fastingDao.getTotalFidyaPaid() ?: 0.0
    }

    // Mapping functions
    private fun FastRecordEntity.toDomain(): FastRecord {
        return FastRecord(
            id = id,
            date = date,
            hijriDate = hijriDate,
            hijriMonth = hijriMonth,
            hijriYear = hijriYear,
            fastType = FastType.fromString(fastType),
            status = FastStatus.fromString(status),
            exemptionReason = ExemptionReason.fromString(exemptionReason),
            suhoorTime = suhoorTime,
            iftarTime = iftarTime,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun FastRecord.toEntity(): FastRecordEntity {
        return FastRecordEntity(
            id = id,
            date = date,
            hijriDate = hijriDate,
            hijriMonth = hijriMonth,
            hijriYear = hijriYear,
            fastType = fastType.name.lowercase(),
            status = status.name.lowercase(),
            exemptionReason = exemptionReason?.name?.lowercase(),
            suhoorTime = suhoorTime,
            iftarTime = iftarTime,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun MakeupFastEntity.toDomain(): MakeupFast {
        return MakeupFast(
            id = id,
            originalDate = originalDate,
            originalHijriDate = originalHijriDate,
            reason = reason,
            status = MakeupFastStatus.fromString(status),
            completedDate = completedDate,
            fidyaAmount = fidyaAmount,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun MakeupFast.toEntity(): MakeupFastEntity {
        return MakeupFastEntity(
            id = id,
            originalDate = originalDate,
            originalHijriDate = originalHijriDate,
            reason = reason,
            status = status.name.lowercase(),
            completedDate = completedDate,
            fidyaAmount = fidyaAmount,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
