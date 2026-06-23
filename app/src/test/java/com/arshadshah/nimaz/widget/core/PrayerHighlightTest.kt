package com.arshadshah.nimaz.widget.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The prayer-times widget highlights the "next" prayer. The highlight must be
 * derived live from the wall clock at render time (not frozen by the 15-minute
 * refresh worker), so these tests pin down [nextPrayerIndex].
 */
class PrayerHighlightTest {

    // A representative day in chronological order: Fajr, Dhuhr, Asr, Maghrib, Isha.
    private val fajr = 1_000L
    private val dhuhr = 2_000L
    private val asr = 3_000L
    private val maghrib = 4_000L
    private val isha = 5_000L
    private val day = listOf(fajr, dhuhr, asr, maghrib, isha)

    @Test
    fun `before fajr highlights fajr`() {
        assertEquals(0, nextPrayerIndex(day, nowMillis = 500L))
    }

    @Test
    fun `between dhuhr and asr highlights asr`() {
        assertEquals(2, nextPrayerIndex(day, nowMillis = 2_500L))
    }

    @Test
    fun `at the exact prayer instant advances to the following prayer`() {
        // Dhuhr's countdown has hit zero, so Asr is now the next prayer.
        assertEquals(2, nextPrayerIndex(day, nowMillis = 2_000L))
    }

    @Test
    fun `after isha nothing is highlighted`() {
        assertEquals(-1, nextPrayerIndex(day, nowMillis = 6_000L))
    }

    @Test
    fun `unknown (zero) epochs are never highlighted`() {
        assertEquals(-1, nextPrayerIndex(listOf(0L, 0L, 0L, 0L, 0L), nowMillis = 6_000L))
    }
}