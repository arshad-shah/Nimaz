package com.arshadshah.nimaz.presentation.foundation.calendar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle

/**
 * Public data models and enums for the reusable calendar component.
 *
 * Kept separate from the composables so the component files hold only UI code.
 * These are re-exported from the `molecules` package via typealiases for
 * backward-compatible imports — see `molecules/CalendarApi.kt`.
 */

/**
 * Position of the status indicator dot within a day cell.
 */
enum class IndicatorPosition {
    BOTTOM_CENTER,
    TOP_END
}

/**
 * How the selected date is visually indicated.
 */
enum class SelectionStyle {
    /** Fills the cell background (used by Islamic calendar, fasting tracker). */
    BACKGROUND,

    /** Draws a border around the cell (used by prayer tracker). */
    BORDER
}

/**
 * Horizontal placement of the navigation header's title/subtitle block.
 * [START] keeps the title on the left with both arrows on the right (the
 * original layout). [CENTER] places the title between the two arrows. [END]
 * right-aligns the title with both arrows on the left.
 */
enum class CalendarHeaderAlignment {
    START,
    CENTER,
    END
}

/**
 * Visual state for a single calendar day cell.
 *
 * @param indicatorColor Color of the status dot. Null means no dot.
 * @param indicatorPosition Where to place the status dot.
 * @param backgroundColor Custom background color for the cell (e.g., Ramadan highlighting).
 *   Null uses the default (today/selected/transparent).
 * @param textColor Custom text color override. Null uses the default.
 * @param fontWeight Custom font weight override. Null uses the default.
 * @param primaryLabel Overrides the large, centered day number. Null falls back
 *   to the Gregorian day-of-month (the default). Set this to the Hijri day to make
 *   the Islamic calendar the primary (centered) one for a cell.
 * @param secondaryLabel The small, muted number tucked in the cell's top-end
 *   corner — the "other" calendar's day. Null hides it (single-date, the default).
 *   This is the dual-date overlay, rendered only when a caller supplies it.
 * @param emphasizePrimary Renders [primaryLabel] in the primary accent + bold —
 *   marks a Hijri month start when the Hijri date is the primary (centered) one.
 * @param emphasizeSecondary Renders [secondaryLabel] in the primary accent + bold —
 *   the robust, layout-stable Hijri month-start marker (just the number, no pill).
 * @param indicatorStyle Whether the dot is a disc or a ring. A ring says the day was
 *   *recorded as not happening*, which an absent dot cannot distinguish from no record at
 *   all. Defaults to a disc, so every caller written before rings existed is unaffected.
 * @param indicatorBar Fraction of the day completed, `0f..1f`, drawn as a short bar under the
 *   day number. `null` draws no bar. Independent of [indicatorColor]: a dot answers "what kind
 *   of day was this", a bar answers "how much of it", and a month grid that can only say the
 *   first cannot show a day where four of five prayers landed. Callers may set either, both, or
 *   neither.
 * @param indicatorBarColor Colour of [indicatorBar]. `null` uses the theme primary.
 */
data class CalendarDayState(
    val indicatorColor: Color? = null,
    val indicatorPosition: IndicatorPosition = IndicatorPosition.BOTTOM_CENTER,
    val backgroundColor: Color? = null,
    val textColor: Color? = null,
    val fontWeight: FontWeight? = null,
    val primaryLabel: String? = null,
    val secondaryLabel: String? = null,
    val emphasizePrimary: Boolean = false,
    val emphasizeSecondary: Boolean = false,
    val indicatorStyle: NimazStatusDotStyle = NimazStatusDotStyle.FILLED,
    val indicatorBar: Float? = null,
    val indicatorBarColor: Color? = null
)

/**
 * A legend entry displayed below the calendar grid.
 *
 * @param indicatorStyle Must match the [CalendarDayState.indicatorStyle] the entry explains —
 *   a legend that draws a disc for a state the grid draws as a ring is worse than no legend.
 */
data class CalendarLegendItem(
    val color: Color,
    val label: String,
    val indicatorStyle: NimazStatusDotStyle = NimazStatusDotStyle.FILLED
)
