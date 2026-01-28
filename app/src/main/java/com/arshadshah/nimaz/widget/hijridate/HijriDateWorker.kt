package com.arshadshah.nimaz.widget.hijridate

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.util.HijriDateCalculator
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

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<HijriDateWorker>(
                Duration.ofHours(6)
            ).build()

            val policy = if (force) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            manager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
        }

        fun enqueueImmediateWork(context: Context) {
            val manager = WorkManager.getInstance(context)
            val request = OneTimeWorkRequestBuilder<HijriDateWorker>().build()
            manager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: HijriDateWidgetState
    ) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = HijriDateStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }
        HijriDateWidget().updateAll(context)
    }

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
            val gregorianDate = "${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"

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
            setWidgetState(glanceIds, HijriDateWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
