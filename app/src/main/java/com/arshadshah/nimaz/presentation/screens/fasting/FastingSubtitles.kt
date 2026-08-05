package com.arshadshah.nimaz.presentation.screens.fasting

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.screens.SubtitleArg
import com.arshadshah.nimaz.presentation.screens.SubtitleSpec

/**
 * What each row of Fasting's "Go deeper" group reports.
 *
 * Same rule as `MoreSubtitles`: titles label, subtitles report, and a row with nothing true to say
 * gets **no subtitle** rather than a dash. Pure functions so the phrasing is testable off-device —
 * "18 fasted this month" against a month with 18 records is the kind of claim a screenshot review
 * cannot check.
 */
object FastingSubtitles {

    /**
     * How much of the shown month has a fast logged against it.
     *
     * Zero is silence rather than "0 fasted this month". Outside Ramadan a month with no voluntary
     * fasts is entirely ordinary, and a row that announces it reads as a reproach.
     */
    fun calendar(fastedThisMonth: Int?): SubtitleSpec? {
        if (fastedThisMonth == null || fastedThisMonth <= 0) return null
        return SubtitleSpec(
            res = R.plurals.fasting_row_calendar_fasted,
            args = listOf(SubtitleArg.Count(fastedThisMonth)),
            quantity = fastedThisMonth,
        )
    }

    /**
     * When the next recommended fast falls.
     *
     * Today and tomorrow get their own words. "In 0 days" is not something anyone says, and "in 1
     * day" is a clumsier way of saying tomorrow — both are frequent enough to be worth the strings.
     */
    fun recommended(daysUntilNext: Int?): SubtitleSpec? {
        if (daysUntilNext == null || daysUntilNext < 0) return null
        return when (daysUntilNext) {
            0 -> SubtitleSpec(R.string.fasting_row_recommended_today)
            1 -> SubtitleSpec(R.string.fasting_row_recommended_tomorrow)
            else -> SubtitleSpec(
                res = R.plurals.fasting_row_recommended_in_days,
                args = listOf(SubtitleArg.Count(daysUntilNext)),
                quantity = daysUntilNext,
            )
        }
    }

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

    /**
     * Where Ramadan is.
     *
     * Three states, and the middle one is the point: *during* Ramadan the useful fact is which day
     * it is, not that it has started. [daysUntil] of zero means it begins tomorrow-at-Maghrib
     * territory, which "begins today" covers honestly enough for a menu row.
     */
    fun ramadan(isRamadan: Boolean, currentDay: Int?, daysUntil: Int?): SubtitleSpec? = when {
        isRamadan && currentDay != null && currentDay > 0 -> SubtitleSpec(
            res = R.string.fasting_row_ramadan_day,
            args = listOf(SubtitleArg.Count(currentDay)),
        )

        isRamadan -> null
        daysUntil == null || daysUntil < 0 -> null
        daysUntil == 0 -> SubtitleSpec(R.string.fasting_row_ramadan_today)
        else -> SubtitleSpec(
            res = R.plurals.fasting_row_ramadan_in_days,
            args = listOf(SubtitleArg.Count(daysUntil)),
            quantity = daysUntil,
        )
    }
}
