package com.arshadshah.nimaz.widget.prayertracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.widget.WidgetEntryPoint
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.EntryPointAccessors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Tapping a tile on the prayer-tracker widget is **the only widget action in the app that writes
 * user data**. Everything else the widget package does is a read that redraws.
 *
 * It cannot be driven through the tap in a JVM test: a Glance `clickable { }` is a lambda action
 * resolved by the AppWidget host, and `glance-appwidget-testing`'s unit-test API is
 * assertion-only — it has no `performClick`. So the action is called directly (it is `internal`
 * for exactly this), and the tile that invokes it is covered by `PrayerTrackerWidgetRenderTest`.
 */
@RunWith(RobolectricTestRunner::class)
class TogglePrayerStatusTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repository: PrayerRepository = mockk(relaxed = true)
    private val today by lazy { LocalDate.now().toUtcMidnightMillis() }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(any<Context>(), WidgetEntryPoint::class.java)
        } returns mockk<WidgetEntryPoint> { every { prayerRepository() } returns repository }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun record(status: PrayerStatus) = PrayerRecord(
        id = 4L,
        date = today,
        prayerName = PrayerName.ASR,
        status = status,
        prayedAt = null,
        scheduledTime = 0L,
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /** Toggling an unprayed prayer marks it prayed, and stamps when. */
    @Test
    fun `an unprayed record is flipped to prayed with a timestamp`() {
        coEvery { repository.getPrayerRecord(today, PrayerName.ASR) } returns
            record(PrayerStatus.NOT_PRAYED)
        val prayedAt = slot<Long?>()

        togglePrayerStatus(context, PrayerName.ASR)

        coVerify(timeout = 5_000) {
            repository.updatePrayerStatus(
                date = today,
                prayerName = PrayerName.ASR,
                status = PrayerStatus.PRAYED,
                prayedAt = captureNullable(prayedAt),
                isJamaah = false,
            )
        }
        assertThat(prayedAt.captured).isNotNull()
    }

    /** And back again — tapping a prayed tile un-marks it, clearing the timestamp. */
    @Test
    fun `a prayed record is flipped back and loses its timestamp`() {
        coEvery { repository.getPrayerRecord(today, PrayerName.ASR) } returns
            record(PrayerStatus.PRAYED)
        val prayedAt = slot<Long?>()

        togglePrayerStatus(context, PrayerName.ASR)

        coVerify(timeout = 5_000) {
            repository.updatePrayerStatus(
                date = today,
                prayerName = PrayerName.ASR,
                status = PrayerStatus.NOT_PRAYED,
                prayedAt = captureNullable(prayedAt),
                isJamaah = false,
            )
        }
        assertThat(prayedAt.captured).isNull()
    }

    /**
     * The first tap of a day has no row to update. Inserting rather than silently doing nothing is
     * what makes the widget usable before the app has been opened.
     */
    @Test
    fun `a day with no record yet gets one inserted as prayed`() {
        coEvery { repository.getPrayerRecord(today, PrayerName.FAJR) } returns null
        val inserted = slot<PrayerRecord>()

        togglePrayerStatus(context, PrayerName.FAJR)

        coVerify(timeout = 5_000) { repository.insertPrayerRecord(capture(inserted)) }
        assertThat(inserted.captured.prayerName).isEqualTo(PrayerName.FAJR)
        assertThat(inserted.captured.status).isEqualTo(PrayerStatus.PRAYED)
        assertThat(inserted.captured.date).isEqualTo(today)
        assertThat(inserted.captured.prayedAt).isNotNull()
        assertThat(inserted.captured.isJamaah).isFalse()
        coVerify(exactly = 0) {
            repository.updatePrayerStatus(any(), any(), any(), any(), any())
        }
    }

    /** The tile has to redraw, or the tap looks like it did nothing. */
    @Test
    fun `the tile is refreshed after the write`() {
        coEvery { repository.getPrayerRecord(any(), any()) } returns null

        togglePrayerStatus(context, PrayerName.ISHA)

        coVerify(timeout = 5_000) { repository.insertPrayerRecord(any()) }
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("PrayerTrackerWorkerOneTime").get()
        assertThat(work).isNotEmpty()
    }

    /**
     * A widget tap runs outside any Activity, so an exception here has nowhere to surface but the
     * system's crash dialog for the *launcher*. It is reported and swallowed.
     */
    @Test
    fun `a repository failure is reported rather than crashing the launcher`() {
        coEvery { repository.getPrayerRecord(any(), any()) } throws IllegalStateException("locked")

        togglePrayerStatus(context, PrayerName.MAGHRIB)

        coVerify(timeout = 5_000) { repository.getPrayerRecord(any(), any()) }
        coVerify(exactly = 0) { repository.insertPrayerRecord(any()) }
    }
}
