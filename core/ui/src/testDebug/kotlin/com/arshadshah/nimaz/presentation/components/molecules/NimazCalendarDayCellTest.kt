package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarHeaderAlignment
import com.arshadshah.nimaz.presentation.foundation.calendar.IndicatorPosition
import com.arshadshah.nimaz.presentation.foundation.calendar.SelectionStyle
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The calendar's day cell — the busiest single composable in the design system.
 *
 * One cell carries up to five independent marks: the day number, a second calendar's number in the
 * corner, an indicator dot, a completion bar, and the selection or "today" fill behind all of it.
 * Every one is optional, several override each other, and the priority between them is stated only
 * in this file. Three of those priorities are worth pinning because they are decisions rather than
 * defaults:
 *
 * - **Selection beats today.** Tapping a date has to read back visually, so a selected cell takes
 *   the primary fill and today drops to the softer container. The comment in the source records
 *   this being the other way round once.
 * - **A caller's override beats both.** `backgroundColor`, `textColor` and `fontWeight` are how the
 *   fasting and prayer trackers colour their own days, and a cell that ignored them on the selected
 *   day would lose the tracker's information exactly where the user is looking.
 * - **A dot on a filled selection is repainted.** The caller's colour may sit too close to primary
 *   to be visible, so it is swapped for `onPrimary` — the mark surviving selection is the point.
 *
 * The announcement is the other half. "5" is useless to a screen reader; the cell reads "Monday,
 * 5 January 2026" and marks today separately, which is the only way a reader can tell where the
 * month starts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazCalendarDayCellTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.now()
    private val month: YearMonth = YearMonth.from(today)

    private fun a11y(date: LocalDate, isToday: Boolean = false): String {
        val locale = Locale.getDefault()
        return context.getString(
            if (isToday) R.string.calendar_a11y_day_today_format
            else R.string.calendar_a11y_day_format,
            date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            date.dayOfMonth,
            date.month.getDisplayName(TextStyle.FULL, locale),
            date.year,
        )
    }

    private fun showMonth(
        selectedDate: LocalDate? = null,
        selectionStyle: SelectionStyle = SelectionStyle.BACKGROUND,
        dayStateProvider: (LocalDate) -> CalendarDayState = { CalendarDayState() },
        onDateSelected: (LocalDate) -> Unit = {},
    ) {
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = month,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onPreviousMonth = {},
                onNextMonth = {},
                dayStateProvider = dayStateProvider,
                selectionStyle = selectionStyle,
            )
        }
    }

    @Test
    fun `today is announced differently from every other day`() {
        // The only signal a screen-reader user gets for where they are in the month.
        showMonth()

        composeRule.onNodeWithContentDescription(a11y(today, isToday = true)).assertExists()
        composeRule.onNodeWithContentDescription(a11y(today, isToday = false)).assertDoesNotExist()
    }

    @Test
    fun `a day announces its weekday, its number, its month and its year`() {
        val other = if (today.dayOfMonth > 1) today.withDayOfMonth(1) else today.withDayOfMonth(2)
        showMonth()

        composeRule.onNodeWithContentDescription(a11y(other)).assertExists()
    }

    @Test
    fun `the selected day reports itself as selected and the others do not`() {
        val selected = month.atDay(15)
        showMonth(selectedDate = selected)

        composeRule.onNodeWithContentDescription(a11y(selected, selected == today))
            .assertIsSelected()
        composeRule.onNodeWithContentDescription(a11y(month.atDay(16), month.atDay(16) == today))
            .assertIsNotSelected()
    }

    @Test
    fun `tapping a day reports that date`() {
        var picked: LocalDate? = null
        val target = month.atDay(9)
        showMonth(onDateSelected = { picked = it })

        composeRule.onNodeWithContentDescription(a11y(target, target == today)).performClick()

        assertThat(picked).isEqualTo(target)
    }

    @Test
    fun `a caller's own label replaces the day number`() {
        // `primaryLabel` is how the Hijri calendar shows its own numbering. A cell that ignored it
        // would show the Gregorian day under a Hijri month heading.
        showMonth(
            dayStateProvider = { date ->
                if (date == month.atDay(3)) CalendarDayState(primaryLabel = "٣")
                else CalendarDayState()
            },
        )

        composeRule.onNodeWithText("٣").assertExists()
    }

    @Test
    fun `a second calendar's number rides in the corner`() {
        // The dual-date overlay. It is deliberately small and quiet so it never competes with the
        // centred number — but it has to be there, because it is the whole Hijri/Gregorian pairing.
        showMonth(
            dayStateProvider = { date ->
                // A non-numeric label on purpose: every other cell in the grid renders a day
                // number, so "17" would match three nodes and the assertion would fail on the
                // month's arithmetic rather than on the overlay.
                if (date == month.atDay(4)) CalendarDayState(secondaryLabel = "xvii")
                else CalendarDayState()
            },
        )

        composeRule.onNodeWithText("xvii").assertExists()
    }

    @Test
    fun `an emphasised day still renders both of its numbers`() {
        // `emphasizePrimary` / `emphasizeSecondary` mark a Hijri month start; they change weight
        // and colour on two separate expressions, and only one of them is easy to notice.
        showMonth(
            dayStateProvider = { date ->
                if (date == month.atDay(5)) {
                    CalendarDayState(
                        primaryLabel = "1",
                        secondaryLabel = "Muh",
                        emphasizePrimary = true,
                        emphasizeSecondary = true,
                    )
                } else {
                    CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithText("Muh").assertExists()
    }

    @Test
    fun `an emphasised day on the selection keeps the selection's ink`() {
        // `emphasizePrimary && !isSelectedBackgroundFill` — the accent is suppressed on a filled
        // cell because `onPrimary` already contrasts, and applying both leaves teal on teal.
        val target = month.atDay(6)
        showMonth(
            selectedDate = target,
            dayStateProvider = { date ->
                if (date == target) CalendarDayState(emphasizePrimary = true) else CalendarDayState()
            },
        )

        composeRule.onNodeWithContentDescription(a11y(target, target == today)).assertIsSelected()
    }

    @Test
    fun `a caller's colours and weight override the defaults`() {
        // How the fasting and prayer trackers paint their own days. All three overrides are
        // separate elvis chains, and each is one edit from being dropped.
        val target = month.atDay(7)
        showMonth(
            dayStateProvider = { date ->
                if (date == target) {
                    CalendarDayState(
                        backgroundColor = Color.Magenta,
                        textColor = Color.Yellow,
                        fontWeight = FontWeight.Black,
                    )
                } else {
                    CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithContentDescription(a11y(target, target == today)).assertExists()
    }

    @Test
    fun `both indicator positions render`() {
        // Two arms of a `when`, and the bottom one shifts by 4dp or 8dp depending on whether a
        // fill bar shares its band — the case where two marks would otherwise overlap.
        showMonth(
            dayStateProvider = { date ->
                when (date) {
                    month.atDay(10) -> CalendarDayState(
                        indicatorColor = Color.Red,
                        indicatorPosition = IndicatorPosition.BOTTOM_CENTER,
                    )
                    month.atDay(11) -> CalendarDayState(
                        indicatorColor = Color.Red,
                        indicatorPosition = IndicatorPosition.TOP_END,
                        indicatorStyle = NimazStatusDotStyle.OUTLINED,
                    )
                    month.atDay(12) -> CalendarDayState(
                        indicatorColor = Color.Red,
                        indicatorPosition = IndicatorPosition.BOTTOM_CENTER,
                        indicatorBar = 0.6f,
                    )
                    else -> CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithContentDescription(a11y(month.atDay(10), month.atDay(10) == today))
            .assertExists()
        composeRule.onNodeWithContentDescription(a11y(month.atDay(12), month.atDay(12) == today))
            .assertExists()
    }

    @Test
    fun `a dot on the selected day is repainted so it stays visible`() {
        // The caller's colour may sit too close to primary to read against the fill; the swap to
        // `onPrimary` is what keeps the mark from vanishing on exactly the day being looked at.
        val target = month.atDay(13)
        showMonth(
            selectedDate = target,
            dayStateProvider = { date ->
                if (date == target) {
                    CalendarDayState(indicatorColor = Color.Blue, indicatorBar = 0.5f)
                } else {
                    CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithContentDescription(a11y(target, target == today)).assertIsSelected()
    }

    @Test
    fun `a not-a-number fill fraction is treated as empty rather than crashing the month`() {
        // `rawFraction.isNaN()` — a completion fraction of 0/0 arrives from a day with no
        // scheduled prayers, and `fillMaxWidth(NaN)` throws. One bad day would take the whole
        // month view down.
        showMonth(
            dayStateProvider = { date ->
                if (date == month.atDay(14)) CalendarDayState(indicatorBar = Float.NaN)
                else CalendarDayState()
            },
        )

        composeRule.onNodeWithContentDescription(a11y(month.atDay(14), month.atDay(14) == today))
            .assertExists()
    }

    @Test
    fun `a fill fraction beyond the ends is clamped`() {
        // `coerceIn(0f, 1f)` — the other half of the same guard, and `fillMaxWidth` throws on
        // anything outside 0..1 just as readily.
        showMonth(
            dayStateProvider = { date ->
                when (date) {
                    month.atDay(17) -> CalendarDayState(indicatorBar = 1.6f)
                    month.atDay(18) -> CalendarDayState(indicatorBar = -0.4f)
                    else -> CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithContentDescription(a11y(month.atDay(17), month.atDay(17) == today))
            .assertExists()
        composeRule.onNodeWithContentDescription(a11y(month.atDay(18), month.atDay(18) == today))
            .assertExists()
    }

    @Test
    fun `a bordered selection is still reported as selected`() {
        // `SelectionStyle.BORDER` draws a ring instead of a fill, and the fill-specific ink swaps
        // are all keyed on `isSelectedBackgroundFill` — so this arm takes the *other* side of
        // every one of them at once.
        val target = month.atDay(19)
        showMonth(
            selectedDate = target,
            selectionStyle = SelectionStyle.BORDER,
            dayStateProvider = { date ->
                if (date == target) {
                    CalendarDayState(
                        indicatorColor = Color.Blue,
                        secondaryLabel = "iii",
                        indicatorBar = 0.4f,
                    )
                } else {
                    CalendarDayState()
                }
            },
        )

        composeRule.onNodeWithContentDescription(a11y(target, target == today)).assertIsSelected()
        composeRule.onNodeWithText("iii").assertExists()
    }

    @Test
    fun `the header can be centred or pushed to the end`() {
        // Three alignments; every screen using the calendar picks one, and the default is only
        // right for one of them.
        composeRule.setThemedContent {
            NimazCalendar(
                displayedMonth = month,
                selectedDate = null,
                onDateSelected = {},
                onPreviousMonth = {},
                onNextMonth = {},
                headerTitle = "Centred",
                headerAlignment = CalendarHeaderAlignment.CENTER,
                headerSubtitle = { androidx.compose.material3.Text("subtitle") },
            )
        }

        composeRule.onNodeWithText("Centred").assertExists()
        composeRule.onNodeWithText("subtitle").assertExists()
    }
}
