package com.arshadshah.nimaz.domain.usecase.fasting

import com.arshadshah.nimaz.core.time.TodayProvider
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import java.time.LocalDate
import javax.inject.Inject

/** How far away the next Ramadan is, and when it starts. */
data class RamadanCountdown(
    val daysAway: Int,
    val startsOn: LocalDate,
)

/**
 * The countdown to the next Ramadan.
 *
 * **Why this is a use case and not two calls inside a composable.** `RamadanCountdownCard` did
 * its own `HijriDateCalculator.daysUntilNextRamadan()` and `HijriDateCalculator.today()` at
 * composition, and `FastTrackerScreen` called `daysUntilNextRamadan()` a third time just to
 * decide whether to show the card at all. That is the pattern [TodayProvider] exists to
 * remove — a screen composed just before midnight keeps counting down to yesterday's answer
 * for as long as it stays open — and it was about to be made worse: issue #492 moves that card
 * into the shared component layer, where a composable that reads the clock is a defect every
 * future caller inherits.
 *
 * Same shape as [GetDaysUntilAyyamAlBeedUseCase], for the same reasons, including
 * [offsetDays]: a user who shifted their Hijri date to match a local moon sighting should see
 * that reflected here as well as everywhere else (registry Open #10).
 */
class GetRamadanCountdownUseCase @Inject constructor(
    private val todayProvider: TodayProvider,
) {

    /** @param offsetDays the user's `hijriDayOffset`, applied the way `HijriDateCalculator.today` applies it. */
    operator fun invoke(offsetDays: Int = 0): RamadanCountdown {
        val today = todayProvider.today().plusDays(offsetDays.toLong())
        val hijriToday = HijriDateCalculator.toHijri(today)

        // Past Ramadan this Hijri year, so the next one is next year's. Month 9 itself counts
        // as "this year" — the countdown reads zero rather than jumping eleven months.
        val targetYear = if (hijriToday.month > RAMADAN) hijriToday.year + 1 else hijriToday.year
        val startsOn = HijriDateCalculator.getFirstDayOfRamadan(targetYear)

        return RamadanCountdown(
            daysAway = java.time.temporal.ChronoUnit.DAYS.between(today, startsOn)
                .toInt()
                .coerceAtLeast(0),
            startsOn = startsOn,
        )
    }

    private companion object {
        const val RAMADAN = 9
    }
}
