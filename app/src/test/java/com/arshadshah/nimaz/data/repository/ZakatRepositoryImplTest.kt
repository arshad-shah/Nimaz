package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ZakatRepositoryImpl]. Mostly thin DAO delegation; the one
 * piece of real logic is [ZakatRepositoryImpl.getTotalPaid] coalescing the
 * DAO's nullable SUM() result to 0.0 (the SUM is null when no rows are paid).
 */
class ZakatRepositoryImplTest {

    private lateinit var dao: ZakatDao
    private lateinit var repository: ZakatRepositoryImpl

    private fun historyEntity(id: Long = 1) = ZakatHistoryEntity(
        id = id, calculatedAt = 1000, totalAssets = 10_000.0, totalLiabilities = 0.0,
        netWorth = 10_000.0, zakatDue = 250.0, nisabType = "GOLD", nisabValue = 5_686.2
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = ZakatRepositoryImpl(dao)
    }

    @Test
    fun `getAllHistory delegates to the dao flow`() = runTest {
        val entities = listOf(historyEntity(1), historyEntity(2))
        every { dao.getAllHistory() } returns flowOf(entities)

        assertThat(repository.getAllHistory().first()).isEqualTo(entities)
    }

    @Test
    fun `insertCalculation returns the new row id from the dao`() = runTest {
        val entity = historyEntity()
        coEvery { dao.insertCalculation(entity) } returns 42L

        assertThat(repository.insertCalculation(entity)).isEqualTo(42L)
        coVerify { dao.insertCalculation(entity) }
    }

    @Test
    fun `getTotalPaid returns the dao sum when present`() = runTest {
        coEvery { dao.getTotalPaid() } returns 250.0
        assertThat(repository.getTotalPaid()).isWithin(1e-9).of(250.0)
    }

    @Test
    fun `getTotalPaid coalesces a null dao sum to zero`() = runTest {
        // SUM() returns null when there are no paid rows.
        coEvery { dao.getTotalPaid() } returns null
        assertThat(repository.getTotalPaid()).isEqualTo(0.0)
    }

    @Test
    fun `markAsPaid and deleteCalculation delegate to the dao`() = runTest {
        repository.markAsPaid(5L, 9999L)
        repository.deleteCalculation(7L)

        coVerify { dao.markAsPaid(5L, 9999L) }
        coVerify { dao.deleteCalculation(7L) }
    }
}
