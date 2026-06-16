package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerStatCount
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class PrayerRepositoryImplTest {

    private lateinit var prayerDao: PrayerDao
    private lateinit var locationDao: LocationDao
    private lateinit var calculator: PrayerTimeCalculator
    private lateinit var repository: PrayerRepositoryImpl

    private val oneDay = 24 * 60 * 60 * 1000L

    // Matches the exact epoch formula used inside calculateStreaks().
    private fun startOfDayUtc(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private val today = startOfDayUtc(LocalDate.now())

    @Before
    fun setUp() {
        prayerDao = mockk(relaxed = true)
        locationDao = mockk(relaxed = true)
        calculator = mockk(relaxed = true)
        repository = PrayerRepositoryImpl(prayerDao, locationDao, calculator)
    }

    private fun recordEntity(
        id: Long = 1,
        date: Long = today,
        prayerName: String = "fajr",
        status: String = "prayed"
    ) = PrayerRecordEntity(
        id = id, date = date, prayerName = prayerName, status = status,
        prayedAt = null, scheduledTime = date, isJamaah = false,
        isQadaFor = null, note = null, createdAt = today, updatedAt = today
    )

    // ── Entity → domain mapping ─────────────────────────────────────

    @Test
    fun `getPrayerRecordsForDate maps entities to domain with enum parsing`() = runTest {
        every { prayerDao.getPrayerRecordsForDate(today) } returns
            flowOf(listOf(recordEntity(prayerName = "maghrib", status = "qada")))

        repository.getPrayerRecordsForDate(today).test {
            val record = awaitItem().single()
            assertThat(record.prayerName).isEqualTo(PrayerName.MAGHRIB)
            assertThat(record.status).isEqualTo(PrayerStatus.QADA)
            awaitComplete()
        }
    }

    @Test
    fun `getTodayPrayerRecords reduces records to a prayer-status map`() = runTest {
        val todayEpoch = LocalDate.now().toEpochDay() * oneDay
        every { prayerDao.getPrayerRecordsForDate(todayEpoch) } returns flowOf(
            listOf(
                recordEntity(prayerName = "fajr", status = "prayed"),
                recordEntity(prayerName = "asr", status = "missed")
            )
        )

        repository.getTodayPrayerRecords().test {
            val map = awaitItem()
            assertThat(map[PrayerName.FAJR]).isEqualTo(PrayerStatus.PRAYED)
            assertThat(map[PrayerName.ASR]).isEqualTo(PrayerStatus.MISSED)
            awaitComplete()
        }
    }

    // ── updatePrayerStatus: insert vs update ────────────────────────

    @Test
    fun `updatePrayerStatus inserts a new record when none exists`() = runTest {
        coEvery { prayerDao.getPrayerRecord(today, "fajr") } returns null

        repository.updatePrayerStatus(today, PrayerName.FAJR, PrayerStatus.PRAYED, 123L, true)

        coVerify {
            prayerDao.insertPrayerRecord(
                match { it.prayerName == "fajr" && it.status == "prayed" && it.isJamaah }
            )
        }
        coVerify(exactly = 0) { prayerDao.updatePrayerStatus(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `updatePrayerStatus updates the existing record when present`() = runTest {
        coEvery { prayerDao.getPrayerRecord(any(), any()) } returns recordEntity()

        repository.updatePrayerStatus(today, PrayerName.FAJR, PrayerStatus.MISSED, null, false)

        // All-matcher form. The trailing any() matches the defaulted
        // `timestamp = System.currentTimeMillis()` param — without it MockK fills the
        // default at verify time, producing an eq() against a different timestamp than
        // the call recorded. isNull() matches the nullable prayedAt.
        coVerify {
            prayerDao.updatePrayerStatus(eq(today), eq("fajr"), eq("missed"), isNull(), eq(false), any())
        }
        coVerify(exactly = 0) { prayerDao.insertPrayerRecord(any()) }
    }

    // ── getPrayerStats aggregation ──────────────────────────────────

    @Test
    fun `getPrayerStats aggregates counts and groups by prayer`() = runTest {
        coEvery { prayerDao.getPrayedCountInRange(any(), any()) } returns 10
        coEvery { prayerDao.getMissedCountInRange(any(), any()) } returns 2
        coEvery { prayerDao.getJamaahCountInRange(any(), any()) } returns 4
        coEvery { prayerDao.getPrayedCountByPrayer(any(), any()) } returns listOf(
            PrayerStatCount("fajr", 3), PrayerStatCount("isha", 2)
        )
        coEvery { prayerDao.getMissedCountByPrayer(any(), any()) } returns listOf(
            PrayerStatCount("asr", 1)
        )
        coEvery { prayerDao.getPerfectDays() } returns emptyList()
        coEvery { prayerDao.getPerfectDaysCount(any(), any()) } returns 5

        val stats = repository.getPrayerStats(0L, today)

        assertThat(stats.totalPrayed).isEqualTo(10)
        assertThat(stats.totalMissed).isEqualTo(2)
        assertThat(stats.totalJamaah).isEqualTo(4)
        assertThat(stats.prayedByPrayer[PrayerName.FAJR]).isEqualTo(3)
        assertThat(stats.prayedByPrayer[PrayerName.ISHA]).isEqualTo(2)
        assertThat(stats.missedByPrayer[PrayerName.ASR]).isEqualTo(1)
        assertThat(stats.perfectDays).isEqualTo(5)
    }

    // ── Streak calculation (the meaty private algorithm) ────────────

    @Test
    fun `current streak counts consecutive perfect days back from today`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(today, today - oneDay, today - 2 * oneDay)

        assertThat(repository.getCurrentStreak(today)).isEqualTo(3)
    }

    @Test
    fun `current streak starts from yesterday when today is not yet perfect`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(today - oneDay, today - 2 * oneDay)

        assertThat(repository.getCurrentStreak(today)).isEqualTo(2)
    }

    @Test
    fun `current streak is zero when neither today nor yesterday is perfect`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns listOf(today - 5 * oneDay)

        assertThat(repository.getCurrentStreak(today)).isEqualTo(0)
    }

    @Test
    fun `streaks are zero when there are no perfect days`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns emptyList()

        assertThat(repository.getCurrentStreak(today)).isEqualTo(0)
        assertThat(repository.getLongestStreak()).isEqualTo(0)
    }

    @Test
    fun `longest streak picks the longest consecutive run despite gaps`() = runTest {
        // Recent run of 2 (today, yesterday) and an isolated older day.
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(today, today - oneDay, today - 10 * oneDay)

        assertThat(repository.getLongestStreak()).isEqualTo(2)
        assertThat(repository.getCurrentStreak(today)).isEqualTo(2)
    }

    // ── Location current-selection ordering ─────────────────────────

    @Test
    fun `setCurrentLocation clears the previous selection before setting the new one`() = runTest {
        repository.setCurrentLocation(5L)

        coVerifyOrder {
            locationDao.clearCurrentLocation()
            locationDao.setCurrentLocation(5L)
        }
    }
}
