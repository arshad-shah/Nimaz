package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Covers [PrayerRepositoryImpl.markUnrecordedAsMissed] — the fix for the "Review" banner no-op:
 * confirming a range must insert a `missed` row for every tracked prayer with none, and update
 * existing `pending`/`not_prayed` rows, without ever touching a `prayed`/`late`/`missed`/`qada`
 * row the user already asserted.
 */
class PrayerRepositoryImplTest {

    private lateinit var prayerDao: PrayerDao
    private lateinit var repository: PrayerRepositoryImpl

    private val oneDayMillis = 24 * 60 * 60 * 1000L
    private val day1 = 1_000L * oneDayMillis // arbitrary UTC-midnight epoch millis
    private val day2 = day1 + oneDayMillis

    @Before
    fun setUp() {
        prayerDao = mockk(relaxed = true)
        val locationDao: LocationDao = mockk(relaxed = true)
        val prayerTimeCalculator: PrayerTimeCalculator = mockk(relaxed = true)
        val settingsRepository: SettingsRepository = mockk(relaxed = true)
        repository = PrayerRepositoryImpl(prayerDao, locationDao, prayerTimeCalculator, settingsRepository)
    }

    private fun entity(
        date: Long,
        prayerName: String,
        status: String,
    ) = PrayerRecordEntity(
        id = 0,
        date = date,
        prayerName = prayerName,
        status = status,
        prayedAt = null,
        scheduledTime = date,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `a day with no rows at all gets five missed rows inserted`() = runTest {
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day1) } returns emptyList()
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day1, any()) } returns 0

        val inserted = slot<List<PrayerRecordEntity>>()
        coEvery { prayerDao.insertPrayerRecords(capture(inserted)) } returns Unit

        val result = repository.markUnrecordedAsMissed(day1, day1)

        assertThat(result).isEqualTo(5)
        assertThat(inserted.captured).hasSize(5)
        assertThat(inserted.captured.map { it.prayerName })
            .containsExactly("fajr", "dhuhr", "asr", "maghrib", "isha")
        assertThat(inserted.captured.all { it.status == "missed" }).isTrue()
        assertThat(inserted.captured.none { it.prayerName == "sunrise" }).isTrue()
    }

    @Test
    fun `a day with a prayed row keeps it and gains four missed rows`() = runTest {
        val existing = listOf(entity(day1, "fajr", "prayed"))
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day1) } returns existing
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day1, any()) } returns 0

        val inserted = slot<List<PrayerRecordEntity>>()
        coEvery { prayerDao.insertPrayerRecords(capture(inserted)) } returns Unit

        val result = repository.markUnrecordedAsMissed(day1, day1)

        assertThat(result).isEqualTo(4)
        assertThat(inserted.captured).hasSize(4)
        assertThat(inserted.captured.map { it.prayerName })
            .containsExactly("dhuhr", "asr", "maghrib", "isha")
        // fajr's existing "prayed" row was never touched: no update/insert targets it.
        assertThat(inserted.captured.none { it.prayerName == "fajr" }).isTrue()
    }

    @Test
    fun `a pending row is confirmed via the update path, not re-inserted`() = runTest {
        val existing = listOf(
            entity(day1, "fajr", "pending"),
            entity(day1, "dhuhr", "prayed"),
            entity(day1, "asr", "missed"),
            entity(day1, "maghrib", "late"),
            entity(day1, "isha", "qada"),
        )
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day1) } returns existing
        // The DAO's own UPDATE only ever matches pending/not_prayed rows — simulate it doing so.
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day1, any()) } returns 1

        val inserted = slot<List<PrayerRecordEntity>>()
        coEvery { prayerDao.insertPrayerRecords(capture(inserted)) } returns Unit

        val result = repository.markUnrecordedAsMissed(day1, day1)

        // All five prayers already have rows, so nothing new is inserted.
        assertThat(inserted.isCaptured).isFalse()
        assertThat(result).isEqualTo(1)
        coVerify(exactly = 1) { prayerDao.markUnrecordedAsMissed(day1, day1, any()) }
    }

    @Test
    fun `a qada row is left untouched and not counted`() = runTest {
        val existing = listOf(
            entity(day1, "fajr", "qada"),
            entity(day1, "dhuhr", "prayed"),
            entity(day1, "asr", "late"),
            entity(day1, "maghrib", "missed"),
            entity(day1, "isha", "prayed"),
        )
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day1) } returns existing
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day1, any()) } returns 0

        val result = repository.markUnrecordedAsMissed(day1, day1)

        assertThat(result).isEqualTo(0)
        coVerify(exactly = 0) { prayerDao.insertPrayerRecords(any()) }
    }

    @Test
    fun `sunrise is never inserted even for a completely empty range`() = runTest {
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day2) } returns emptyList()
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day2, any()) } returns 0

        val inserted = slot<List<PrayerRecordEntity>>()
        coEvery { prayerDao.insertPrayerRecords(capture(inserted)) } returns Unit

        repository.markUnrecordedAsMissed(day1, day2)

        assertThat(inserted.captured.none { it.prayerName == "sunrise" }).isTrue()
    }

    @Test
    fun `the range is inclusive at both ends`() = runTest {
        coEvery { prayerDao.getPrayerRecordsInRangeSync(day1, day2) } returns emptyList()
        coEvery { prayerDao.markUnrecordedAsMissed(day1, day2, any()) } returns 0

        val inserted = slot<List<PrayerRecordEntity>>()
        coEvery { prayerDao.insertPrayerRecords(capture(inserted)) } returns Unit

        val result = repository.markUnrecordedAsMissed(day1, day2)

        // Two days, five tracked prayers each.
        assertThat(result).isEqualTo(10)
        assertThat(inserted.captured.map { it.date }.toSet()).containsExactly(day1, day2)
        assertThat(inserted.captured.count { it.date == day1 }).isEqualTo(5)
        assertThat(inserted.captured.count { it.date == day2 }).isEqualTo(5)
    }
}
