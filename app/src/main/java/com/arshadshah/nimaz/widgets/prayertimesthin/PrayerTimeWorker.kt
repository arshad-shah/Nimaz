package com.arshadshah.nimaz.widgets.prayertimesthin

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

@HiltWorker
class PrayerTimeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimesRepository: PrayerTimesRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private val uniqueWorkName = PrayerTimeWorker::class.java.simpleName

        fun enqueue(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val requestBuilder = PeriodicWorkRequestBuilder<PrayerTimeWorker>(
                Duration.ofMinutes(30)
            )
            val workPolicy = if (force) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            manager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                workPolicy,
                requestBuilder.build()
            )

            Log.d("PrayerTimeWorker", "enqueue: PrayerTimeWorker enqueued")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
            Log.d("PrayerTimeWorker", "cancel: PrayerTimeWorker cancelled")
        }
    }

    private suspend fun setWidgetState(glanceIds: List<GlanceId>, newState: PrayerTimesWidget) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = PrayerTimesStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }
        NimazWidget().updateAll(context)
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NimazWidget::class.java)

        return try {
            // Update state to indicate loading
            setWidgetState(glanceIds, PrayerTimesWidget.Loading)

            // Update state with new data
            val prayerTimes = prayerTimesRepository.getPrayerTimes(context).data
                ?: throw IllegalStateException("Prayer times data is null")

            setWidgetState(glanceIds, PrayerTimesWidget.Success(prayerTimes))
            Result.success()
        } catch (e: Exception) {
            setWidgetState(glanceIds, PrayerTimesWidget.Error(e.message.orEmpty()))
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}