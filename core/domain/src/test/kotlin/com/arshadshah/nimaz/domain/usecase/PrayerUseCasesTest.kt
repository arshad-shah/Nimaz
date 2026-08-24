package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The prayer and location use cases, and the two of them that are more than a delegation.
 *
 * Most of this file is one-line pass-throughs, and a pass-through has exactly one failure mode
 * worth a test: **calling the wrong neighbour.** `getLongestStreak` reaching
 * `getCurrentStreak` type-checks, returns an `Int`, and is wrong every day the user's streak is
 * not also their best one. There is no other way to notice it, so each one is pinned to the
 * repository call it is supposed to make.
 *
 * Two carry real behaviour:
 *
 * **[SaveCurrentLocationUseCase]** composes the `Location` itself rather than taking one. Its
 * KDoc records why: a ViewModel building the row invented an `id` of 0 against an autogenerate
 * primary key and inserted a duplicate on every selection. So the fields it fixes — `id`,
 * `isCurrentLocation`, `city` — are the point of the class, not incidental.
 *
 * **[GetRecentLocationsUseCase]** carries a default limit, which is the kind of constant that
 * gets "tidied" into a caller and then differs between two callers.
 */
class PrayerUseCasesTest {

    private val repository: PrayerRepository = mockk(relaxed = true)

    private fun record(name: PrayerName) = PrayerRecord(
        id = 1,
        date = 20_260_101,
        prayerName = name,
        status = PrayerStatus.PRAYED,
        prayedAt = null,
        scheduledTime = 0,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0,
        updatedAt = 0,
    )

    private fun location(id: Long, name: String) = Location(
        id = id,
        name = name,
        latitude = 1.0,
        longitude = 2.0,
        timezone = "UTC",
        country = "Nowhere",
        city = name,
        isCurrentLocation = false,
        isFavorite = false,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        fajrAngle = null,
        ishaAngle = null,
    )

    // ---- Records ----

    @Test
    fun `records for a date come from that date`() = runTest {
        every { repository.getPrayerRecordsForDate(20_260_101) } returns
            flowOf(listOf(record(PrayerName.FAJR)))

        val result = GetPrayerRecordsForDateUseCase(repository)(20_260_101).first()

        assertThat(result.map { it.prayerName }).containsExactly(PrayerName.FAJR)
    }

    @Test
    fun `a range is passed through in the order it was given`() = runTest {
        every { repository.getPrayerRecordsInRange(any(), any()) } returns flowOf(emptyList())

        GetPrayerRecordsInRangeUseCase(repository)(from(), to()).first()

        verify { repository.getPrayerRecordsInRange(from(), to()) }
    }

    private fun from() = 20_260_101L
    private fun to() = 20_260_131L

    @Test
    fun `today's records are the repository's today, not a date the caller supplies`() = runTest {
        every { repository.getTodayPrayerRecords() } returns
            flowOf(mapOf(PrayerName.FAJR to PrayerStatus.PRAYED))

        val result = GetTodayPrayerRecordsUseCase(repository)().first()

        assertThat(result).containsExactly(PrayerName.FAJR, PrayerStatus.PRAYED)
    }

    @Test
    fun `a status change carries every field the caller set`() = runTest {
        UpdatePrayerStatusUseCase(repository)(
            date = 20_260_101,
            prayerName = PrayerName.ASR,
            status = PrayerStatus.PRAYED,
            prayedAt = 1_700_000_000_000,
            isJamaah = true,
        )

        coVerify {
            repository.updatePrayerStatus(
                20_260_101, PrayerName.ASR, PrayerStatus.PRAYED, 1_700_000_000_000, true,
            )
        }
    }

    // ---- Streaks and statistics ----

    @Test
    fun `the current streak and the longest one are different questions`() = runTest {
        coEvery { repository.getCurrentStreak(any()) } returns 3
        coEvery { repository.getLongestStreak() } returns 40

        assertThat(GetCurrentStreakUseCase(repository)(20_260_101)).isEqualTo(3)
        assertThat(GetLongestStreakUseCase(repository)()).isEqualTo(40)
    }

    @Test
    fun `the current streak is asked as of the date it was given`() = runTest {
        GetCurrentStreakUseCase(repository)(20_260_115)

        coVerify { repository.getCurrentStreak(20_260_115) }
    }

    @Test
    fun `statistics span the range they were asked for`() = runTest {
        coEvery { repository.getPrayerStats(any(), any()) } returns PrayerStats(
            totalPrayed = 0, totalMissed = 0, totalJamaah = 0,
            prayedByPrayer = emptyMap(), missedByPrayer = emptyMap(),
            currentStreak = 0, longestStreak = 0, perfectDays = 0,
            startDate = from(), endDate = to(),
        )

        GetPrayerStatsUseCase(repository)(from(), to())

        coVerify { repository.getPrayerStats(from(), to()) }
    }

    @Test
    fun `confirming missed prayers reports how many were confirmed`() = runTest {
        // The only way a prayer enters the qada list — nothing marks one on the user's behalf.
        coEvery { repository.markUnrecordedAsMissed(any(), any()) } returns 7

        assertThat(MarkUnrecordedAsMissedUseCase(repository)(from(), to())).isEqualTo(7)
    }

