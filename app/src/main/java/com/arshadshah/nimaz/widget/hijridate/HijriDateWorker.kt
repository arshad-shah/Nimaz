package com.arshadshah.nimaz.widget.hijridate

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
class HijriDateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSource: HijriDateWidgetDataSource
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "HijriDateWorker"
        private const val ONE_TIME_WORK_NAME = "HijriDateWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofHours(6)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<HijriDateWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<HijriDateWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    override suspend fun doWork(): Result = refreshWidget(
        context = context,
        widget = HijriDateWidget(),
        definition = HijriDateStateDefinition,
        widgetClass = HijriDateWidget::class.java,
        workerName = "HijriDateWorker",
        success = { HijriDateWidgetState.Success(dataSource.load()) },
        error = { message -> HijriDateWidgetState.Error(message) },
        hasData = { it.hasData },
    )
}
