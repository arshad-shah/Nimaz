package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.entity.LocationEntity
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.HighLatitudeRule
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * [PrayerRepositoryImpl] beyond `markUnrecordedAsMissed`, which [PrayerRepositoryImplTest]
 * already owns.
 *
 * Three of these pin a defect the repository's own KDoc records rather than a hypothetical.
 * The settings parsing exists because four ViewModels each rolled their own `valueOf` and each
 * caught the throw by substituting Muslim World League — so a user on ISNA silently got MWL
 * times, and nothing failed. The adjustment map is a positional `zip`, which means reordering
 * two arguments in a six-way `combine` moves Fajr's adjustment onto Sunrise with no compiler
 * complaint. And `setCurrentLocation` clears before it sets, because the other order leaves two
 * rows flagged current and `getCurrentLocation()` then returns whichever one the query happens
 * to reach first.
 */
class PrayerRepositoryImplSettingsTest {

    private lateinit var prayerDao: PrayerDao
    private lateinit var locationDao: LocationDao
    private lateinit var settings: SettingsRepository
    private lateinit var repository: PrayerRepositoryImpl

    @Before
    fun setUp() {
        prayerDao = mockk(relaxed = true)
        locationDao = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        val calculator: PrayerTimeCalculator = mockk(relaxed = true)
        repository = PrayerRepositoryImpl(prayerDao, locationDao, calculator, settings)
    }

    /**
     * Stubs every preference the six-way `combine` reads. All of them, every time: a `combine`
     * emits nothing until each source has produced a value, so one unstubbed flow makes
     * `first()` hang rather than fail, and a hang reads as an infrastructure problem.
     */
    private fun stubSettings(
        latitude: Double = 51.5,
        longitude: Double = -0.12,
        name: String = "London",
        method: String = "MWL",
        asr: String = "standard",
        highLat: String = "middle_of_the_night",
        adjustments: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    ) {
        every { settings.latitude } returns flowOf(latitude)
        every { settings.longitude } returns flowOf(longitude)
        every { settings.locationName } returns flowOf(name)
        every { settings.calculationMethod } returns flowOf(method)
        every { settings.asrCalculation } returns flowOf(asr)
        every { settings.highLatitudeRule } returns flowOf(highLat)
        every { settings.fajrAdjustment } returns flowOf(adjustments[0])
        every { settings.sunriseAdjustment } returns flowOf(adjustments[1])
        every { settings.dhuhrAdjustment } returns flowOf(adjustments[2])
        every { settings.asrAdjustment } returns flowOf(adjustments[3])
        every { settings.maghribAdjustment } returns flowOf(adjustments[4])
        every { settings.ishaAdjustment } returns flowOf(adjustments[5])
    }

