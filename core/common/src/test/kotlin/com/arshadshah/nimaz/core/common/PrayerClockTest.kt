package com.arshadshah.nimaz.core.common

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.junit.Test

/**
 * The three clock-derived views over a day's prayer instants.
 *
 * `PrayerClock.kt`'s own KDoc records the bug it was written to fix: the previous derivation
 * compared **times of day**, so after Isha nothing highlighted, and before Fajr today's
 * not-yet-happened Isha rendered as "current". Both are boundary conditions at the ends of the
 * day, and neither had a test — the fix was verified by reading. They are pinned here, because
 * "which prayer is now" being wrong for two windows a day is not something a screenshot catches.
 *
 * (For the record: `PrayerClock` was listed in #557's triage as *"currently imported by nothing;
 * confirm it is not dead code before moving it"*. It is not dead. All three functions are called
 * from `presentation/` — `PrayerTimeDisplay.kt` and `TodaysProgressCard.kt`. A grep for the
 * *file* name finds nothing because everything in it is a top-level function.)
 */
class PrayerClockTest {

    private val fajr = Instant.parse("2026-03-14T05:00:00Z")
    private val sunrise = Instant.parse("2026-03-14T06:30:00Z")
    private val dhuhr = Instant.parse("2026-03-14T12:00:00Z")
    private val asr = Instant.parse("2026-03-14T15:30:00Z")
    private val maghrib = Instant.parse("2026-03-14T18:00:00Z")
    private val isha = Instant.parse("2026-03-14T19:30:00Z")
    private val day = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    @Test
    fun `the next prayer is the first one that has not started`() {
        assertThat(nextPrayerIndexAt(day, fajr - 1.minutes)).isEqualTo(0)
        assertThat(nextPrayerIndexAt(day, dhuhr - 1.minutes)).isEqualTo(2)
        assertThat(nextPrayerIndexAt(day, maghrib + 1.minutes)).isEqualTo(5)
    }

    @Test
    fun `a prayer's own instant counts as started, not as upcoming`() {
        // `it > now`, not `>=`. At exactly Dhuhr the next prayer is Asr, and the row that
        // highlights is Dhuhr's — off-by-one here would make the app announce a prayer it is
        // already inside.
        assertThat(nextPrayerIndexAt(day, dhuhr)).isEqualTo(3)
        assertThat(currentPrayerIndexAt(day, dhuhr)).isEqualTo(2)
    }

    @Test
    fun `after the last prayer there is no next one`() {
        // The old time-of-day comparison returned -1 here too, but then the *current* index fell
        // through to lastIndex only by accident. Both halves are asserted.
        assertThat(nextPrayerIndexAt(day, isha + 1.hours)).isEqualTo(-1)
        assertThat(currentPrayerIndexAt(day, isha + 1.hours)).isEqualTo(5)
    }

    @Test
    fun `before the first prayer nothing is in effect`() {
        // The documented bug: this used to wrap to Isha, so at 04:00 the app showed last night's
        // Isha as the current prayer. -1 is deliberate so a caller can tell "before Fajr" from
        // "in Fajr".
        assertThat(currentPrayerIndexAt(day, fajr - 1.minutes)).isEqualTo(-1)
    }

    @Test
    fun `progress runs from zero at the first instant to one at the last`() {
        assertThat(prayerTimelineProgressAt(day, fajr)).isEqualTo(0f)
        assertThat(prayerTimelineProgressAt(day, isha)).isEqualTo(1f)
        assertThat(prayerTimelineProgressAt(day, fajr - 1.hours)).isEqualTo(0f)
        assertThat(prayerTimelineProgressAt(day, isha + 1.hours)).isEqualTo(1f)
    }

    @Test
    fun `progress interpolates within an interval rather than stepping at each prayer`() {
        // The point of the interpolation: halfway between Dhuhr (index 2) and Asr (index 3) of
        // five intervals is 2.5/5. Stepping would give 2/5 for the whole afternoon.
        val halfwayDhuhrToAsr = dhuhr + (asr - dhuhr) / 2
        assertThat(prayerTimelineProgressAt(day, halfwayDhuhrToAsr)).isWithin(0.001f).of(0.5f)

        val quarterFajrToSunrise = fajr + (sunrise - fajr) / 4
        assertThat(prayerTimelineProgressAt(day, quarterFajrToSunrise)).isWithin(0.001f).of(0.05f)
    }

    @Test
    fun `a degenerate day does not divide by zero`() {
        assertThat(prayerTimelineProgressAt(emptyList(), dhuhr)).isEqualTo(0f)
        assertThat(prayerTimelineProgressAt(listOf(dhuhr), dhuhr)).isEqualTo(0f)
        // Two identical instants: the `now <= first` guard is checked before `now >= last`, so a
        // zero-width day reads as 0f rather than 1f. Either would be defensible; what matters is
        // that it is decided by an explicit guard rather than by a division by zero.
        assertThat(prayerTimelineProgressAt(listOf(dhuhr, dhuhr), dhuhr)).isEqualTo(0f)
    }

    @Test
    fun `an empty day has no next and no current prayer`() {
        assertThat(nextPrayerIndexAt(emptyList(), dhuhr)).isEqualTo(-1)
        // lastIndex of an empty list is -1, which is the same "nothing" the caller checks for.
        assertThat(currentPrayerIndexAt(emptyList(), dhuhr)).isEqualTo(-1)
    }
}
