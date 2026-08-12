package com.arshadshah.nimaz.widget.prayertracker

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
class PrayerTrackerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSource: PrayerTrackerWidgetDataSource
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "PrayerTrackerWorker"
        private const val ONE_TIME_WORK_NAME = "PrayerTrackerWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(30)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<PrayerTrackerWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<PrayerTrackerWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    override suspend fun doWork(): Result = refreshWidget(
        context = context,
        widget = PrayerTrackerWidget(),
        definition = PrayerTrackerStateDefinition,
        widgetClass = PrayerTrackerWidget::class.java,
        workerName = "PrayerTrackerWorker",
        success = { PrayerTrackerWidgetState.Success(dataSource.load()) },
        error = { message -> PrayerTrackerWidgetState.Error(message) },
    )
}
