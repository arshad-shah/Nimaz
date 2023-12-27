package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.data.local.models.PrayerTrackerWithTime
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerTimesTrackerWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    companion object {

        private val uniqueWorkName = PrayerTimesTrackerWorker::class.java.simpleName

        fun enqueue(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val requestBuilder = PeriodicWorkRequestBuilder<PrayerTimesTrackerWorker>(
                Duration.ofMinutes(30)
            )
            var workPolicy = ExistingPeriodicWorkPolicy.KEEP

            // Replace any enqueued work and expedite the request
            if (force) {
                workPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            }

            manager.enqueueUniquePeriodicWork(
                uniqueWorkName,
                workPolicy,
                requestBuilder.build()
            )

            Log.d("PrayerTimeTrackerWorker", "enqueue: PrayerTimeTrackerWorker enqueued")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)

            Log.d("PrayerTimeTrackerWorker", "cancel: PrayerTimeTrackerWorker cancelled")
        }
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: PrayerTimesTrackerWidget,
    ) {

        Log.d("PrayerTimeTrackerWorker", "glanceIds: $glanceIds")
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = PrayerTimesTrackerStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }

        Log.d(
            "PrayerTimeTrackerWorker",
            "setWidgetState: PrayerTimeTrackerWorker updated new state $newState"
        )
        NimazWidgetPrayerTracker().updateAll(context)
    }


    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NimazWidgetPrayerTracker::class.java)
        return try {
            // Update state to indicate loading
            setWidgetState(glanceIds, PrayerTimesTrackerWidget.Loading)

            val tracker = PrayerTrackerRepository.getTrackerForDate(
                LocalDate.now()
            )
            val prayerTimes = PrayerTimesRepository.getPrayerTimes(context).data
            Log.d("PrayerTimeTrackerWorker", "prayerTimes: $prayerTimes")

            if(
                prayerTimes?.fajr != null
                && prayerTimes.dhuhr != null
                && prayerTimes.asr != null
                && prayerTimes.maghrib!= null
                && prayerTimes.isha!= null) {
                val prayerTrackerWithTime = PrayerTrackerWithTime(
                    date = tracker.date,
                    fajr = tracker.fajr,
                    fajrTime = prayerTimes.fajr!!,
                    dhuhr = tracker.dhuhr,
                    dhuhrTime = prayerTimes.dhuhr!!,
                    asr = tracker.asr,
                    asrTime = prayerTimes.asr!!,
                    maghrib = tracker.maghrib,
                    maghribTime = prayerTimes.maghrib!!,
                    isha = tracker.isha,
                    ishaTime = prayerTimes.isha!!,
                )
                // Update state with new data
                setWidgetState(
                    glanceIds,
                    PrayerTimesTrackerWidget.Success(prayerTrackerWithTime)
                )

                Result.success()
            }else{
                Result.failure()
            }
        } catch (e: Exception) {
            setWidgetState(glanceIds, PrayerTimesTrackerWidget.Error(e.message.orEmpty()))
            if (runAttemptCount < 5) {
                // Exponential backoff strategy will avoid the request to repeat
                // too fast in case of failures.
                Log.w("PrayerTimeTrackerWorker", "doWork: Failure occurred Retrying...")
                Result.retry()
            } else {
                Log.e("PrayerTimeTrackerWorker", e.message.orEmpty())
                Result.failure()
            }
        }
    }
}