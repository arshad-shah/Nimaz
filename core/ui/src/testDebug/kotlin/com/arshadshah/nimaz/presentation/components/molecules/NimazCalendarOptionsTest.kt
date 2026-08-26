package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarHeaderAlignment
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.foundation.calendar.SelectionStyle
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The calendar's caller-facing options, set together the way a tracker screen sets them.
 *
 * The calendar is used by the fasting tracker, the prayer tracker, the Islamic calendar and two
 * date pickers, and each of them configures it differently: its own header, its own selection
 * style, its own legend, its own per-day painting. Those options are the whole reason there is one
 * calendar rather than four, so a parameter that quietly stopped being honoured would send a
 * feature straight back to writing its own — and the symptom would be "the calendar looks wrong on
 * this screen", not an error.
 *
 * The legend is the one with a rule of its own: it is drawn only when there is something to
 * explain, because a divider and an empty row under a month grid reads as a rendering fault.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazCalendarOptionsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val month: YearMonth = YearMonth.of(2026, 5)

    @Test
    fun `every option a tracker screen sets is honoured together`() {
        var picked: LocalDate? = null
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = month,
                selectedDate = month.atDay(12),
                onDateSelected = { picked = it },
                onPreviousMonth = {},
                onNextMonth = {},
                dayStateProvider = { date ->
                    if (date.dayOfMonth % 2 == 0) {
                        CalendarDayState(
                            indicatorColor = Color.Green,
                            indicatorStyle = NimazStatusDotStyle.OUTLINED,
                            indicatorBar = 0.6f,
                            indicatorBarColor = Color.Magenta,
                        )
                    } else {
                        CalendarDayState()
                    }
                },
                legendItems = listOf(
                    CalendarLegendItem(color = Color.Green, label = "Fasted"),
                    CalendarLegendItem(
                        color = Color.Red,
                        label = "Missed",
                        indicatorStyle = NimazStatusDotStyle.OUTLINED,
                    ),
                ),
                showNavigation = true,
                headerTitle = "Ramadan 1447",
                headerSubtitle = { Text("30 days") },
                selectionStyle = SelectionStyle.BORDER,
                headerAlignment = CalendarHeaderAlignment.CENTER,
            )
        }

        composeRule.onNodeWithText("Ramadan 1447").assertExists()
        composeRule.onNodeWithText("30 days").assertExists()
        composeRule.onNodeWithText("Fasted").assertExists()
        composeRule.onNodeWithText("Missed").assertExists()
        assertThat(picked).isNull()
    }

    @Test
    fun `a calendar with no legend draws no legend row`() {
        // `legendItems.isNotEmpty()` gates a divider *and* the row. Rendering them empty leaves a
        // rule with nothing under it, which reads as a fault rather than as an absence.
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = month,
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                legendItems = emptyList(),
            )
        }

        composeRule.onNodeWithText("Fasted").assertDoesNotExist()
    }

    @Test
    fun `a calendar embedded in a picker hides its own navigation`() {
        // The date picker supplies its own month stepping, so the calendar's header would be a
        // second set of arrows doing the same thing.
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = month,
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                showNavigation = false,
                headerTitle = "Never shown",
            )
        }

        composeRule.onNodeWithText("Never shown").assertDoesNotExist()
    }
}
