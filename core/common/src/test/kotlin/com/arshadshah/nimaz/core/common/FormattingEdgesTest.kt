package com.arshadshah.nimaz.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The formatting helpers' edges — the arms the happy-path tests never reach.
 *
 * Every one of these is a branch that only fires in a situation a developer does not have: a
 * currency the device has no symbol for, a countdown that has just run out, a widget rendering
 * at 00:00, a Spanish date pattern. They are one line each and none of them is checked anywhere
 * else, which is the combination that lets a wrong one ship.
 *
 * `formatWidgetTime` is the sharpest. Widgets render through Glance, so a throw inside one does
 * not surface as a crash the user can report — the widget just stops updating and keeps showing
 * whatever it last drew. An out-of-range hour reaching `LocalTime.of` would do exactly that,
 * which is why the coercion is there and why it is worth a test.
 */
class FormattingEdgesTest {

    // ---- Widget clock ----

    @Test
    fun `a 12-hour widget time carries no am pm marker, because the grid is too narrow`() {
        val formatted = formatWidgetTime(hour = 13, minute = 5)

        assertThat(formatted).isEqualTo("1:05")
    }

    @Test
    fun `a 24-hour widget time is zero-padded`() {
        assertThat(formatWidgetTime(hour = 5, minute = 5, use24Hour = true)).isEqualTo("05:05")
    }

    @Test
    fun `asking for the marker overrides the bare form`() {
        val withMarker = formatWidgetTime(hour = 13, minute = 5, includeAmPm = true)

        assertThat(withMarker).isNotEqualTo("1:05")
        assertThat(withMarker).contains("1:05")
    }

    @Test
    fun `24-hour wins over the marker, because it has no marker to show`() {
        assertThat(formatWidgetTime(hour = 13, minute = 5, includeAmPm = true, use24Hour = true))
            .isEqualTo("13:05")
    }

    @Test
    fun `midnight and the last minute of the day both render`() {
        assertThat(formatWidgetTime(hour = 0, minute = 0)).isEqualTo("12:00")
        assertThat(formatWidgetTime(hour = 23, minute = 59, use24Hour = true)).isEqualTo("23:59")
    }

    @Test
    fun `an impossible time is clamped rather than thrown`() {
        // A throw inside a Glance widget is not a crash the user can report — the widget simply
        // stops updating and keeps showing whatever it last drew.
        assertThat(formatWidgetTime(hour = 99, minute = 99, use24Hour = true)).isEqualTo("23:59")
        assertThat(formatWidgetTime(hour = -5, minute = -5, use24Hour = true)).isEqualTo("00:00")
    }

    // ---- Widget countdown ----

    @Test
    fun `a countdown over an hour shows hours and minutes, and drops the seconds`() {
        assertThat(formatWidgetCountdown(2 * 3600 + 30 * 60 + 15)).isEqualTo("2h 30m")
    }

    @Test
    fun `a countdown under an hour shows minutes and seconds`() {
        assertThat(formatWidgetCountdown(15 * 60 + 42)).isEqualTo("15m 42s")
    }

    @Test
    fun `a countdown under a minute shows seconds alone`() {
        assertThat(formatWidgetCountdown(30)).isEqualTo("30s")
    }

    @Test
    fun `a countdown that has run out shows a dash, not a zero or a negative`() {
        assertThat(formatWidgetCountdown(0)).isEqualTo("—")
        assertThat(formatWidgetCountdown(-90)).isEqualTo("—")
    }

    @Test
    fun `the boundaries between the three shapes fall the right way`() {
        assertThat(formatWidgetCountdown(3600)).isEqualTo("1h 0m")
        assertThat(formatWidgetCountdown(3599)).isEqualTo("59m 59s")
        assertThat(formatWidgetCountdown(60)).isEqualTo("1m 0s")
        assertThat(formatWidgetCountdown(59)).isEqualTo("59s")
    }

    // ---- Countdown decomposition ----

    @Test
    fun `the lead unit is the coarsest one that is not zero`() {
        // It drives which units a renderer shows, so an hour-long wait must not read as minutes.
        assertThat(countdownOf(2.hours).leadUnit).isEqualTo(CountdownUnit.HOURS)
        assertThat(countdownOf(20.minutes).leadUnit).isEqualTo(CountdownUnit.MINUTES)
        assertThat(countdownOf(20.seconds).leadUnit).isEqualTo(CountdownUnit.SECONDS)
    }

    @Test
    fun `an elapsed countdown leads with seconds rather than nothing`() {
        assertThat(CountdownParts.ZERO.leadUnit).isEqualTo(CountdownUnit.SECONDS)
        assertThat(CountdownParts.ZERO.elapsed).isTrue()
    }

