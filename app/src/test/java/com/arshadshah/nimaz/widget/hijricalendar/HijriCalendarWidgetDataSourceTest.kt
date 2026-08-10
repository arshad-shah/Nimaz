package com.arshadshah.nimaz.widget.hijricalendar

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * What the Hijri-calendar widget shows.
 *
 * `HijriCalendarWorker.doWork()` returns early when no widget is placed, which is always the case
 * on a test device, so none of this was covered by `WidgetWorkersTest` (#474).
 */
class HijriCalendarWidgetDataSourceTest {

    private val settings: SettingsRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    private val wednesday = LocalDate.of(2026, 8, 12)

    private fun source() = HijriCalendarWidgetDataSource(settings, todayProvider)

    private fun given(offset: Int = 0) {
        every { settings.hijriDayOffset } returns flowOf(offset)
        every { todayProvider.today() } returns wednesday
    }

    @Test
    fun `the month is described consistently`() = runTest {
        given()

        val data = source().load()

        assertThat(data.hijriMonth).isIn(1..12)
        assertThat(data.hijriMonthName).isNotEmpty()
        assertThat(data.hijriYear).isGreaterThan(1400)
        assertThat(data.todayHijriDay).isIn(1..30)
    }

    /**
     * A Hijri month is 29 or 30 days. A grid built on anything else is not a calendar.
     */
    @Test
    fun `the month has 29 or 30 days`() = runTest {
        given()

        assertThat(source().load().daysInMonth).isIn(listOf(29, 30))
    }

    /**
     * The grid starts on Sunday, but `java.time` numbers Monday=1 … Sunday=7. Getting the wrap
     * wrong shifts every date in the month by a column — it still looks like a calendar, which
     * is what makes it worth a test.
     */
    @Test
    fun `the first-day offset is a valid Sunday-first column`() = runTest {
        given()

        assertThat(source().load().firstDayOfWeekOffset).isIn(0..6)
    }

    @Test
    fun `the first-day offset agrees with the actual first of the month`() = runTest {
        given()

        val data = source().load()
        val firstOfMonth = HijriDateCalculator.toGregorian(1, data.hijriMonth, data.hijriYear)
        val expected = if (firstOfMonth.dayOfWeek.value == 7) 0 else firstOfMonth.dayOfWeek.value

        assertThat(data.firstDayOfWeekOffset).isEqualTo(expected)
    }

    @Test
    fun `today's events are only today's`() = runTest {
        given()

        val data = source().load()
        val all = HijriDateCalculator.getIslamicEvents(data.hijriYear)
        val expected = all.count { it.day == data.todayHijriDay && it.month == data.hijriMonth }

        assertThat(data.events).hasSize(expected)
    }

    @Test
    fun `with no offset the Gregorian half is today`() = runTest {
        given(offset = 0)

        assertThat(source().load().gregorianDate).isEqualTo("12 Aug")
    }

    /**
     * **Pins the same bug as the Hijri-date widget, deliberately** — see #509. The offset is a
     * Hijri correction and should not move the Gregorian date, but it does. Stated here rather
     * than left lurking; this is the test to invert when #509 is fixed.
     */
    @Test
    fun `the offset shifts the Gregorian date too - current behaviour, filed as #509`() = runTest {
        given(offset = 1)

        assertThat(source().load().gregorianDate).isEqualTo("13 Aug")
    }
}
