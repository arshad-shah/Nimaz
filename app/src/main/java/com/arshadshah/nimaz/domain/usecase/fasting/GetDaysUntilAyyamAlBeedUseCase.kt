package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import javax.inject.Inject

/**
 * How many days until the next Ayyām al-Bīḍ — the white days, the 13th, 14th and 15th of each
 * Hijri month, on which fasting is recommended.
 *
 * Zero while they are in progress; otherwise the count to the next 13th, which may be in this
 * Hijri month or the next one.
 *
 * **Why this is a use case and not a private function in a screen.** It was
 * `FastTrackerScreen.calculateAyyamAlBeedDays(today: LocalDate)`, and it had both of the
 * problems that shape implies (audit §5.1):
 *
 *  - it is business logic about the Hijri calendar, sitting in the presentation layer where no
 *    test could reach it;
 *  - one of its two call sites passed `LocalDate.now()` directly, which is the pattern
 *    [TodayProvider] exists to remove — a screen composed just before midnight kept counting
 *    down to yesterday's next-white-day for as long as it stayed open.
 *
 * It also ignored the user's `hijriDayOffset`, which is registry Open #10: only
 * `HijriDateCalculator.today(offsetDays)` honoured that preference, so a user who had shifted
 * their Hijri date by a day to match their local moon sighting saw events matched against the
 * shifted date and this countdown against the unshifted one. [offsetDays] is that preference,
 * and it now reaches the same calculation everything else uses.
 */
class GetDaysUntilAyyamAlBeedUseCase @Inject constructor(
    private val todayProvider: TodayProvider,
) {

    /** @param offsetDays the user's `hijriDayOffset`, applied the way `HijriDateCalculator.today` applies it. */
    operator fun invoke(offsetDays: Int = 0): Int {
        val hijriDate = HijriDateCalculator.toHijri(
            todayProvider.today().plusDays(offsetDays.toLong())
        )
        return when (val hijriDay = hijriDate.day) {
            in WHITE_DAYS -> 0
            in 1 until WHITE_DAYS.first -> WHITE_DAYS.first - hijriDay
            else -> {
                // Past the 15th: run out this month, then on to the next 13th.
                val daysInMonth =
                    HijriDateCalculator.getDaysInHijriMonth(hijriDate.year, hijriDate.month)
                (daysInMonth - hijriDay) + WHITE_DAYS.first
            }
        }
    }

    private companion object {
        /** The 13th, 14th and 15th — al-ayyām al-bīḍ, when the moon is full. */
        val WHITE_DAYS = 13..15
    }
}
