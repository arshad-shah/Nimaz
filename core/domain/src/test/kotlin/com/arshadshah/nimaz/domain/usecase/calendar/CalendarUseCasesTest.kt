package com.arshadshah.nimaz.domain.usecase.calendar

import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The Hijri projection, at the level where it can actually be tested.
 *
 * An [IslamicEvent] carries a Hijri month and day and recurs every Hijri year, so projecting
 * it onto the Gregorian calendar always means choosing a year. The ViewModel chose the
 * *current* one and stopped, which is right for eleven months of the year and wrong for the
 * twelfth — and being wrong in the twelfth month means losing exactly the events people look
 * for at the turn of the year.
 */
class CalendarUseCasesTest {

    private val upcoming = GetUpcomingEventsUseCase()
    private val eventsForMonth = GetEventsForMonthUseCase()
    private val buildMonth = BuildCalendarMonthUseCase(GetEventsForDateUseCase())

    private val newYear = event(id = "new-year", hijriMonth = 1, hijriDay = 1)
    private val ashura = event(id = "ashura", hijriMonth = 1, hijriDay = 10)
    private val ramadanStart = event(id = "ramadan", hijriMonth = 9, hijriDay = 1)

    private val events = listOf(newYear, ashura, ramadanStart)

    @Test
    fun `Muharram events are upcoming when today is late Dhul-Hijjah`() {
        // 25 Dhul-Hijjah: Islamic New Year and Ashura are days away, in the *next* Hijri year.
        val today = HijriDateCalculator.toGregorian(25, 12, 1446)

        val result = upcoming(events, today).map { it.id }

        // Projected into the current Hijri year alone, both land ~355 days in the past and
        // are filtered out as already gone — the list silently loses them.
        assertThat(result).containsAtLeast("new-year", "ashura")
    }

    @Test
    fun `an upcoming event is dated in the year it will actually happen`() {
        val today = HijriDateCalculator.toGregorian(25, 12, 1446)

        val ashuraDate = upcoming(events, today).first { it.id == "ashura" }.gregorianDate

        assertThat(ashuraDate).isNotNull()
        assertThat(ashuraDate!!).isAtLeast(today)
        assertThat(HijriDateCalculator.toHijri(ashuraDate).year).isEqualTo(1447)
    }

    @Test
    fun `events outside the window are left out`() {
        val today = HijriDateCalculator.toGregorian(1, 1, 1447)

        // Ramadan is eight Hijri months away — well outside the three-month window.
        assertThat(upcoming(events, today).map { it.id }).doesNotContain("ramadan")
    }

    @Test
    fun `the month that contains 1 Muharram lists its Muharram events`() {
        // The Gregorian month straddling the Hijri year boundary: its first day is in 1446,
        // its later days in 1447. Taking the Hijri year from the first day alone projects
        // every Muharram event ~11 months earlier and drops all of them.
        val firstMuharram = HijriDateCalculator.toGregorian(1, 1, 1447)

        val result = eventsForMonth(firstMuharram.monthValue, firstMuharram.year, events)

        assertThat(result.map { it.id }).contains("new-year")
    }

    @Test
    fun `an ordinary month is unaffected by trying the next year too`() {
        val ramadanDay = HijriDateCalculator.toGregorian(1, 9, 1447)

        val result = eventsForMonth(ramadanDay.monthValue, ramadanDay.year, events)

        // Exactly one projection may land in the window — never a duplicate from both years.
        assertThat(result.filter { it.id == "ramadan" }).hasSize(1)
    }

    @Test
    fun `today is whatever the caller says it is, not what the system clock says`() {
        val someMonth = LocalDate.of(2026, 3, 1)
        val chosenToday = LocalDate.of(2026, 3, 14)

        val month = buildMonth(3, 2026, events, chosenToday)

        assertThat(month.days.filter { it.isToday }.map { it.gregorianDate })
            .containsExactly(chosenToday)
        assertThat(month.days.first().gregorianDate).isEqualTo(someMonth)
    }

    @Test
    fun `a month built for a day outside it highlights nothing`() {
        val month = buildMonth(3, 2026, events, LocalDate.of(2026, 5, 2))

        assertThat(month.days.none { it.isToday }).isTrue()
    }
}

private fun event(id: String, hijriMonth: Int, hijriDay: Int) = IslamicEvent(
    id = id,
    nameArabic = "",
    nameEnglish = id,
    description = null,
    hijriMonth = hijriMonth,
    hijriDay = hijriDay,
    eventType = IslamicEventType.HISTORICAL,
    isHoliday = false,
    isFastingDay = false,
    isNightOfPower = false,
    gregorianDate = null,
    year = null,
    notes = null,
    priority = 0,
)
