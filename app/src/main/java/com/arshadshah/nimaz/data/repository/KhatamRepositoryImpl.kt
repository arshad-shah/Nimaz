package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamProgressCalculator
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KhatamRepositoryImpl @Inject constructor(
    private val khatamDao: KhatamDao
) : KhatamRepository {

    override suspend fun createKhatam(khatam: Khatam): Long {
        return khatamDao.insertKhatam(khatam.toEntity())
    }

    override suspend fun updateKhatam(khatam: Khatam) {
        khatamDao.updateKhatam(khatam.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteKhatam(khatamId: Long) {
        khatamDao.deleteKhatam(khatamId)
    }

    override suspend fun getKhatamById(khatamId: Long): Khatam? {
        return khatamDao.getKhatamById(khatamId)?.toDomain()
    }

    override fun observeKhatamById(khatamId: Long): Flow<Khatam?> {
        return khatamDao.observeKhatamById(khatamId).map { it?.toDomain() }
    }

    override fun observeActiveKhatam(): Flow<Khatam?> {
        return khatamDao.observeActiveKhatam().map { it?.toDomain() }
    }

    override fun observeInProgressKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeInProgressKhatams().mapItems { it.toDomain() }
    }

    override fun observeCompletedKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeCompletedKhatams().mapItems { it.toDomain() }
    }

    override fun observeAbandonedKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeAbandonedKhatams().mapItems { it.toDomain() }
    }

    override fun observeAllKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeAllKhatams().mapItems { it.toDomain() }
    }

    override suspend fun setActiveKhatam(khatamId: Long) {
        khatamDao.setActiveKhatam(khatamId)
    }

    override suspend fun markAyahsRead(khatamId: Long, ayahIds: List<Int>) {
        khatamDao.markAyahsRead(khatamId, ayahIds)
    }

    override fun observeReadAyahIds(khatamId: Long): Flow<Set<Int>> {
        return khatamDao.observeReadAyahIds(khatamId).map { it.toSet() }
    }

    override fun observeReadAyahCount(khatamId: Long): Flow<Int> {
        return khatamDao.observeReadAyahCount(khatamId)
    }

    override suspend fun getNextUnreadPosition(khatamId: Long): Pair<Int, Int>? {
        val result = khatamDao.getNextUnreadAyah(khatamId) ?: return null
        return Pair(result.surahId, result.numberInSurah)
    }

    override suspend fun unmarkAyahRead(khatamId: Long, ayahId: Int) {
        khatamDao.unmarkAyahRead(khatamId, ayahId)
        khatamDao.recalculateTotalAyahsRead(khatamId)
    }

    override suspend fun markSurahAsRead(khatamId: Long, surahNumber: Int) {
        khatamDao.markSurahAsRead(khatamId, surahNumber)
    }

    /**
     * Derives all 30 juz from the single read-ayah set rather than issuing 30 range
     * queries, so this is both live and cheaper than the one-shot version it replaces.
     */
    override fun observeJuzProgress(khatamId: Long): Flow<List<JuzProgressInfo>> {
        return khatamDao.observeJuzProgress(khatamId).map { rows ->
            rows.map {
                JuzProgressInfo(
                    juzNumber = it.juzNumber,
                    totalAyahs = it.totalAyahs,
                    readAyahs = it.readAyahs,
                )
            }
        }
    }

    override fun observeDailyLogs(khatamId: Long): Flow<List<DailyLogEntry>> {
        return khatamDao.observeDailyLogs(khatamId).map { list ->
            list.map { DailyLogEntry(date = it.date, ayahsRead = it.ayahsRead) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeKhatamDetail(khatamId: Long): Flow<KhatamDetailSnapshot?> {
        return khatamDao.observeKhatamById(khatamId).flatMapLatest { entity ->
            if (entity == null) {
                // Emit null rather than stalling, so a deleted khatam resolves the
                // loading state instead of spinning forever.
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                combine(
                    observeReadAyahIds(khatamId),
                    observeDailyLogs(khatamId),
                    observeJuzProgress(khatamId)
                ) { readIds, logs, juz ->
                    val khatam = entity.toDomain()
                    KhatamDetailSnapshot(
                        khatam = khatam,
                        juzProgress = juz,
                        dailyLogs = logs,
                        insights = KhatamProgressCalculator.insights(khatam, logs, juz),
                        readAyahIds = readIds
                    )
                }
            }
        }
    }

    override suspend fun logDailyProgress(khatamId: Long, date: Long, ayahsRead: Int) {
        khatamDao.upsertDailyLog(
            KhatamDailyLogEntity(khatamId = khatamId, date = date, ayahsRead = ayahsRead)
        )
    }

    override suspend fun completeKhatam(khatamId: Long) {
        khatamDao.completeKhatam(khatamId)
    }

    override suspend fun abandonKhatam(khatamId: Long) {
        khatamDao.abandonKhatam(khatamId)
    }

    override suspend fun reactivateKhatam(khatamId: Long) {
        khatamDao.reactivateKhatam(khatamId)
    }

    /**
     * Real lifetime stats. Replaces a stub that returned all zeros while satisfying
     * its signature — which type-checked and shipped, but would have silently
     * reported "0 khatams completed" to any screen that used it.
     */
    override fun observeKhatamStats(): Flow<KhatamStats> {
        return combine(
            khatamDao.observeCompletedKhatamCount(),
            khatamDao.observeActiveKhatamCount(),
            khatamDao.observeTotalAyahsReadAllTime(),
            khatamDao.observeAllDailyLogs()
        ) { completed, active, totalAyahs, logEntities ->
            val logs = logEntities.map { DailyLogEntry(date = it.date, ayahsRead = it.ayahsRead) }
            KhatamStats(
                totalKhatamsCompleted = completed,
                totalKhatamsActive = active,
                totalAyahsReadAllTime = totalAyahs,
                longestStreak = KhatamProgressCalculator.longestStreak(logs),
                currentStreak = KhatamProgressCalculator.currentStreak(logs)
            )
        }
    }

    private fun KhatamEntity.toDomain() = Khatam(
        id = id,
        name = name,
        notes = notes,
        status = KhatamStatus.fromString(status),
        isActive = isActive,
        dailyTarget = dailyTarget,
        deadline = deadline,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        totalAyahsRead = totalAyahsRead,
        createdAt = createdAt,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt
    )

    private fun Khatam.toEntity() = KhatamEntity(
        id = id,
        name = name,
        notes = notes,
        status = status.toDbString(),
        isActive = isActive,
        dailyTarget = dailyTarget,
        deadline = deadline,
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime,
        totalAyahsRead = totalAyahsRead,
        createdAt = createdAt,
        startedAt = startedAt,
        completedAt = completedAt,
        updatedAt = updatedAt
    )
}
