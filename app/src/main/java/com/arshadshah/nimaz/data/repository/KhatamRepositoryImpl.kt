package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.database.entity.KhatamDailyLogEntity
import com.arshadshah.nimaz.data.local.database.entity.KhatamEntity
import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamConstants
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import kotlinx.coroutines.flow.Flow
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
        khatamDao.updateKhatam(khatam.toEntity())
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

    override suspend fun getActiveKhatam(): Khatam? {
        return khatamDao.getActiveKhatam()?.toDomain()
    }

    override fun observeInProgressKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeInProgressKhatams().map { list -> list.map { it.toDomain() } }
    }

    override fun observeCompletedKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeCompletedKhatams().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAbandonedKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeAbandonedKhatams().map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllKhatams(): Flow<List<Khatam>> {
        return khatamDao.observeAllKhatams().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun setActiveKhatam(khatamId: Long) {
        khatamDao.setActiveKhatam(khatamId)
    }

    override suspend fun markAyahsRead(khatamId: Long, ayahIds: List<Int>) {
        khatamDao.markAyahsRead(khatamId, ayahIds)
    }

    override suspend fun getReadAyahIds(khatamId: Long): Set<Int> {
        return khatamDao.getReadAyahIds(khatamId).toSet()
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

    override suspend fun getJuzProgress(khatamId: Long): List<JuzProgressInfo> {
        return KhatamConstants.JUZ_AYAH_RANGES.mapIndexed { index, (startAyahId, endAyahId) ->
            val readIds = khatamDao.getReadAyahIdsInRange(khatamId, startAyahId, endAyahId)
            JuzProgressInfo(
                juzNumber = index + 1,
                totalAyahs = endAyahId - startAyahId + 1,
                readAyahs = readIds.size
            )
        }
    }

    override fun observeDailyLogs(khatamId: Long): Flow<List<DailyLogEntry>> {
        return khatamDao.observeDailyLogs(khatamId).map { list ->
            list.map { DailyLogEntry(date = it.date, ayahsRead = it.ayahsRead) }
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

    override suspend fun getKhatamStats(): KhatamStats {
        // Simplified stats - computed from observing all khatams
        return KhatamStats(
            totalKhatamsCompleted = 0,
            totalKhatamsActive = 0,
            totalAyahsReadAllTime = 0,
            longestStreak = 0,
            currentStreak = 0
        )
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
