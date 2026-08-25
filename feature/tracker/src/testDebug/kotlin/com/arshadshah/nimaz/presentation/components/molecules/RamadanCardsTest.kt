package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.formatLongDate
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The three Ramadan cards, on their own.
 *
 * They are tested here rather than only through `FastTrackerScreen` because two of their
 * failure modes are numbers, and the tracker screen has a month calendar on it — every integer
 * from 1 to 31 already appears as a day cell, so "the countdown reads 12" cannot be asserted
 * there without matching a date.
 *
 * `RamadanMissedFastsTracker` is the third card and has **no caller at all** today: the tracker
 * redesign dropped it from the screen, and it survives because the count it renders
 * (`unloggedDays`, from `CountUnloggedRamadanDaysUseCase`) is still computed and still in the
 * state. Its contract is that it draws *nothing* at zero so a caller can place it
 * unconditionally — which is exactly the behaviour a future caller will rely on without
 * checking, and exactly what nothing else here would catch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class RamadanCardsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the banner reports the day, the progress and the three counts`() {
        composeRule.setThemedContent {
            RamadanBanner(
                fastedDays = 7,
                totalDays = 30,
                currentDay = 8,
                missedDays = 1,
                remainingDays = 22,
            )
        }

        composeRule.onNodeWithText(string(R.string.fasting_ramadan_day, 8)).assertExists()
        composeRule.onNodeWithText("7/30").assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_fasted)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_missed)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_remaining)).assertExists()
        composeRule.onNodeWithText("22").assertExists()
    }

    @Test
    fun `the banner's stat row is withheld when there are no counts to show`() {
        composeRule.setThemedContent {
            RamadanBanner(fastedDays = 0, totalDays = 0, currentDay = 1)
        }

        // `totalDays = 0` is the day Ramadan is detected before the record set has loaded. The
        // card must still draw — and its progress bar must not divide by zero.
        composeRule.onNodeWithText("0/0").assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_missed)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.fasting_remaining)).assertDoesNotExist()
    }

    @Test
    fun `the banner reports whichever counts it is given`() {
        composeRule.setThemedContent {
            RamadanBanner(fastedDays = 4, totalDays = 30, currentDay = 5, missedDays = 1)
        }

        // The three counts are independently optional: the calendar's caller supplies missed and
        // remaining, and a caller with only one of them must get a row with only that one rather
        // than a row with a hole in it.
        composeRule.onNodeWithText(string(R.string.fasting_missed)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_remaining)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.fasting_fasted)).assertExists()
    }

    @Test
    fun `the countdown names the number of days and the date it starts`() {
        val startsOn = LocalDate.of(2027, 2, 8)
        composeRule.setThemedContent {
            RamadanCountdownCard(daysAway = 12, startsOn = startsOn)
        }

        composeRule.onNodeWithText(string(R.string.fasting_ramadan_starts_in)).assertExists()
        composeRule.onNodeWithText("12").assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_days)).assertExists()
        composeRule.onNodeWithText(startsOn.formatLongDate()).assertExists()
    }

    @Test
    fun `the countdown says day, singular, on the eve`() {
        composeRule.setThemedContent {
            RamadanCountdownCard(daysAway = 1, startsOn = LocalDate.of(2027, 2, 8))
        }

        composeRule.onNodeWithText(string(R.string.fasting_day)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_days)).assertDoesNotExist()
    }

    @Test
    fun `the unlogged-days nudge counts the days and says what to do`() {
        composeRule.setThemedContent { RamadanMissedFastsTracker(unloggedDays = 3) }

        composeRule.onNodeWithText("3").assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_unlogged_days)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_log_calendar_hint)).assertExists()
    }

    @Test
    fun `the nudge is singular for a single day`() {
        composeRule.setThemedContent { RamadanMissedFastsTracker(unloggedDays = 1) }

        composeRule.onNodeWithText(string(R.string.fasting_unlogged_day)).assertExists()
        composeRule.onNodeWithText(string(R.string.fasting_unlogged_days)).assertDoesNotExist()
    }

    @Test
    fun `the nudge draws nothing when nothing is unlogged`() {
        composeRule.setThemedContent { RamadanMissedFastsTracker(unloggedDays = 0) }

        // The documented contract, and the reason a caller may place it unconditionally: a card
        // that rendered an empty shell at zero would leave a blank box on the screen instead.
        composeRule.onNodeWithText(string(R.string.fasting_log_calendar_hint)).assertDoesNotExist()
        composeRule.onNodeWithText("0").assertDoesNotExist()
    }
}
