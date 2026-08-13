package com.arshadshah.nimaz.widget.nextprayer

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.refreshWidget
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

    override suspend fun doWork(): Result = refreshWidget(
        context = context,
        widget = NextPrayerWidget(),
        definition = NextPrayerStateDefinition,
        widgetClass = NextPrayerWidget::class.java,
        workerName = "NextPrayerWorker",
        success = { NextPrayerWidgetState.Success(dataSource.load()) },
        error = { message -> NextPrayerWidgetState.Error(message) },
    )
}
