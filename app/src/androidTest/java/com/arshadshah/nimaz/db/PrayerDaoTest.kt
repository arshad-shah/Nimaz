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

/**
 * Round-trip coverage for the prayer-tracker persistence: the table that records
 * whether each of the five daily prayers was prayed, on time, in congregation, etc.
 */
@RunWith(AndroidJUnit4::class)
class PrayerDaoTest {

    @get:Rule
    val dbRule = NimazDbRule()
    private val dao get() = dbRule.userDb.prayerDao()

    @Test
    fun insertedDailyPrayers_areReadBackForThatDate() = runTest {
        dao.insertPrayerRecords(TestData.dailyPrayers(date = TestData.DAY))

        val records = dao.getPrayerRecordsForDate(TestData.DAY).first()

        assertThat(records).hasSize(5)
        assertThat(records.map { it.prayerName })
            .containsExactly("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    @Test
    fun getPrayerRecord_returnsTheMatchingPrayer() = runTest {
        dao.insertPrayerRecords(TestData.dailyPrayers())

        val fajr = dao.getPrayerRecord(TestData.DAY, "Fajr")

        assertThat(fajr).isNotNull()
        assertThat(fajr!!.prayerName).isEqualTo("Fajr")
        assertThat(fajr.status).isEqualTo("pending")
    }

    @Test
    fun updatePrayerStatus_marksPrayerAsPrayedInCongregation() = runTest {
        dao.insertPrayerRecord(TestData.prayerRecord(prayerName = "Asr"))

        dao.updatePrayerStatus(TestData.DAY, "Asr", "prayed", TestData.T0, /* isJamaah = */ true)

        val asr = dao.getPrayerRecord(TestData.DAY, "Asr")!!
        assertThat(asr.status).isEqualTo("prayed")
        assertThat(asr.isJamaah).isTrue()
        assertThat(asr.prayedAt).isEqualTo(TestData.T0)
    }

    @Test
    fun rangeQueries_andSyncQuery_seeAllInsertedRecords() = runTest {
        dao.insertPrayerRecords(TestData.dailyPrayers())

        val inRange = dao.getPrayerRecordsInRange(TestData.DAY - 1, TestData.DAY + 1).first()
        val sync = dao.getPrayerRecordsForDateSync(TestData.DAY)

        assertThat(inRange).hasSize(5)
        assertThat(sync).hasSize(5)
        assertThat(dao.getAllPrayerRecords()).hasSize(5)
    }

    @Test
    fun deleteAllUserData_clearsTheTable() = runTest {
        dao.insertPrayerRecords(TestData.dailyPrayers())

        dao.deleteAllUserData()

        assertThat(dao.getAllPrayerRecords()).isEmpty()
    }
}
