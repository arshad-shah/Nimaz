package com.arshadshah.nimaz.widget.core

import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarData
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidgetState
import com.arshadshah.nimaz.widget.hijridate.HijriDateData
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetState
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetData
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetState
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerData
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerEntry
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetState
import com.arshadshah.nimaz.widget.nextprayer.nextEntry
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesData
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetState
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerData
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWidgetState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which persisted widget states are worth keeping when a refresh fails.
 *
 * A failed refresh used to overwrite whatever was on screen with the error frame, so one
 * transient throw turned a widget showing correct prayer times into "tap to set up" and left it
 * that way until a later run happened to succeed. `refreshWidget` now asks these predicates
 * first, and the thing they have to get right is the distinction the default state makes
 * awkward: every widget's default is `Success` with an empty payload — that is what draws the
 * em-dash skeleton before the first worker run — so "is it Success" answers nothing.
 */
class WidgetStateRetentionTest {

    @Test
    fun `the empty default every widget starts on is not data worth keeping`() {
        assertThat(NextPrayerWidgetState.Success(NextPrayerData()).hasData).isFalse()
        assertThat(PrayerTimesWidgetState.Success(PrayerTimesData()).hasData).isFalse()
        assertThat(PrayerTrackerWidgetState.Success(PrayerTrackerData()).hasData).isFalse()
        assertThat(HijriDateWidgetState.Success(HijriDateData()).hasData).isFalse()
        assertThat(HijriCalendarWidgetState.Success(HijriCalendarData()).hasData).isFalse()
        assertThat(KhatamWidgetState.Success(KhatamWidgetData()).hasData).isFalse()
    }

    @Test
    fun `a loaded state is data worth keeping`() {
        assertThat(
            NextPrayerWidgetState.Success(NextPrayerData(nextPrayerEpochMillis = 1L)).hasData
        ).isTrue()
        assertThat(
            PrayerTimesWidgetState.Success(PrayerTimesData(fajrEpochMillis = 1L)).hasData
        ).isTrue()
        assertThat(
            PrayerTrackerWidgetState.Success(PrayerTrackerData(dateLabel = "Wed")).hasData
        ).isTrue()
        assertThat(
            HijriDateWidgetState.Success(HijriDateData(hijriMonth = "Safar")).hasData
        ).isTrue()
        assertThat(
            HijriCalendarWidgetState.Success(HijriCalendarData(hijriMonthName = "Safar")).hasData
        ).isTrue()
        assertThat(
            KhatamWidgetState.Success(KhatamWidgetData(hasActiveKhatam = true)).hasData
        ).isTrue()
    }

    /** A tracker day with nothing prayed yet is a real reading, not an empty one. */
    @Test
    fun `a tracked day with no prayers recorded is still a real reading`() {
        val untouchedDay = PrayerTrackerData(dateLabel = "Wed", prayedCount = 0)

        assertThat(PrayerTrackerWidgetState.Success(untouchedDay).hasData).isTrue()
    }

    @Test
    fun `loading and error states are never worth keeping`() {
        assertThat(NextPrayerWidgetState.Loading.hasData).isFalse()
        assertThat(NextPrayerWidgetState.Error("boom").hasData).isFalse()
        assertThat(PrayerTimesWidgetState.Loading.hasData).isFalse()
        assertThat(PrayerTimesWidgetState.Error("boom").hasData).isFalse()
    }

    /**
     * State written by a version before the schedule existed still has to render. The flat
     * fields are the worker's own answer, which is exactly what the widget used to draw.
     */
    @Test
    fun `a state with no schedule falls back to the flat fields`() {
        val legacy = NextPrayerData(
            prayerName = "Asr",
            prayerTime = "17:00",
            nextPrayerEpochMillis = 5_000L,
        )

        val entry = legacy.nextEntry(nowMillis = 1_000L)

        assertThat(entry.prayerName).isEqualTo("Asr")
        assertThat(entry.prayerTime).isEqualTo("17:00")
        assertThat(entry.epochMillis).isEqualTo(5_000L)
    }

    /**
     * Once the whole schedule has passed there is nothing ahead to select, and the flat fields
     * are just as stale. Falling back to them is still right: the widget renders a name and an
     * em dash rather than an empty box, and the next refresh replaces the lot.
     */
    @Test
    fun `a fully-passed schedule falls back rather than picking a passed prayer`() {
        val data = NextPrayerData(
            prayerName = "Isha",
            nextPrayerEpochMillis = 2_000L,
            schedule = listOf(
                NextPrayerEntry(prayerName = "Maghrib", epochMillis = 1_000L),
                NextPrayerEntry(prayerName = "Isha", epochMillis = 2_000L),
            ),
        )

        assertThat(data.nextEntry(nowMillis = 9_000L).prayerName).isEqualTo("Isha")
    }
}
