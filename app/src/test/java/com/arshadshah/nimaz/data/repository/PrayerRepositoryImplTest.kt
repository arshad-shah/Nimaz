package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for [PrayerRepositoryImpl]. The DAOs are mocked (matching the
 * style of [FastingRepositoryImplTest]) so the focus is on the repository's
 * own logic: the perfect-day streak algorithm, entity<->domain mapping, the
 * lowercase status/name conventions, and the create-vs-update branch in
 * updatePrayerStatus.
 */
class PrayerRepositoryImplTest {

    private lateinit var prayerDao: PrayerDao
    private lateinit var locationDao: LocationDao
    private lateinit var calculator: PrayerTimeCalculator
    private lateinit var repository: PrayerRepositoryImpl

    private val oneDay = 86_400_000L
    private val todayEpoch =
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        prayerDao = mockk(relaxed = true)
        locationDao = mockk(relaxed = true)
        calculator = mockk(relaxed = true)
        repository = PrayerRepositoryImpl(prayerDao, locationDao, calculator)
    }

    private fun entity(
        id: Long = 1,
        date: Long = todayEpoch,
        prayerName: String = "fajr",
        status: String = "prayed",
        prayedAt: Long? = 1500,
        isJamaah: Boolean = false
    ) = PrayerRecordEntity(
        id = id, date = date, prayerName = prayerName, status = status,
        prayedAt = prayedAt, scheduledTime = date, isJamaah = isJamaah,
        isQadaFor = null, note = null, createdAt = 100, updatedAt = 200
    )

    // ── Streak algorithm ────────────────────────────────────────────

    @Test
    fun `current streak counts consecutive perfect days ending today`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(todayEpoch, todayEpoch - oneDay, todayEpoch - 2 * oneDay)

        assertThat(repository.getCurrentStreak(todayEpoch)).isEqualTo(3)
    }

    @Test
    fun `current streak anchors to yesterday when today is not yet perfect`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(todayEpoch - oneDay, todayEpoch - 2 * oneDay)

        assertThat(repository.getCurrentStreak(todayEpoch)).isEqualTo(2)
    }

    @Test
    fun `current streak is zero when neither today nor yesterday is perfect`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(todayEpoch - 3 * oneDay, todayEpoch - 4 * oneDay)

        assertThat(repository.getCurrentStreak(todayEpoch)).isEqualTo(0)
    }

    @Test
    fun `longest streak finds the longest consecutive run regardless of recency`() = runTest {
        val base = todayEpoch - 100 * oneDay
        coEvery { prayerDao.getPerfectDays() } returns listOf(
            base, base + oneDay, base + 2 * oneDay,          // run of 3
            base + 10 * oneDay, base + 11 * oneDay           // run of 2
        )

        assertThat(repository.getLongestStreak()).isEqualTo(3)
    }

    @Test
    fun `empty perfect-day history yields zero streaks`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns emptyList()

        assertThat(repository.getCurrentStreak(todayEpoch)).isEqualTo(0)
        assertThat(repository.getLongestStreak()).isEqualTo(0)
    }

    // ── Entity <-> domain mapping ───────────────────────────────────

    @Test
    fun `records for a date are mapped from entities to domain models`() = runTest {
        every { prayerDao.getPrayerRecordsForDate(todayEpoch) } returns
            flowOf(listOf(entity(prayerName = "isha", status = "missed", prayedAt = null)))

        val records = repository.getPrayerRecordsForDate(todayEpoch).first()

        assertThat(records).hasSize(1)
        assertThat(records.first().prayerName).isEqualTo(PrayerName.ISHA)
        assertThat(records.first().status).isEqualTo(PrayerStatus.MISSED)
        assertThat(records.first().prayedAt).isNull()
    }

    @Test
    fun `getPrayerRecord lowercases the prayer name for the query`() = runTest {
        coEvery { prayerDao.getPrayerRecord(todayEpoch, "dhuhr") } returns
            entity(prayerName = "dhuhr", status = "prayed")

        val record = repository.getPrayerRecord(todayEpoch, PrayerName.DHUHR)

        assertThat(record).isNotNull()
        assertThat(record!!.prayerName).isEqualTo(PrayerName.DHUHR)
        coVerify { prayerDao.getPrayerRecord(todayEpoch, "dhuhr") }
    }

    @Test
    fun `today's records are exposed as a name-to-status map`() = runTest {
        every { prayerDao.getPrayerRecordsForDate(any()) } returns flowOf(
            listOf(
                entity(prayerName = "fajr", status = "prayed"),
                entity(prayerName = "asr", status = "missed")
            )
        )

        val map = repository.getTodayPrayerRecords().first()

        assertThat(map[PrayerName.FAJR]).isEqualTo(PrayerStatus.PRAYED)
        assertThat(map[PrayerName.ASR]).isEqualTo(PrayerStatus.MISSED)
    }

    // ── updatePrayerStatus create-vs-update branch ──────────────────

    @Test
    fun `updatePrayerStatus inserts a new lowercase record when none exists`() = runTest {
        coEvery { prayerDao.getPrayerRecord(todayEpoch, "maghrib") } returns null

        repository.updatePrayerStatus(
            todayEpoch, PrayerName.MAGHRIB, PrayerStatus.PRAYED, prayedAt = 1700, isJamaah = true
        )

        coVerify {
            prayerDao.insertPrayerRecord(
                match {
                    it.id == 0L &&
                        it.prayerName == "maghrib" &&
                        it.status == "prayed" &&
                        it.prayedAt == 1700L &&
                        it.isJamaah &&
                        it.scheduledTime == todayEpoch
                }
            )
        }
        coVerify(exactly = 0) {
            prayerDao.updatePrayerStatus(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `updatePrayerStatus updates in place when a record already exists`() = runTest {
        coEvery { prayerDao.getPrayerRecord(todayEpoch, "fajr") } returns entity()

        repository.updatePrayerStatus(
            todayEpoch, PrayerName.FAJR, PrayerStatus.LATE, prayedAt = 1600, isJamaah = false
        )

        // The DAO method has a trailing `timestamp: Long = now()` default param,
        // so match it with any() rather than a fixed value.
        coVerify {
            prayerDao.updatePrayerStatus(todayEpoch, "fajr", "late", 1600L, false, any())
        }
        coVerify(exactly = 0) { prayerDao.insertPrayerRecord(any()) }
    }

    @Test
    fun `inserting a record lowercases name and status for storage`() = runTest {
        val record = PrayerRecord(
            id = 0, date = todayEpoch, prayerName = PrayerName.ISHA,
            status = PrayerStatus.PRAYED, prayedAt = 1500, scheduledTime = todayEpoch,
            isJamaah = true, isQadaFor = null, note = null, createdAt = 100, updatedAt = 200
        )

        repository.insertPrayerRecord(record)

        coVerify {
            prayerDao.insertPrayerRecord(
                match { it.prayerName == "isha" && it.status == "prayed" && it.isJamaah }
            )
        }
    }

    // ── Stats aggregation ───────────────────────────────────────────

    @Test
    fun `prayer stats echo scalar counts and the queried date range`() = runTest {
        coEvery { prayerDao.getPrayedCountInRange(any(), any()) } returns 10
        coEvery { prayerDao.getMissedCountInRange(any(), any()) } returns 2
        coEvery { prayerDao.getJamaahCountInRange(any(), any()) } returns 4
        coEvery { prayerDao.getPerfectDaysCount(any(), any()) } returns 3
        coEvery { prayerDao.getPerfectDays() } returns emptyList()

        val stats = repository.getPrayerStats(startDate = 1000, endDate = 5000)

        assertThat(stats.totalPrayed).isEqualTo(10)
        assertThat(stats.totalMissed).isEqualTo(2)
        assertThat(stats.totalJamaah).isEqualTo(4)
        assertThat(stats.perfectDays).isEqualTo(3)
        assertThat(stats.startDate).isEqualTo(1000)
        assertThat(stats.endDate).isEqualTo(5000)
        assertThat(stats.currentStreak).isEqualTo(0)
        assertThat(stats.longestStreak).isEqualTo(0)
    }
}
