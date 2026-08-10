package com.arshadshah.nimaz.widget.prayertimes

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.updateWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

@HiltWorker
class PrayerTimesWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSource: PrayerTimesWidgetDataSource
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "PrayerTimesWorker"
        private const val ONE_TIME_WORK_NAME = "PrayerTimesWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(15)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<PrayerTimesWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<PrayerTimesWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: PrayerTimesWidgetState
    ) = updateWidgetState(
        context,
        PrayerTimesWidget(),
        PrayerTimesStateDefinition,
        glanceIds,
        newState
    )

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(PrayerTimesWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            setWidgetState(glanceIds, PrayerTimesWidgetState.Success(dataSource.load()))
            Result.success()
        } catch (e: Exception) {
            CrashReporter.log("PrayerTimesWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, PrayerTimesWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
