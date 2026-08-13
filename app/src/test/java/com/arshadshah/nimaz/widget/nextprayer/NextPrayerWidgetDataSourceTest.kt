package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.core.time.TodayProvider
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
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

/**
 * What the next-prayer widget shows.
 *
 * This is the branchiest of the six widgets — today's next prayer, tomorrow's Fajr once the day
 * has run out, and a last-resort state when even that cannot be computed — and until now not one
 * of those branches was covered. `NextPrayerWorker.doWork()` returns early when no widget is
 * placed, which is always the case on a test device (#474).
 *
 * The rollover to tomorrow is the branch that matters most: it is what the widget displays every
 * evening, and it was untestable while the worker called `Clock.System.now()` and
 * `LocalDate.now()` directly.
 */
class NextPrayerWidgetDataSourceTest {

    private val calculator: PrayerTimeCalculator = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate.of(2026, 8, 12)

    /** Builds an Instant at a wall-clock time today, in the system zone the source reads. */
    private fun at(hour: Int, minute: Int = 0): Instant {
        val local = java.time.LocalDateTime.of(today, java.time.LocalTime.of(hour, minute))
        return Instant.fromEpochMilliseconds(
            local.atZone(ZoneId.of(zone.id)).toInstant().toEpochMilli()
        )
    }

    private fun clockAt(hour: Int, minute: Int = 0): Clock =
        Clock.fixed(
            java.time.Instant.ofEpochMilli(at(hour, minute).toEpochMilliseconds()),
            ZoneId.of(zone.id),
        )

    private val todaysPrayers = listOf(
        PrayerTime(PrayerType.FAJR, at(5, 0)),
        PrayerTime(PrayerType.DHUHR, at(13, 0)),
        PrayerTime(PrayerType.ASR, at(17, 0)),
        PrayerTime(PrayerType.MAGHRIB, at(20, 0)),
        PrayerTime(PrayerType.ISHA, at(21, 30)),
    )

    @Before
    fun setUp() {
        every { settings.latitude } returns flowOf(51.5)
        every { settings.longitude } returns flowOf(-0.12)
        every { settings.use24HourFormat } returns flowOf(true)
        every { todayProvider.today() } returns today
    }

    private fun source(clock: Clock) =
        NextPrayerWidgetDataSource(calculator, settings, todayProvider, clock)

    private fun givenToday(prayers: List<PrayerTime> = todaysPrayers) {
        every { calculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any()) } returns prayers
    }

    @Test
    fun `mid-morning, the next prayer is Dhuhr`() = runTest {
        givenToday()

        val data = source(clockAt(9, 0)).load()

        assertThat(data.prayerName).isEqualTo(PrayerType.DHUHR.displayName)
        assertThat(data.isTomorrow).isFalse()
        assertThat(data.isValid).isTrue()
        assertThat(data.nextPrayerEpochMillis).isEqualTo(at(13, 0).toEpochMilliseconds())
    }

    /** A prayer exactly now has passed — strictly-after, or the widget sticks on it for a minute. */
    @Test
    fun `a prayer at exactly the current time is treated as passed`() = runTest {
        givenToday()

        val data = source(clockAt(13, 0)).load()

        assertThat(data.prayerName).isEqualTo(PrayerType.ASR.displayName)
    }

    @Test
    fun `the countdown is populated for today's next prayer`() = runTest {
        givenToday()

        val data = source(clockAt(12, 0)).load()

        assertThat(data.countdown).isNotEmpty()
        assertThat(data.countdown).isNotEqualTo("—")
    }

    /**
     * After Isha, every prayer for today has passed. This is the state the widget sits in every
     * single evening, and it was the least testable part of the worker.
     */
    @Test
    fun `after the last prayer it rolls over to tomorrow's Fajr`() = runTest {
        val tomorrowFajr = PrayerTime(
            PrayerType.FAJR,
            Instant.fromEpochMilliseconds(at(5, 0).toEpochMilliseconds() + 24 * 60 * 60 * 1000L),
        )
        every {
            calculator.getPrayerTimes(any(), any(), eq(today), any(), any(), any(), any())
        } returns todaysPrayers
        every {
            calculator.getPrayerTimes(any(), any(), eq(today.plusDays(1)), any(), any(), any(), any())
        } returns listOf(tomorrowFajr)

        val data = source(clockAt(22, 0)).load()

        assertThat(data.isTomorrow).isTrue()
        assertThat(data.prayerName).isEqualTo(PrayerType.FAJR.displayName)
        assertThat(data.isValid).isTrue()
        // Deliberately blank: the widget renders "Tomorrow" beside the countdown, and a bare
        // time with no date beside it reads as today's.
        assertThat(data.prayerTime).isEmpty()
        assertThat(data.nextPrayerEpochMillis).isEqualTo(tomorrowFajr.time.toEpochMilliseconds())
    }

    /**
     * A bad location can leave even tomorrow uncomputable. The widget must still name a prayer:
     * "Fajr —" reads as "not known yet", an empty box reads as broken.
     */
    @Test
    fun `an uncomputable tomorrow still renders a named prayer`() = runTest {
        every {
            calculator.getPrayerTimes(any(), any(), eq(today), any(), any(), any(), any())
        } returns todaysPrayers
        every {
            calculator.getPrayerTimes(any(), any(), eq(today.plusDays(1)), any(), any(), any(), any())
        } returns emptyList()

        val data = source(clockAt(22, 0)).load()

        assertThat(data.isTomorrow).isTrue()
        assertThat(data.prayerName).isEqualTo(PrayerType.FAJR.displayName)
        assertThat(data.countdown).isEqualTo("—")
        assertThat(data.isValid).isTrue()
    }

    @Test
    fun `no prayer times at all still rolls over rather than failing`() = runTest {
        every { calculator.getPrayerTimes(any(), any(), any(), any(), any(), any(), any()) } returns emptyList()

        val data = source(clockAt(9, 0)).load()

        assertThat(data.isTomorrow).isTrue()
        assertThat(data.isValid).isTrue()
    }
}
