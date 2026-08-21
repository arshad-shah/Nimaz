package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.domain.time.TodayProvider
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * What the prayer-tracker widget shows.
 *
 * The widget is the app's most-glanced-at surface, and until now nothing asserted a single value
 * on it: `PrayerTrackerWorker.doWork()` returns early when no widget is placed, which is always
 * the case on a test device, so `WidgetWorkersTest` never reached this code (#474).
 */
class PrayerTrackerWidgetDataSourceTest {

    private val dao: PrayerDao = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    /** A Wednesday, so the three-letter label is unambiguous. */
    private val wednesday = LocalDate.of(2026, 8, 12)

    private fun dataSource() = PrayerTrackerWidgetDataSource(dao, todayProvider)

    private fun record(name: String, status: String) = PrayerRecordEntity(
        date = 0L,
        prayerName = name,
        status = status,
        scheduledTime = 0L,
        prayedAt = null,
        note = null,
    )

    private suspend fun given(vararg records: PrayerRecordEntity) {
        every { todayProvider.today() } returns wednesday
        coEvery { dao.getPrayerRecordsForDateSync(any()) } returns records.toList()
    }

    @Test
    fun `no records means nothing prayed`() = runTest {
        given()

        val data = dataSource().load()

        assertThat(data.prayedCount).isEqualTo(0)
        assertThat(data.totalCount).isEqualTo(5)
        assertThat(listOf(data.fajr, data.dhuhr, data.asr, data.maghrib, data.isha))
            .containsExactly(false, false, false, false, false)
    }

    @Test
    fun `each prayer maps to its own flag`() = runTest {
        given(
            record("fajr", "prayed"),
            record("asr", "prayed"),
            record("isha", "prayed"),
        )

        val data = dataSource().load()

        assertThat(data.fajr).isTrue()
        assertThat(data.dhuhr).isFalse()
        assertThat(data.asr).isTrue()
        assertThat(data.maghrib).isFalse()
        assertThat(data.isha).isTrue()
        assertThat(data.prayedCount).isEqualTo(3)
    }

    /**
     * Only "prayed" counts. A qada prayer has been made up, not prayed on time, and the widget
     * distinguishing the two is the whole point of it — the other statuses are "missed", "qada"
     * and "pending".
     */
    @Test
    fun `only the prayed status counts`() = runTest {
        given(
            record("fajr", "missed"),
            record("dhuhr", "qada"),
            record("asr", "pending"),
            record("maghrib", "prayed"),
        )

        val data = dataSource().load()

        assertThat(data.prayedCount).isEqualTo(1)
        assertThat(data.maghrib).isTrue()
        assertThat(data.fajr).isFalse()
        assertThat(data.dhuhr).isFalse()
        assertThat(data.asr).isFalse()
    }

    /**
     * The count and the five flags used to be computed from ten separate string comparisons —
     * the same expression written twice per prayer — so they could disagree. They now derive
     * from one list, and this is what holds that.
     */
    @Test
    fun `the count always agrees with the flags`() = runTest {
        given(
            record("fajr", "prayed"),
            record("dhuhr", "prayed"),
            record("asr", "prayed"),
            record("maghrib", "prayed"),
            record("isha", "prayed"),
        )

        val data = dataSource().load()

        val flagged = listOf(data.fajr, data.dhuhr, data.asr, data.maghrib, data.isha).count { it }
        assertThat(data.prayedCount).isEqualTo(flagged)
        assertThat(data.prayedCount).isEqualTo(5)
    }

    @Test
    fun `the date label is the day abbreviation, capitalised`() = runTest {
        given()

        assertThat(dataSource().load().dateLabel).isEqualTo("Wed")
    }

    /**
     * An unknown prayer name must not be counted. The widget shows five, and a stray row — a
     * future prayer type, a typo in a migration — must not make it show six.
     */
    @Test
    fun `an unrecognised prayer name is ignored`() = runTest {
        given(
            record("fajr", "prayed"),
            record("tahajjud", "prayed"),
        )

        val data = dataSource().load()

        assertThat(data.prayedCount).isEqualTo(1)
        assertThat(data.totalCount).isEqualTo(5)
    }
}
