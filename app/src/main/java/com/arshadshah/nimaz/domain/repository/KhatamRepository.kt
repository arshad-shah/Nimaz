package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamStats
import kotlinx.coroutines.flow.Flow

interface KhatamRepository {
    suspend fun createKhatam(khatam: Khatam): Long
    suspend fun updateKhatam(khatam: Khatam)
    suspend fun deleteKhatam(khatamId: Long)
    suspend fun getKhatamById(khatamId: Long): Khatam?
    fun observeKhatamById(khatamId: Long): Flow<Khatam?>
    fun observeActiveKhatam(): Flow<Khatam?>
    fun observeInProgressKhatams(): Flow<List<Khatam>>
    fun observeCompletedKhatams(): Flow<List<Khatam>>
    fun observeAbandonedKhatams(): Flow<List<Khatam>>
    fun observeAllKhatams(): Flow<List<Khatam>>

    suspend fun setActiveKhatam(khatamId: Long)
    suspend fun markAyahsRead(khatamId: Long, ayahIds: List<Int>)
    fun observeReadAyahIds(khatamId: Long): Flow<Set<Int>>
    fun observeReadAyahCount(khatamId: Long): Flow<Int>

    suspend fun getNextUnreadPosition(khatamId: Long): Pair<Int, Int>?
    suspend fun unmarkAyahRead(khatamId: Long, ayahId: Int)
    suspend fun markSurahAsRead(khatamId: Long, surahNumber: Int)

    /**
     * Live per-juz progress, derived from the read-ayah set rather than 30 separate
     * range queries. Replaces the former one-shot `getJuzProgress`, which forced
     * callers such as the juz grid to recompute progress client-side.
     */
    fun observeJuzProgress(khatamId: Long): Flow<List<JuzProgressInfo>>

    fun observeDailyLogs(khatamId: Long): Flow<List<DailyLogEntry>>
    suspend fun logDailyProgress(khatamId: Long, date: Long, ayahsRead: Int)

    /**
     * Everything the detail screen needs for one khatam, combined into a single Flow
     * so the khatam row, its juz breakdown, its logs and its derived insights can
     * never be observed out of step with one another.
     */
    fun observeKhatamDetail(khatamId: Long): Flow<KhatamDetailSnapshot?>

    suspend fun completeKhatam(khatamId: Long)
    suspend fun abandonKhatam(khatamId: Long)
    suspend fun reactivateKhatam(khatamId: Long)

    fun observeKhatamStats(): Flow<KhatamStats>
}
