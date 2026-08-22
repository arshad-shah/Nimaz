package com.arshadshah.nimaz.widget.core

/**
 * Index of the prayer to highlight as "next" — the first prayer in [prayerEpochMillis]
 * whose instant is still in the future relative to [nowMillis]. Returns -1 when every
 * prayer for the day has already passed, in which case nothing should be highlighted.
 *
 * Epochs are expected in chronological order. Entries of 0L (unknown) are treated as
 * already-passed and are never highlighted.
 *
 * This is evaluated at render time so the highlight tracks the wall clock on every widget
 * redraw, instead of being frozen by the 15-minute refresh worker (which left the
 * highlight on an already-passed prayer between refreshes / under Doze throttling).
 */
fun nextPrayerIndex(prayerEpochMillis: List<Long>, nowMillis: Long): Int =
    prayerEpochMillis.indexOfFirst { it > nowMillis }
