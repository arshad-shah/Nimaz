package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ZakatUseCases(
    val getAllHistory: GetAllHistoryUseCase,
    val insertCalculation: InsertCalculationUseCase,
    val markAsPaid: MarkAsPaidUseCase,
    val getTotalPaid: GetTotalPaidUseCase,
    val deleteCalculation: DeleteCalculationUseCase
)

class GetAllHistoryUseCase @Inject constructor(private val repository: ZakatRepository) {
    operator fun invoke(): Flow<List<ZakatHistoryEntry>> = repository.getAllHistory()
}

class InsertCalculationUseCase @Inject constructor(private val repository: ZakatRepository) {
    suspend operator fun invoke(entry: ZakatHistoryEntry): Long =
        repository.insertCalculation(entry)
}

class MarkAsPaidUseCase @Inject constructor(private val repository: ZakatRepository) {
    suspend operator fun invoke(id: Long, paidAt: Long) = repository.markAsPaid(id, paidAt)
}

class GetTotalPaidUseCase @Inject constructor(private val repository: ZakatRepository) {
    suspend operator fun invoke(): Double = repository.getTotalPaid()
}

class DeleteCalculationUseCase @Inject constructor(private val repository: ZakatRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteCalculation(id)
}
