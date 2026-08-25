package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatWeekdayDayMonth
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.ExemptionReason
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingCalendarUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingTrackerUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.MakeupFastsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.RamadanTrackerUiState
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

/**
 * The fasting tracker: one scroll that reports the selected day.
 *
 * Five sections and two sheets, none of which had ever been composed. Three things here are
 * decided in this file and nowhere else, and each fails silently:
 *
 *  - **Which Ramadan card shows.** The banner runs during Ramadan and the countdown only within
 *    thirty days of it. Both at once, or neither, is the failure — and it is a `when` over
 *    ViewModel state that no ViewModel test can see the effect of.
 *  - **Month paging arithmetic.** January's previous month is December *of the year before*, and
 *    December's next is January of the next. Getting the year wrong loads a month of records
 *    twelve months out and looks like an empty month.
 *  - **The exemption and note sheets.** They write to the *selected* day, which is not
 *    necessarily today; a sheet that saved against today would silently rewrite the wrong day's
 *    record.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class FastTrackerScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.now()

    private fun record(
        date: LocalDate,
        status: FastStatus,
        reason: ExemptionReason? = null,
        note: String? = null,
    ) = FastRecord(
        id = date.toEpochDay(),
        date = date.toEpochDay() * MILLIS_PER_DAY,
        hijriDate = null,
        hijriMonth = null,
        hijriYear = null,
        fastType = FastType.VOLUNTARY,
        status = status,
        exemptionReason = reason,
        suhoorTime = null,
        iftarTime = null,
        note = note,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private val trackerState = MutableStateFlow(
        FastingTrackerUiState(selectedDate = today, isSelectedToday = true, isLoading = false)
    )
    private val makeupState = MutableStateFlow(MakeupFastsUiState(isLoading = false))
    private val ramadanState = MutableStateFlow(RamadanTrackerUiState(isLoading = false))
    private val calendarState = MutableStateFlow(
        FastingCalendarUiState(
            selectedMonth = today.monthValue,
            selectedYear = today.year,
            isLoading = false,
        )
    )
    private val events = mutableListOf<FastingEvent>()
    private val navigations = mutableListOf<String>()

    private val viewModel: FastingViewModel = mockk(relaxed = true) {
        every { this@mockk.trackerState } returns this@FastTrackerScreenTest.trackerState
        every { this@mockk.makeupState } returns this@FastTrackerScreenTest.makeupState
        every { this@mockk.ramadanState } returns this@FastTrackerScreenTest.ramadanState
        every { this@mockk.calendarState } returns this@FastTrackerScreenTest.calendarState
        every { onEvent(any()) } answers { events += firstArg<FastingEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            FastTrackerScreen(
                onNavigateBack = { navigations += "back" },
                onNavigateToCalendar = { navigations += "calendar" },
                onNavigateToMakeup = { navigations += "makeup" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /**
     * The day card's third segment.
     *
     * "Exempt" is also the calendar legend's label for the same status further down the screen,
     * so the word alone matches twice. The segment is the one that is selectable.
     */
    private fun exemptCell() = composeRule
        .onAllNodesWithText(string(R.string.fasting_seg_exempt))
        .filterToOne(isSelectable())

    @Test
    fun `outside Ramadan and outside the countdown window, neither card shows`() {
        ramadanState.value = ramadanState.value.copy(
            isRamadan = false,
            daysUntilRamadan = 120,
            ramadanStartsOn = today.plusDays(120),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_current)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.fasting_ramadan_starts_in)).assertDoesNotExist()
    }

    @Test
    fun `inside the window the countdown shows, and it alone`() {
        ramadanState.value = ramadanState.value.copy(
            isRamadan = false,
            daysUntilRamadan = 12,
            ramadanStartsOn = today.plusDays(12),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_ramadan_starts_in)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_days)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_current)).assertDoesNotExist()
    }

    @Test
    fun `during Ramadan the banner takes over from the countdown`() {
        ramadanState.value = ramadanState.value.copy(
            isRamadan = true,
            currentDay = 9,
            fastedDays = 7,
            missedDays = 1,
            remainingDays = 21,
            // Still inside the window numerically — the banner must win anyway, or the screen
            // shows a countdown to a Ramadan that has already begun.
            daysUntilRamadan = 0,
            ramadanStartsOn = today,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_current)).assertExists()
        // Twice, and that is the point: the banner announces the day and the day card badges it,
        // and the two read the same `currentDay` so they cannot disagree.
        composeRule.onAllNodesWithText(string(R.string.fasting_ramadan_day, 9)).assertCountEquals(2)
        composeRule.onNodeWithText("7/29").assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_ramadan_starts_in)).assertDoesNotExist()
    }

    @Test
    fun `the countdown says day or days as the number requires`() {
        ramadanState.value = ramadanState.value.copy(
            daysUntilRamadan = 1,
            ramadanStartsOn = today.plusDays(1),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_day)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_days)).assertDoesNotExist()
    }

    @Test
    fun `the countdown card is withheld when there is no start date to show`() {
        ramadanState.value = ramadanState.value.copy(
            daysUntilRamadan = 5,
            ramadanStartsOn = null,
        )
        setContent()

        // "5 days until — " is worse than nothing; the card needs both halves.
        composeRule.onNodeWithText(string(R.string.fasting_ramadan_starts_in)).assertDoesNotExist()
    }

    @Test
    fun `the month header counts only the days actually fasted`() {
        calendarState.value = calendarState.value.copy(
            records = listOf(
                record(today.withDayOfMonth(1), FastStatus.FASTED),
                record(today.withDayOfMonth(2), FastStatus.FASTED),
                record(today.withDayOfMonth(3), FastStatus.NOT_FASTED),
                record(today.withDayOfMonth(4), FastStatus.EXEMPTED),
            ),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_your_month)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_fasted_count, 2)).assertExists()
    }

    @Test
    fun `paging back from January lands in the December before it`() {
        calendarState.value = calendarState.value.copy(selectedMonth = 1, selectedYear = 2026)
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_previous_month)).performClick()

        // The year rolls with the month. Keeping 2026 would load a month of records a year away
        // and present it as an empty January.
        assertThat(events).contains(FastingEvent.SelectMonth(12, 2025))
    }

    @Test
    fun `paging forward from December lands in the January after it`() {
        calendarState.value = calendarState.value.copy(selectedMonth = 12, selectedYear = 2026)
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_next_month)).performClick()

        assertThat(events).contains(FastingEvent.SelectMonth(1, 2027))
    }

    @Test
    fun `paging within a year leaves the year alone`() {
        calendarState.value = calendarState.value.copy(selectedMonth = 6, selectedYear = 2026)
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_previous_month)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.cd_next_month)).performClick()

        assertThat(events).containsExactly(
            FastingEvent.SelectMonth(5, 2026),
            FastingEvent.SelectMonth(7, 2026),
        ).inOrder()
    }

    @Test
    fun `the coming-up row lists the weekly sunnah days and Ayyam al-Beed`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_coming_up)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_monday)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_thursday)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_ayyam_al_beed)).assertExists()
    }

    @Test
    fun `logging a coming-up fast selects that day and marks it fasted`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_monday)).performClick()

        // Two events, in this order: the tap is "log *that* day", so selecting it first is what
        // stops the status landing on whichever day was already selected.
        val nextMonday = events.filterIsInstance<FastingEvent.SelectDate>().first().date
        assertThat(events).containsExactly(
            FastingEvent.SelectDate(nextMonday),
            FastingEvent.SetFastStatus(nextMonday, FastStatus.FASTED),
        ).inOrder()
        assertThat(nextMonday.dayOfWeek.value).isEqualTo(1)
    }

    @Test
    fun `a day already fasted reads as logged rather than as an invitation`() {
        val nextMonday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
        calendarState.value = calendarState.value.copy(
            records = listOf(record(nextMonday, FastStatus.FASTED)),
        )
        setContent()

        composeRule.onAllNodesWithText(string(R.string.fasting_logged)).assertCountEquals(1)
    }

    @Test
    fun `the makeup row reports nothing outstanding when there is nothing`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_row_makeup)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_row_makeup_none)).assertExists()
    }

    @Test
    fun `the makeup row badges what is owed and leads to the make-up screen`() {
        makeupState.value = makeupState.value.copy(pendingCount = 3, totalFidyaPaid = 24.0)
        setContent()

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.fasting_row_makeup_pending, 3, 3)
        ).assertExists()

        composeRule.onNodeWithText(string(R.string.fasting_row_makeup)).performClick()
        assertThat(navigations).containsExactly("makeup")
    }

    @Test
    fun `with nothing owed the row leads with the fidya already paid`() {
        makeupState.value = makeupState.value.copy(
            pendingCount = 0,
            totalFidyaPaid = 24.0,
            currency = "GBP",
        )
        setContent()

        // The count lives in the badge, so the subtitle carries the other fact — and only when
        // there is one.
        composeRule.onNodeWithText(string(R.string.fasting_row_makeup_fidya, "£24.00")).assertExists()
    }

    @Test
    fun `the exemption sheet writes its reason against the selected day, not today`() {
        val yesterday = today.minusDays(1)
        trackerState.value = trackerState.value.copy(
            selectedDate = yesterday,
            isSelectedToday = false,
        )
        setContent()

        // The day card's third cell opens the sheet rather than setting a status: an exemption
        // without a reason is not worth recording.
        exemptCell().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.fasting_why_exempt)).assertExists()
        // Twice: the day card's header and the sheet's subtitle, both naming the selected day.
        // That agreement is the assertion — a sheet subtitled with *today* would be editing a
        // different record from the one on screen.
        composeRule.onAllNodesWithText(yesterday.formatWeekdayDayMonth()).assertCountEquals(2)

        composeRule.onNodeWithText(ExemptionReason.TRAVEL.displayName()).performClick()
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()

        assertThat(events).containsExactly(
            FastingEvent.SaveExemption(yesterday, ExemptionReason.TRAVEL)
        )
    }

    @Test
    fun `saving an exemption without picking a reason records it as other`() {
        setContent()

        exemptCell().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()

        // Making the user answer "why" to dismiss a sheet they opened by accident is worse than
        // defaulting, and OTHER is honest about an unanswered question.
        assertThat(events).containsExactly(
            FastingEvent.SaveExemption(today, ExemptionReason.OTHER)
        )
    }

    @Test
    fun `cancelling the exemption sheet records nothing`() {
        setContent()

        exemptCell().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeRule.waitForIdle()

        assertThat(events).isEmpty()
    }

    @Test
    fun `the exemption sheet opens on the reason already on file`() {
        trackerState.value = trackerState.value.copy(
            selectedRecord = record(today, FastStatus.EXEMPTED, reason = ExemptionReason.ILLNESS),
        )
        setContent()

        exemptCell().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()

        // Re-saving an untouched sheet must not silently downgrade a recorded reason to OTHER.
        assertThat(events).containsExactly(
            FastingEvent.SaveExemption(today, ExemptionReason.ILLNESS)
        )
    }

    @Test
    fun `the note sheet saves what was typed against the selected day`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_add_note)).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Travelling home")
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()

        assertThat(events).containsExactly(FastingEvent.SaveNote(today, "Travelling home"))
    }

    @Test
    fun `the note sheet opens on whatever note the day already carries`() {
        trackerState.value = trackerState.value.copy(
            selectedRecord = record(today, FastStatus.FASTED, note = "Kept it"),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.fasting_add_note)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()

        // A note sheet that opened blank would wipe the note on the next save.
        assertThat(events).containsExactly(FastingEvent.SaveNote(today, "Kept it"))
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(navigations).containsExactly("back")
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
