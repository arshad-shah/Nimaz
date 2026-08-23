package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
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
 * What the prayer table holds after a day of logging, and what the review banner is allowed to
 * change.
 *
 * [PrayerDao.markUnrecordedAsMissed] is the one worth reading twice: it is a bulk `UPDATE` over
 * a date range, so every way it can be wrong writes plausible rows rather than failing — a
 * `prayed` overwritten as missed, sunrise landing in the qada list, or a neighbouring day caught
 * by an off-by-one in the range. The statistics queries are here for the same reason: a `COUNT`
 * with a wrong `WHERE` is a number, not an error.
 */
@RunWith(RobolectricTestRunner::class)
class PrayerDaoTest {

    private lateinit var db: NimazUserDatabase
    private lateinit var dao: PrayerDao

    private val day1 = 1_000L
    private val day2 = 2_000L
    private val day3 = 3_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.prayerDao()
    }

    @After
    fun tearDown() = db.close()

    // ---- One row per prayer per day ----

    @Test
    fun `re-inserting a day's prayer replaces it instead of duplicating it`() = runTest {
        dao.insertPrayerRecord(record(day1, "fajr", status = "pending", scheduledTime = 10))
        dao.insertPrayerRecord(record(day1, "fajr", status = "prayed", scheduledTime = 10))

        // (date, prayerName) is a unique index and the insert is REPLACE, so re-seeding a day
        // that was already logged must not leave two rows behind for it.
        assertThat(dao.getPrayerRecordsForDateSync(day1)).hasSize(1)
        assertThat(dao.getPrayerRecord(day1, "fajr")?.status).isEqualTo("prayed")
    }

    @Test
    fun `a day's prayers come back in the order they fall`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "isha", scheduledTime = 50),
                record(day1, "fajr", scheduledTime = 10),
                record(day1, "asr", scheduledTime = 30),
            )
        )

        assertThat(dao.getPrayerRecordsForDate(day1).first().map { it.prayerName })
            .containsExactly("fajr", "asr", "isha").inOrder()
    }

    @Test
    fun `a range is ordered by day and then by time within the day`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day2, "fajr", scheduledTime = 10),
                record(day1, "isha", scheduledTime = 50),
                record(day1, "fajr", scheduledTime = 10),
            )
        )

        assertThat(dao.getPrayerRecordsInRangeSync(day1, day2).map { it.date to it.prayerName })
            .containsExactly(day1 to "fajr", day1 to "isha", day2 to "fajr").inOrder()
        assertThat(dao.getPrayerRecordsInRange(day1, day1).first()).hasSize(2)
    }

    // ---- Logging a prayer ----

    @Test
    fun `logging a prayer records how and when, and only for that prayer`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "pending", scheduledTime = 10),
                record(day1, "dhuhr", status = "pending", scheduledTime = 20),
            )
        )

        dao.updatePrayerStatus(
            date = day1,
            prayerName = "fajr",
            status = "prayed",
            prayedAt = 111,
            isJamaah = true,
            timestamp = 999,
        )

        val fajr = dao.getPrayerRecord(day1, "fajr")
        assertThat(fajr?.status).isEqualTo("prayed")
        assertThat(fajr?.prayedAt).isEqualTo(111)
        assertThat(fajr?.isJamaah).isTrue()
        assertThat(fajr?.updatedAt).isEqualTo(999)
        assertThat(dao.getPrayerRecord(day1, "dhuhr")?.status).isEqualTo("pending")
    }

    @Test
    fun `missed prayers awaiting qada exclude the ones already made up`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "missed", scheduledTime = 10),
                record(day2, "asr", status = "missed", scheduledTime = 30, isQadaFor = day1),
                record(day2, "isha", status = "prayed", scheduledTime = 50),
            )
        )

        assertThat(dao.getMissedPrayersRequiringQada().first().map { it.prayerName })
            .containsExactly("fajr")
        assertThat(dao.getPrayerRecordsByStatus("missed").first()).hasSize(2)
    }

    // ---- Confirming a range as missed ----

    @Test
    fun `confirming a range only touches prayers nobody logged`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "pending", scheduledTime = 10),
                record(day1, "dhuhr", status = "not_prayed", scheduledTime = 20),
                record(day1, "asr", status = "prayed", scheduledTime = 30),
                record(day1, "maghrib", status = "late", scheduledTime = 40),
                record(day1, "isha", status = "qada", scheduledTime = 50),
            )
        )

        val changed = dao.markUnrecordedAsMissed(from = day1, to = day1, timestamp = 777)

        assertThat(changed).isEqualTo(2)
        assertThat(dao.getPrayerRecord(day1, "fajr")?.status).isEqualTo("missed")
        assertThat(dao.getPrayerRecord(day1, "dhuhr")?.status).isEqualTo("missed")
        // An assertion the user already made is never overwritten — the invariant the whole
        // review banner rests on.
        assertThat(dao.getPrayerRecord(day1, "asr")?.status).isEqualTo("prayed")
        assertThat(dao.getPrayerRecord(day1, "maghrib")?.status).isEqualTo("late")
        assertThat(dao.getPrayerRecord(day1, "isha")?.status).isEqualTo("qada")
    }

    @Test
    fun `confirming a range never marks sunrise missed`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "sunrise", status = "pending", scheduledTime = 15),
                record(day1, "fajr", status = "pending", scheduledTime = 10),
            )
        )

        dao.markUnrecordedAsMissed(from = day1, to = day1)

        // Sunrise is not a prayer; letting it through fills the qada list with rows nobody owes.
        assertThat(dao.getPrayerRecord(day1, "sunrise")?.status).isEqualTo("pending")
        assertThat(dao.getPrayerRecord(day1, "fajr")?.status).isEqualTo("missed")
    }

    @Test
    fun `confirming a range stops at its ends`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "pending", scheduledTime = 10),
                record(day2, "fajr", status = "pending", scheduledTime = 10),
                record(day3, "fajr", status = "pending", scheduledTime = 10),
            )
        )

        val changed = dao.markUnrecordedAsMissed(from = day2, to = day2)

        assertThat(changed).isEqualTo(1)
        assertThat(dao.getPrayerRecord(day1, "fajr")?.status).isEqualTo("pending")
        assertThat(dao.getPrayerRecord(day2, "fajr")?.status).isEqualTo("missed")
        assertThat(dao.getPrayerRecord(day3, "fajr")?.status).isEqualTo("pending")
    }

    @Test
    fun `a day with no rows at all is left for the caller to fill in`() = runTest {
        // The `UPDATE` can only change rows that exist; a day the app was never opened on has
        // none, which is why `markUnrecordedAsMissed` in the repository inserts as well.
        assertThat(dao.markUnrecordedAsMissed(from = day1, to = day3)).isEqualTo(0)
    }

    // ---- Statistics ----

    @Test
    fun `counts are scoped to the range asked for`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "prayed", scheduledTime = 10, isJamaah = true),
                record(day1, "dhuhr", status = "missed", scheduledTime = 20),
                record(day2, "fajr", status = "prayed", scheduledTime = 10),
                record(day3, "fajr", status = "prayed", scheduledTime = 10, isJamaah = true),
            )
        )

        assertThat(dao.getPrayedCountInRange(day1, day2)).isEqualTo(2)
        assertThat(dao.getMissedCountInRange(day1, day2)).isEqualTo(1)
        assertThat(dao.getJamaahCountInRange(day1, day2)).isEqualTo(1)
        assertThat(dao.getJamaahCountInRange(day1, day3)).isEqualTo(2)
    }

    @Test
    fun `per-prayer counts are grouped by name`() = runTest {
        dao.insertPrayerRecords(
            listOf(
                record(day1, "fajr", status = "prayed", scheduledTime = 10),
                record(day2, "fajr", status = "prayed", scheduledTime = 10),
                record(day1, "asr", status = "missed", scheduledTime = 30),
            )
        )

        assertThat(dao.getPrayedCountByPrayer(day1, day3).map { it.prayerName to it.count })
            .containsExactly("fajr" to 2)
        assertThat(dao.getMissedCountByPrayer(day1, day3).map { it.prayerName to it.count })
            .containsExactly("asr" to 1)
    }

    @Test
    fun `a perfect day is all five prayers, late ones included and sunrise ignored`() = runTest {
        dao.insertPrayerRecords(fullDay(day1, status = "prayed") + record(day1, "sunrise", status = "prayed", scheduledTime = 15))
        dao.insertPrayerRecords(
            fullDay(day2, status = "prayed").dropLast(1) +
                record(day2, "isha", status = "late", scheduledTime = 50)
        )

        assertThat(dao.getPerfectDays()).containsExactly(day2, day1).inOrder()
        assertThat(dao.getPerfectDaysCount(day1, day3)).isEqualTo(2)
    }

    @Test
    fun `four prayers is not a perfect day`() = runTest {
        dao.insertPrayerRecords(fullDay(day1, status = "prayed").dropLast(1))

        assertThat(dao.getPerfectDays()).isEmpty()
        assertThat(dao.getPerfectDaysCount(day1, day3)).isEqualTo(0)
    }

    @Test
    fun `a missed prayer disqualifies the day even with five rows`() = runTest {
        dao.insertPrayerRecords(
            fullDay(day1, status = "prayed").dropLast(1) +
                record(day1, "isha", status = "missed", scheduledTime = 50)
        )

        assertThat(dao.getPerfectDays()).isEmpty()
    }

    @Test
    fun `deleting all user data empties the table`() = runTest {
        dao.insertPrayerRecords(fullDay(day1, status = "prayed"))

        dao.deleteAllUserData()

        assertThat(dao.getAllPrayerRecords()).isEmpty()
    }

    @Test
    fun `logging a prayer without naming a moment stamps it now`() = runTest {
        dao.insertPrayerRecord(record(day1, "fajr", status = "pending", scheduledTime = 10))
        val before = System.currentTimeMillis()

        // Production omits the timestamp; the test above passes one so it can assert on it.
        dao.updatePrayerStatus(day1, "fajr", status = "prayed", prayedAt = 111, isJamaah = false)

        assertThat(dao.getPrayerRecord(day1, "fajr")?.updatedAt).isAtLeast(before)
    }

    @Test
    fun `editing a record wholesale keeps one row for the prayer`() = runTest {
        dao.insertPrayerRecord(record(day1, "fajr", status = "pending", scheduledTime = 10))
        val stored = dao.getPrayerRecord(day1, "fajr")!!

        dao.updatePrayerRecord(stored.copy(status = "late", note = "overslept"))

        assertThat(dao.getPrayerRecordsForDateSync(day1)).hasSize(1)
        assertThat(dao.getPrayerRecord(day1, "fajr")?.note).isEqualTo("overslept")
    }

    private fun fullDay(date: Long, status: String) = listOf(
        record(date, "fajr", status = status, scheduledTime = 10),
        record(date, "dhuhr", status = status, scheduledTime = 20),
        record(date, "asr", status = status, scheduledTime = 30),
        record(date, "maghrib", status = status, scheduledTime = 40),
        record(date, "isha", status = status, scheduledTime = 50),
    )

    private fun record(
        date: Long,
        prayerName: String,
        status: String = "pending",
        scheduledTime: Long = 0,
        isJamaah: Boolean = false,
        isQadaFor: Long? = null,
    ) = PrayerRecordEntity(
        id = 0,
        date = date,
        prayerName = prayerName,
        status = status,
        prayedAt = null,
        scheduledTime = scheduledTime,
        isJamaah = isJamaah,
        isQadaFor = isQadaFor,
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )
}
