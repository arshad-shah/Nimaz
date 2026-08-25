package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.DayPrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
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
 * The month timetable: what the grid shows, and what the export chooser above it does.
 *
 * **The header and the grid are two renderings of one fact** — which month is on screen — and the
 * screen draws them from different places: the header from `currentMonth`, every row from its own
 * `DayPrayerTimes.date`. `:feature:calendar` shipped exactly this bug (fixed by
 * `CalendarMonth.displayedMonth`): a header naming one month above a grid of another, which does
 * not look like a bug, it looks like the prayer times are wrong. Nothing in the ViewModel test can
 * see it, because the ViewModel holds one month and is self-consistent by construction.
 *
 * **The export path is the other reason this exists.** `shareRows` renders a PDF *inside a click
 * handler* and reports the outcome through the ViewModel — start, then completed-or-failed. Under
 * Robolectric `PdfDocument` cannot render at all (`"document is closed!"`), which makes this the
 * one place the failure arm can be exercised for real: the assertion is that a month whose PDF
 * cannot be produced still reports, still leaves the timetable on screen, and never throws out of
 * composition. The exporter itself stays at 0% — see the module's `nimazCoverage` KDoc.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp-mdpi")
class MonthlyPrayerTimesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val month = YearMonth.of(2026, 3)

    private val state = MutableStateFlow(MonthlyPrayerTimesUiState())
    private val events = mutableListOf<MonthlyPrayerTimesEvent>()

    private val viewModel: MonthlyPrayerTimesViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@MonthlyPrayerTimesScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<MonthlyPrayerTimesEvent>() }
    }

    private var backs = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun at(date: LocalDate, hour: Int, minute: Int): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(
            date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    private fun day(date: LocalDate) = DayPrayerTimes(
        date = date,
        fajr = at(date, 5, 45),
        sunrise = at(date, 7, 12),
        dhuhr = at(date, 12, 30),
        asr = at(date, 15, 15),
        maghrib = at(date, 17, 48),
        isha = at(date, 19, 18),
        fastMinutes = 723,
    )

    private fun daysOf(vararg dayOfMonth: Int) = dayOfMonth.map { day(month.atDay(it)) }

    private fun render() {
        composeRule.setThemedContent {
            MonthlyPrayerTimesScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
        composeRule.waitForIdle()
    }

    private fun loaded(
        days: List<DayPrayerTimes> = daysOf(1, 2, 3),
        locationName: String? = "Dublin, Ireland",
        isUsingFallbackLocation: Boolean = false,
        ramadanHijriYear: Int? = null,
    ) {
        state.value = MonthlyPrayerTimesUiState(
            currentMonth = month,
            dayPrayerTimes = days,
            locationName = locationName,
            isUsingFallbackLocation = isUsingFallbackLocation,
            methodLabel = "MWL · Standard",
            ramadanHijriYear = ramadanHijriYear,
            isLoading = false,
        )
    }

    @Test
    fun `the header names the month the rows belong to`() {
        loaded(days = daysOf(1, 2, 3))
        render()

        // "March 2026" above rows dated in March. A header drawn from a second source of truth
        // is how a timetable ends up captioned with the wrong month.
        composeRule.onNodeWithText("March 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Sun, 1 March", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Tue, 3 March", substring = true).assertIsDisplayed()
    }

    @Test
    fun `one row per day, and no more`() {
        loaded(days = daysOf(1, 2, 3, 4))
        render()

        // Every row carries the expand affordance, so counting them counts rows — a day
        // silently dropped from the grid is a day with no prayer times at all.
        composeRule.onAllNodesWithContentDescription(str(R.string.cd_expand))
            .assertCountEquals(4)
    }

    @Test
    fun `the month arrows page in both directions`() {
        loaded()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_next_month)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.cd_previous_month)).performClick()

        assertThat(events).containsExactly(
            MonthlyPrayerTimesEvent.NextMonth,
            MonthlyPrayerTimesEvent.PreviousMonth,
        ).inOrder()
    }

    @Test
    fun `tapping a day asks to expand that day, not another`() {
        loaded(days = daysOf(1, 2, 3))
        render()

        composeRule.onNodeWithText("Tue, 3 March", substring = true).performClick()

        // The date travels with the event: a row that dispatches its neighbour's date expands
        // the wrong day's times, which reads as the timetable being wrong.
        assertThat(events).containsExactly(
            MonthlyPrayerTimesEvent.ToggleDayExpanded(month.atDay(3))
        )
    }

    @Test
    fun `the expanded day shows all six times and the others do not`() {
        state.value = MonthlyPrayerTimesUiState(
            currentMonth = month,
            dayPrayerTimes = daysOf(1, 2),
            locationName = "Dublin, Ireland",
            isLoading = false,
            expandedDay = month.atDay(2),
        )
        render()

        // Six labels, one per prayer, and only for the expanded row: the grid is drawn per card,
        // so a card that ignores `expandedDay` opens every row at once.
        listOf(
            R.string.prayer_fajr,
            R.string.prayer_sunrise,
            R.string.prayer_dhuhr,
            R.string.prayer_asr,
            R.string.prayer_maghrib,
            R.string.prayer_isha,
        ).forEach { composeRule.onNodeWithText(str(it)).assertIsDisplayed() }

        composeRule.onNodeWithContentDescription(str(R.string.cd_collapse)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(str(R.string.cd_expand)).assertCountEquals(1)
    }

    @Test
    fun `a fallback location is named as a default, never as a city`() {
        loaded(locationName = "Dublin, Ireland", isUsingFallbackLocation = true)
        render()

        // `isUsingFallbackLocation` wins over any name still sitting in state — the header must
        // not caption a timetable with a city the reader has never been to.
        composeRule.onNodeWithText(str(R.string.location_using_default)).assertIsDisplayed()
    }

    @Test
    fun `a blank location name reads as not set rather than as an empty caption`() {
        loaded(locationName = "  ")
        render()

        composeRule.onNodeWithText(str(R.string.location_not_set)).assertIsDisplayed()
    }

    @Test
    fun `the ramadan badge and the ramadan export appear together, and only in ramadan`() {
        loaded()
        render()

        composeRule.onNodeWithText(str(R.string.ramadan_month_label)).assertDoesNotExist()

        state.value = state.value.copy(ramadanHijriYear = 1447)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.ramadan_month_label)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).performClick()
        composeRule.onNodeWithText(str(R.string.ramadan_year_format, 1447)).assertIsDisplayed()
    }

    @Test
    fun `the export action is offered only once there is a month to export`() {
        state.value = MonthlyPrayerTimesUiState(currentMonth = month, isLoading = true)
        render()

        // Exporting an empty month produces an empty PDF and a share sheet for it.
        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).assertIsNotEnabled()

        loaded()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).assertIsEnabled()
    }

    @Test
    fun `the export chooser counts the days it would export`() {
        loaded(days = daysOf(1, 2, 3, 4, 5))
        render()

        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).performClick()

        composeRule.onNodeWithText(str(R.string.monthly_this_month)).assertIsDisplayed()
        // The row says what it would produce — month and day count — so a chooser offering to
        // export a month it does not actually hold is visible before the share sheet opens.
        composeRule.onNodeWithText("March 2026 \u00b7 5 days").assertIsDisplayed()
    }

    @Test
    fun `a month export that cannot render is reported, and the timetable stays up`() {
        loaded()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).performClick()
        composeRule.onNodeWithText(str(R.string.monthly_this_month)).performClick()
        composeRule.waitForIdle()

        // Started first, then exactly one outcome. The screen measures the duration itself
        // because the render runs here, on the click handler's thread; the ViewModel is the only
        // thing that reports. A silent failure here is a share button that does nothing.
        assertThat(events.first()).isEqualTo(MonthlyPrayerTimesEvent.ExportStarted)
        assertThat(
            events.count {
                it is MonthlyPrayerTimesEvent.ExportFailed ||
                    it is MonthlyPrayerTimesEvent.ExportCompleted
            }
        ).isEqualTo(1)
        composeRule.onNodeWithText("March 2026").assertIsDisplayed()
    }

    @Test
    fun `asking for the ramadan timetable dispatches the request rather than computing it`() {
        loaded(ramadanHijriYear = 1447)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.export_as_pdf)).performClick()
        composeRule.onNodeWithText(str(R.string.ramadan_year_format, 1447)).performClick()
        composeRule.waitForIdle()

        // A month of astronomy belongs off the click handler: the row asks, the ViewModel
        // computes, and the result comes back through state.
        assertThat(events).contains(MonthlyPrayerTimesEvent.PrepareRamadanExport)
        assertThat(events).doesNotContain(MonthlyPrayerTimesEvent.ExportStarted)
    }

    @Test
    fun `a ramadan timetable that lands in state is shared once and then cleared`() {
        loaded(ramadanHijriYear = 1447)
        render()

        state.value = state.value.copy(ramadanExport = daysOf(1, 2, 3))
        composeRule.waitForIdle()

        // Consumed explicitly, because it is a one-shot held in state: without the acknowledgment
        // the next recomposition re-opens the share sheet.
        assertThat(events).contains(MonthlyPrayerTimesEvent.ExportStarted)
        assertThat(events).contains(MonthlyPrayerTimesEvent.RamadanExportConsumed)
    }

    @Test
    fun `a day whose times could not be computed shows placeholders, not blanks`() {
        val date = month.atDay(1)
        state.value = MonthlyPrayerTimesUiState(
            currentMonth = month,
            dayPrayerTimes = listOf(
                DayPrayerTimes(date, null, null, null, null, null, null, fastMinutes = null)
            ),
            locationName = "Tromso, Norway",
            isLoading = false,
            expandedDay = date,
        )
        render()

        // At high latitudes a prayer can genuinely have no time on a given day. Six empty cells
        // read as a rendering bug; six "--:--" read as what they are. The row still has to be
        // there either way — dropping it would leave a gap in the month.
        // The card merges its children's semantics, so the six cells are only individually
        // addressable in the unmerged tree.
        composeRule.onAllNodesWithText("--:--", useUnmergedTree = true).assertCountEquals(6)
        composeRule.onNodeWithText("Sun, 1 March", substring = true).assertIsDisplayed()
    }

    @Test
    fun `back navigates back`() {
        loaded()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
