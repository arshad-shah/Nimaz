package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarHeaderAlignment
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stepping the calendar between months.
 *
 * The grid animates the swap, and the *direction* is worked out by comparing the incoming month
 * against the previous frame's — forward slides in from the right, back from the left. That
 * comparison is a single `targetState > initialState`, and inverted it makes every month change
 * slide the wrong way: the calendar still works, and every navigation feels backwards.
 *
 * Driving it needs the month to actually change, which means hoisting the state the way a screen
 * does rather than passing a constant. That is also the only way to reach the grid's re-render at
 * all — a fixed `displayedMonth` composes one grid and never runs the transition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazCalendarMonthSwapTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val start: YearMonth = YearMonth.of(2026, 6)

    private fun label(month: YearMonth) =
        month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + month.year

    private fun showSteppable(alignment: CalendarHeaderAlignment) {
        composeRule.setThemedContent {
            var month by remember { mutableStateOf(start) }
            NimazCalendar(
                displayedMonth = month,
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = { month = month.minusMonths(1) },
                onNextMonth = { month = month.plusMonths(1) },
                headerAlignment = alignment,
            )
        }
    }

    @Test
    fun `the header names the month it is showing`() {
        showSteppable(CalendarHeaderAlignment.START)

        composeRule.onNodeWithText(label(start)).assertExists()
    }

    @Test
    fun `stepping forward slides to the next month`() {
        showSteppable(CalendarHeaderAlignment.START)

        composeRule.onNodeWithContentDescriptionSubstring("Next").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(label(start.plusMonths(1))).assertExists()
    }

    @Test
    fun `stepping back slides to the previous month`() {
        // The other side of the direction comparison — and the arm an inverted `>` would swap.
        showSteppable(CalendarHeaderAlignment.END)

        composeRule.onNodeWithContentDescriptionSubstring("Previous").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(label(start.minusMonths(1))).assertExists()
    }

    @Test
    fun `stepping forward and back returns to where it started`() {
        showSteppable(CalendarHeaderAlignment.CENTER)

        composeRule.onNodeWithContentDescriptionSubstring("Next").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescriptionSubstring("Previous").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(label(start)).assertExists()
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionSubstring(
    value: String,
) = onNode(
    androidx.compose.ui.test.hasContentDescription(value, substring = true, ignoreCase = true)
)
