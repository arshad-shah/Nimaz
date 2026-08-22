package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.common.mapItems
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZakatRepositoryImpl @Inject constructor(
    private val zakatDao: ZakatDao
) : ZakatRepository {

    override fun getAllHistory(): Flow<List<ZakatHistoryEntry>> {
        return zakatDao.getAllHistory().mapItems { it.toDomain() }
    }

    override suspend fun insertCalculation(entry: ZakatHistoryEntry): Long {
        return zakatDao.insertCalculation(entry.toEntity())
    }

    override suspend fun markAsPaid(id: Long, paidAt: Long) {
        zakatDao.markAsPaid(id, paidAt)
    }

    override suspend fun getTotalPaid(): Double {
        return zakatDao.getTotalPaid() ?: 0.0
    }

    override suspend fun deleteCalculation(id: Long) {
        zakatDao.deleteCalculation(id)
    }
}

private fun ZakatHistoryEntity.toDomain() = ZakatHistoryEntry(
    id = id,
    calculatedAt = calculatedAt,
    totalAssets = totalAssets,
    totalLiabilities = totalLiabilities,
    netWorth = netWorth,
    zakatDue = zakatDue,
    nisabType = runCatching { NisabType.valueOf(nisabType) }
        .onFailure { CrashReporter.recordException(it) }
        .getOrDefault(NisabType.GOLD),
    nisabValue = nisabValue,
    isPaid = isPaid,
    paidAt = paidAt,
    notes = notes
)

private fun ZakatHistoryEntry.toEntity() = ZakatHistoryEntity(
    id = id,
    calculatedAt = calculatedAt,
    totalAssets = totalAssets,
    totalLiabilities = totalLiabilities,
    netWorth = netWorth,
    zakatDue = zakatDue,
    nisabType = nisabType.name,
    nisabValue = nisabValue,
    isPaid = isPaid,
    paidAt = paidAt,
    notes = notes
)
