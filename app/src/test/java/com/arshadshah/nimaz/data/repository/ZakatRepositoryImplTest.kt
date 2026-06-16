package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ZakatRepositoryImplTest {

    private lateinit var dao: ZakatDao
    private lateinit var repository: ZakatRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = ZakatRepositoryImpl(dao)
    }

    private fun entity(id: Long = 1, zakatDue: Double = 250.0) = ZakatHistoryEntity(
        id = id, calculatedAt = 1_000L, totalAssets = 10_000.0, totalLiabilities = 0.0,
        netWorth = 10_000.0, zakatDue = zakatDue, nisabType = "GOLD", nisabValue = 5_000.0
    )

    @Test
    fun `getAllHistory forwards the dao flow`() = runTest {
        every { dao.getAllHistory() } returns flowOf(listOf(entity(id = 3)))

        repository.getAllHistory().test {
            assertThat(awaitItem().single().id).isEqualTo(3)
            awaitComplete()
        }
    }

    @Test
    fun `insertCalculation returns the new row id from the dao`() = runTest {
        coEvery { dao.insertCalculation(any()) } returns 99L

        val id = repository.insertCalculation(entity())

        assertThat(id).isEqualTo(99L)
        coVerify { dao.insertCalculation(any()) }
    }

    @Test
    fun `getTotalPaid returns the dao sum when present`() = runTest {
        coEvery { dao.getTotalPaid() } returns 750.0
        assertThat(repository.getTotalPaid()).isEqualTo(750.0)
    }

    @Test
    fun `getTotalPaid falls back to zero when the dao returns null`() = runTest {
        // SUM over zero paid rows is null in SQLite; the repo must coalesce to 0.
        coEvery { dao.getTotalPaid() } returns null
        assertThat(repository.getTotalPaid()).isEqualTo(0.0)
    }

    @Test
    fun `markAsPaid delegates to the dao`() = runTest {
        repository.markAsPaid(5L, 1_234L)
        coVerify { dao.markAsPaid(5L, 1_234L) }
    }

    @Test
    fun `deleteCalculation delegates to the dao`() = runTest {
        repository.deleteCalculation(7L)
        coVerify { dao.deleteCalculation(7L) }
    }
}
