package com.arshadshah.nimaz.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.support.NimazDbRule
import com.arshadshah.nimaz.support.TestData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Zakat calculation history and payment tracking. */
@RunWith(AndroidJUnit4::class)
class ZakatDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.userDb.zakatDao()

    @Test
    fun insertCalculation_appearsInHistory() = runTest {
        val id = dao.insertCalculation(TestData.zakat(zakatDue = 250.0))

        assertThat(id).isGreaterThan(0L)
        val history = dao.getAllHistory().first()
        assertThat(history).hasSize(1)
        assertThat(history.first().zakatDue).isEqualTo(250.0)
    }

    @Test
    fun markAsPaid_countsTowardTotalPaid() = runTest {
        val id = dao.insertCalculation(TestData.zakat(zakatDue = 300.0, isPaid = false))

        dao.markAsPaid(id, paidAt = TestData.T0)

        assertThat(dao.getTotalPaid()).isEqualTo(300.0)
    }

    @Test
    fun deleteCalculation_removesEntry() = runTest {
        val id = dao.insertCalculation(TestData.zakat())

        dao.deleteCalculation(id)

        assertThat(dao.getAllHistory().first()).isEmpty()
    }

    @Test
    fun deleteAllUserData_clearsHistory() = runTest {
        dao.insertCalculations(listOf(TestData.zakat(), TestData.zakat(calculatedAt = TestData.T0 + 1)))

        dao.deleteAllUserData()

        assertThat(dao.getAllHistorySync()).isEmpty()
    }
}
