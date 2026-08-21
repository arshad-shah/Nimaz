package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerCalculationSettings
import com.arshadshah.nimaz.domain.model.PrayerTime
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private val prayerRepository: PrayerRepository = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate.of(2026, 8, 12)

    /** The user's settings, which the widget must compute with rather than the defaults. */
    private val calculationSettings = PrayerCalculationSettings(
        location = resolveLocation(53.34, -6.26, "Dublin, Leinster, Ireland"),
        calculationMethod = CalculationMethod.NORTH_AMERICA,
        asrCalculation = AsrCalculation.HANAFI,
        highLatitudeRule = null,
        adjustments = emptyMap(),
    )

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
        every { settings.use24HourFormat } returns flowOf(true)
        every { todayProvider.today() } returns today
        every { prayerRepository.observeCalculationSettings() } returns flowOf(calculationSettings)
        every { prayerRepository.getDaySchedule(today, any()) } returns fullDay
    }

    private fun source() = PrayerTimesWidgetDataSource(prayerRepository, settings, todayProvider)

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

    /**
     * The widget used to compute with `getPrayerTimes(latitude, longitude)` and take all four
     * calculation defaults, so the five times on the home screen disagreed with the five in the
     * app for anyone not on Muslim World League and Shafi.
     */
    @Test
    fun `prayer times are computed with the user's calculation settings`() = runTest {
        source().load()

        verify { prayerRepository.getDaySchedule(today, calculationSettings) }
    }

    /** The stored name is "City, Region, Country"; the widget has room for the city. */
    @Test
    fun `the location name is trimmed to its first component`() = runTest {
        assertThat(source().load().locationName).isEqualTo("Dublin")
    }

    /**
     * A real position whose reverse geocoding failed has no name. It used to render a hardcoded
     * "Dublin" — a city the reader is not in, stated as fact beside their own prayer times. The
     * widget shows its own localized "Location" label instead when this is empty.
     */
    @Test
    fun `an unnamed location is left empty rather than named wrongly`() = runTest {
        every { prayerRepository.observeCalculationSettings() } returns
            flowOf(calculationSettings.copy(location = resolveLocation(53.34, -6.26, "   ")))

        assertThat(source().load().locationName).isEmpty()
    }

    /** With no position stored at all, the resolved fallback still names itself. */
    @Test
    fun `an unset location falls back to the fallback location's name`() = runTest {
        every { prayerRepository.observeCalculationSettings() } returns
            flowOf(calculationSettings.copy(location = resolveLocation(0.0, 0.0)))

        assertThat(source().load().locationName).isEqualTo("Dublin")
    }

    /**
     * A missing prayer renders an em dash, not a blank — the row still has to read as a row.
     * Prayer times can genuinely be absent at high latitudes.
     */
    @Test
    fun `a missing prayer renders a dash and a zero epoch`() = runTest {
        every { prayerRepository.getDaySchedule(today, any()) } returns
            listOf(PrayerTime(PrayerType.FAJR, at(5, 12)))

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
