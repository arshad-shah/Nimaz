package com.arshadshah.nimaz.widget.hijridate

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.updateWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@HiltWorker
class HijriDateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
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

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: HijriDateWidgetState
    ) = updateWidgetState(context, HijriDateWidget(), HijriDateStateDefinition, glanceIds, newState)

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(HijriDateWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val hijriDate = HijriDateCalculator.today()
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val gregorianDate = "${today.dayOfMonth} ${
                today.month.getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                )
            }"

            val data = HijriDateData(
                hijriDay = hijriDate.day,
                hijriMonth = hijriDate.monthName,
                hijriYear = hijriDate.year,
                gregorianDayOfWeek = dayOfWeek,
                gregorianDate = gregorianDate
            )

            setWidgetState(glanceIds, HijriDateWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            CrashReporter.log("HijriDateWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, HijriDateWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
