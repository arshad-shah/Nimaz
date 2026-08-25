package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatFastLength
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.DayPrayerTimes
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.model.IslamicEvents
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * What a single row of the month table says about its day, beyond the six times.
 *
 * Three things are computed per row rather than passed in, and each is a claim about the day:
 *
 * - **"Today"**, which is compared against the *system* date inside the composable rather than
 *   taken from state. It is what makes one row in thirty findable at a glance, and a row that
 *   loses it leaves the reader counting.
 * - **The Islamic event**, matched by Hijri month and day against the shipped catalogue and
 *   reduced to the highest-priority one. A row that showed a *lower*-priority event, or none,
 *   would silently drop Ashura or an Eid from the timetable of the month it falls in.
 * - **The fast length**, shown only in Ramadan. Elsewhere it is noise; in Ramadan it is the
 *   number people plan their day around.
 *
 * Dates here are derived from `HijriDateCalculator`, not hardcoded: an assertion pinned to a
 * Gregorian date would start failing on its own the year the Hijri calendar moved past it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp-mdpi")
class MonthlyPrayerTimesEventRowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(MonthlyPrayerTimesUiState())

    private val viewModel: MonthlyPrayerTimesViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@MonthlyPrayerTimesEventRowTest.state
        every { onEvent(any()) } answers { firstArg<MonthlyPrayerTimesEvent>(); Unit }
    }

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun at(date: LocalDate, hour: Int, minute: Int): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(
            date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    private fun day(date: LocalDate, fastMinutes: Int? = null) = DayPrayerTimes(
        date = date,
        fajr = at(date, 5, 45),
        sunrise = at(date, 7, 12),
        dhuhr = at(date, 12, 30),
        asr = at(date, 15, 15),
        maghrib = at(date, 17, 48),
        isha = at(date, 19, 18),
        fastMinutes = fastMinutes,
    )

    private fun render(days: List<DayPrayerTimes>, month: YearMonth = YearMonth.from(days.first().date)) {
        state.value = MonthlyPrayerTimesUiState(
            currentMonth = month,
            dayPrayerTimes = days,
            locationName = "Dublin, Ireland",
            methodLabel = "MWL · Standard",
            isLoading = false,
        )
        composeRule.setThemedContent {
            MonthlyPrayerTimesScreen(onNavigateBack = {}, viewModel = viewModel)
        }
        composeRule.waitForIdle()
    }

    /** The next Gregorian date carrying an event of [type], searched forward from today. */
    private fun dateWithEvent(type: IslamicEventType): Pair<LocalDate, String> {
        val hijriToday = HijriDateCalculator.toHijri(LocalDate.now())
        for (year in hijriToday.year..(hijriToday.year + 1)) {
            val event = IslamicEvents.events.firstOrNull { it.eventType == type } ?: continue
            val date = HijriDateCalculator.toGregorian(event.hijriDay, event.hijriMonth, year)
            return date to event.nameEnglish
        }
        error("no event of type $type in the shipped catalogue")
    }

    @Test
    fun `today's row is labelled as today`() {
        val today = LocalDate.now()
        render(listOf(day(today.minusDays(1)), day(today), day(today.plusDays(1))))

        // Compared against the system date inside the row, so this is the only place the "today"
        // treatment — the label, the gradient fill and the highlighted day badge — can be
        // checked at all.
        composeRule.onAllNodesWithText(str(R.string.today), substring = true).onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun `a day with no event carries no event tag`() {
        val (eventDate, eventName) = dateWithEvent(IslamicEventType.HOLIDAY)
        // The day after an event is deliberately chosen: an off-by-one in the Hijri match would
        // put the tag here instead.
        render(listOf(day(eventDate.plusDays(1))))

        composeRule.onNodeWithText(eventName, substring = true).assertDoesNotExist()
    }

    @Test
    fun `a holiday is tagged, and starred`() {
        val (date, name) = dateWithEvent(IslamicEventType.HOLIDAY)
        render(listOf(day(date)))

        // Holidays get a star ahead of the name — the one event kind the row distinguishes
        // visually rather than only by colour.
        composeRule.onNodeWithText("★ $name").assertIsDisplayed()
    }

    @Test
    fun `a fasting day is tagged without the star`() {
        val (date, name) = dateWithEvent(IslamicEventType.FAST)
        render(listOf(day(date)))

        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun `a night of worship is tagged`() {
        val (date, name) = dateWithEvent(IslamicEventType.NIGHT)
        render(listOf(day(date)))

        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun `a historical day is tagged`() {
        val (date, name) = dateWithEvent(IslamicEventType.HISTORICAL)
        render(listOf(day(date)))

        composeRule.onNodeWithText(name).assertIsDisplayed()
    }

    @Test
    fun `in ramadan the row states the fast length`() {
        val hijriYear = HijriDateCalculator.toHijri(LocalDate.now()).year
        val ramadanDay = HijriDateCalculator.getFirstDayOfRamadan(hijriYear).plusDays(3)
        render(listOf(day(ramadanDay, fastMinutes = 903)))

        composeRule.onNodeWithText(
            str(R.string.monthly_fast_length_format, formatFastLength(903)),
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun `outside ramadan the fast length is not shown even when it is known`() {
        val hijriYear = HijriDateCalculator.toHijri(LocalDate.now()).year
        // Shawwal — the month straight after Ramadan, so the guard is `month == 9` and not
        // "some month near Ramadan".
        val afterRamadan = HijriDateCalculator.getLastDayOfRamadan(hijriYear).plusDays(5)
        render(listOf(day(afterRamadan, fastMinutes = 903)))

        composeRule.onNodeWithText(formatFastLength(903), substring = true).assertDoesNotExist()
    }
}
