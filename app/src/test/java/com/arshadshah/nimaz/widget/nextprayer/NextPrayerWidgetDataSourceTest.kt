package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.core.time.TodayProvider
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

    private val prayerRepository: PrayerRepository = mockk(relaxed = true)
    private val settings: SettingsRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    private val zone = TimeZone.currentSystemDefault()
    private val today = LocalDate.of(2026, 8, 12)

    /** The user's settings, which the widget must compute with rather than the defaults. */
    private val calculationSettings = PrayerCalculationSettings(
        location = resolveLocation(51.5, -0.12, "London, England"),
        calculationMethod = CalculationMethod.NORTH_AMERICA,
        asrCalculation = AsrCalculation.HANAFI,
        highLatitudeRule = null,
        adjustments = mapOf(PrayerType.FAJR to 3),
    )

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

    private val tomorrowFajr = PrayerTime(
        PrayerType.FAJR,
        Instant.fromEpochMilliseconds(at(5, 0).toEpochMilliseconds() + DAY_MILLIS),
    )

    @Before
    fun setUp() {
        every { settings.use24HourFormat } returns flowOf(true)
        every { todayProvider.today() } returns today
        every { prayerRepository.observeCalculationSettings() } returns flowOf(calculationSettings)
    }

    private fun source(clock: Clock) =
        NextPrayerWidgetDataSource(prayerRepository, settings, todayProvider, clock)

    private fun givenSchedule(
        todaysSchedule: List<PrayerTime> = todaysPrayers,
        tomorrowsSchedule: List<PrayerTime> = listOf(tomorrowFajr),
    ) {
        every { prayerRepository.getDaySchedule(today, any()) } returns todaysSchedule
        every { prayerRepository.getDaySchedule(today.plusDays(1), any()) } returns tomorrowsSchedule
    }

    @Test
    fun `mid-morning, the next prayer is Dhuhr`() = runTest {
        givenSchedule()

        val data = source(clockAt(9, 0)).load()

        assertThat(data.prayerName).isEqualTo(PrayerType.DHUHR.displayName)
        assertThat(data.isTomorrow).isFalse()
        assertThat(data.isValid).isTrue()
        assertThat(data.nextPrayerEpochMillis).isEqualTo(at(13, 0).toEpochMilliseconds())
    }

    /** A prayer exactly now has passed — strictly-after, or the widget sticks on it for a minute. */
    @Test
    fun `a prayer at exactly the current time is treated as passed`() = runTest {
        givenSchedule()

        val data = source(clockAt(13, 0)).load()

        assertThat(data.prayerName).isEqualTo(PrayerType.ASR.displayName)
    }

    @Test
    fun `the countdown is populated for today's next prayer`() = runTest {
        givenSchedule()

        val data = source(clockAt(12, 0)).load()

        assertThat(data.countdown).isNotEmpty()
        assertThat(data.countdown).isNotEqualTo("—")
    }

    /**
     * The whole point of the schedule: the widget redraws every minute and the worker runs every
     * fifteen, so the answer to "which prayer is next" has to be something the widget can work
     * out for itself. Without this the widget kept naming a prayer that had already started.
     */
    @Test
    fun `the whole day is published so the widget can advance on its own`() = runTest {
        givenSchedule()

        val data = source(clockAt(9, 0)).load()

        assertThat(data.schedule.map { it.prayerName })
            .containsExactly("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Fajr")
            .inOrder()
        // Selecting from that same payload two hours later moves on without the worker running.
        assertThat(data.nextEntry(at(14, 0).toEpochMilliseconds()).prayerName)
            .isEqualTo(PrayerType.ASR.displayName)
        assertThat(data.nextEntry(at(21, 0).toEpochMilliseconds()).prayerName)
            .isEqualTo(PrayerType.ISHA.displayName)
    }

    /** Past Isha, the widget selects tomorrow's Fajr out of the payload it already holds. */
    @Test
    fun `the schedule carries into tomorrow so the evening never runs out`() = runTest {
        givenSchedule()

        val data = source(clockAt(9, 0)).load()
        val lateEvening = data.nextEntry(at(23, 0).toEpochMilliseconds())

        assertThat(lateEvening.prayerName).isEqualTo(PrayerType.FAJR.displayName)
        assertThat(lateEvening.isTomorrow).isTrue()
    }

    /**
     * The widget used to compute with `getPrayerTimes(latitude, longitude)` and take all four
     * calculation defaults, so it showed Muslim World League / Shafi times to a user reading
     * North America / Hanafi times inside the app.
     */
    @Test
    fun `prayer times are computed with the user's calculation settings`() = runTest {
        givenSchedule()

        source(clockAt(9, 0)).load()

        verify { prayerRepository.getDaySchedule(today, calculationSettings) }
    }

    /**
     * After Isha, every prayer for today has passed. This is the state the widget sits in every
     * single evening, and it was the least testable part of the worker.
     */
    @Test
    fun `after the last prayer it rolls over to tomorrow's Fajr`() = runTest {
        givenSchedule()

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
        givenSchedule(tomorrowsSchedule = emptyList())

        val data = source(clockAt(22, 0)).load()

        assertThat(data.isTomorrow).isTrue()
        assertThat(data.prayerName).isEqualTo(PrayerType.FAJR.displayName)
        assertThat(data.countdown).isEqualTo("—")
        assertThat(data.isValid).isTrue()
    }

    @Test
    fun `no prayer times at all still rolls over rather than failing`() = runTest {
        givenSchedule(todaysSchedule = emptyList(), tomorrowsSchedule = emptyList())

        val data = source(clockAt(9, 0)).load()

        assertThat(data.isTomorrow).isTrue()
        assertThat(data.isValid).isTrue()
    }

    private companion object {
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
