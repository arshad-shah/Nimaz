package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.FastRecordEntity
import com.arshadshah.nimaz.data.local.database.entity.MakeupFastEntity
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A fasting day, and the debt a missed one leaves behind.
 *
 * Two shapes are worth pinning down here. A fast record is one row per *day* — the unique index
 * says so, and re-logging a day has to replace rather than accumulate. And a makeup fast leaves
 * the `pending` list by two different doors, completed or paid as fidya, each of which writes a
 * different column; a query that forgets one of them shows a debt the person has already settled.
 */
@RunWith(RobolectricTestRunner::class)
class FastingDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: FastingDao

    private val day1 = 1_000L
    private val day2 = 2_000L
    private val day3 = 3_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.fastingDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- One record per day ----

    @Test
    fun `re-logging a day replaces that day's record`() = runTest {
        dao.insertFastRecord(fast(day1, status = "not_fasted"))
        dao.insertFastRecord(fast(day1, status = "fasted"))

        assertThat(dao.getFastRecordsInRange(day1, day1).first()).hasSize(1)
        assertThat(dao.getFastRecordForDate(day1)?.status).isEqualTo("fasted")
    }

    @Test
    fun `changing a day's status leaves the rest of the row alone`() = runTest {
        dao.insertFastRecord(fast(day1, status = "not_fasted", fastType = "ramadan", note = "ill"))

        dao.updateFastStatus(day1, status = "exempted", timestamp = 555)

        val record = dao.getFastRecordForDate(day1)
        assertThat(record?.status).isEqualTo("exempted")
        assertThat(record?.fastType).isEqualTo("ramadan")
        assertThat(record?.note).isEqualTo("ill")
        assertThat(record?.updatedAt).isEqualTo(555)
    }

    @Test
    fun `a range is ordered oldest first and stops at its ends`() = runTest {
        dao.insertFastRecords(listOf(fast(day3), fast(day1), fast(day2)))

        assertThat(dao.getFastRecordsInRange(day1, day2).first().map { it.date })
            .containsExactly(day1, day2).inOrder()
        assertThat(dao.getAllFastRecords().map { it.date })
            .containsExactly(day1, day2, day3).inOrder()
    }

    @Test
    fun `records are filtered by hijri month, type and status`() = runTest {
        dao.insertFastRecords(
            listOf(
                fast(day1, hijriMonth = 9, fastType = "ramadan", status = "fasted"),
                fast(day2, hijriMonth = 9, fastType = "ramadan", status = "not_fasted"),
                fast(day3, hijriMonth = 10, fastType = "voluntary", status = "fasted"),
            )
        )

        assertThat(dao.getFastRecordsByHijriMonth(9).first().map { it.date })
            .containsExactly(day1, day2).inOrder()
        assertThat(dao.getFastRecordsByType("voluntary").first().map { it.date })
            .containsExactly(day3)
        assertThat(dao.getFastRecordsByStatus("fasted").first().map { it.date })
            .containsExactly(day3, day1).inOrder()
    }

    @Test
    fun `deleting a day removes only that day`() = runTest {
        dao.insertFastRecords(listOf(fast(day1), fast(day2)))

        dao.deleteFastRecordByDate(day1)

        assertThat(dao.getAllFastRecords().map { it.date }).containsExactly(day2)
    }

    // ---- Makeup fasts ----

    @Test
    fun `a makeup fast is pending until it is completed`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day1))
        val id = dao.getAllMakeupFastsSync().single().id

        assertThat(dao.getPendingMakeupFastCount().first()).isEqualTo(1)

        dao.markMakeupFastCompleted(id, completedDate = day3, timestamp = 555)

        val done = dao.getMakeupFastById(id)
        assertThat(done?.status).isEqualTo("completed")
        assertThat(done?.completedDate).isEqualTo(day3)
        assertThat(done?.updatedAt).isEqualTo(555)
        assertThat(dao.getPendingMakeupFasts().first()).isEmpty()
        assertThat(dao.getPendingMakeupFastCount().first()).isEqualTo(0)
    }

    @Test
    fun `paying fidya settles a makeup fast without completing it`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day1))
        val id = dao.getAllMakeupFastsSync().single().id

        dao.markFidyaPaid(id, amount = 5.0, timestamp = 555)

        val settled = dao.getMakeupFastById(id)
        assertThat(settled?.status).isEqualTo("fidya_paid")
        assertThat(settled?.fidyaAmount).isEqualTo(5.0)
        // Fidya is not a fast that was made up — the completion date stays empty.
        assertThat(settled?.completedDate).isNull()
        assertThat(dao.getPendingMakeupFasts().first()).isEmpty()
    }

    @Test
    fun `pending makeup fasts are listed oldest debt first`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day3))
        dao.insertMakeupFast(makeup(originalDate = day1))
        dao.insertMakeupFast(makeup(originalDate = day2, status = "completed"))

        assertThat(dao.getPendingMakeupFasts().first().map { it.originalDate })
            .containsExactly(day1, day3).inOrder()
        // The full list is newest first — it is a history, not a queue.
        assertThat(dao.getAllMakeupFasts().first().map { it.originalDate })
            .containsExactly(day3, day2, day1).inOrder()
    }

    @Test
    fun `a day already owed can be recognised before a second debt is logged for it`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day1))

        assertThat(dao.getMakeupFastCountForDate(day1)).isEqualTo(1)
        assertThat(dao.getMakeupFastCountForDate(day2)).isEqualTo(0)
    }

    // ---- Statistics ----

    @Test
    fun `counts distinguish ramadan from voluntary fasts`() = runTest {
        dao.insertFastRecords(
            listOf(
                fast(day1, hijriMonth = 9, fastType = "ramadan", status = "fasted"),
                fast(day2, hijriMonth = 9, fastType = "ramadan", status = "not_fasted"),
                fast(day3, hijriMonth = 10, fastType = "voluntary", status = "fasted"),
            )
        )

        assertThat(dao.getRamadanFastedCount()).isEqualTo(1)
        assertThat(dao.getVoluntaryFastCount()).isEqualTo(1)
        assertThat(dao.getFastedCountInRange(day1, day3)).isEqualTo(2)
        assertThat(dao.getFastedCountInRange(day1, day2)).isEqualTo(1)
    }

    @Test
    fun `recent fasted records stop at today and come back newest first`() = runTest {
        dao.insertFastRecords(
            listOf(
                fast(day1, status = "fasted"),
                fast(day2, status = "not_fasted"),
                fast(day3, status = "fasted"),
            )
        )

        // The streak is walked backwards from today, so a fast logged for a future day must not
        // be at the head of the list.
        assertThat(dao.getRecentFastedRecords(todayTimestamp = day2).map { it.date })
            .containsExactly(day1)
        assertThat(dao.getRecentFastedRecords(todayTimestamp = day3).map { it.date })
            .containsExactly(day3, day1).inOrder()
    }

    @Test
    fun `fidya paid sums only settled debts, and is null before any`() = runTest {
        assertThat(dao.getTotalFidyaPaid()).isNull()

        dao.insertMakeupFast(makeup(originalDate = day1))
        dao.insertMakeupFast(makeup(originalDate = day2))
        val ids = dao.getAllMakeupFastsSync().map { it.id }
        dao.markFidyaPaid(ids[0], amount = 5.0)
        dao.markFidyaPaid(ids[1], amount = 2.5)

        assertThat(dao.getTotalFidyaPaid()).isEqualTo(7.5)
    }

    @Test
    fun `deleting all user data empties both fasting tables`() = runTest {
        dao.insertFastRecord(fast(day1))
        dao.insertMakeupFast(makeup(originalDate = day1))

        dao.deleteAllUserData()

        assertThat(dao.getAllFastRecords()).isEmpty()
        assertThat(dao.getAllMakeupFastsSync()).isEmpty()
    }

    // ---- The timestamps nobody passes ----

    @Test
    fun `changing a status without naming a moment stamps it now`() = runTest {
        dao.insertFastRecord(fast(day1, status = "not_fasted"))
        val before = System.currentTimeMillis()

        // The app never passes the timestamp; the tests above do, so the default is the branch
        // nothing exercised.
        dao.updateFastStatus(day1, status = "fasted")

        assertThat(dao.getFastRecordForDate(day1)?.status).isEqualTo("fasted")
        assertThat(dao.getFastRecordForDate(day1)?.updatedAt).isAtLeast(before)
    }

    @Test
    fun `completing a makeup fast without a moment stamps it now`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day1))
        val id = dao.getAllMakeupFastsSync().single().id
        val before = System.currentTimeMillis()

        dao.markMakeupFastCompleted(id, completedDate = day3)

        assertThat(dao.getMakeupFastById(id)?.status).isEqualTo("completed")
        assertThat(dao.getMakeupFastById(id)?.updatedAt).isAtLeast(before)
    }

    @Test
    fun `paying fidya without a moment stamps it now`() = runTest {
        dao.insertMakeupFast(makeup(originalDate = day1))
        val id = dao.getAllMakeupFastsSync().single().id
        val before = System.currentTimeMillis()

        dao.markFidyaPaid(id, amount = 5.0)

        assertThat(dao.getMakeupFastById(id)?.updatedAt).isAtLeast(before)
    }

    @Test
    fun `editing a record wholesale keeps it a single day`() = runTest {
        dao.insertFastRecord(fast(day1, status = "not_fasted"))
        val stored = dao.getFastRecordForDate(day1)!!

        dao.updateFastRecord(stored.copy(status = "exempted", exemptionReason = "travel"))

        assertThat(dao.getAllFastRecords()).hasSize(1)
        assertThat(dao.getFastRecordForDate(day1)?.exemptionReason).isEqualTo("travel")
    }

    private fun fast(
        date: Long,
        status: String = "fasted",
        fastType: String = "voluntary",
        hijriMonth: Int? = null,
        note: String? = null,
    ) = FastRecordEntity(
        id = 0,
        date = date,
        hijriDate = null,
        hijriMonth = hijriMonth,
        hijriYear = null,
        fastType = fastType,
        status = status,
        exemptionReason = null,
        suhoorTime = null,
        iftarTime = null,
        note = note,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun makeup(
        originalDate: Long,
        status: String = "pending",
    ) = MakeupFastEntity(
        id = 0,
        originalDate = originalDate,
        originalHijriDate = null,
        reason = "travel",
        status = status,
        completedDate = null,
        fidyaAmount = null,
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
