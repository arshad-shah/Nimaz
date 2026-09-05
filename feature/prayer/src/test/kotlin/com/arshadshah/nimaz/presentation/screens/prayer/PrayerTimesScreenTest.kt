package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatWeekdayDayMonth
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesViewModel
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
import java.time.ZoneId

/**
 * The day pager: which day is on screen, how it says so, and what the rows on it can do.
 *
 * **Three of these only exist on screen.** The ViewModel publishes `selectedDate` and `isToday`
 * and nothing else about the framing; whether a reader can tell they are looking at *tomorrow's*
 * times rather than today's — and whether they have any way back — is decided entirely here, by
 * the relative label, by the "Today" chip that renders `if (!state.isToday)`, and by the tracking
 * toggles that are withheld on a future day. A screen that loses the chip strands the reader on
 * a day they browsed to; a screen that keeps the toggles invites them to tick a prayer that has
 * not happened yet.
 *
 * The location line is the fourth: `FallbackLocation` exists so that a surface can *say* it is
 * using a stand-in, and the header is the only place that promise is kept.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp-mdpi")
class PrayerTimesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val today: LocalDate = LocalDate.now()

    private val state = MutableStateFlow(PrayerTimesUiState())
    private val events = mutableListOf<PrayerTimesEvent>()

    private val viewModel: PrayerTimesViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@PrayerTimesScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<PrayerTimesEvent>() }
    }

    private var backs = 0
    private var settingsOpens = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun at(date: LocalDate, hour: Int, minute: Int): kotlin.time.Instant =
        kotlin.time.Instant.fromEpochMilliseconds(
            date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    private fun prayersFor(date: LocalDate) = listOf(
        PrayerTimeDisplay(PrayerType.FAJR, "Fajr", at(date, 5, 45)),
        PrayerTimeDisplay(PrayerType.SUNRISE, "Sunrise", at(date, 7, 12)),
        PrayerTimeDisplay(PrayerType.DHUHR, "Dhuhr", at(date, 12, 30)),
        PrayerTimeDisplay(PrayerType.ASR, "Asr", at(date, 15, 15)),
        PrayerTimeDisplay(PrayerType.MAGHRIB, "Maghrib", at(date, 17, 48)),
        PrayerTimeDisplay(PrayerType.ISHA, "Isha", at(date, 19, 18)),
    )

    private fun render() {
        composeRule.setThemedContent {
            PrayerTimesScreen(
                onNavigateBack = { backs++ },
                onNavigateToSettings = { settingsOpens++ },
                viewModel = viewModel,
            )
        }
        composeRule.waitForIdle()
    }

    private fun show(
        date: LocalDate = today,
        isToday: Boolean = date == today,
        locationName: String = "Dublin, Ireland",
        isUsingFallbackLocation: Boolean = false,
    ) {
        state.value = PrayerTimesUiState(
            locationName = locationName,
            isUsingFallbackLocation = isUsingFallbackLocation,
            selectedDate = date,
            isToday = isToday,
            prayers = prayersFor(date),
            tomorrowFajrAt = at(date.plusDays(1), 5, 44),
            sunriseAt = at(date, 7, 12),
            sunsetAt = at(date, 17, 48),
            daylight = "10h 36m",
            methodLabel = "MWL · Standard",
        )
    }

    @Test
    fun `today's six prayers are listed`() {
        show()
        render()

        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach {
            composeRule.onAllNodesWithText(it).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun `the header names the place, and says so when it is only a default`() {
        show(locationName = "Dublin, Ireland")
        render()
        composeRule.onNodeWithText("Dublin, Ireland").assertIsDisplayed()

        // The stand-in must never be captioned as somewhere the reader chose. `locationName` is
        // still populated in this state — the flag, not the emptiness of the name, is what
        // decides the copy.
        show(locationName = "Dublin, Ireland", isUsingFallbackLocation = true)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(str(R.string.location_using_default)).assertIsDisplayed()
    }

    /**
     * The week rail replaced the prev/next arrows. It reaches three days either side of the
     * selection, so the cell after the middle one is tomorrow.
     */
    @Test
    fun `the rail selects another day`() {
        show()
        render()

        composeRule
            .onNodeWithContentDescription(today.plusDays(1).formatWeekdayDayMonth())
            .performClick()

        assertThat(events).containsExactly(PrayerTimesEvent.SelectDate(today.plusDays(1)))
    }

    @Test
    fun `on today there is no way back to it offered`() {
        show()
        render()

        // The pill renders only `if (!state.isToday)` — offering "Today" while on today is a
        // control that does nothing.
        composeRule.onAllNodesWithText(str(R.string.today)).assertCountEquals(0)
    }

    @Test
    fun `browsing off today labels the day and offers the way back`() {
        show(date = today.plusDays(1))
        render()

        // The sky's status line carries the relative label now that the card owns the window.
        composeRule.onNodeWithText(str(R.string.fasting_tomorrow)).assertIsDisplayed()

        composeRule.onNodeWithText(str(R.string.today)).performClick()
        assertThat(events).containsExactly(PrayerTimesEvent.GoToToday)
    }

    @Test
    fun `yesterday and a distant day both get a relative label`() {
        show(date = today.minusDays(1))
        render()
        composeRule.onNodeWithText(str(R.string.relative_yesterday)).assertIsDisplayed()

        show(date = today.plusDays(4))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("In 4 days").assertIsDisplayed()

        show(date = today.minusDays(3))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("3 days ago").assertIsDisplayed()
    }

    /** On today the card names the window the reader is inside, rather than the daylight. */
    @Test
    fun `on today the card states the window you are inside`() {
        show()
        render()

        composeRule.onNodeWithText(str(R.string.prayer_window_lede)).assertIsDisplayed()
    }

    /**
     * On any other day there is no "now", so "you are in the window of Dhuhr" is a sentence that
     * does not exist. The card leads with the day's daylight instead.
     */
    @Test
    fun `on another day the card states the daylight instead`() {
        show(date = today.plusDays(2))
        render()

        composeRule.onNodeWithText(str(R.string.prayer_daylight_lede)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.prayer_window_lede)).assertCountEquals(0)
    }

    /**
     * The reframe, asserted at the screen as well as at the ViewModel: Prayer Times answers
     * *when*, so no row invites a tap that would record anything.
     */
    @Test
    fun `no prayer row is clickable`() {
        show()
        render()

        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEach {
            composeRule.onAllNodesWithText(it).onFirst().assertHasNoClickAction()
        }
    }

    @Test
    fun `the day info card reports the sun, the daylight and the method`() {
        show()
        render()

        composeRule.onNodeWithText(str(R.string.prayer_info_daylight)).assertIsDisplayed()
        composeRule.onAllNodesWithText("10h 36m").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.prayer_info_method)).assertIsDisplayed()
        composeRule.onNodeWithText("MWL · Standard").assertIsDisplayed()
    }

    @Test
    fun `the month button opens a picker and picking a day selects it`() {
        show()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_pick_month)).performClick()
        composeRule.waitForIdle()

        // The calendar is the only way to reach a day more than a few taps away; a sheet that
        // opens but does not report the choice makes the arrows the only navigation there is.
        val target = today.withDayOfMonth(if (today.dayOfMonth == 15) 16 else 15)
        composeRule.onAllNodesWithText(target.dayOfMonth.toString()).onFirst().performClick()
        composeRule.waitForIdle()

        assertThat(events).contains(PrayerTimesEvent.SelectDate(target))
    }

    @Test
    fun `a day with no sun times shows placeholders rather than an empty row`() {
        state.value = PrayerTimesUiState(
            locationName = "Tromso, Norway",
            selectedDate = today,
            isToday = true,
            prayers = prayersFor(today),
            sunriseAt = null,
            sunsetAt = null,
            daylight = "—",
            methodLabel = "MWL · Standard",
        )
        render()

        // Above the Arctic Circle there are days with no sunrise at all. The info card still has
        // to say something: an empty value beside "Sunrise / Sunset" reads as a failed load.
        composeRule.onNodeWithText("--:-- — --:--").assertIsDisplayed()
    }

    @Test
    fun `swiping left and right pages the day`() {
        show()
        render()

        // Swiped across the whole root, not across a label: the handler ignores drags shorter
        // than 64dp, and a node-width swipe on a short piece of text never reaches that.
        composeRule.onRoot().performTouchInput {
            swipeLeft(startX = right - 1f, endX = left + 1f, durationMillis = 200)
        }
        composeRule.waitForIdle()
        composeRule.onRoot().performTouchInput {
            swipeRight(startX = left + 1f, endX = right - 1f, durationMillis = 200)
        }
        composeRule.waitForIdle()

        // The arrows are not the only way through the pager, and the gesture is the one people
        // actually use. A drag threshold read with the wrong sign pages the wrong way, which is
        // indistinguishable from the arrows being swapped.
        assertThat(events).containsExactly(
            PrayerTimesEvent.NextDay,
            PrayerTimesEvent.PreviousDay,
        ).inOrder()
    }

    @Test
    fun `the sky's back and settings pills navigate`() {
        show()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.cd_back)).performClick()
        assertThat(backs).isEqualTo(1)

        composeRule.onNodeWithContentDescription(str(R.string.settings)).performClick()
        assertThat(settingsOpens).isEqualTo(1)
    }
}