    private fun record(
        date: Long,
        prayerName: String,
        status: String,
        id: Long = 0,
    ) = PrayerRecordEntity(
        id = id,
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

    private fun locationEntity(
        id: Long = 7,
        method: String? = "MUSLIM_WORLD_LEAGUE",
        asr: String? = "hanafi",
        highLat: String? = "seventh_of_the_night",
    ) = LocationEntity(
        id = id,
        name = "Mecca",
        latitude = 21.4225,
        longitude = 39.8262,
        timezone = "Asia/Riyadh",
        country = "SA",
        city = "Mecca",
        isCurrentLocation = true,
        isFavorite = true,
        calculationMethod = method,
        asrCalculation = asr,
        highLatitudeRule = highLat,
        fajrAngle = 18.5,
        ishaAngle = 19.0,
    )

    // ---- observeCalculationSettings -------------------------------------------------------

    @Test
    fun `the persisted aliases the app itself writes are parsed, not defaulted`() = runTest {
        stubSettings(method = "ISNA", asr = "hanafi", highLat = "twilight_angle")

        val resolved = repository.observeCalculationSettings().first()

        assertThat(resolved.calculationMethod).isEqualTo(CalculationMethod.NORTH_AMERICA)
        assertThat(resolved.asrCalculation).isEqualTo(AsrCalculation.HANAFI)
        assertThat(resolved.highLatitudeRule).isEqualTo(HighLatitudeRule.TWILIGHT_ANGLE)
    }

    @Test
    fun `MAKKAH is Umm al-Qura, not the Muslim World League default`() = runTest {
        stubSettings(method = "MAKKAH")

        assertThat(repository.observeCalculationSettings().first().calculationMethod)
            .isEqualTo(CalculationMethod.UMM_AL_QURA)
    }

    @Test
    fun `an unreadable method still resolves, to Muslim World League`() = runTest {
        stubSettings(method = "not-a-method-anyone-shipped")

        assertThat(repository.observeCalculationSettings().first().calculationMethod)
            .isEqualTo(CalculationMethod.MUSLIM_WORLD_LEAGUE)
    }

    /**
     * The positional `zip`. Six distinct values so a swapped pair cannot pass — with the usual
     * `listOf(0, 0, …)` any permutation is the same map.
     */
    @Test
    fun `each adjustment lands on its own prayer`() = runTest {
        stubSettings(adjustments = listOf(1, 2, 3, 4, 5, 6))

        val adjustments = repository.observeCalculationSettings().first().adjustments

        assertThat(adjustments).containsExactly(
            PrayerType.FAJR, 1,
            PrayerType.SUNRISE, 2,
            PrayerType.DHUHR, 3,
            PrayerType.ASR, 4,
            PrayerType.MAGHRIB, 5,
            PrayerType.ISHA, 6,
        )
    }

    @Test
    fun `an unset location falls back rather than computing times for the equator`() = runTest {
        stubSettings(latitude = 0.0, longitude = 0.0, name = "")

        val location = repository.observeCalculationSettings().first().location

        assertThat(location.isFallback).isTrue()
        assertThat(location.name).isNotEmpty()
    }

    @Test
    fun `a set location is carried through with its name trimmed`() = runTest {
        stubSettings(latitude = 21.4225, longitude = 39.8262, name = "  Mecca  ")

        val location = repository.observeCalculationSettings().first().location

        assertThat(location.isFallback).isFalse()
        assertThat(location.name).isEqualTo("Mecca")
        assertThat(location.latitude).isEqualTo(21.4225)
    }

    // ---- record mapping --------------------------------------------------------------------

    /**
     * The shape Home's prayer card reads. It is a `Map`, so two rows for one prayer collapse and
     * the *last* wins — worth knowing, since the DAO does not constrain that.
     */
    @Test
    fun `today's records arrive as a prayer-to-status map`() = runTest {
        val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        every { prayerDao.getPrayerRecordsForDate(today) } returns flowOf(
            listOf(
                record(today, "fajr", "prayed"),
                record(today, "dhuhr", "missed"),
                record(today, "asr", "pending"),
            )
        )

        val statuses = repository.getTodayPrayerRecords().first()

        assertThat(statuses).containsExactly(
            PrayerName.FAJR, PrayerStatus.PRAYED,
            PrayerName.DHUHR, PrayerStatus.MISSED,
            PrayerName.ASR, PrayerStatus.PENDING,
        )
    }

    @Test
    fun `a day with no rows yields an empty map rather than a default-filled one`() = runTest {
        val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        every { prayerDao.getPrayerRecordsForDate(today) } returns flowOf(emptyList())

        assertThat(repository.getTodayPrayerRecords().first()).isEmpty()
    }

    @Test
    fun `records for a date are mapped to domain models`() = runTest {
        every { prayerDao.getPrayerRecordsForDate(500L) } returns
            flowOf(listOf(record(500L, "maghrib", "late", id = 42)))

        val records = repository.getPrayerRecordsForDate(500L).first()

        assertThat(records).hasSize(1)
        assertThat(records[0].id).isEqualTo(42)
        assertThat(records[0].prayerName).isEqualTo(PrayerName.MAGHRIB)
        assertThat(records[0].status).isEqualTo(PrayerStatus.LATE)
    }

    @Test
    fun `records in a range are mapped to domain models`() = runTest {
        every { prayerDao.getPrayerRecordsInRange(1L, 2L) } returns
            flowOf(listOf(record(1L, "isha", "qada")))

        assertThat(repository.getPrayerRecordsInRange(1L, 2L).first().single().status)
            .isEqualTo(PrayerStatus.QADA)
    }

    /** The enum reaches the DAO lower-cased, which is how the column is written. */
    @Test
    fun `a status query passes the lower-case column value`() = runTest {
        every { prayerDao.getPrayerRecordsByStatus("not_prayed") } returns
            flowOf(listOf(record(1L, "fajr", "not_prayed")))

        val records = repository.getPrayerRecordsByStatus(PrayerStatus.NOT_PRAYED).first()

        assertThat(records.single().status).isEqualTo(PrayerStatus.NOT_PRAYED)
    }

    @Test
    fun `qada candidates are mapped to domain models`() = runTest {
        every { prayerDao.getMissedPrayersRequiringQada() } returns
            flowOf(listOf(record(1L, "asr", "missed")))

        assertThat(repository.getMissedPrayersRequiringQada().first()).hasSize(1)
    }

    @Test
    fun `a single record lookup lower-cases the prayer name`() = runTest {
        coEvery { prayerDao.getPrayerRecord(9L, "maghrib") } returns record(9L, "maghrib", "prayed")

        assertThat(repository.getPrayerRecord(9L, PrayerName.MAGHRIB)?.status)
            .isEqualTo(PrayerStatus.PRAYED)
    }

    @Test
    fun `a missing record lookup is null, not an empty record`() = runTest {
        coEvery { prayerDao.getPrayerRecord(9L, "fajr") } returns null

        assertThat(repository.getPrayerRecord(9L, PrayerName.FAJR)).isNull()
    }

    // ---- updatePrayerStatus ----------------------------------------------------------------

    @Test
    fun `marking a prayer with no row inserts one rather than updating nothing`() = runTest {
        coEvery { prayerDao.getPrayerRecord(100L, "asr") } returns null
        val inserted = slot<PrayerRecordEntity>()
        coEvery { prayerDao.insertPrayerRecord(capture(inserted)) } returns Unit

        repository.updatePrayerStatus(100L, PrayerName.ASR, PrayerStatus.PRAYED, 123L, isJamaah = true)

        assertThat(inserted.captured.prayerName).isEqualTo("asr")
        assertThat(inserted.captured.status).isEqualTo("prayed")
        assertThat(inserted.captured.prayedAt).isEqualTo(123L)
        assertThat(inserted.captured.isJamaah).isTrue()
        assertThat(inserted.captured.id).isEqualTo(0)
        coVerify(exactly = 0) {
            prayerDao.updatePrayerStatus(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `marking a prayer that already has a row updates it rather than inserting a duplicate`() =
        runTest {
            coEvery { prayerDao.getPrayerRecord(100L, "isha") } returns record(100L, "isha", "pending")

            repository.updatePrayerStatus(100L, PrayerName.ISHA, PrayerStatus.MISSED, null, false)

            // `timestamp` is `System.currentTimeMillis()` by default, so it is matched loosely —
            // pinning it would assert the clock, not the call.
            coVerify(exactly = 1) {
                prayerDao.updatePrayerStatus(100L, "isha", "missed", null, false, any())
            }
            coVerify(exactly = 0) { prayerDao.insertPrayerRecord(any()) }
        }

    // ---- streaks ---------------------------------------------------------------------------

    private fun daysAgo(n: Long): Long =
        LocalDate.now().minusDays(n).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun `no perfect days is a zero streak, not an error`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns emptyList()

        assertThat(repository.getCurrentStreak(0L)).isEqualTo(0)
        assertThat(repository.getLongestStreak()).isEqualTo(0)
    }

    @Test
    fun `a run ending today counts`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns listOf(daysAgo(0), daysAgo(1), daysAgo(2))

        assertThat(repository.getCurrentStreak(0L)).isEqualTo(3)
    }

    /**
     * Today is not over. A run that ended yesterday is still live, so the streak anchors on
     * yesterday when today has no perfect day yet — otherwise every user's streak reads zero
     * until they finish Isha.
     */
    @Test
    fun `a run ending yesterday still counts`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns listOf(daysAgo(1), daysAgo(2))

        assertThat(repository.getCurrentStreak(0L)).isEqualTo(2)
    }

    @Test
    fun `a run that ended two days ago is over`() = runTest {
        coEvery { prayerDao.getPerfectDays() } returns listOf(daysAgo(2), daysAgo(3), daysAgo(4))

        assertThat(repository.getCurrentStreak(0L)).isEqualTo(0)
    }

    @Test
    fun `the longest streak is the longest run anywhere, not the current one`() = runTest {
        // A four-day run long ago, and a one-day run today.
        coEvery { prayerDao.getPerfectDays() } returns
            listOf(daysAgo(0), daysAgo(10), daysAgo(11), daysAgo(12), daysAgo(13))

        assertThat(repository.getCurrentStreak(0L)).isEqualTo(1)
        assertThat(repository.getLongestStreak()).isEqualTo(4)
    }

    @Test
    fun `stats carry both per-prayer breakdowns and the streaks`() = runTest {
        coEvery { prayerDao.getPrayedCountInRange(1L, 9L) } returns 12
        coEvery { prayerDao.getMissedCountInRange(1L, 9L) } returns 3
        coEvery { prayerDao.getJamaahCountInRange(1L, 9L) } returns 5
        coEvery { prayerDao.getPrayedCountByPrayer(1L, 9L) } returns emptyList()
        coEvery { prayerDao.getMissedCountByPrayer(1L, 9L) } returns emptyList()
        coEvery { prayerDao.getPerfectDays() } returns listOf(daysAgo(0), daysAgo(1))
        coEvery { prayerDao.getPerfectDaysCount(1L, 9L) } returns 2

        val stats = repository.getPrayerStats(1L, 9L)

        assertThat(stats.totalPrayed).isEqualTo(12)
        assertThat(stats.totalMissed).isEqualTo(3)
        assertThat(stats.totalJamaah).isEqualTo(5)
        assertThat(stats.perfectDays).isEqualTo(2)
        assertThat(stats.currentStreak).isEqualTo(2)
        assertThat(stats.startDate).isEqualTo(1L)
        assertThat(stats.endDate).isEqualTo(9L)
    }

    // ---- locations -------------------------------------------------------------------------

    @Test
    fun `a location survives the round trip through the entity`() = runTest {
        every { locationDao.getAllLocations() } returns flowOf(listOf(locationEntity()))

        val location = repository.getAllLocations().first().single()

        assertThat(location.name).isEqualTo("Mecca")
        assertThat(location.calculationMethod).isEqualTo(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        assertThat(location.asrCalculation).isEqualTo(AsrCalculation.HANAFI)
        assertThat(location.highLatitudeRule).isEqualTo(HighLatitudeRule.SEVENTH_OF_THE_NIGHT)
        assertThat(location.isFavorite).isTrue()

        val written = slot<LocationEntity>()
        coEvery { locationDao.insertLocation(capture(written)) } returns 1L
        repository.insertLocation(location)

        // The three enums are written back in the spellings `fromString` accepts — the method
        // upper-case, the other two lower-case. They disagree, which is exactly why this
        // asserts on the strings rather than trusting symmetry.
        assertThat(written.captured.calculationMethod).isEqualTo("MUSLIM_WORLD_LEAGUE")
        assertThat(written.captured.asrCalculation).isEqualTo("hanafi")
        assertThat(written.captured.highLatitudeRule).isEqualTo("seventh_of_the_night")
    }

    @Test
    fun `a location row with null calculation columns still resolves`() = runTest {
        every { locationDao.getAllLocations() } returns
            flowOf(listOf(locationEntity(method = null, asr = null, highLat = null)))

        val location = repository.getAllLocations().first().single()

        assertThat(location.calculationMethod).isEqualTo(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        assertThat(location.asrCalculation).isEqualTo(AsrCalculation.STANDARD)
        assertThat(location.highLatitudeRule).isNull()
    }

    @Test
    fun `no current location is null rather than a blank one`() = runTest {
        every { locationDao.getCurrentLocation() } returns flowOf(null)

        assertThat(repository.getCurrentLocation().first()).isNull()
    }

    @Test
    fun `the current location is mapped when there is one`() = runTest {
        every { locationDao.getCurrentLocation() } returns flowOf(locationEntity())

        assertThat(repository.getCurrentLocation().first()?.name).isEqualTo("Mecca")
    }

    @Test
    fun `favorite and recent location flows are mapped`() = runTest {
        every { locationDao.getFavoriteLocations() } returns flowOf(listOf(locationEntity()))
        every { locationDao.getRecentLocations(3) } returns flowOf(listOf(locationEntity(id = 8)))

        assertThat(repository.getFavoriteLocations().first().single().id).isEqualTo(7)
        assertThat(repository.getRecentLocations(3).first().single().id).isEqualTo(8)
    }

    @Test
    fun `a location search is mapped and passes the query through untouched`() = runTest {
        every { locationDao.searchLocations("mec") } returns flowOf(listOf(locationEntity()))

        assertThat(repository.searchLocations("mec").first()).hasSize(1)
    }

    /**
     * Clear, then set. The other order leaves the previous row still flagged current, and
     * `getCurrentLocation()` then returns whichever the query reaches first.
     */
    @Test
    fun `setting the current location clears the previous one first`() = runTest {
        repository.setCurrentLocation(4L)

        coVerifyOrder {
            locationDao.clearCurrentLocation()
            locationDao.setCurrentLocation(4L)
        }
    }
}
