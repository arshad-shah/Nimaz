package com.arshadshah.nimaz.presentation.screens.prayer

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression coverage for the emulator bug where a day in progress -- some prayers
 * [PrayerDisplayStatus.NOT_RECORDED], some still [PrayerDisplayStatus.UPCOMING], none actually
 * [PrayerDisplayStatus.MISSED] -- was classified as [DayBucket.HAS_MISSED] and painted a solid
 * red "you missed these" dot. See bucket()'s doc comment in PrayerTrackerScreen.kt.
 */
class DayBucketTest {

    private fun day(
        fajr: PrayerDisplayStatus,
        dhuhr: PrayerDisplayStatus,
        asr: PrayerDisplayStatus,
        maghrib: PrayerDisplayStatus,
        isha: PrayerDisplayStatus,
    ): Map<PrayerName, PrayerDisplayStatus> = mapOf(
        PrayerName.FAJR to fajr,
        PrayerName.DHUHR to dhuhr,
        PrayerName.ASR to asr,
        PrayerName.MAGHRIB to maghrib,
        PrayerName.ISHA to isha,
    )

    @Test
    fun `today in progress with nothing missed is not the red bucket`() {
        val statuses = day(
            fajr = PrayerDisplayStatus.NOT_RECORDED,
            dhuhr = PrayerDisplayStatus.NOT_RECORDED,
            asr = PrayerDisplayStatus.UPCOMING,
            maghrib = PrayerDisplayStatus.UPCOMING,
            isha = PrayerDisplayStatus.UPCOMING,
        )

        assertThat(statuses.bucket()).isEqualTo(DayBucket.ALL_UNRECORDED)
    }

    @Test
    fun `a genuinely missed day with nothing fulfilled is the red bucket`() {
        val statuses = day(
            fajr = PrayerDisplayStatus.MISSED,
            dhuhr = PrayerDisplayStatus.MISSED,
            asr = PrayerDisplayStatus.NOT_RECORDED,
            maghrib = PrayerDisplayStatus.NOT_RECORDED,
            isha = PrayerDisplayStatus.NOT_RECORDED,
        )

        assertThat(statuses.bucket()).isEqualTo(DayBucket.HAS_MISSED)
    }

    @Test
    fun `all five fulfilled is all done`() {
        val statuses = day(
            fajr = PrayerDisplayStatus.PRAYED,
            dhuhr = PrayerDisplayStatus.LATE,
            asr = PrayerDisplayStatus.QADA,
            maghrib = PrayerDisplayStatus.PRAYED,
            isha = PrayerDisplayStatus.PRAYED,
        )

        assertThat(statuses.bucket()).isEqualTo(DayBucket.ALL_DONE)
    }

    @Test
    fun `some fulfilled and some missed is partial, not the red bucket`() {
        val statuses = day(
            fajr = PrayerDisplayStatus.PRAYED,
            dhuhr = PrayerDisplayStatus.MISSED,
            asr = PrayerDisplayStatus.NOT_RECORDED,
            maghrib = PrayerDisplayStatus.NOT_RECORDED,
            isha = PrayerDisplayStatus.UPCOMING,
        )

        assertThat(statuses.bucket()).isEqualTo(DayBucket.PARTIAL)
    }

    @Test
    fun `a wholly unrecorded past day is a ring, not the red bucket`() {
        val statuses = day(
            fajr = PrayerDisplayStatus.NOT_RECORDED,
            dhuhr = PrayerDisplayStatus.NOT_RECORDED,
            asr = PrayerDisplayStatus.NOT_RECORDED,
            maghrib = PrayerDisplayStatus.NOT_RECORDED,
            isha = PrayerDisplayStatus.NOT_RECORDED,
        )

        assertThat(statuses.bucket()).isEqualTo(DayBucket.ALL_UNRECORDED)
    }
}
