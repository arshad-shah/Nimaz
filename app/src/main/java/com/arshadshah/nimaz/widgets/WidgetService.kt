package com.arshadshah.nimaz.widgets

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter
import java.util.*

class WidgetService : Service()
{

	private var timer: Timer? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.d("WidgetService", "Service started")
		// Start the timer to update the widget every 5 seconds
		timer = Timer().apply {
			scheduleAtFixedRate(object : TimerTask() {
				override fun run() {
					updateWidget()
				}
			}, 0, 1000 * 60 * 60 * 3)
		}
		return START_STICKY
	}

	override fun onDestroy() {
		super.onDestroy()
		// Stop the timer when the service is destroyed
		timer?.cancel()
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	private fun updateWidget() {
		val context = this
		val appWidgetManager = AppWidgetManager.getInstance(context)
		val widgetIds = appWidgetManager.getAppWidgetIds(
				ComponentName(this, Nimaz::class.java)
														)

		// Update the widget UI on the main thread
		Handler(Looper.getMainLooper()).post {
			Log.d("WidgetService" , "Updating widget")
			widgetIds.forEach { widgetId ->
				val views = RemoteViews(packageName , R.layout.nimaz)
				val intent = Intent(context , MainActivity::class.java)
				val pendingIntent = PendingIntent.getActivity(
						context ,
						AppConstants.WIDGET_PENDING_INTENT_REQUEST_CODE ,
						intent ,
						PendingIntent.FLAG_IMMUTABLE
															 )
				views.setOnClickPendingIntent(R.id.widget , pendingIntent)

				// Set the random number text
				runBlocking {
					val repository = PrayerTimesRepository.getPrayerTimesForWidget(context)
					views.setTextViewText(
							R.id.Fajr_time , repository.data?.fajr?.format(
							DateTimeFormatter.ofPattern("hh:mm a")))
					views.setTextViewText(
							R.id.Zuhar_time , repository.data?.dhuhr?.format(
							DateTimeFormatter.ofPattern("hh:mm a")))
					views.setTextViewText(
							R.id.Asar_time , repository.data?.asr?.format(
							DateTimeFormatter.ofPattern("hh:mm a")))
					views.setTextViewText(
							R.id.Maghrib_time ,
							repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
										 )
					views.setTextViewText(
							R.id.Ishaa_time , repository.data?.isha?.format(
							DateTimeFormatter.ofPattern("hh:mm a")))
				}
				// Update the widget
				appWidgetManager.updateAppWidget(widgetId, views)
				Log.d("WidgetService" , "Widget updated")
			}
		}
	}
}