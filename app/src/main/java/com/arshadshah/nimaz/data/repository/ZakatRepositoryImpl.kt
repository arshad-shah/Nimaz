package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.repository.ZakatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZakatRepositoryImpl @Inject constructor(
    private val zakatDao: ZakatDao
) : ZakatRepository {

    override fun getAllHistory(): Flow<List<ZakatHistoryEntity>> {
        return zakatDao.getAllHistory()
    }

    override suspend fun insertCalculation(entry: ZakatHistoryEntity): Long {
        return zakatDao.insertCalculation(entry)
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