    @Test
    fun `the parts add back up to the duration they came from`() {
        val parts = countdownOf(2.hours + 30.minutes + 15.seconds)

        assertThat(parts.totalSeconds).isEqualTo(2 * 3600 + 30 * 60 + 15)
    }

    @Test
    fun `a duration that has just elapsed clamps rather than going negative`() {
        // The `isPassed` check and the render happen at different points in a frame, so an
        // instant can elapse between them.
        assertThat(countdownOf((-1).seconds)).isEqualTo(CountdownParts.ZERO)
    }

    // ---- Currency ----

    @Test
    fun `a currency the device knows renders its symbol`() {
        assertThat(currencySymbolOf("USD")).isNotEmpty()
        assertThat(currencyLabel("USD")).contains("(")
    }

    @Test
    fun `a code that is not a currency renders as itself rather than throwing`() {
        // The picker is fed ISO codes from settings, which a restored backup can carry from a
        // build that knew more of them than this device does.
        assertThat(currencySymbolOf("NOTACODE")).isEqualTo("NOTACODE")
        assertThat(currencyLabel("NOTACODE")).isEqualTo("NOTACODE")
    }

    @Test
    fun `a currency with no symbol on this device is not written twice`() {
        // `Currency` returns the code as the symbol when it has none, and "Cayman Islands Dollar
        // (KYD)" is the shape that gets avoided.
        val label = currencyLabel("KYD")

        if (currencySymbolOf("KYD") == "KYD") {
            assertThat(label).doesNotContain("(KYD)")
        } else {
            assertThat(label).contains("(")
        }
    }

    @Test
    fun `grouped numbers carry their separators`() {
        val grouped = formatGrouped(1_234_567)

        assertThat(grouped).isNotEqualTo("1234567")
        assertThat(grouped.filter { it.isDigit() }).hasLength(7)
    }

    @Test
    fun `zero and a negative both format`() {
        assertThat(formatGrouped(0)).isEqualTo("0")
        assertThat(formatGrouped(-1000)).contains("1")
    }

    // ---- Date patterns ----

    @Test
    fun `every date shape renders for a fixed date`() {
        val date = LocalDate.of(2026, 3, 20)

        assertThat(date.formatFullDate(Locale.UK)).isNotEmpty()
        assertThat(date.formatLongDate(Locale.UK)).isNotEmpty()
        assertThat(date.formatMediumDate(Locale.UK)).isNotEmpty()
        assertThat(date.formatMonthYear(Locale.UK)).isNotEmpty()
        assertThat(date.formatDayMonth(Locale.UK)).isNotEmpty()
        assertThat(date.formatWeekdayDayMonth(Locale.UK)).isNotEmpty()
        assertThat(date.formatWeekday(Locale.UK)).isNotEmpty()
        assertThat(YearMonth.of(2026, 3).formatMonthYear(Locale.UK)).isNotEmpty()
    }

    @Test
    fun `each shape is asked without a locale too, taking the device's`() {
        // The default-argument overloads are what every call site actually uses.
        val date = LocalDate.of(2026, 3, 20)

        assertThat(date.formatLongDate()).isNotEmpty()
        assertThat(date.formatMediumDate()).isNotEmpty()
        assertThat(date.formatMonthYear()).isNotEmpty()
        assertThat(date.formatDayMonth()).isNotEmpty()
        assertThat(date.formatWeekdayDayMonth()).isNotEmpty()
        assertThat(date.formatWeekday()).isNotEmpty()
        assertThat(YearMonth.of(2026, 3).formatMonthYear()).isNotEmpty()
    }

    @Test
    fun `a locale whose pattern quotes literals keeps them as words`() {
        // Spanish writes "d 'de' MMMM 'de' y"; treating that quoted `d` as a day field mangles it.
        val spanish = LocalDate.of(2026, 3, 20).formatLongDate(Locale.forLanguageTag("es"))

        assertThat(spanish).contains("de")
        assertThat(spanish).contains("2026")
    }

    @Test
    fun `a day-and-month shape drops the year, whatever the locale writes`() {
        val date = LocalDate.of(2026, 3, 20)

        assertThat(date.formatDayMonth(Locale.UK)).doesNotContain("2026")
        assertThat(date.formatDayMonth(Locale.US)).doesNotContain("2026")
        assertThat(date.formatDayMonth(Locale.forLanguageTag("es"))).doesNotContain("2026")
    }

    @Test
    fun `a weekday shape carries no digits at all`() {
        assertThat(LocalDate.of(2026, 3, 20).formatWeekday(Locale.UK).any { it.isDigit() })
            .isFalse()
    }

    @Test
    fun `midnight of a date is the same instant whichever way it is asked`() {
        val date = LocalDate.of(2026, 3, 20)

        assertThat(date.toUtcMidnightMillis()).isEqualTo(date.toEpochDay() * 86_400_000L)
    }
}
