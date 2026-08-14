package com.arshadshah.nimaz.core.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * What day it is, and when that changes.
 *
 * `LocalDate.now()` was called directly at 39 sites across 12 ViewModels, always at `init` or
 * collection time, and nothing re-evaluated it. There was no seam to fake in a test and no
 * notion of "the day changed" anywhere in the ViewModel layer, so a whole family of defects
 * shipped together: Home's "fasting today" bound a Room query to a fixed day range forever,
 * the daily hadith and the time-of-day dua froze at whatever they were when the app started,
 * and a month grid generated at 23:59 kept highlighting yesterday.
 *
 * Two things are needed to fix those, and this provides both:
 *
 *  - [today], so a test can decide what day it is. `Clock.fixed(...)` in production terms.
 *  - [todayChanges], so a screen that stays open across midnight is told. It emits the
 *    current date immediately and again at each local midnight, which makes "re-arm
 *    everything that is scoped to today" an ordinary flow collection rather than a
 *    hand-rolled check nobody remembers to write.
 */
interface TodayProvider {

    /** The current local date. */
    fun today(): LocalDate

    /**
     * The current local date, re-emitted at every local midnight.
     *
     * Emits immediately on collection, so a collector never has to seed itself separately.
     */
    val todayChanges: Flow<LocalDate>
}

@Singleton
class SystemTodayProvider @Inject constructor(
    private val clock: Clock,
) : TodayProvider {

    override fun today(): LocalDate = LocalDate.now(clock)

    override val todayChanges: Flow<LocalDate> = flow {
        while (true) {
            val date = today()
            emit(date)
            // Sleep to the next local midnight rather than polling. Re-read the date after
            // waking instead of assuming `date + 1`: a long doze, a timezone change or a
            // clock correction can land the wake-up on a different day than arithmetic
            // would predict, and the loop re-emits whatever is actually true.
            delay(millisUntilNextMidnight().milliseconds)
        }
    }

    private fun millisUntilNextMidnight(): Long {
        val now = LocalDate.now(clock).atTime(LocalTime.now(clock))
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(MIN_TICK_MILLIS)
    }

    private companion object {
        /**
         * Never schedule a zero-length wait: at exactly midnight the computed delay can round
         * to 0 and spin the loop. A second is far below any resolution a date needs.
         */
        const val MIN_TICK_MILLIS = 1_000L
    }
}
