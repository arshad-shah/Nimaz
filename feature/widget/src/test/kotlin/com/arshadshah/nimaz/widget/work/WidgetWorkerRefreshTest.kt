package com.arshadshah.nimaz.widget.work

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarData
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidgetDataSource
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWidgetState
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWorker
import com.arshadshah.nimaz.widget.hijridate.HijriDateData
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetDataSource
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetState
import com.arshadshah.nimaz.widget.hijridate.HijriDateWorker
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetData
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetDataSource
import com.arshadshah.nimaz.widget.khatam.KhatamWidgetState
import com.arshadshah.nimaz.widget.khatam.KhatamWorker
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerData
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetDataSource
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWidgetState
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWorker
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesData
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetDataSource
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWidgetState
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWorker
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerData
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWidgetDataSource
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWidgetState
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Each worker's `doWork`, executed for real.
 *
 * The six bodies are the same five arguments filled in differently, which is exactly the shape a
 * copy-paste mistake hides in: a worker wired to another widget's state definition compiles,
 * passes review, and then overwrites the wrong widget's state at runtime. These assert that each
 * worker publishes *its own* state type, loaded from *its own* data source.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetWorkerRefreshTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun stubGlanceHost() {
        mockkConstructor(GlanceAppWidgetManager::class)
        mockkStatic("androidx.glance.appwidget.state.GlanceAppWidgetStateKt")
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<GlanceAppWidget>().updateAll(any()) } just Runs
        coEvery {
            anyConstructed<GlanceAppWidgetManager>().getGlanceIds<GlanceAppWidget>(any())
        } returns listOf(mockk<GlanceId>())
    }

    @After
    fun tearDown() = unmockkAll()

    /** Builds [W] with the real assisted constructor, bypassing Hilt. */
    private inline fun <reified W : ListenableWorker> build(
        crossinline create: (Context, WorkerParameters) -> W,
    ): W = TestListenableWorkerBuilder<W>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = create(appContext, workerParameters)
        })
        .build()

    /** Runs [worker] and returns the state it published. */
    @Suppress("UNCHECKED_CAST")
    private fun <S> publishedBy(worker: ListenableWorker): Pair<ListenableWorker.Result, S> =
        runBlocking {
            val published = slot<suspend (S) -> S>()
            coEvery {
                updateAppWidgetState(any(), any<GlanceStateDefinition<S>>(), any(), capture(published))
            } answers { thirdArg<Any>() as S }

            val result = (worker as androidx.work.CoroutineWorker).doWork()
            result to published.captured.invoke(null as S)
        }

    @Test
    fun `the hijri-date worker publishes the date its data source loaded`() {
        val source = mockk<HijriDateWidgetDataSource> {
            coEvery { load() } returns HijriDateData(hijriMonth = "Rajab")
        }
        val worker = build { c, p -> HijriDateWorker(c, p, source) }

        val (result, state) = publishedBy<HijriDateWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as HijriDateWidgetState.Success).data.hijriMonth).isEqualTo("Rajab")
    }

    @Test
    fun `the hijri-calendar worker publishes the month its data source loaded`() {
        val source = mockk<HijriCalendarWidgetDataSource> {
            coEvery { load() } returns HijriCalendarData(hijriMonthName = "Shaban")
        }
        val worker = build { c, p -> HijriCalendarWorker(c, p, source) }

        val (result, state) = publishedBy<HijriCalendarWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as HijriCalendarWidgetState.Success).data.hijriMonthName)
            .isEqualTo("Shaban")
    }

    @Test
    fun `the khatam worker publishes the khatam its data source loaded`() {
        val source = mockk<KhatamWidgetDataSource> {
            coEvery { load() } returns KhatamWidgetData(hasActiveKhatam = true, name = "Daily")
        }
        val worker = build { c, p -> KhatamWorker(c, p, source) }

        val (result, state) = publishedBy<KhatamWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as KhatamWidgetState.Success).data.name).isEqualTo("Daily")
    }

    @Test
    fun `the next-prayer worker publishes the prayer its data source loaded`() {
        val source = mockk<NextPrayerWidgetDataSource> {
            coEvery { load() } returns NextPrayerData(prayerName = "Asr")
        }
        val worker = build { c, p -> NextPrayerWorker(c, p, source) }

        val (result, state) = publishedBy<NextPrayerWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as NextPrayerWidgetState.Success).data.prayerName).isEqualTo("Asr")
    }

    @Test
    fun `the prayer-times worker publishes the times its data source loaded`() {
        val source = mockk<PrayerTimesWidgetDataSource> {
            coEvery { load() } returns PrayerTimesData(locationName = "Cork")
        }
        val worker = build { c, p -> PrayerTimesWorker(c, p, source) }

        val (result, state) = publishedBy<PrayerTimesWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as PrayerTimesWidgetState.Success).data.locationName).isEqualTo("Cork")
    }

    @Test
    fun `the prayer-tracker worker publishes the tally its data source loaded`() {
        val source = mockk<PrayerTrackerWidgetDataSource> {
            coEvery { load() } returns PrayerTrackerData(dateLabel = "Mon", prayedCount = 3)
        }
        val worker = build { c, p -> PrayerTrackerWorker(c, p, source) }

        val (result, state) = publishedBy<PrayerTrackerWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat((state as PrayerTrackerWidgetState.Success).data.prayedCount).isEqualTo(3)
    }

    /**
     * A data source that throws must not take the worker down — it publishes the error frame and
     * asks to be retried, carrying the failure's own message so a crash report says which load
     * failed.
     */
    @Test
    fun `a data source that throws yields an error state and a retry`() {
        val source = mockk<HijriDateWidgetDataSource> {
            coEvery { load() } throws IllegalStateException("no calendar")
        }
        val worker = build { c, p -> HijriDateWorker(c, p, source) }
        coEvery {
            androidx.glance.appwidget.state.getAppWidgetState(
                any(),
                any<GlanceStateDefinition<HijriDateWidgetState>>(),
                any(),
            )
        } returns HijriDateWidgetState.Success(HijriDateData())

        val (result, state) = publishedBy<HijriDateWidgetState>(worker)

        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
        assertThat((state as HijriDateWidgetState.Error).message).isEqualTo("no calendar")
    }
}
