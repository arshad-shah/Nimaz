package com.arshadshah.nimaz.presentation.screens.fasting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.ProvideNimazClock
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingTrackerUiState
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The suhoor→iftar band, and the one line above it that says how long is left.
 *
 * Four sentences share one `when`, and which one a user sees depends on where *now* falls
 * against the selected day's window. That makes it the part of the fasting tracker most likely
 * to be wrong for exactly the reader who needs it — someone opening the app an hour before
 * iftar — and the least likely to be caught by hand, because reproducing each case means waiting
 * for the right hour of the right day.
 *
 * Time is supplied through `ProvideNimazClock`'s `timeSource` seam rather than read from the
 * machine, so each case is a fixed point in the window rather than whatever the CI runner's
 * clock happens to say.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class FastingWindowTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.of(2026, 8, 13)
    /**
     * A minute-aligned "now".
     *
     * `rememberNow(MINUTES)` truncates whatever it is handed, so an instant with seconds on it
     * would make every expected duration a few seconds short of the rendered one.
     */
    private val now: Instant = Instant.fromEpochMilliseconds(
        Clock.System.now().toEpochMilliseconds() / 60_000L * 60_000L
    )

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    /** The countdown formats, from the resources — the phrasing is the translators' to change. */
    private fun hoursAndMinutes(hours: Int, minutes: Int) =
        string(R.string.countdown_hm, hours, minutes)

    private fun minutesOnly(minutes: Int) = string(R.string.countdown_m, minutes)

    private fun render(
        suhoorAt: Instant?,
        iftarAt: Instant?,
        isSelectedToday: Boolean = true,
    ) {
        composeRule.setThemedContent {
            ProvideNimazClock(timeSource = { now }) {
                FastingDayCard(
                    state = FastingTrackerUiState(
                        selectedDate = today,
                        selectedSuhoorAt = suhoorAt,
                        selectedIftarAt = iftarAt,
                        isSelectedToday = isSelectedToday,
                        isLoading = false,
                    ),
                    ramadanDay = null,
                    onSetStatus = {},
                    onOpenExemption = {},
                    onOpenNote = {},
                    onBackToToday = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `before suhoor the lede counts down to suhoor`() {
        render(suhoorAt = now + 40.minutes, iftarAt = now + 14.hours)

        // The reader is awake and eating; the number that matters is how long they have left to.
        composeRule.onNodeWithText(
            string(R.string.fasting_window_suhoor_in, minutesOnly(40)),
        ).assertExists()
    }

    @Test
    fun `inside the fast the lede counts down to iftar`() {
        render(suhoorAt = now - 6.hours, iftarAt = now + 3.hours - 42.minutes)

        composeRule.onNodeWithText(
            string(R.string.fasting_window_iftar_in, hoursAndMinutes(2, 18)),
        ).assertExists()
    }

    @Test
    fun `after iftar the window is reported closed rather than counting down to nothing`() {
        render(suhoorAt = now - 16.hours, iftarAt = now - 1.hours)

        // Both ends are behind us, so there is no duration to report. A countdown here would
        // run backwards or wrap.
        composeRule.onNodeWithText(string(R.string.fasting_window_closed)).assertExists()
    }

    @Test
    fun `a day that is not today reports the window's length, not a countdown`() {
        render(
            suhoorAt = now - 30.hours,
            iftarAt = now - 30.hours + 15.hours + 30.minutes,
            isSelectedToday = false,
        )

        // "Iftar in −6h" for yesterday is meaningless; how long that day's fast ran is not.
        composeRule.onNodeWithText(
            string(R.string.fasting_window_length, hoursAndMinutes(15, 30)),
        ).assertExists()
    }

    @Test
    fun `with no schedule the band shows placeholders and says the window is closed`() {
        render(suhoorAt = null, iftarAt = null)

        composeRule.onNodeWithText(string(R.string.fasting_window_closed)).assertExists()
        // Not "00:00", which would read as a real time of day at the top of the band.
        composeRule.onNodeWithContentDescription(
            string(R.string.fasting_window_a11y, "--:--", "--:--"),
        ).assertExists()
    }

    @Test
    fun `half a schedule is treated as no schedule`() {
        // A partial day is reachable: the two instants are resolved separately, so a location
        // change between them can leave one set. Counting down to an iftar with no suhoor would
        // report a fast that has not started.
        render(suhoorAt = now - 2.hours, iftarAt = null)

        composeRule.onNodeWithText(string(R.string.fasting_window_closed)).assertExists()
    }

    @Test
    fun `the band is described for a screen reader by both of its ends`() {
        render(suhoorAt = now - 6.hours, iftarAt = now + 3.hours)

        // The track sets its own semantics over the whole band, so its end labels are not
        // addressable as text — the description is the only thing TalkBack gets where the
        // whole day's shape is. Matched on the format's leading words rather than a rendered
        // clock time, which depends on the 12/24-hour setting.
        val prefix = string(R.string.fasting_window_a11y, "@", "@").substringBefore("@")
        composeRule.onNodeWithContentDescription(prefix, substring = true).assertExists()
        // …and it is a real schedule, not the placeholder one.
        composeRule.onNodeWithContentDescription(
            string(R.string.fasting_window_a11y, "--:--", "--:--"),
        ).assertDoesNotExist()
    }
}
