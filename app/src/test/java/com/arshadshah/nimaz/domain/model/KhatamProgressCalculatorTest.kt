package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for the pure khatam progress maths.
 *
 * These exist because the previous implementation had no coverage at all, which is why a
 * stats function returning all zeros and a streak concept that was never implemented both
 * survived review.
 */
class KhatamProgressCalculatorTest {

    private val day = TimeUnit.DAYS.toMillis(1)

    /** A fixed "now" so tests never depend on the wall clock. */
    private val now = 1_700_000_000_000L

    private fun daysAgo(n: Int) = startOfDay(now) - n * day

    private fun startOfDay(ts: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun logs(vararg daysAgoValues: Int) =
        daysAgoValues.map { DailyLogEntry(date = daysAgo(it), ayahsRead = 10) }

    // ---- daysActive ----

    @Test
    fun `days active is zero when never started`() {
        assertThat(KhatamProgressCalculator.daysActive(null, now)).isEqualTo(0)
    }

    @Test
    fun `days active floors at one on the first day`() {
        // Started an hour ago: less than a full day elapsed, but reading has begun.
        val started = now - TimeUnit.HOURS.toMillis(1)
        assertThat(KhatamProgressCalculator.daysActive(started, now)).isEqualTo(1)
    }

    @Test
    fun `days active counts whole elapsed days`() {
        assertThat(KhatamProgressCalculator.daysActive(now - 21 * day, now)).isEqualTo(21)
    }

    @Test
    fun `days active is zero for a future start date`() {
        assertThat(KhatamProgressCalculator.daysActive(now + 5 * day, now)).isEqualTo(0)
    }

    // ---- averagePace ----

    @Test
    fun `average pace divides read by days`() {
        assertThat(KhatamProgressCalculator.averagePace(600, 6)).isEqualTo(100f)
    }

    @Test
    fun `average pace is zero rather than dividing by zero`() {
        assertThat(KhatamProgressCalculator.averagePace(600, 0)).isEqualTo(0f)
    }

    // ---- paceStatus ----

    @Test
    fun `not started until at least one day is active`() {
        assertThat(KhatamProgressCalculator.paceStatus(0f, 20, 0))
            .isEqualTo(KhatamPace.NOT_STARTED)
    }

    @Test
    fun `meeting the target is on track`() {
        assertThat(KhatamProgressCalculator.paceStatus(20f, 20, 5))
            .isEqualTo(KhatamPace.ON_TRACK)
    }

    @Test
    fun `beating the target is on track`() {
        assertThat(KhatamProgressCalculator.paceStatus(45f, 20, 5))
            .isEqualTo(KhatamPace.ON_TRACK)
    }

    @Test
    fun `just under the target is only slightly behind`() {
        // 16 / 20 = 0.8, above the 0.75 tolerance.
        assertThat(KhatamProgressCalculator.paceStatus(16f, 20, 5))
            .isEqualTo(KhatamPace.SLIGHTLY_BEHIND)
    }

    @Test
    fun `well under the target is behind`() {
        assertThat(KhatamProgressCalculator.paceStatus(5f, 20, 5))
            .isEqualTo(KhatamPace.BEHIND)
    }

    @Test
    fun `a zero target cannot put the reader behind`() {
        assertThat(KhatamProgressCalculator.paceStatus(0f, 0, 5))
            .isEqualTo(KhatamPace.ON_TRACK)
    }

    // ---- estimatedDaysRemaining ----

    @Test
    fun `estimate uses actual pace when there is one`() {
        assertThat(KhatamProgressCalculator.estimatedDaysRemaining(1000, 100f, 20))
            .isEqualTo(10)
    }

    @Test
    fun `estimate falls back to the target before a pace exists`() {
        assertThat(KhatamProgressCalculator.estimatedDaysRemaining(1000, 0f, 100))
            .isEqualTo(10)
    }

    @Test
    fun `estimate rounds partial days up`() {
        // 101 ayahs at 100/day needs a second day, not 1.01 days.
        assertThat(KhatamProgressCalculator.estimatedDaysRemaining(101, 100f, 20))
            .isEqualTo(2)
    }

    @Test
    fun `estimate is null once nothing remains`() {
        assertThat(KhatamProgressCalculator.estimatedDaysRemaining(0, 100f, 20)).isNull()
    }

    // ---- currentStreak ----

    @Test
    fun `no logs means no streak`() {
        assertThat(KhatamProgressCalculator.currentStreak(emptyList(), now)).isEqualTo(0)
    }

    @Test
    fun `consecutive days ending today count`() {
        assertThat(KhatamProgressCalculator.currentStreak(logs(0, 1, 2), now)).isEqualTo(3)
    }

    @Test
    fun `a streak ending yesterday is still alive`() {
        // Today may simply not have happened yet — the streak should not be lost
        // just because the reader has not opened the app this morning.
        assertThat(KhatamProgressCalculator.currentStreak(logs(1, 2, 3), now)).isEqualTo(3)
    }

    @Test
    fun `a two day gap breaks the streak`() {
        assertThat(KhatamProgressCalculator.currentStreak(logs(2, 3, 4), now)).isEqualTo(0)
    }

    @Test
    fun `the streak stops at the first gap`() {
        // Read today, yesterday, then a gap, then more history.
        assertThat(KhatamProgressCalculator.currentStreak(logs(0, 1, 3, 4), now)).isEqualTo(2)
    }

    @Test
    fun `days with nothing read do not extend a streak`() {
        val entries = listOf(
            DailyLogEntry(daysAgo(0), 10),
            DailyLogEntry(daysAgo(1), 0),
            DailyLogEntry(daysAgo(2), 10),
        )
        assertThat(KhatamProgressCalculator.currentStreak(entries, now)).isEqualTo(1)
    }

    @Test
    fun `duplicate entries for one day count once`() {
        val entries = listOf(
            DailyLogEntry(daysAgo(0), 10),
            DailyLogEntry(daysAgo(0) + 3_600_000, 5),
            DailyLogEntry(daysAgo(1), 10),
        )
        assertThat(KhatamProgressCalculator.currentStreak(entries, now)).isEqualTo(2)
    }

    // ---- longestStreak ----

    @Test
    fun `longest streak finds the best run in history`() {
        // A 2-day run recently, and a 4-day run further back.
        assertThat(KhatamProgressCalculator.longestStreak(logs(0, 1, 5, 6, 7, 8)))
            .isEqualTo(4)
    }

    @Test
    fun `longest streak of isolated days is one`() {
        assertThat(KhatamProgressCalculator.longestStreak(logs(0, 3, 9))).isEqualTo(1)
    }

    @Test
    fun `longest streak with no logs is zero`() {
        assertThat(KhatamProgressCalculator.longestStreak(emptyList())).isEqualTo(0)
    }

    // ---- insights ----

    @Test
    fun `insights combine into a consistent view`() {
        val khatam = Khatam(
            id = 1,
            name = "Test",
            dailyTarget = 100,
            totalAyahsRead = 1000,
            startedAt = now - 10 * day,
        )
        val juz = List(30) { i ->
            JuzProgressInfo(juzNumber = i + 1, totalAyahs = 200, readAyahs = if (i < 5) 200 else 0)
        }

        val insights = KhatamProgressCalculator.insights(khatam, logs(0, 1, 2), juz, now)

        assertThat(insights.daysActive).isEqualTo(10)
        assertThat(insights.averagePace).isEqualTo(100f)
        assertThat(insights.paceStatus).isEqualTo(KhatamPace.ON_TRACK)
        assertThat(insights.juzCompleted).isEqualTo(5)
        assertThat(insights.currentStreak).isEqualTo(3)
        assertThat(insights.remainingAyahs)
            .isEqualTo(Khatam.TOTAL_QURAN_AYAHS - 1000)
        assertThat(insights.projectedCompletionAt).isNotNull()
    }

    @Test
    fun `a finished khatam has no projected completion`() {
        val khatam = Khatam(
            id = 1,
            name = "Done",
            totalAyahsRead = Khatam.TOTAL_QURAN_AYAHS,
            startedAt = now - 30 * day,
        )
        val juz = List(30) { i ->
            JuzProgressInfo(juzNumber = i + 1, totalAyahs = 200, readAyahs = 200)
        }

        val insights = KhatamProgressCalculator.insights(khatam, logs(0), juz, now)

        assertThat(insights.remainingAyahs).isEqualTo(0)
        assertThat(insights.estimatedDaysRemaining).isNull()
        assertThat(insights.projectedCompletionAt).isNull()
        assertThat(insights.juzCompleted).isEqualTo(30)
    }

    // ---- juz mapping ----

    @Test
    fun `juz boundaries map ayah ids correctly`() {
        assertThat(KhatamConstants.juzForAyahId(1)).isEqualTo(1)
        assertThat(KhatamConstants.juzForAyahId(148)).isEqualTo(1)
        assertThat(KhatamConstants.juzForAyahId(149)).isEqualTo(2)
        assertThat(KhatamConstants.juzForAyahId(Khatam.TOTAL_QURAN_AYAHS)).isEqualTo(30)
    }

    @Test
    fun `out of range ayah ids have no juz`() {
        assertThat(KhatamConstants.juzForAyahId(0)).isNull()
        assertThat(KhatamConstants.juzForAyahId(Khatam.TOTAL_QURAN_AYAHS + 1)).isNull()
    }

    @Test
    fun `juz ranges cover every ayah exactly once`() {
        val covered = KhatamConstants.JUZ_AYAH_RANGES.sumOf { (start, end) -> end - start + 1 }
        assertThat(covered).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
        assertThat(KhatamConstants.JUZ_AYAH_RANGES).hasSize(Khatam.TOTAL_JUZ)
    }
}
