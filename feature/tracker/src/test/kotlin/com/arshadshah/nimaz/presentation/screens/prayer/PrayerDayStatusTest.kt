package com.arshadshah.nimaz.presentation.screens.prayer

import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus
import com.arshadshah.nimaz.presentation.model.isDone
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerDayStatusTest {

    private val day: LocalDate = LocalDate.of(2026, 8, 13)

    private fun testLocation() = Location(
        id = 1, name = "Test",
        latitude = 0.0, longitude = 0.0,
        timezone = "UTC",
        country = null, city = null,
        isCurrentLocation = true, isFavorite = false,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null, fajrAngle = null, ishaAngle = null,
    )

    private val location = testLocation()

    private val times = PrayerTimes(
        fajr = day.atTime(4, 31),
        sunrise = day.atTime(6, 5),
        dhuhr = day.atTime(13, 35),
        asr = day.atTime(17, 35),
        maghrib = day.atTime(20, 58),
        isha = day.atTime(22, 35),
        date = day,
        location = location,
    )

    private fun record(prayer: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = 0L, date = 0L, prayerName = prayer, status = status,
        prayedAt = null, scheduledTime = 0L, isJamaah = false,
        isQadaFor = null, note = null, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `sunrise is never tracked`() {
        assertThat(TRACKED_PRAYERS).containsExactly(
            PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR,
            PrayerName.MAGHRIB, PrayerName.ISHA,
        ).inOrder()

        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(23, 0))
        assertThat(resolved).doesNotContainKey(PrayerName.SUNRISE)
    }

    @Test
    fun `on today a passed prayer with no record is not recorded, not missed`() {
        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(18, 10))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.MAGHRIB]).isEqualTo(PrayerDisplayStatus.UPCOMING)
        assertThat(resolved[PrayerName.ISHA]).isEqualTo(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `a prayer exactly at its time has not passed yet`() {
        val resolved = resolvePrayerStatuses(emptyList(), times, day, day.atTime(17, 35))
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `every prayer on a past day with no record is not recorded`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = times,
            date = day, now = day.plusDays(3).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `every prayer on a future day is upcoming`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = times,
            date = day, now = day.minusDays(2).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `an asserted record beats the derivation`() {
        val records = listOf(
            record(PrayerName.FAJR, PrayerStatus.PRAYED),
            record(PrayerName.DHUHR, PrayerStatus.LATE),
            record(PrayerName.ASR, PrayerStatus.MISSED),
            record(PrayerName.MAGHRIB, PrayerStatus.QADA),
        )
        val resolved = resolvePrayerStatuses(records, times, day, day.atTime(23, 59))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.PRAYED)
        assertThat(resolved[PrayerName.DHUHR]).isEqualTo(PrayerDisplayStatus.LATE)
        assertThat(resolved[PrayerName.ASR]).isEqualTo(PrayerDisplayStatus.MISSED)
        assertThat(resolved[PrayerName.MAGHRIB]).isEqualTo(PrayerDisplayStatus.QADA)
        assertThat(resolved[PrayerName.ISHA]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `NOT_PRAYED and PENDING are absence, not assertions`() {
        val records = listOf(
            record(PrayerName.FAJR, PrayerStatus.NOT_PRAYED),
            record(PrayerName.DHUHR, PrayerStatus.PENDING),
        )
        val resolved = resolvePrayerStatuses(records, times, day, day.atTime(18, 10))

        assertThat(resolved[PrayerName.FAJR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
        assertThat(resolved[PrayerName.DHUHR]).isEqualTo(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `without prayer times a past day still derives not recorded`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = null,
            date = day, now = day.plusDays(1).atTime(9, 0),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.NOT_RECORDED)
    }

    @Test
    fun `without prayer times today claims nothing has passed`() {
        val resolved = resolvePrayerStatuses(
            records = emptyList(), times = null,
            date = day, now = day.atTime(23, 59),
        )
        assertThat(resolved.values.toSet()).containsExactly(PrayerDisplayStatus.UPCOMING)
    }

    @Test
    fun `isDone counts the three ways a prayer can be fulfilled`() {
        assertThat(PrayerDisplayStatus.entries.filter { it.isDone() }).containsExactly(
            PrayerDisplayStatus.PRAYED, PrayerDisplayStatus.LATE, PrayerDisplayStatus.QADA,
        )
    }

    @Test
    fun `today counts only the prayers whose time has passed`() {
        val onlyNotRecorded = resolvePrayerStatuses(emptyList(), times, day, day.atTime(18, 10))
        assertThat(onlyNotRecorded.values.count { it == PrayerDisplayStatus.NOT_RECORDED })
            .isEqualTo(3)
    }
}
