package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.NisabType
import com.arshadshah.nimaz.domain.model.ZakatHistoryEntry
import com.arshadshah.nimaz.domain.repository.ZakatRepository
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

class ZakatUseCasesTest {

    private lateinit var repo: ZakatRepository
    private lateinit var useCases: ZakatUseCases

    private val now = System.currentTimeMillis()

    private fun makeEntry(id: Long = 1L, isPaid: Boolean = false) = ZakatHistoryEntry(
        id = id, calculatedAt = now, totalAssets = 10000.0,
        totalLiabilities = 0.0, netWorth = 10000.0,
        zakatDue = 250.0, nisabType = NisabType.GOLD,
        nisabValue = 5000.0, isPaid = isPaid, paidAt = null, notes = null
    )

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        useCases = ZakatUseCases(
            getAllHistory = GetAllHistoryUseCase(repo),
            insertCalculation = InsertCalculationUseCase(repo),
            markAsPaid = MarkAsPaidUseCase(repo),
            getTotalPaid = GetTotalPaidUseCase(repo),
            deleteCalculation = DeleteCalculationUseCase(repo)
        )
    }

    @Test
    fun `getAllHistory returns flow of entries`() = runTest {
        val entries = listOf(makeEntry(1L), makeEntry(2L))
        every { repo.getAllHistory() } returns flowOf(entries)

        val result = useCases.getAllHistory().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].zakatDue).isEqualTo(250.0)
    }

    @Test
    fun `getAllHistory returns empty flow when no history`() = runTest {
        every { repo.getAllHistory() } returns flowOf(emptyList())

        val result = useCases.getAllHistory().first()

        assertThat(result).isEmpty()
    }

    @Test
    fun `insertCalculation delegates to repo and returns id`() = runTest {
        val entry = makeEntry()
        coEvery { repo.insertCalculation(entry) } returns 42L

        val id = useCases.insertCalculation(entry)

        assertThat(id).isEqualTo(42L)
        coVerify { repo.insertCalculation(entry) }
    }

    @Test
    fun `markAsPaid delegates with id and timestamp`() = runTest {
        useCases.markAsPaid(1L, now)
        coVerify { repo.markAsPaid(1L, now) }
    }

    @Test
    fun `getTotalPaid returns total from repo`() = runTest {
        coEvery { repo.getTotalPaid() } returns 750.0

        val total = useCases.getTotalPaid()

        assertThat(total).isEqualTo(750.0)
    }

    @Test
    fun `getTotalPaid returns zero when nothing paid`() = runTest {
        coEvery { repo.getTotalPaid() } returns 0.0

        assertThat(useCases.getTotalPaid()).isEqualTo(0.0)
    }

    @Test
    fun `deleteCalculation delegates to repo`() = runTest {
        useCases.deleteCalculation(5L)
        coVerify { repo.deleteCalculation(5L) }
    }
}
