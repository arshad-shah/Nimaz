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

/** Fasting tracker + make-up (qada) fast persistence. */
@RunWith(AndroidJUnit4::class)
class FastingDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.userDb.fastingDao()

    @Test
    fun insertFastRecord_isReadBackByDate() = runTest {
        dao.insertFastRecord(TestData.fastRecord(date = TestData.DAY, status = "fasted"))

        val record = dao.getFastRecordForDate(TestData.DAY)

        assertThat(record).isNotNull()
        assertThat(record!!.status).isEqualTo("fasted")
        assertThat(record.fastType).isEqualTo("ramadan")
    }

    @Test
    fun updateFastStatus_changesStatus() = runTest {
        dao.insertFastRecord(TestData.fastRecord(status = "pending"))

        dao.updateFastStatus(TestData.DAY, "fasted")

        assertThat(dao.getFastRecordForDate(TestData.DAY)!!.status).isEqualTo("fasted")
    }

    @Test
    fun recordsInRange_andByMonth_areQueryable() = runTest {
        dao.insertFastRecords(
            listOf(
                TestData.fastRecord(date = TestData.DAY, hijriMonth = 9),
                TestData.fastRecord(date = TestData.DAY + 86_400_000L, hijriMonth = 9),
            )
        )

        assertThat(dao.getFastRecordsInRange(TestData.DAY - 1, TestData.DAY + 86_400_001L).first())
            .hasSize(2)
        assertThat(dao.getFastRecordsByHijriMonth(9).first()).hasSize(2)
    }

    @Test
    fun makeupFast_insertsAndCompletes() = runTest {
        dao.insertMakeupFast(TestData.makeupFast(status = "pending"))
        val pending = dao.getPendingMakeupFasts().first()
        assertThat(pending).hasSize(1)

        dao.markMakeupFastCompleted(pending.first().id, completedDate = TestData.DAY)

        assertThat(dao.getPendingMakeupFastCount().first()).isEqualTo(0)
    }

    @Test
    fun deleteAllUserData_clearsFastsAndMakeups() = runTest {
        dao.insertFastRecord(TestData.fastRecord())
        dao.insertMakeupFast(TestData.makeupFast())

        dao.deleteAllUserData()

        assertThat(dao.getAllFastRecords()).isEmpty()
        assertThat(dao.getAllMakeupFastsSync()).isEmpty()
    }
}
