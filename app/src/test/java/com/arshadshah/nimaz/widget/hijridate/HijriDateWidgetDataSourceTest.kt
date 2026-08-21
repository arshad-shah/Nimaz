package com.arshadshah.nimaz.widget.hijridate

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * What the Hijri-date widget shows.
 *
 * `HijriDateWorker.doWork()` returns early when no widget is placed, which is always the case on
 * a test device, so none of this was covered by `WidgetWorkersTest` (#474).
 *
 * The offset behaviour asserted here is **current behaviour, not desired behaviour** — see
 * `the offset shifts the Gregorian date too` below.
 */
class HijriDateWidgetDataSourceTest {

    private val settings: SettingsRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    /** A Wednesday, so the weekday label is unambiguous. */
    private val wednesday = LocalDate.of(2026, 8, 12)

    private fun dataSource() = HijriDateWidgetDataSource(settings, todayProvider)

    private fun given(offset: Int) {
        every { settings.hijriDayOffset } returns flowOf(offset)
        every { todayProvider.today() } returns wednesday
    }

    @Test
    fun `with no offset the Gregorian half is today`() = runTest {
        given(offset = 0)

        val data = dataSource().load()

        assertThat(data.gregorianDayOfWeek).isEqualTo("Wednesday")
        assertThat(data.gregorianDate).isEqualTo("12 Aug")
    }

    @Test
    fun `the Hijri half is populated`() = runTest {
        given(offset = 0)

        val data = dataSource().load()

        assertThat(data.hijriDay).isIn(1..30)
        assertThat(data.hijriMonth).isNotEmpty()
        assertThat(data.hijriYear).isGreaterThan(1400)
    }

    /**
     * **This pins a bug, deliberately.**
     *
     * `hijriDayOffset` is a moon-sighting correction to the *Hijri* date. It is also added to the
     * Gregorian date rendered beside it, so a user who sets +1 sees **tomorrow's** Gregorian day
     * and weekday on their widget — a date the phone knows exactly and cannot be wrong about.
     *
     * The behaviour is carried over unchanged by the extraction it sits in, because a refactor
     * that silently changes what a widget displays is not reviewable. This test exists so the
     * behaviour is *stated* rather than lurking, and it is the test that will need inverting when
     * the fix lands. See #509.
     */
    @Test
    fun `the offset shifts the Gregorian date too - current behaviour, filed as a bug`() = runTest {
        given(offset = 1)

        val data = dataSource().load()

        assertThat(data.gregorianDayOfWeek).isEqualTo("Thursday")
        assertThat(data.gregorianDate).isEqualTo("13 Aug")
    }

    @Test
    fun `a negative offset shifts it backwards`() = runTest {
        given(offset = -1)

        val data = dataSource().load()

        assertThat(data.gregorianDayOfWeek).isEqualTo("Tuesday")
        assertThat(data.gregorianDate).isEqualTo("11 Aug")
    }
}
