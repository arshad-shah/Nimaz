package com.arshadshah.nimaz.presentation.screens.fasting

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.screens.SubtitleArg
import com.arshadshah.nimaz.presentation.screens.SubtitleSpec

/**
 * What the fasting screen's make-up row reports.
 *
 * Same rule as `MoreSubtitles`: titles label, subtitles report, and a row with nothing true to say
 * gets **no subtitle** rather than a dash. A pure function so the phrasing is testable off-device.
 *
 * This object used to carry four mappers, one per row of a "Go deeper" menu group. The 2026-08
 * redesign dissolved that group — the calendar and the recommended fasts are simply on the screen
 * now, so a row summarising them has nothing left to summarise. [makeup] survives because its row
 * does: it is the one thing on the fasting screen that still leads somewhere else.
 */
object FastingSubtitles {

    /**
     * Makeup fasts still owed.
     *
     * The row keeps its badge whatever this returns — the count belongs there, not buried in prose
     * — so the subtitle carries the *fidya* figure instead when one has been paid, and nothing when
     * there is neither.
     */
    fun makeup(pending: Int?, fidyaPaid: String?): SubtitleSpec? {
        val paid = fidyaPaid?.takeIf { it.isNotBlank() }
        return when {
            pending == null -> null
            pending > 0 -> SubtitleSpec(
                res = R.plurals.fasting_row_makeup_pending,
                args = listOf(SubtitleArg.Count(pending)),
                quantity = pending,
            )
            // Nothing outstanding. Worth saying so exactly once, because "you are square" is the
            // answer someone opened this row to get.
            paid != null -> SubtitleSpec(
                res = R.string.fasting_row_makeup_fidya,
                args = listOf(SubtitleArg.Text(paid)),
            )

            else -> SubtitleSpec(R.string.fasting_row_makeup_none)
        }
    }
}
