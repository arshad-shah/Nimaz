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
import com.arshadshah.nimaz.data.remote.repositories.PrayerTrackerRepository
import java.time.Duration
import java.time.LocalDate

class PrayerTimesTrackerWorker(private val context : Context , workerParams : WorkerParameters) :
	CoroutineWorker(context , workerParams)
{

	companion object
	{

		private val uniqueWorkName = PrayerTimesTrackerWorker::class.java.simpleName

		fun enqueue(context : Context , force : Boolean = false)
		{
			val manager = WorkManager.getInstance(context)
			val requestBuilder = PeriodicWorkRequestBuilder<PrayerTimesTrackerWorker>(
					 Duration.ofMinutes(30)
																					 )
			var workPolicy = ExistingPeriodicWorkPolicy.KEEP

			// Replace any enqueued work and expedite the request
			if (force)
			{
				workPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
			}

			manager.enqueueUniquePeriodicWork(
					 uniqueWorkName ,
					 workPolicy ,
					 requestBuilder.build()
											 )

			Log.d("PrayerTimeTrackerWorker" , "enqueue: PrayerTimeTrackerWorker enqueued")
		}

		fun cancel(context : Context)
		{
			WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)

			Log.d("PrayerTimeTrackerWorker" , "cancel: PrayerTimeTrackerWorker cancelled")
		}
	}

	private suspend fun setWidgetState(
		glanceIds : List<GlanceId> ,
		newState : PrayerTimesTrackerWidget ,
									  )
	{
		glanceIds.forEach { glanceId ->
			updateAppWidgetState(
					 context = context ,
					 definition = PrayerTimesTrackerStateDefinition ,
					 glanceId = glanceId ,
					 updateState = { newState }
								)
		}

		Log.d(
				 "PrayerTimeTrackerWorker" ,
				 "setWidgetState: PrayerTimeTrackerWorker updated new state $newState"
			 )
		NimazWidgetPrayerTracker().updateAll(context)
	}


	override suspend fun doWork() : Result
	{
		val manager = GlanceAppWidgetManager(context)
		val glanceIds = manager.getGlanceIds(NimazWidgetPrayerTracker::class.java)
		return try
		{
			// Update state to indicate loading
			setWidgetState(glanceIds , PrayerTimesTrackerWidget.Loading)

			val tracker = PrayerTrackerRepository.getTrackerForDate(
					 LocalDate.now().toString()
																   )
			// Update state with new data
			setWidgetState(
					 glanceIds ,
					 PrayerTimesTrackerWidget.Success(tracker)
						  )

			Result.success()
		} catch (e : Exception)
		{
			setWidgetState(glanceIds , PrayerTimesTrackerWidget.Error(e.message.orEmpty()))
			if (runAttemptCount < 20)
			{
				// Exponential backoff strategy will avoid the request to repeat
				// too fast in case of failures.
				Result.retry()
			} else
			{
				Result.failure()
			}
		}
	}
}