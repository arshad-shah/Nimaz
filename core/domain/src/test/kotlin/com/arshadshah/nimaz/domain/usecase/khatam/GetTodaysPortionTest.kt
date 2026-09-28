package com.arshadshah.nimaz.domain.usecase.khatam

import com.arshadshah.nimaz.domain.model.Khatam
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a plan asks for *today*, which is the sentence the detail screen never had.
 */
class GetTodaysPortionTest {

    private val portion = GetTodaysPortion()

    private fun khatam(dailyTarget: Int = 20, read: Int = 0) = Khatam(
        name = "Ramadan",
        dailyTarget = dailyTarget,
        totalAyahsRead = read,
    )

    @Test
    fun `a fresh plan starts at the beginning`() {
        val today = portion(khatam(), nextUnreadAyahId = null)

        assertThat(today).isEqualTo(KhatamPortion(fromAyahId = 1, toAyahId = 20))
        assertThat(today!!.ayahCount).isEqualTo(20)
    }

    @Test
    fun `a partly-read plan starts at the next unread verse`() {
        val today = portion(khatam(), nextUnreadAyahId = 101)

        assertThat(today).isEqualTo(KhatamPortion(fromAyahId = 101, toAyahId = 120))
    }

    @Test
    fun `a plan behind schedule is still asked for one day, not the backlog`() {
        // Two days missed does not make today's instruction sixty verses. Falling behind is
        // what the pace status is for; the portion stays a day's worth.
        val today = portion(khatam(dailyTarget = 20), nextUnreadAyahId = 41)

        assertThat(today!!.ayahCount).isEqualTo(20)
    }

    @Test
    fun `the last portion stops at the end of the book`() {
        val today = portion(khatam(), nextUnreadAyahId = Khatam.TOTAL_QURAN_AYAHS - 4)

        assertThat(today!!.toAyahId).isEqualTo(Khatam.TOTAL_QURAN_AYAHS)
        assertThat(today.ayahCount).isEqualTo(5)
    }

    @Test
    fun `a finished plan has no portion`() {
        val today = portion(khatam(), nextUnreadAyahId = Khatam.TOTAL_QURAN_AYAHS + 1)

        // A plan that is done should stop giving orders.
        assertThat(today).isNull()
    }

    @Test
    fun `a plan with no daily target has no portion to name`() {
        assertThat(portion(khatam(dailyTarget = 0), nextUnreadAyahId = 1)).isNull()
    }
    private val zone = java.time.ZoneId.of("Europe/Dublin")
    private val morning = java.time.Instant.parse("2026-09-28T09:00:00Z").toEpochMilli()
    private val yesterday = java.time.Instant.parse("2026-09-27T09:00:00Z").toEpochMilli()

    @Test
    fun `marking verses does not move today's finish line across use case instances`() {
        val marks = (1..100).associateWith { yesterday }
        val expected = portion.forDay(khatam(), marks, morning, zone)!!
        val afterReading = marks + (101..110).associateWith { morning }
        val actual = GetTodaysPortion().forDay(khatam(read = 110), afterReading, morning, zone)!!
        assertThat(actual).isEqualTo(expected)
        assertThat(actual.readCount(afterReading.keys)).isEqualTo(10)
        assertThat(actual.toAyahId).isEqualTo(120)
    }

    @Test
    fun `finishing today's portion does not assign another one`() {
        val marks = (1..100).associateWith { yesterday } + (101..125).associateWith { morning }
        val today = portion.forDay(khatam(read = 125), marks, morning, zone)!!
        assertThat(today.ayahIds).containsExactlyElementsIn(101..120).inOrder()
        assertThat(today.readCount(marks.keys)).isEqualTo(20)
    }

    @Test
    fun `tomorrow begins from the next unread verse`() {
        val marks = (1..100).associateWith { yesterday } + (101..120).associateWith { morning }
        val tomorrow = java.time.Instant.parse("2026-09-29T09:00:00Z").toEpochMilli()
        assertThat(portion.forDay(khatam(read = 120), marks, tomorrow, zone)!!.fromAyahId).isEqualTo(121)
    }

    @Test
    fun `out of order history skips old marks without reducing today's assignment`() {
        val today = portion.forDay(khatam(dailyTarget = 3), mapOf(2 to yesterday, 4 to yesterday), morning, zone)!!
        assertThat(today.ayahIds).containsExactly(1, 3, 5).inOrder()
        assertThat(today.ayahCount).isEqualTo(3)
        assertThat(today.readCount(setOf(2, 4))).isEqualTo(0)
    }

    @Test
    fun `undoing today's mark reopens the same portion`() {
        val marks = (1..10).associateWith { morning }
        val before = portion.forDay(khatam(), marks, morning, zone)!!
        val after = portion.forDay(khatam(), marks - 5, morning, zone)!!
        assertThat(after).isEqualTo(before)
        assertThat(after.readCount((marks - 5).keys)).isEqualTo(9)
    }

    @Test
    fun `completed archived and fully read plans have no assignment`() {
        for (status in listOf(com.arshadshah.nimaz.domain.model.KhatamStatus.COMPLETED, com.arshadshah.nimaz.domain.model.KhatamStatus.ABANDONED)) {
            assertThat(portion.forDay(khatam().copy(status = status), emptyMap(), morning, zone)).isNull()
            assertThat(portion(khatam().copy(status = status), null)).isNull()
        }
        assertThat(portion.forDay(khatam(read = 6236), emptyMap(), morning, zone)).isNull()
    }

    @Test
    fun `the last few unread verses are a smaller final portion`() {
        val marks = (1..6233).associateWith { yesterday }
        assertThat(portion.forDay(khatam(read = 6233), marks, morning, zone)!!.ayahIds).containsExactly(6234, 6235, 6236)
        assertThat(portion.forDay(khatam(), (1..6236).associateWith { yesterday }, morning, zone)).isNull()
        assertThat(portion.forDay(khatam(dailyTarget = -1), emptyMap(), morning, zone)).isNull()
    }

    @Test
    fun `local midnight uses the daylight saving offset`() {
        // Dublin midnight on 25 October is 23:00 UTC on the 24th, before clocks fall back.
        val now = java.time.Instant.parse("2026-10-25T12:00:00Z").toEpochMilli()
        val before = java.time.Instant.parse("2026-10-24T22:59:59Z").toEpochMilli()
        val midnight = java.time.Instant.parse("2026-10-24T23:00:00Z").toEpochMilli()
        val today = portion.forDay(khatam(), mapOf(1 to before, 2 to midnight), now, zone)!!
        assertThat(today.fromAyahId).isEqualTo(2)
        assertThat(today.toAyahId).isEqualTo(21)
    }

}
