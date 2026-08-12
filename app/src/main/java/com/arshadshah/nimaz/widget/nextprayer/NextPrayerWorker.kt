package com.arshadshah.nimaz.widget.nextprayer

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
class NextPrayerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSource: NextPrayerWidgetDataSource
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "NextPrayerWorker"
        private const val ONE_TIME_WORK_NAME = "NextPrayerWorkerOneTime"

        // WorkManager minimum interval is 15 minutes; per-minute updates use AlarmManager.
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(15)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<NextPrayerWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<NextPrayerWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: NextPrayerWidgetState
    ) = updateWidgetState(
        context,
        NextPrayerWidget(),
        NextPrayerStateDefinition,
        glanceIds,
        newState
    )

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NextPrayerWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            setWidgetState(glanceIds, NextPrayerWidgetState.Success(dataSource.load()))
            Result.success()
        } catch (e: Exception) {
            CrashReporter.log("NextPrayerWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, NextPrayerWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
