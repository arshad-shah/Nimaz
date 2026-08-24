package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.arshadshah.nimaz.widget.hijridate.HijriDateStateDefinition
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidget
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetState
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
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
 * `refreshWidget` is the body of all six widget workers. Its branches decide whether a widget that
 * is showing correct data keeps it through a transient failure, or is replaced by "tap to refresh"
 * until some later run happens to succeed — a regression a user experiences as the widget being
 * broken, and never reports as a crash.
 */
@RunWith(RobolectricTestRunner::class)
class RefreshWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val widget = HijriDateWidget()
    private val definition: GlanceStateDefinition<HijriDateWidgetState> = HijriDateStateDefinition

    private val loaded = HijriDateWidgetState.Success(
        com.arshadshah.nimaz.widget.hijridate.HijriDateData(hijriMonth = "Ramadan"),
    )

    private fun worker(attempt: Int): CoroutineWorker =
        TestListenableWorkerBuilder<ProbeWorker>(context)
            .setRunAttemptCount(attempt)
            .build()

    @Before
    fun stubGlanceHost() {
        mockkConstructor(GlanceAppWidgetManager::class)
        mockkStatic("androidx.glance.appwidget.state.GlanceAppWidgetStateKt")
        mockkStatic("androidx.glance.appwidget.GlanceAppWidgetKt")
        coEvery { any<GlanceAppWidget>().updateAll(any()) } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    private fun placed(count: Int) {
        val ids = List(count) { index -> mockk<GlanceId>(name = "glance-$index") }
        coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(HijriDateWidget::class.java) } returns ids
    }

    /**
     * A widget nobody has placed must not do the work. A periodic job that loads anyway burns the
     * battery of every user who removed the widget but still has the app installed.
     */
    @Test
    fun `a widget on no home screen succeeds without loading anything`() = runBlocking {
        placed(0)
        var loads = 0

        val result = worker(0).refreshWidget(
            context = context,
            widget = widget,
            definition = definition,
            widgetClass = HijriDateWidget::class.java,
            workerName = "probe",
            success = { loads++; loaded },
            error = { HijriDateWidgetState.Error(it) },
        )

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(loads).isEqualTo(0)
    }

    @Test
    fun `a successful load is published to every placed instance`() = runBlocking {
        placed(3)
        coEvery {
            updateAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any(), any())
        } returns loaded

        val result = worker(0).refreshWidget(
            context = context,
            widget = widget,
            definition = definition,
            widgetClass = HijriDateWidget::class.java,
            workerName = "probe",
            success = { loaded },
            error = { HijriDateWidgetState.Error(it) },
        )

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        coVerify(exactly = 3) {
            updateAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any(), any())
        }
        coVerify(exactly = 1) { widget.updateAll(context) }
    }

    /**
     * The regression this branch exists for: one transient throw used to overwrite a widget that
     * was showing correct prayer times with "tap to set up", and leave it that way.
     */
    @Test
    fun `a failed load leaves a widget that already has real data alone`() = runBlocking {
        placed(1)
        coEvery {
            getAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any())
        } returns loaded

        val result = worker(0).refreshWidget(
            context = context,
            widget = widget,
            definition = definition,
            widgetClass = HijriDateWidget::class.java,
            workerName = "probe",
            success = { error("datastore lost a race") },
            error = { HijriDateWidgetState.Error(it) },
            hasData = { it.hasData },
        )

        // Nothing was written, but the widget is still redrawn so its countdowns advance.
        coVerify(exactly = 0) {
            updateAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any(), any())
        }
        coVerify { widget.updateAll(context) }
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `a failed load replaces a widget that has nothing worth keeping`() = runBlocking {
        placed(1)
        coEvery {
            getAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any())
        } returns HijriDateWidgetState.Success(
            com.arshadshah.nimaz.widget.hijridate.HijriDateData(),
        )
        val published = slot<suspend (HijriDateWidgetState) -> HijriDateWidgetState>()
        coEvery {
            updateAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any(), capture(published))
        } returns loaded

        worker(0).refreshWidget(
            context = context,
            widget = widget,
            definition = definition,
            widgetClass = HijriDateWidget::class.java,
            workerName = "probe",
            success = { error("no hijri date") },
            error = { HijriDateWidgetState.Error(it) },
            hasData = { it.hasData },
        )

        val written = published.captured.invoke(HijriDateWidgetState.Loading)
        assertThat(written).isInstanceOf(HijriDateWidgetState.Error::class.java)
        assertThat((written as HijriDateWidgetState.Error).message).isEqualTo("no hijri date")
    }

    /**
     * Three attempts, then stop. A worker that retries for ever holds a scheduler slot for a
     * widget nobody is looking at.
     */
    @Test
    fun `retries are given up after the third attempt`() = runBlocking {
        placed(1)
        coEvery {
            getAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any())
        } returns loaded

        fun run(attempt: Int) = runBlocking {
            worker(attempt).refreshWidget(
                context = context,
                widget = widget,
                definition = definition,
                widgetClass = HijriDateWidget::class.java,
                workerName = "probe",
                success = { error("still failing") },
                error = { HijriDateWidgetState.Error(it) },
                hasData = { it.hasData },
            )
        }

        assertThat(run(0)).isEqualTo(ListenableWorker.Result.retry())
        assertThat(run(2)).isEqualTo(ListenableWorker.Result.retry())
        assertThat(run(3)).isEqualTo(ListenableWorker.Result.failure())
    }

    /**
     * Listing the placed widgets talks to the AppWidget host, which can be down right after a
     * boot. That used to throw straight out of the worker — a failure with no retry, so the
     * widget waited a whole period for its next chance.
     */
    @Test
    fun `a host that cannot be reached retries instead of failing outright`() = runBlocking {
        coEvery { anyConstructed<GlanceAppWidgetManager>().getGlanceIds(HijriDateWidget::class.java) } throws
            IllegalStateException("host not up")

        assertThat(
            worker(0).refreshWidget(
                context = context,
                widget = widget,
                definition = definition,
                widgetClass = HijriDateWidget::class.java,
                workerName = "probe",
                success = { loaded },
                error = { HijriDateWidgetState.Error(it) },
            ),
        ).isEqualTo(ListenableWorker.Result.retry())
    }

    /** The failure handler failing is not worth failing the worker over. */
    @Test
    fun `a failure while publishing the error state is swallowed`() = runBlocking {
        placed(1)
        coEvery {
            getAppWidgetState(any(), any<GlanceStateDefinition<HijriDateWidgetState>>(), any())
        } throws IllegalStateException("state file gone")

        assertThat(
            worker(0).refreshWidget(
                context = context,
                widget = widget,
                definition = definition,
                widgetClass = HijriDateWidget::class.java,
                workerName = "probe",
                success = { error("load failed") },
                error = { HijriDateWidgetState.Error(it) },
                hasData = { it.hasData },
            ),
        ).isEqualTo(ListenableWorker.Result.retry())
    }
}

/**
 * A worker with no body of its own — `refreshWidget` is an extension on `CoroutineWorker`, and
 * `runAttemptCount` (which decides retry vs failure) can only be set through the real builder.
 * Public and top-level because WorkManager's test builder instantiates it reflectively.
 */
class ProbeWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = Result.success()
}