    @Test
    fun `the qada list is the missed prayers, not all of them`() = runTest {
        every { repository.getMissedPrayersRequiringQada() } returns
            flowOf(listOf(record(PrayerName.ISHA)))

        val result = GetMissedPrayersRequiringQadaUseCase(repository)().first()

        assertThat(result).hasSize(1)
    }

    // ---- Locations ----

    @Test
    fun `each location query reaches its own repository call`() = runTest {
        every { repository.getCurrentLocation() } returns flowOf(location(1, "current"))
        every { repository.getAllLocations() } returns flowOf(listOf(location(2, "all")))
        every { repository.getFavoriteLocations() } returns flowOf(listOf(location(3, "fave")))

        assertThat(GetCurrentLocationUseCase(repository)().first()?.name).isEqualTo("current")
        assertThat(GetAllLocationsUseCase(repository)().first().map { it.name })
            .containsExactly("all")
        assertThat(GetFavoriteLocationsUseCase(repository)().first().map { it.name })
            .containsExactly("fave")
    }

    @Test
    fun `recent locations default to five`() = runTest {
        every { repository.getRecentLocations(any()) } returns flowOf(emptyList())

        GetRecentLocationsUseCase(repository)().first()

        verify { repository.getRecentLocations(5) }
    }

    @Test
    fun `a caller that wants a different number of recents gets it`() = runTest {
        every { repository.getRecentLocations(any()) } returns flowOf(emptyList())

        GetRecentLocationsUseCase(repository)(limit = 12).first()

        verify { repository.getRecentLocations(12) }
    }

    @Test
    fun `inserting a location reports the id the database gave it`() = runTest {
        coEvery { repository.insertLocation(any()) } returns 99

        assertThat(InsertLocationUseCase(repository)(location(0, "new"))).isEqualTo(99)
    }

    @Test
    fun `deleting, selecting and favouriting each reach their own call`() = runTest {
        val victim = location(4, "gone")

        DeleteLocationUseCase(repository)(victim)
        SetCurrentLocationUseCase(repository)(4)
        ToggleLocationFavoriteUseCase(repository)(4)

        coVerify { repository.deleteLocation(victim) }
        coVerify { repository.setCurrentLocation(4) }
        coVerify { repository.toggleFavorite(4) }
    }

    // ---- Saving where the user actually is ----

    @Test
    fun `a saved location is composed here, not by the caller`() = runTest {
        val saved = slot<Location>()
        coEvery { repository.saveCurrentLocation(capture(saved), any()) } returns 1

        SaveCurrentLocationUseCase(repository)(
            name = "Sarajevo",
            country = "Bosnia and Herzegovina",
            latitude = 43.8563,
            longitude = 18.4131,
            timezone = "Europe/Sarajevo",
        )

        with(saved.captured) {
            // id 0 against an autogenerate key is what makes this an insert rather than a
            // duplicate of whatever the ViewModel last held.
            assertThat(id).isEqualTo(0)
            assertThat(isCurrentLocation).isTrue()
            assertThat(isFavorite).isFalse()
            assertThat(name).isEqualTo("Sarajevo")
            assertThat(city).isEqualTo("Sarajevo")
            assertThat(country).isEqualTo("Bosnia and Herzegovina")
            assertThat(latitude).isEqualTo(43.8563)
            assertThat(longitude).isEqualTo(18.4131)
            assertThat(timezone).isEqualTo("Europe/Sarajevo")
        }
    }

    @Test
    fun `a saved location does not carry calculation overrides the user never set`() = runTest {
        val saved = slot<Location>()
        coEvery { repository.saveCurrentLocation(capture(saved), any()) } returns 1

        SaveCurrentLocationUseCase(repository)("Cairo", "Egypt", 30.0, 31.2, "Africa/Cairo")

        with(saved.captured) {
            // Per-location angles are an override; a place picked from a list has none, and
            // inventing them here would silently pin that location to different times.
            assertThat(fajrAngle).isNull()
            assertThat(ishaAngle).isNull()
            assertThat(highLatitudeRule).isNull()
            assertThat(calculationMethod).isEqualTo(CalculationMethod.MUSLIM_WORLD_LEAGUE)
            assertThat(asrCalculation).isEqualTo(AsrCalculation.STANDARD)
        }
    }

    @Test
    fun `the caller can supply the clock, so a save is not tied to the wall clock`() = runTest {
        coEvery { repository.saveCurrentLocation(any(), any()) } returns 1

        SaveCurrentLocationUseCase(repository)(
            "Cairo", "Egypt", 30.0, 31.2, "Africa/Cairo", now = 1_700_000_000_000,
        )

        coVerify { repository.saveCurrentLocation(any(), 1_700_000_000_000) }
    }

    @Test
    fun `saving reports the row id back`() = runTest {
        coEvery { repository.saveCurrentLocation(any(), any()) } returns 42

        val id = SaveCurrentLocationUseCase(repository)("Cairo", "Egypt", 30.0, 31.2, "Africa/Cairo")

        assertThat(id).isEqualTo(42)
    }
}
