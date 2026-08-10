package com.arshadshah.nimaz.widget.khatam

import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * What the khatam widget actually shows.
 *
 * These assertions were impossible before the computation came out of [KhatamWorker]: `doWork()`
 * opens by asking `GlanceAppWidgetManager` for glance ids, a test device has no widget placed, so
 * the early return skipped the entire body. `WidgetWorkersTest` passed for a year without
 * touching a single line of this. See #474.
 */
class KhatamWidgetDataSourceTest {

    private val repository: KhatamRepository = mockk(relaxed = true)

    private fun dataSource() = KhatamWidgetDataSource(repository)

    private fun khatam(
        id: Long = 1L,
        name: String = "Ramadan 1447",
        dailyTarget: Int = 20,
        totalAyahsRead: Int = 0,
    ) = Khatam(
        id = id,
        name = name,
        isActive = true,
        dailyTarget = dailyTarget,
        totalAyahsRead = totalAyahsRead,
    )

    private fun snapshot(khatam: Khatam, insights: KhatamInsights) = mockk<KhatamDetailSnapshot> {
        every { this@mockk.insights } returns insights
        every { this@mockk.khatam } returns khatam
    }

    @Test
    fun `no active khatam renders the empty state`() = runTest {
        every { repository.observeActiveKhatam() } returns flowOf(null)

        val data = dataSource().load()

        assertThat(data.hasActiveKhatam).isFalse()
    }

    @Test
    fun `an active khatam carries its name, target and insights`() = runTest {
        val k = khatam(name = "Ramadan 1447", dailyTarget = 30, totalAyahsRead = 1246)
        every { repository.observeActiveKhatam() } returns flowOf(k)
        every { repository.observeKhatamDetail(1L) } returns flowOf(
            snapshot(k, KhatamInsights(currentJuz = 5, juzCompleted = 4, currentStreak = 12, remainingAyahs = 4990))
        )

        val data = dataSource().load()

        assertThat(data.hasActiveKhatam).isTrue()
        assertThat(data.name).isEqualTo("Ramadan 1447")
        assertThat(data.dailyTarget).isEqualTo(30)
        assertThat(data.currentJuz).isEqualTo(5)
        assertThat(data.juzCompleted).isEqualTo(4)
        assertThat(data.currentStreak).isEqualTo(12)
        assertThat(data.remainingAyahs).isEqualTo(4990)
    }

    /**
     * A khatam that exists but has no detail row yet is a real state — the snapshot is computed
     * separately. Every insight field has to fall back rather than crash the worker, which would
     * leave the widget on its error state until the next 30-minute run.
     */
    @Test
    fun `a missing detail snapshot falls back instead of failing`() = runTest {
        val k = khatam(totalAyahsRead = 100)
        every { repository.observeActiveKhatam() } returns flowOf(k)
        every { repository.observeKhatamDetail(1L) } returns flowOf(null)

        val data = dataSource().load()

        assertThat(data.hasActiveKhatam).isTrue()
        assertThat(data.currentJuz).isEqualTo(1)
        assertThat(data.juzCompleted).isEqualTo(0)
        assertThat(data.currentStreak).isEqualTo(0)
        // Falls back to the khatam's own arithmetic rather than to zero, which would render as
        // "finished".
        assertThat(data.remainingAyahs).isEqualTo(k.remainingAyahs)
    }

    /**
     * A khatam recorded past its own total would otherwise render a progress bar wider than its
     * track. The clamp is the only arithmetic in this class, so it is the one worth pinning.
     */
    @Test
    fun `progress is clamped to 0-100`() = runTest {
        val over = khatam(totalAyahsRead = Khatam.TOTAL_QURAN_AYAHS * 2)
        every { repository.observeActiveKhatam() } returns flowOf(over)
        every { repository.observeKhatamDetail(any()) } returns flowOf(null)

        assertThat(dataSource().load().progressPercent).isEqualTo(100)
    }

    @Test
    fun `a khatam with nothing read reports zero progress`() = runTest {
        val fresh = khatam(totalAyahsRead = 0)
        every { repository.observeActiveKhatam() } returns flowOf(fresh)
        every { repository.observeKhatamDetail(any()) } returns flowOf(null)

        assertThat(dataSource().load().progressPercent).isEqualTo(0)
    }
}
