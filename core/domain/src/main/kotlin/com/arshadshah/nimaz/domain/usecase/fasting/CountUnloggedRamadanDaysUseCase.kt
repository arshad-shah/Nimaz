package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import java.time.LocalDate
import javax.inject.Inject

/**
 * How many days of the current Ramadan have gone by with nothing logged against them.
 *
 * **Why this is a use case and not arithmetic inside a card.** It was six lines inside
 * `RamadanMissedFastsTracker`, a private composable, computed from `LocalDate.now()` read at
 * composition — so nothing could test it, and the count went stale the moment the screen was
 * left open across midnight. Issue #492 moves that card to the shared component layer, which
 * makes both problems everyone's rather than one screen's.
 *
 * "Unlogged" is not "missed": a day the user explicitly recorded as not fasted is logged, and
 * the card is prompting for the days with no record at all.
 */
class CountUnloggedRamadanDaysUseCase @Inject constructor(
    private val todayProvider: TodayProvider,
) {

    /**
     * @param currentDay today's day of Ramadan, 1..30.
     * @param records the fast records for this Ramadan.
     */
    operator fun invoke(currentDay: Int, records: List<FastRecord>): Int {
        val today = todayProvider.today()
        // Today is still in progress, so it cannot be behind on anything.
        val daysElapsed = currentDay - 1
        if (daysElapsed <= 0) return 0

        val recorded = records.count { record ->
            val recordDate = LocalDate.ofEpochDay(
                record.date / MILLIS_PER_DAY
            )
            recordDate.isBefore(today) && HijriDateCalculator.isRamadan(recordDate)
        }
        return (daysElapsed - recorded).coerceAtLeast(0)
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
