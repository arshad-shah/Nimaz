package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class NimazCalendarTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `renders default month header and weekday labels`() {
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = LocalDate.of(2026, 1, 15),
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
            )
        }
        // Default header formats as "Month Year".
        composeRule.onNodeWithText("January 2026").assertExists()
        // Weekday header labels.
        composeRule.onNodeWithText("SUN").assertExists()
        composeRule.onNodeWithText("FRI").assertExists()
    }

    @Test
    fun `renders custom header title when provided`() {
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                headerTitle = "Rajab 1447",
            )
        }
        composeRule.onNodeWithText("Rajab 1447").assertExists()
    }

    @Test
    fun `clicking a day triggers onDateSelected with that date`() {
        var selected: LocalDate? = null
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = LocalDate.of(2026, 1, 15),
                onDateSelected = { selected = it },
                onPreviousMonth = {},
                onNextMonth = {},
            )
        }
        // The 15th appears once within January 2026's grid.
        composeRule.onNodeWithText("15").performClick()
        assertThat(selected).isEqualTo(LocalDate.of(2026, 1, 15))
    }

    @Test
    fun `navigation buttons trigger callbacks`() {
        var prev = false
        var next = false
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = { prev = true },
                onNextMonth = { next = true },
            )
        }
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        assertThat(prev).isTrue()

        composeRule.onNodeWithContentDescription("Next month").performClick()
        assertThat(next).isTrue()
    }

    @Test
    fun `hides navigation header when showNavigation is false`() {
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                showNavigation = false,
            )
        }
        composeRule.onNodeWithContentDescription("Previous month").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Next month").assertDoesNotExist()
        // The grid still renders.
        composeRule.onNodeWithText("SUN").assertExists()
    }

    @Test
    fun `renders legend items when provided`() {
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = YearMonth.of(2026, 1),
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                legendItems = listOf(
                    CalendarLegendItem(Color(0xFFEAB308), "Eid"),
                    CalendarLegendItem(Color(0xFF22C55E), "Holy Night"),
                ),
            )
        }
        composeRule.onNodeWithText("Eid").assertExists()
        composeRule.onNodeWithText("Holy Night").assertExists()
    }
}
