package com.arshadshah.nimaz.widget.prayertimes

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

    override suspend fun doWork(): Result = refreshWidget(
        context = context,
        widget = PrayerTimesWidget(),
        definition = PrayerTimesStateDefinition,
        widgetClass = PrayerTimesWidget::class.java,
        workerName = "PrayerTimesWorker",
        success = { PrayerTimesWidgetState.Success(dataSource.load()) },
        error = { message -> PrayerTimesWidgetState.Error(message) },
    )
}
