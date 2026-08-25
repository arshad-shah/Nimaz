package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatFullDate
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerHistoryUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerStatsUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerUiState
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.tracker.QadaPrayersUiState
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
 * The prayer tracker, and the banner that is the only way into the qada list.
 *
 * `:core:database` (#597) pins what the confirm-unrecorded DAO call does — it never overwrites a
 * logged prayer, never marks sunrise, and stops at the range's ends. What nothing pins is the
 * *offer*: that the banner appears only when there is genuinely something to confirm, that its
 * count is the count of unrecorded prayers in the last week, and that confirming asks for the
 * right seven days. A banner that offered to mark a week missed when the week was fully logged
 * would be a one-tap way to fabricate a qada list, and the DAO test cannot see it.
 *
 * The month load window is the other thing only this file can see. The screen deliberately loads
 * the displayed month **and** the trailing review window, always — paging to a month that does
 * not contain today used to leave the banner and the rail resolving the last seven real days
 * from an empty record set, so every one read NOT_RECORDED and the banner announced a fabricated
 * count.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class PrayerTrackerScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.now()

    private val location = Location(
        id = 1, name = "Test",
        latitude = 0.0, longitude = 0.0,
        timezone = "UTC",
        country = null, city = null,
        isCurrentLocation = true, isFavorite = false,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null, fajrAngle = null, ishaAngle = null,
    )

    /** A schedule for [date] whose every prayer is in the small hours, so the day has "passed". */
    private fun times(date: LocalDate) = PrayerTimes(
        fajr = date.atTime(0, 1),
        sunrise = date.atTime(0, 2),
        dhuhr = date.atTime(0, 3),
        asr = date.atTime(0, 4),
        maghrib = date.atTime(0, 5),
        isha = date.atTime(0, 6),
        date = date,
        location = location,
    )

    private fun record(date: LocalDate, prayer: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = date.toEpochDay() * 10 + prayer.ordinal,
        date = date.toUtcMidnightMillis(),
        prayerName = prayer,
        status = status,
        prayedAt = null,
        scheduledTime = 0L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /** Every tracked prayer on [date] recorded with [status]. */
    private fun wholeDay(date: LocalDate, status: PrayerStatus) =
        TRACKED_PRAYERS.map { record(date, it, status) }

    /** The seven days the review window looks at, each fully recorded. */
    private fun fullyLoggedWeek() =
        (1..7L).flatMap { wholeDay(today.minusDays(it), PrayerStatus.PRAYED) }

    private val trackerState = MutableStateFlow(
        PrayerTrackerUiState(selectedDate = today, prayerTimes = times(today), isLoading = false)
    )
    private val statsState = MutableStateFlow(PrayerStatsUiState(isLoading = false))
    private val historyState = MutableStateFlow(
        PrayerHistoryUiState(
            records = fullyLoggedWeek(),
            startDate = today.minusDays(7),
            endDate = today,
            isLoading = false,
        )
    )
    private val qadaState = MutableStateFlow(QadaPrayersUiState(isLoading = false))
    private val events = mutableListOf<PrayerTrackerEvent>()
    private val navigations = mutableListOf<String>()

    private val viewModel: PrayerTrackerViewModel = mockk(relaxed = true) {
        every { this@mockk.trackerState } returns this@PrayerTrackerScreenTest.trackerState
        every { this@mockk.statsState } returns this@PrayerTrackerScreenTest.statsState
        every { this@mockk.historyState } returns this@PrayerTrackerScreenTest.historyState
        every { this@mockk.qadaState } returns this@PrayerTrackerScreenTest.qadaState
        every { onEvent(any()) } answers { events += firstArg<PrayerTrackerEvent>() }
    }

    private fun setContent() {
        composeRule.setThemedContent {
            PrayerTrackerScreen(
                onNavigateBack = { navigations += "back" },
                onNavigateToStats = { navigations += "stats" },
                onNavigateToQada = { navigations += "qada" },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /**
     * The picker's "On time" cell.
     *
     * A row that already carries the status shows it in the accordion header's badge too, and a
     * clickable header merges its children's semantics — so the word matches the header as well
     * as the cell. The cell is the selectable one.
     */
    private fun onTimeCell() = composeRule
        .onAllNodesWithText(string(R.string.on_time))
        .filterToOne(isSelectable())

    private fun bannerText(count: Int) =
        context.resources.getQuantityString(R.plurals.prayer_unrecorded_banner, count, count)

    @Test
    fun `the screen loads the displayed month and the trailing review window together`() {
        setContent()

        val load = events.filterIsInstance<PrayerTrackerEvent.LoadHistory>().first()
        // Loading only the displayed month is what made the banner count days it had no records
        // for. The window must reach back a week from *today*, whatever month is on screen.
        assertThat(load.startDate).isAtMost(today.minusDays(7))
        assertThat(load.endDate).isAtLeast(today)
    }

    @Test
    fun `a fully logged week is offered no review banner`() {
        setContent()

        // The banner is the only door into the qada list, and it writes MISSED against every
        // unrecorded prayer it counted. Offering it over a complete week would be a one-tap way
        // to fabricate a debt that cannot be undone.
        composeRule.onNodeWithText(string(R.string.prayer_unrecorded_banner_action))
            .assertDoesNotExist()
    }

    @Test
    fun `the banner counts the prayers actually left unrecorded`() {
        // Six of the seven days fully logged; yesterday has Fajr and nothing else, so four of
        // the week's thirty-five prayers are unrecorded.
        historyState.value = historyState.value.copy(
            records = (2..7L).flatMap { wholeDay(today.minusDays(it), PrayerStatus.PRAYED) } +
                    record(today.minusDays(1), PrayerName.FAJR, PrayerStatus.PRAYED),
        )
        setContent()

        composeRule.onNodeWithText(bannerText(4)).assertExists()
    }

    @Test
    fun `a prayer explicitly marked missed is not counted as unrecorded`() {
        historyState.value = historyState.value.copy(
            records = (2..7L).flatMap { wholeDay(today.minusDays(it), PrayerStatus.PRAYED) } +
                    wholeDay(today.minusDays(1), PrayerStatus.MISSED),
        )
        setContent()

        // "You missed these" and "nobody has said" are different claims — the whole point of the
        // redesign. A missed prayer is already recorded, so the banner has nothing to offer.
        composeRule.onNodeWithText(string(R.string.prayer_unrecorded_banner_action))
            .assertDoesNotExist()
    }

    @Test
    fun `a NOT_PRAYED row reads as unrecorded rather than as an assertion`() {
        historyState.value = historyState.value.copy(
            records = (2..7L).flatMap { wholeDay(today.minusDays(it), PrayerStatus.PRAYED) } +
                    wholeDay(today.minusDays(1), PrayerStatus.NOT_PRAYED),
        )
        setContent()

        // Clearing a status writes NOT_PRAYED, and that has to read back as "nobody has said" —
        // otherwise clearing a prayer would quietly hide it from the review.
        composeRule.onNodeWithText(bannerText(5)).assertExists()
    }

    @Test
    fun `confirming the review asks for the seven days behind today, not today itself`() {
        historyState.value = historyState.value.copy(records = emptyList())
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_unrecorded_banner_action)).performClick()

        // Today is excluded: its later prayers have not happened yet, and marking them missed
        // would be an accusation about a day still in progress.
        assertThat(events).contains(
            PrayerTrackerEvent.ConfirmUnrecordedAsMissed(
                from = today.minusDays(7),
                to = today.minusDays(1),
            )
        )
    }

    @Test
    fun `the day card reports how many of the five are recorded`() {
        trackerState.value = trackerState.value.copy(
            prayerTimes = times(today),
        )
        historyState.value = historyState.value.copy(
            records = fullyLoggedWeek() +
                    record(today, PrayerName.FAJR, PrayerStatus.PRAYED) +
                    record(today, PrayerName.DHUHR, PrayerStatus.LATE),
        )
        setContent()

        // LATE counts as done — it is still an assertion that the prayer was performed.
        composeRule.onNodeWithText(string(R.string.prayer_recorded_count_format, 2, 5))
            .assertExists()
        // The timeline carries the same sentence as its screen-reader description, so TalkBack
        // gets the count rather than five bare prayer names with no status.
        composeRule.onNodeWithContentDescription(
            string(R.string.prayer_recorded_count_format, 2, 5)
        ).assertExists()
    }

    @Test
    fun `setting a prayer's status reports the prayer and the status`() {
        setContent()

        composeRule.onNodeWithText(PrayerName.FAJR.displayName()).performClick()
        composeRule.waitForIdle()
        onTimeCell().performClick()

        assertThat(events).contains(
            PrayerTrackerEvent.SetPrayerStatus(PrayerName.FAJR, PrayerStatus.PRAYED)
        )
    }

    @Test
    fun `choosing the status a prayer already has withdraws it`() {
        historyState.value = historyState.value.copy(
            records = fullyLoggedWeek() + record(today, PrayerName.ASR, PrayerStatus.PRAYED),
        )
        setContent()

        composeRule.onNodeWithText(PrayerName.ASR.displayName()).performClick()
        composeRule.waitForIdle()
        onTimeCell().performClick()

        // Tap-to-clear. Without it there is no way to take back a status set by mistake, because
        // the picker has no "unset" cell.
        assertThat(events).contains(
            PrayerTrackerEvent.SetPrayerStatus(PrayerName.ASR, null)
        )
    }

    @Test
    fun `an unrecorded prayer whose time has passed explains itself`() {
        setContent()

        composeRule.onNodeWithText(PrayerName.ISHA.displayName()).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.prayer_not_recorded_note)).assertExists()
    }

    @Test
    fun `made-up is offered only once a prayer's time has passed`() {
        // A schedule whose prayers are all still ahead of now, so nothing has passed yet.
        val ahead = today.atTime(23, 59)
        trackerState.value = trackerState.value.copy(
            prayerTimes = times(today).copy(
                fajr = ahead, dhuhr = ahead, asr = ahead, maghrib = ahead, isha = ahead,
            ),
        )
        setContent()

        composeRule.onNodeWithText(PrayerName.FAJR.displayName()).performClick()
        composeRule.waitForIdle()

        // Marking a prayer that has not happened yet as *made up* is incoherent — and it used to
        // be reachable by marking it prayed early, which flips its status without its time
        // having arrived.
        composeRule.onNodeWithText(string(R.string.made_up)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.on_time)).assertExists()
    }

    @Test
    fun `with no schedule the card says the times are missing, not that the day is done`() {
        trackerState.value = trackerState.value.copy(prayerTimes = null)
        setContent()

        // The same null makes all five rows read UPCOMING, so "Day complete" here would be the
        // card contradicting itself over breakfast.
        composeRule.onNodeWithText(string(R.string.prayer_day_no_schedule)).assertExists()
        composeRule.onNodeWithText(string(R.string.prayer_day_complete)).assertDoesNotExist()
    }

    @Test
    fun `a past day summarises what was recorded rather than counting down to it`() {
        val yesterday = today.minusDays(1)
        trackerState.value = trackerState.value.copy(
            selectedDate = yesterday,
            prayerTimes = times(yesterday),
        )
        historyState.value = historyState.value.copy(
            records = wholeDay(yesterday, PrayerStatus.PRAYED),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_day_summary_past, 5, 5)).assertExists()
    }

    @Test
    fun `a day other than today offers a way back to it`() {
        trackerState.value = trackerState.value.copy(
            selectedDate = today.minusDays(2),
            prayerTimes = null,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.back_to_today)).performClick()

        assertThat(events).contains(PrayerTrackerEvent.SelectDate(today))
    }

    @Test
    fun `today's card offers no way back to today`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.back_to_today)).assertDoesNotExist()
    }

    @Test
    fun `the streak badge appears only once there is a streak`() {
        setContent()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.khatam_widget_streak, 4, 4)
        ).assertDoesNotExist()

        statsState.value = statsState.value.copy(currentStreak = 4)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.khatam_widget_streak, 4, 4)
        ).assertExists()
    }

    @Test
    fun `selecting a day from the rail reports it`() {
        setContent()

        // The rail is centred on the selected day, three either side, and future cells are
        // disabled — so the cell two back is the first that is both present and selectable.
        val twoBack = today.minusDays(2)
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_prayed, twoBack.formatFullDate())
        ).performClick()

        assertThat(events).contains(PrayerTrackerEvent.SelectDate(twoBack))
    }

    @Test
    fun `the rail describes each day by what is recorded on it`() {
        val partial = today.minusDays(1)
        val missed = today.minusDays(2)
        historyState.value = historyState.value.copy(
            records = (3..7L).flatMap { wholeDay(today.minusDays(it), PrayerStatus.PRAYED) } +
                    listOf(record(partial, PrayerName.FAJR, PrayerStatus.PRAYED)) +
                    wholeDay(missed, PrayerStatus.MISSED),
        )
        setContent()

        // Four states, four sentences — and TalkBack gets nothing else: the marker is a coloured
        // dot with no text of its own, so a day that fell into the wrong bucket would be
        // indistinguishable to a screen-reader user and to anyone reading the dots at a glance.
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_partial, partial.formatFullDate())
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_missed, missed.formatFullDate())
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_prayed, today.minusDays(3).formatFullDate())
        ).assertExists()
    }

    @Test
    fun `a day with nothing on it is described as not recorded, not as missed`() {
        historyState.value = historyState.value.copy(records = emptyList())
        setContent()

        // The distinction the whole redesign turns on: "nobody has said" is not an accusation,
        // and the rail must not paint it as one.
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_not_recorded, today.minusDays(1).formatFullDate())
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            string(R.string.a11y_prayer_state_missed, today.minusDays(1).formatFullDate())
        ).assertDoesNotExist()
    }

    @Test
    fun `a future day on the rail cannot be selected`() {
        setContent()

        val tomorrow = today.plusDays(1)
        composeRule.onNodeWithContentDescription(
            "${tomorrow.formatFullDate()}, ${string(R.string.upcoming)}"
        ).performClick()

        // Disabled rather than absent: the rail is centred, so the days ahead are on screen and
        // must simply not respond. Logging a prayer that has not happened is not a thing to
        // offer.
        assertThat(events.filterIsInstance<PrayerTrackerEvent.SelectDate>()).isEmpty()
    }

    @Test
    fun `the month header counts the days on which all five were recorded`() {
        historyState.value = historyState.value.copy(
            records = fullyLoggedWeek() +
                    wholeDay(today.minusDays(8), PrayerStatus.PRAYED) +
                    listOf(record(today.minusDays(9), PrayerName.FAJR, PrayerStatus.PRAYED)),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.prayer_month_section)).assertExists()
        // The seven review-window days plus the eighth; the ninth has one prayer and does not
        // count. Days after today are excluded whatever the records say.
        val complete = (1..8L).count { !today.minusDays(it).isAfter(today) }
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.prayer_complete_days, complete, complete)
        ).assertExists()
    }

    @Test
    fun `the qada row says nothing is outstanding when nothing is`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.qada_summary_empty)).assertExists()
    }

    @Test
    fun `the qada row counts what is owed and leads to the list`() {
        qadaState.value = qadaState.value.copy(
            missedPrayers = listOf(
                record(today.minusDays(3), PrayerName.FAJR, PrayerStatus.MISSED),
                record(today.minusDays(4), PrayerName.ISHA, PrayerStatus.MISSED),
            ),
            totalMissed = 2,
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.qada_summary_subtitle)).assertExists()
        composeRule.onNodeWithText(string(R.string.qada_prayers)).performClick()

        assertThat(navigations).containsExactly("qada")
    }

    @Test
    fun `the statistics action navigates`() {
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.view_statistics)).performClick()

        assertThat(navigations).containsExactly("stats")
    }

    @Test
    fun `the back arrow goes back`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.prayer_tracker_title))
            .assertCountEquals(1)
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(navigations).containsExactly("back")
    }

}
