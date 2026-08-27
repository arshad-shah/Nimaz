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
}
