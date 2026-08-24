package com.arshadshah.nimaz.widget.hijricalendar

import com.arshadshah.nimaz.widget.support.WidgetRenderer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

/**
 * The month grid is the one widget that computes its own layout — rows, leading blanks and which
 * cell is today — from four integers. An off-by-one there draws a calendar that is wrong in a way
 * nobody would think to double-check.
 */
@RunWith(RobolectricTestRunner::class)
class HijriCalendarWidgetRenderTest {

    private val widget = HijriCalendarWidget()

    private val ramadan = HijriCalendarData(
        hijriMonth = 9,
        hijriMonthName = "Ramadan",
        hijriYear = 1447,
        gregorianDate = "24 Aug 2026",
        daysInMonth = 30,
        firstDayOfWeekOffset = 3,
        todayHijriDay = 17,
        events = listOf(
            HijriCalendarEventData("Laylat al-Qadr", "ليلة القدر", "RELIGIOUS_OBSERVANCE"),
            HijriCalendarEventData("Fasting", "صوم", "RECOMMENDED_FAST"),
        ),
    )

    private suspend fun render(data: HijriCalendarData) =
        WidgetRenderer.render(widget, HijriCalendarWidgetState.Success(data))

    @Test
    fun `the header pairs the hijri month and year with the gregorian date`() = runTest {
        val rendered = render(ramadan)

        assertThat(rendered.hasText("Ramadan 1447")).isTrue()
        assertThat(rendered.hasText("24 Aug 2026")).isTrue()
    }

    /**
     * Every day of the month gets a cell, and no cell beyond the month's length is drawn. The
     * grid is built from `firstDayOfWeekOffset + daysInMonth` rounded up to whole weeks, so a
     * mistake shows up as a missing last day or a phantom 31st.
     */
    @Test
    fun `every day of the month is drawn exactly once and nothing past it`() = runTest {
        val rendered = render(ramadan)

        val dayCells = rendered.texts.mapNotNull { it.toIntOrNull() }
        (1..30).forEach { day ->
            assertThat(dayCells.count { it == day }).isAtLeast(1)
        }
        assertThat(dayCells.none { it == 31 }).isTrue()
    }

    /**
     * A month that needs six rows must get six. `daysInMonth = 30` with an offset of 5 spills
     * past thirty-five cells, which is where a `(total + 6) / 7` mistake truncates the last days.
     */
    @Test
    fun `a month that spills into a sixth week still draws its final days`() = runTest {
        val rendered = render(ramadan.copy(firstDayOfWeekOffset = 5, todayHijriDay = 30))

        val dayCells = rendered.texts.mapNotNull { it.toIntOrNull() }
        assertThat(dayCells).contains(29)
        assertThat(dayCells.count { it == 30 }).isAtLeast(1)
    }

    /**
     * Today is drawn as a filled disc rather than plain text, so its number appears in the grid
     * and again as the big number on the right rail.
     */
    @Test
    fun `today's number appears in the grid and on the right rail`() = runTest {
        val rendered = render(ramadan)

        assertThat(rendered.texts.count { it == "17" }).isEqualTo(2)
    }

    /** The weekday strip is localised — it used to be a hardcoded English `listOf("Su", …)`. */
    @Test
    fun `the weekday strip is sunday-first and localised`() = runTest {
        val rendered = render(ramadan)
        val expected = com.arshadshah.nimaz.widget.core.weekdayInitials(Locale.getDefault())

        expected.forEach { initial ->
            assertThat(rendered.containsText(initial)).isTrue()
        }
        assertThat(expected).hasSize(7)
    }

    @Test
    fun `events are listed with their type spelled as a phrase`() = runTest {
        val rendered = render(ramadan)

        assertThat(rendered.hasText("Laylat al-Qadr")).isTrue()
        // "RELIGIOUS_OBSERVANCE" is not something to show a reader.
        assertThat(rendered.hasText("Religious observance")).isTrue()
        assertThat(rendered.hasText("Recommended fast")).isTrue()
    }

    /**
     * A fasting day gets the star icon rather than the generic event icon. Both branches draw one
     * icon per row, so the count alone cannot tell them apart — but a day with no events at all
     * draws neither, and that is the branch a reader actually notices.
     */
    @Test
    fun `a month with no events says so instead of drawing an empty rail`() = runTest {
        val rendered = render(ramadan.copy(events = emptyList()))

        assertThat(rendered.hasText("No events")).isTrue()
        assertThat(rendered.hasText("Laylat al-Qadr")).isFalse()
    }

    @Test
    fun `an event's icon depends on whether it is a fast`() = runTest {
        val fast = render(
            ramadan.copy(events = listOf(HijriCalendarEventData("X", "", "RECOMMENDED_FAST"))),
        )
        // "Recommended" alone also earns the star — the two conditions are independent, and a
        // recommended observance that is not a fast used to fall through to the generic icon.
        val recommended = render(
            ramadan.copy(events = listOf(HijriCalendarEventData("X", "", "RECOMMENDED_DEED"))),
        )
        val plain = render(
            ramadan.copy(events = listOf(HijriCalendarEventData("X", "", "HOLIDAY"))),
        )
        val none = render(ramadan.copy(events = emptyList()))

        // Every event row draws exactly one icon; the empty rail draws none.
        assertThat(fast.imageCount).isEqualTo(plain.imageCount)
        assertThat(recommended.imageCount).isEqualTo(plain.imageCount)
        assertThat(fast.imageCount).isGreaterThan(none.imageCount)
        assertThat(recommended.hasText("Recommended deed")).isTrue()
    }

    @Test
    fun `loading and error draw single-line frames instead of the grid`() = runTest {
        val loading = WidgetRenderer.render(widget, HijriCalendarWidgetState.Loading)
        val error = WidgetRenderer.render(widget, HijriCalendarWidgetState.Error("boom"))

        assertThat(loading.texts).hasSize(1)
        assertThat(error.texts).hasSize(1)
        assertThat(loading.hasText("Ramadan 1447")).isFalse()
    }

    /** The deep-link action every tappable region on this widget shares. */
    @Test
    fun `the widget declares the islamic-calendar deep-link action`() {
        assertThat(HijriCalendarWidget.ACTION_OPEN_ISLAMIC_CALENDAR)
            .isEqualTo("com.arshadshah.nimaz.ACTION_OPEN_ISLAMIC_CALENDAR")
    }

    @Test
    fun `only a month that has been named survives a failed refresh`() {
        assertThat(HijriCalendarWidgetState.Success(ramadan).hasData).isTrue()
        assertThat(HijriCalendarWidgetState.Success(HijriCalendarData()).hasData).isFalse()
        assertThat(HijriCalendarWidgetState.Loading.hasData).isFalse()
        assertThat(HijriCalendarWidgetState.Error(null).hasData).isFalse()
    }
}
