package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/**
 * What the prayer-times widget shows.
 *
 * `PrayerTimesWorker.doWork()` returns early when no widget is placed, which is always the case
 * on a test device, so none of this was covered by `WidgetWorkersTest` (#474).
 */
class PrayerTimesWidgetDataSourceTest {

    private val calculator: PrayerTimeCalculator = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate.of(2026, 8, 12)

    private fun at(hour: Int, minute: Int): Instant {
        val local = java.time.LocalDateTime.of(today, java.time.LocalTime.of(hour, minute))
        return Instant.fromEpochMilliseconds(
            local.atZone(ZoneId.of(zone.id)).toInstant().toEpochMilli()
        )
    }

    private val fullDay = listOf(
        PrayerTime(PrayerType.FAJR, at(5, 12)),
        PrayerTime(PrayerType.DHUHR, at(13, 5)),
        PrayerTime(PrayerType.ASR, at(17, 30)),
        PrayerTime(PrayerType.MAGHRIB, at(20, 45)),
        PrayerTime(PrayerType.ISHA, at(22, 15)),
    )

    @Before
    fun setUp() {
        every { settings.latitude } returns flowOf(53.34)
        every { settings.longitude } returns flowOf(-6.26)
        every { settings.use24HourFormat } returns flowOf(true)
        every { settings.locationName } returns flowOf("Dublin, Leinster, Ireland")
        every { calculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any()) } returns fullDay
    }

    private fun source() = PrayerTimesWidgetDataSource(calculator, settings)

    @Test
    fun `each prayer lands in its own field`() = runTest {
        val data = source().load()

        assertThat(data.fajrTime).isEqualTo("05:12")
        assertThat(data.dhuhrTime).isEqualTo("13:05")
        assertThat(data.asrTime).isEqualTo("17:30")
        assertThat(data.maghribTime).isEqualTo("20:45")
        assertThat(data.ishaTime).isEqualTo("22:15")
    }

    @Test
    fun `epoch millis are carried for every prayer`() = runTest {
        val data = source().load()

        assertThat(data.fajrEpochMillis).isEqualTo(at(5, 12).toEpochMilliseconds())
        assertThat(data.ishaEpochMillis).isEqualTo(at(22, 15).toEpochMilliseconds())
    }

    /** The stored name is "City, Region, Country"; the widget has room for the city. */
    @Test
    fun `the location name is trimmed to its first component`() = runTest {
        assertThat(source().load().locationName).isEqualTo("Dublin")
    }

    @Test
    fun `a blank location name falls back rather than rendering empty`() = runTest {
        every { settings.locationName } returns flowOf("   ")

        assertThat(source().load().locationName).isNotEmpty()
    }

    /**
     * A missing prayer renders an em dash, not a blank — the row still has to read as a row.
     * Prayer times can genuinely be absent at high latitudes.
     */
    @Test
    fun `a missing prayer renders a dash and a zero epoch`() = runTest {
        every {
            calculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any())
        } returns listOf(PrayerTime(PrayerType.FAJR, at(5, 12)))

        val data = source().load()

        assertThat(data.fajrTime).isEqualTo("05:12")
        assertThat(data.dhuhrTime).isEqualTo("—")
        assertThat(data.ishaTime).isEqualTo("—")
        assertThat(data.ishaEpochMillis).isEqualTo(0L)
    }

    @Test
    fun `12-hour format is honoured`() = runTest {
        every { settings.use24HourFormat } returns flowOf(false)

        val data = source().load()

        assertThat(data.dhuhrTime).doesNotContain("13")
        assertThat(data.dhuhrTime).contains("1")
    }

    @Test
    fun `the hijri date is rendered as day and month name`() = runTest {
        val hijri = source().load().hijriDate

        assertThat(hijri).isNotEmpty()
        assertThat(hijri).contains(" ")
    }
}
