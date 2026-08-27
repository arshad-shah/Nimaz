package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ZakatRepositoryImplTest {

    private lateinit var dao: ZakatDao
    private lateinit var repository: ZakatRepositoryImpl

    private val now = System.currentTimeMillis()

    private fun makeEntity(id: Long = 1L, isPaid: Boolean = false) = ZakatHistoryEntity(
        id = id, calculatedAt = now, totalAssets = 10000.0, totalLiabilities = 0.0,
        netWorth = 10000.0, zakatDue = 250.0, nisabType = "GOLD",
        nisabValue = 5000.0, isPaid = isPaid, paidAt = null, notes = null
    )

    private fun makeEntry(id: Long = 1L) = ZakatHistoryEntry(
        id = id, calculatedAt = now, totalAssets = 10000.0, totalLiabilities = 0.0,
        netWorth = 10000.0, zakatDue = 250.0, nisabType = NisabType.GOLD,
        nisabValue = 5000.0, isPaid = false, paidAt = null, notes = null
    )

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = ZakatRepositoryImpl(dao)
    }

    @Test
    fun `getAllHistory returns mapped domain entries`() = runTest {
        every { dao.getAllHistory() } returns flowOf(listOf(makeEntity(1L), makeEntity(2L)))

        val result = repository.getAllHistory().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1L)
        assertThat(result[0].nisabType).isEqualTo(NisabType.GOLD)
        assertThat(result[0].zakatDue).isEqualTo(250.0)
    }

    @Test
    fun `getAllHistory returns empty when no records`() = runTest {
        every { dao.getAllHistory() } returns flowOf(emptyList())

        val result = repository.getAllHistory().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `insertCalculation converts domain to entity and returns id`() = runTest {
        coEvery { dao.insertCalculation(any()) } returns 42L

        val id = repository.insertCalculation(makeEntry())

        assertThat(id).isEqualTo(42L)
        coVerify {
            dao.insertCalculation(match { entity ->
                entity.totalAssets == 10000.0 && entity.zakatDue == 250.0 &&
                    entity.nisabType == "GOLD"
            })
        }
    }

    @Test
    fun `markAsPaid delegates to dao`() = runTest {
        repository.markAsPaid(1L, now)
        coVerify { dao.markAsPaid(1L, now) }
    }

    @Test
    fun `getTotalPaid returns total from dao`() = runTest {
        coEvery { dao.getTotalPaid() } returns 500.0

        assertThat(repository.getTotalPaid()).isEqualTo(500.0)
    }

    @Test
    fun `getTotalPaid returns 0 when dao returns null`() = runTest {
        coEvery { dao.getTotalPaid() } returns null

        assertThat(repository.getTotalPaid()).isEqualTo(0.0)
    }

    @Test
    fun `deleteCalculation delegates to dao`() = runTest {
        repository.deleteCalculation(3L)
        coVerify { dao.deleteCalculation(3L) }
    }

    @Test
    fun `entity isPaid flag maps to domain`() = runTest {
        every { dao.getAllHistory() } returns flowOf(listOf(makeEntity(1L, isPaid = true)))

        val result = repository.getAllHistory().first()

        assertThat(result[0].isPaid).isTrue()
    }

    @Test
    fun `entity with SILVER nisabType maps correctly`() = runTest {
        every { dao.getAllHistory() } returns flowOf(
            listOf(makeEntity().copy(nisabType = "SILVER"))
        )

        val result = repository.getAllHistory().first()

        assertThat(result[0].nisabType).isEqualTo(NisabType.SILVER)
    }
}
