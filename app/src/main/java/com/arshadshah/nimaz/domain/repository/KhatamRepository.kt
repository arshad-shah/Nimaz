package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStats
import kotlinx.coroutines.flow.Flow

interface KhatamRepository {
    suspend fun createKhatam(khatam: Khatam): Long
    suspend fun updateKhatam(khatam: Khatam)
    suspend fun deleteKhatam(khatamId: Long)
    suspend fun getKhatamById(khatamId: Long): Khatam?
    fun observeKhatamById(khatamId: Long): Flow<Khatam?>
    fun observeActiveKhatam(): Flow<Khatam?>
    suspend fun getActiveKhatam(): Khatam?
    fun observeInProgressKhatams(): Flow<List<Khatam>>
    fun observeCompletedKhatams(): Flow<List<Khatam>>
    fun observeAbandonedKhatams(): Flow<List<Khatam>>
    fun observeAllKhatams(): Flow<List<Khatam>>

    suspend fun setActiveKhatam(khatamId: Long)
    suspend fun markAyahsRead(khatamId: Long, ayahIds: List<Int>)
    suspend fun getReadAyahIds(khatamId: Long): Set<Int>
    fun observeReadAyahIds(khatamId: Long): Flow<Set<Int>>
    fun observeReadAyahCount(khatamId: Long): Flow<Int>

    suspend fun getNextUnreadPosition(khatamId: Long): Pair<Int, Int>?
    suspend fun unmarkAyahRead(khatamId: Long, ayahId: Int)
    suspend fun markSurahAsRead(khatamId: Long, surahNumber: Int)

    suspend fun getJuzProgress(khatamId: Long): List<JuzProgressInfo>
    fun observeDailyLogs(khatamId: Long): Flow<List<DailyLogEntry>>
    suspend fun logDailyProgress(khatamId: Long, date: Long, ayahsRead: Int)

    suspend fun completeKhatam(khatamId: Long)
    suspend fun abandonKhatam(khatamId: Long)
    suspend fun reactivateKhatam(khatamId: Long)
    suspend fun getKhatamStats(): KhatamStats
}
