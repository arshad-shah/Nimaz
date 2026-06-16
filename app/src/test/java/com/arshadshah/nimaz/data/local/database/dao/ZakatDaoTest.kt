package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.entity.ZakatHistoryEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Room DAO tests for [ZakatDao] using an in-memory database under Robolectric.
 * Verifies the custom SQL — ordering, the conditional SUM for paid zakat, and
 * the paid-flag update — against a real (in-memory) SQLite engine rather than a
 * mock, which is the only way these queries get exercised.
 */
@RunWith(RobolectricTestRunner::class)
class ZakatDaoTest {

    private lateinit var database: NimazDatabase
    private lateinit var dao: ZakatDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.zakatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        calculatedAt: Long,
        zakatDue: Double = 250.0,
        isPaid: Boolean = false
    ) = ZakatHistoryEntity(
        id = 0, calculatedAt = calculatedAt, totalAssets = 10_000.0,
        totalLiabilities = 0.0, netWorth = 10_000.0, zakatDue = zakatDue,
        nisabType = "GOLD", nisabValue = 5_000.0, isPaid = isPaid
    )

    @Test
    fun `insertCalculation returns a positive row id`() = runTest {
        val id = dao.insertCalculation(entity(calculatedAt = 100))
        assertThat(id).isGreaterThan(0L)
    }

    @Test
    fun `getAllHistory returns rows ordered by calculatedAt descending`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 100))
        dao.insertCalculation(entity(calculatedAt = 300))
        dao.insertCalculation(entity(calculatedAt = 200))

        val history = dao.getAllHistory().first()

        assertThat(history.map { it.calculatedAt }).containsExactly(300L, 200L, 100L).inOrder()
    }

    @Test
    fun `getTotalPaid is null when nothing has been paid`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 100, isPaid = false))
        // SQLite SUM over zero matching rows yields NULL.
        assertThat(dao.getTotalPaid()).isNull()
    }

    @Test
    fun `getTotalPaid sums only the paid calculations`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 100, zakatDue = 100.0, isPaid = true))
        dao.insertCalculation(entity(calculatedAt = 200, zakatDue = 200.0, isPaid = true))
        dao.insertCalculation(entity(calculatedAt = 300, zakatDue = 999.0, isPaid = false))

        assertThat(dao.getTotalPaid()).isEqualTo(300.0)
    }

    @Test
    fun `markAsPaid flips the paid flag and records the timestamp`() = runTest {
        val id = dao.insertCalculation(entity(calculatedAt = 100, zakatDue = 150.0))

        dao.markAsPaid(id, paidAt = 555L)

        val row = dao.getAllHistory().first().single()
        assertThat(row.isPaid).isTrue()
        assertThat(row.paidAt).isEqualTo(555L)
        assertThat(dao.getTotalPaid()).isEqualTo(150.0)
    }

    @Test
    fun `deleteCalculation removes the targeted row`() = runTest {
        val keep = dao.insertCalculation(entity(calculatedAt = 100))
        val drop = dao.insertCalculation(entity(calculatedAt = 200))

        dao.deleteCalculation(drop)

        val remaining = dao.getAllHistory().first()
        assertThat(remaining.map { it.id }).containsExactly(keep)
    }

    @Test
    fun `bulk insert and sync read round-trip the rows`() = runTest {
        dao.insertCalculations(
            listOf(entity(calculatedAt = 100), entity(calculatedAt = 200))
        )
        assertThat(dao.getAllHistorySync()).hasSize(2)
    }

    @Test
    fun `deleteAllUserData clears the table`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 100))
        dao.insertCalculation(entity(calculatedAt = 200))

        dao.deleteAllUserData()

        assertThat(dao.getAllHistory().first()).isEmpty()
    }
}
