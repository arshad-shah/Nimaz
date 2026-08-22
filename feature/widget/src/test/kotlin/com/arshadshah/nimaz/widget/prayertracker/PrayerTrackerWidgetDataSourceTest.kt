package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.time.TodayProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * What the prayer-tracker widget shows.
 *
 * The widget is the app's most-glanced-at surface, and until now nothing asserted a single value
 * on it: `PrayerTrackerWorker.doWork()` returns early when no widget is placed, which is always
 * the case on a test device, so `WidgetWorkersTest` never reached this code (#474).
 *
 * Rewritten against [PrayerRepository] in PR 13 of #551. It mocked [PrayerName]-less `PrayerDao`
 * rows before, because the data source injected the DAO directly — a widget reaching past its
 * repository into `:core:database`. Two of these tests changed shape as a result, and both got
 * stronger for it; see `only the prayed status counts` and `sunrise is not one of the five`.
 */
class PrayerTrackerWidgetDataSourceTest {

    private val prayerRepository: PrayerRepository = mockk(relaxed = true)
    private val todayProvider: TodayProvider = mockk(relaxed = true)

    /** A Wednesday, so the three-letter label is unambiguous. */
    private val wednesday = LocalDate.of(2026, 8, 12)

    private fun dataSource() = PrayerTrackerWidgetDataSource(prayerRepository, todayProvider)

    private fun record(name: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = 0L,
        date = 0L,
        prayerName = name,
        status = status,
        prayedAt = null,
        scheduledTime = 0L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun given(vararg records: PrayerRecord) {
        every { todayProvider.today() } returns wednesday
        coEvery { prayerRepository.getPrayerRecordsForDate(any()) } returns flowOf(records.toList())
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
            record(PrayerName.FAJR, PrayerStatus.PRAYED),
            record(PrayerName.ASR, PrayerStatus.PRAYED),
            record(PrayerName.ISHA, PrayerStatus.PRAYED),
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
     * Only [PrayerStatus.PRAYED] counts. A qada prayer has been made up, not prayed on time, and
     * the widget distinguishing the two is the whole point of it.
     *
     * This now covers **all five** non-prayed statuses rather than the three the string version
     * managed. `LATE` and `NOT_PRAYED` were missing from the old test — not by choice, but
     * because the DAO's `status` was a `String` and nothing enumerated the values, so writing the
     * list meant remembering them. `PrayerStatus.entries` cannot forget one, and a status added
     * later joins this test for free.
     */
    @Test
    fun `only the prayed status counts`() = runTest {
        val notPrayed = PrayerStatus.entries.filter { it != PrayerStatus.PRAYED }
        assertThat(notPrayed).hasSize(5)

        notPrayed.forEach { status ->
            given(record(PrayerName.FAJR, status))

            val data = dataSource().load()

            assertThat(data.prayedCount).isEqualTo(0)
            assertThat(data.fajr).isFalse()
        }

        given(record(PrayerName.FAJR, PrayerStatus.PRAYED))
        assertThat(dataSource().load().fajr).isTrue()
    }

    /**
     * The count and the five flags used to be computed from ten separate string comparisons —
     * the same expression written twice per prayer — so they could disagree. They now derive
     * from one list, and this is what holds that.
     */
    @Test
    fun `the count always agrees with the flags`() = runTest {
        given(
            record(PrayerName.FAJR, PrayerStatus.PRAYED),
            record(PrayerName.DHUHR, PrayerStatus.PRAYED),
            record(PrayerName.ASR, PrayerStatus.PRAYED),
            record(PrayerName.MAGHRIB, PrayerStatus.PRAYED),
            record(PrayerName.ISHA, PrayerStatus.PRAYED),
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
     * The widget shows five, and a sixth row must not make it show six.
     *
     * The old test fed the DAO a row named `"tahajjud"` — a string no `PrayerName` parses, which
     * cannot occur now that the repository returns the enum. The real risk it was groping at is
     * live and typed: [PrayerName] has **six** entries, because `SUNRISE` is one of them. It is a
     * time rather than a prayer, `PrayerRepository` will hand one over like any other record, and
     * writing `PrayerName.entries` in the data source instead of the explicit five is all it
     * would take to make the widget read "3 of 6".
     */
    @Test
    fun `sunrise is not one of the five`() = runTest {
        given(
            record(PrayerName.FAJR, PrayerStatus.PRAYED),
            record(PrayerName.SUNRISE, PrayerStatus.PRAYED),
        )

        val data = dataSource().load()

        assertThat(data.prayedCount).isEqualTo(1)
        assertThat(data.totalCount).isEqualTo(5)
    }
}
