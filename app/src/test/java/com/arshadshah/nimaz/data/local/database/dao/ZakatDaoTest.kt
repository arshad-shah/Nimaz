package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Instrumented-style DAO tests backed by a real in-memory Room database running
 * under Robolectric (so they execute in the standard unit-test lane). These
 * exercise the actual SQL — ordering, the conditional SUM, the partial UPDATE,
 * REPLACE-on-conflict and deletes — which the mock-DAO repository tests cannot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZakatDaoTest {

    private lateinit var db: NimazDatabase
    private lateinit var dao: ZakatDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, NimazDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.zakatDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: Long = 0,
        calculatedAt: Long = 1000,
        zakatDue: Double = 250.0,
        isPaid: Boolean = false,
        paidAt: Long? = null
    ) = ZakatHistoryEntity(
        id = id, calculatedAt = calculatedAt, totalAssets = 10_000.0,
        totalLiabilities = 0.0, netWorth = 10_000.0, zakatDue = zakatDue,
        nisabType = "GOLD", nisabValue = 5_686.2, isPaid = isPaid, paidAt = paidAt
    )

    @Test
    fun `insert returns a row id and the row is retrievable`() = runTest {
        val id = dao.insertCalculation(entity())
        assertThat(id).isGreaterThan(0L)

        val all = dao.getAllHistorySync()
        assertThat(all).hasSize(1)
        assertThat(all.first().id).isEqualTo(id)
        assertThat(all.first().zakatDue).isWithin(1e-9).of(250.0)
    }

    @Test
    fun `getAllHistory is ordered by calculatedAt descending`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 1000))
        dao.insertCalculation(entity(calculatedAt = 3000))
        dao.insertCalculation(entity(calculatedAt = 2000))

        val ordered = dao.getAllHistory().first()
        assertThat(ordered.map { it.calculatedAt }).containsExactly(3000L, 2000L, 1000L).inOrder()
    }

    @Test
    fun `getTotalPaid is null when nothing is paid and sums only paid rows`() = runTest {
        dao.insertCalculation(entity(zakatDue = 100.0, isPaid = false))
        dao.insertCalculation(entity(zakatDue = 200.0, isPaid = false))

        // SUM() over zero matching rows returns NULL.
        assertThat(dao.getTotalPaid()).isNull()

        val paidId = dao.insertCalculation(entity(zakatDue = 50.0, isPaid = false))
        dao.markAsPaid(paidId, paidAt = 9999)

        assertThat(dao.getTotalPaid()).isWithin(1e-9).of(50.0)
    }

    @Test
    fun `markAsPaid flips the paid flag and stamps paidAt`() = runTest {
        val id = dao.insertCalculation(entity(isPaid = false))

        dao.markAsPaid(id, paidAt = 12345)

        val row = dao.getAllHistorySync().first { it.id == id }
        assertThat(row.isPaid).isTrue()
        assertThat(row.paidAt).isEqualTo(12345)
    }

    @Test
    fun `deleteCalculation removes only the targeted row`() = runTest {
        val keep = dao.insertCalculation(entity(calculatedAt = 1000))
        val remove = dao.insertCalculation(entity(calculatedAt = 2000))

        dao.deleteCalculation(remove)

        val ids = dao.getAllHistorySync().map { it.id }
        assertThat(ids).containsExactly(keep)
    }

    @Test
    fun `insertCalculations replaces rows on primary key conflict`() = runTest {
        val id = dao.insertCalculation(entity(zakatDue = 100.0))

        // Same id, different value -> REPLACE strategy overwrites.
        dao.insertCalculations(listOf(entity(id = id, zakatDue = 999.0)))

        val all = dao.getAllHistorySync()
        assertThat(all).hasSize(1)
        assertThat(all.first().zakatDue).isWithin(1e-9).of(999.0)
    }

    @Test
    fun `deleteAllUserData clears the table`() = runTest {
        dao.insertCalculation(entity(calculatedAt = 1000))
        dao.insertCalculation(entity(calculatedAt = 2000))

        dao.deleteAllUserData()

        assertThat(dao.getAllHistorySync()).isEmpty()
    }
}
