package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.DailyLogEntry
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStats
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class KhatamUseCases(
    val createKhatam: CreateKhatamUseCase,
    val getActiveKhatam: GetActiveKhatamUseCase,
    val observeActiveKhatam: ObserveActiveKhatamUseCase,
    val setActiveKhatam: SetActiveKhatamUseCase,
    val markAyahsRead: MarkAyahsReadUseCase,
    val getReadAyahIds: GetReadAyahIdsUseCase,
    val observeReadAyahIds: ObserveReadAyahIdsUseCase,
    val getJuzProgress: GetJuzProgressUseCase,
    val observeDailyLogs: ObserveDailyLogsUseCase,
    val completeKhatam: CompleteKhatamUseCase,
    val abandonKhatam: AbandonKhatamUseCase,
    val reactivateKhatam: ReactivateKhatamUseCase,
    val deleteKhatam: DeleteKhatamUseCase,
    val observeAllKhatams: ObserveAllKhatamsUseCase,
    val observeInProgressKhatams: ObserveInProgressKhatamsUseCase,
    val observeCompletedKhatams: ObserveCompletedKhatamsUseCase,
    val observeAbandonedKhatams: ObserveAbandonedKhatamsUseCase,
    val observeKhatamById: ObserveKhatamByIdUseCase,
    val logDailyProgress: LogDailyProgressUseCase,
    val getKhatamStats: GetKhatamStatsUseCase,
    val getNextUnreadPosition: GetNextUnreadPositionUseCase,
    val unmarkAyahRead: UnmarkAyahReadUseCase,
    val markSurahAsRead: MarkSurahAsReadUseCase
)

class CreateKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatam: Khatam): Long = repository.createKhatam(khatam)
}

class GetActiveKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(): Khatam? = repository.getActiveKhatam()
}

class ObserveActiveKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(): Flow<Khatam?> = repository.observeActiveKhatam()
}

class SetActiveKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long) = repository.setActiveKhatam(khatamId)
}

class MarkAyahsReadUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long, ayahIds: List<Int>) = repository.markAyahsRead(khatamId, ayahIds)
}

class GetReadAyahIdsUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long): Set<Int> = repository.getReadAyahIds(khatamId)
}

class ObserveReadAyahIdsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(khatamId: Long): Flow<Set<Int>> = repository.observeReadAyahIds(khatamId)
}

class GetJuzProgressUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long): List<JuzProgressInfo> = repository.getJuzProgress(khatamId)
}

class ObserveDailyLogsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(khatamId: Long): Flow<List<DailyLogEntry>> = repository.observeDailyLogs(khatamId)
}

class CompleteKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long) = repository.completeKhatam(khatamId)
}

class AbandonKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long) = repository.abandonKhatam(khatamId)
}

class ReactivateKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long) = repository.reactivateKhatam(khatamId)
}

class DeleteKhatamUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long) = repository.deleteKhatam(khatamId)
}

class ObserveAllKhatamsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(): Flow<List<Khatam>> = repository.observeAllKhatams()
}

class ObserveInProgressKhatamsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(): Flow<List<Khatam>> = repository.observeInProgressKhatams()
}

class ObserveCompletedKhatamsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(): Flow<List<Khatam>> = repository.observeCompletedKhatams()
}

class ObserveAbandonedKhatamsUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(): Flow<List<Khatam>> = repository.observeAbandonedKhatams()
}

class ObserveKhatamByIdUseCase @Inject constructor(private val repository: KhatamRepository) {
    operator fun invoke(khatamId: Long): Flow<Khatam?> = repository.observeKhatamById(khatamId)
}

class LogDailyProgressUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long, date: Long, ayahsRead: Int) =
        repository.logDailyProgress(khatamId, date, ayahsRead)
}

class GetKhatamStatsUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(): KhatamStats = repository.getKhatamStats()
}

class GetNextUnreadPositionUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long): Pair<Int, Int>? = repository.getNextUnreadPosition(khatamId)
}

class UnmarkAyahReadUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long, ayahId: Int) = repository.unmarkAyahRead(khatamId, ayahId)
}

class MarkSurahAsReadUseCase @Inject constructor(private val repository: KhatamRepository) {
    suspend operator fun invoke(khatamId: Long, surahNumber: Int) = repository.markSurahAsRead(khatamId, surahNumber)
}
