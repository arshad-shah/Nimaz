package com.arshadshah.nimaz.widgets

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
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import java.time.Duration

class PrayerTimeWorker(private val context: Context , workerParams: WorkerParameters) : CoroutineWorker(context , workerParams)
{
	companion object{

		private val uniqueWorkName = PrayerTimeWorker::class.java.simpleName

		fun enqueue(context: Context, force: Boolean = false)
		{
			val manager = WorkManager.getInstance(context)
			val requestBuilder = PeriodicWorkRequestBuilder<PrayerTimeWorker>(
					 Duration.ofMinutes(30)
																		  )
			var workPolicy = ExistingPeriodicWorkPolicy.KEEP

			// Replace any enqueued work and expedite the request
			if (force) {
				workPolicy = ExistingPeriodicWorkPolicy.UPDATE
			}

			manager.enqueueUniquePeriodicWork(
					 uniqueWorkName,
					 workPolicy,
					 requestBuilder.build()
											 )

			Log.d("PrayerTimeWorker" , "enqueue: PrayerTimeWorker enqueued")
		}

		fun cancel(context: Context)
		{
			WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)

			Log.d("PrayerTimeWorker" , "cancel: PrayerTimeWorker cancelled")
		}
	}

	private suspend fun setWidgetState(glanceIds: List<GlanceId> , newState: PrayerTimesWidget) {
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


	override suspend fun doWork(): Result
	{
		val manager = GlanceAppWidgetManager(context)
		val glanceIds = manager.getGlanceIds(NimazWidget::class.java)
		return try {
			// Update state to indicate loading
			setWidgetState(glanceIds, PrayerTimesWidget.Loading)
			// Update state with new data
			setWidgetState(glanceIds, PrayerTimesWidget.Success(PrayerTimesRepository.getPrayerTimes(context).data!!))

			Result.success()
		} catch (e: Exception) {
			setWidgetState(glanceIds, PrayerTimesWidget.Error(e.message.orEmpty()))
			if (runAttemptCount < 20) {
				// Exponential backoff strategy will avoid the request to repeat
				// too fast in case of failures.
				Result.retry()
			} else {
				Result.failure()
			}
		}
	}
}