package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastingStats
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.repository.FastingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class FastingUseCases(
    val getFastRecordForDate: GetFastRecordForDateUseCase,
    val getFastRecordsInRange: GetFastRecordsInRangeUseCase,
    val insertFastRecord: InsertFastRecordUseCase,
    val updateFastRecord: UpdateFastRecordUseCase,
    val updateFastStatus: UpdateFastStatusUseCase,
    val deleteFastRecordByDate: DeleteFastRecordByDateUseCase,
    val getRamadanFastedCount: GetRamadanFastedCountUseCase,
    val getVoluntaryFastCount: GetVoluntaryFastCountUseCase,
    val getFastingStats: GetFastingStatsUseCase,
    val getAllMakeupFasts: GetAllMakeupFastsUseCase,
    val getPendingMakeupFasts: GetPendingMakeupFastsUseCase,
    val getMakeupFastCountForDate: GetMakeupFastCountForDateUseCase,
    val insertMakeupFast: InsertMakeupFastUseCase,
    val updateMakeupFast: UpdateMakeupFastUseCase,
    val markMakeupFastCompleted: MarkMakeupFastCompletedUseCase,
    val markFidyaPaid: MarkFidyaPaidUseCase,
    val getTotalFidyaPaid: GetTotalFidyaPaidUseCase
)

class GetFastRecordForDateUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(date: Long): FastRecord? = repository.getFastRecordForDate(date)
}

class GetFastRecordsInRangeUseCase @Inject constructor(private val repository: FastingRepository) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<FastRecord>> =
        repository.getFastRecordsInRange(startDate, endDate)
}

class InsertFastRecordUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(record: FastRecord) = repository.insertFastRecord(record)
}

class UpdateFastRecordUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(record: FastRecord) = repository.updateFastRecord(record)
}

class UpdateFastStatusUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(date: Long, status: FastStatus) =
        repository.updateFastStatus(date, status)
}

class DeleteFastRecordByDateUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(date: Long) = repository.deleteFastRecordByDate(date)
}

class GetRamadanFastedCountUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(): Int = repository.getRamadanFastedCount()
}

class GetVoluntaryFastCountUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(): Int = repository.getVoluntaryFastCount()
}

class GetFastingStatsUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(startDate: Long, endDate: Long): FastingStats =
        repository.getFastingStats(startDate, endDate)
}

class GetAllMakeupFastsUseCase @Inject constructor(private val repository: FastingRepository) {
    operator fun invoke(): Flow<List<MakeupFast>> = repository.getAllMakeupFasts()
}

class GetPendingMakeupFastsUseCase @Inject constructor(private val repository: FastingRepository) {
    operator fun invoke(): Flow<List<MakeupFast>> = repository.getPendingMakeupFasts()
}

class GetMakeupFastCountForDateUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(originalDate: Long): Int =
        repository.getMakeupFastCountForDate(originalDate)
}

class InsertMakeupFastUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(makeupFast: MakeupFast) = repository.insertMakeupFast(makeupFast)
}

class UpdateMakeupFastUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(makeupFast: MakeupFast) = repository.updateMakeupFast(makeupFast)
}

class MarkMakeupFastCompletedUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(id: Long, completedDate: Long) =
        repository.markMakeupFastCompleted(id, completedDate)
}

class MarkFidyaPaidUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(id: Long, amount: Double) = repository.markFidyaPaid(id, amount)
}

class GetTotalFidyaPaidUseCase @Inject constructor(private val repository: FastingRepository) {
    suspend operator fun invoke(): Double = repository.getTotalFidyaPaid()
}
