package com.arshadshah.nimaz.widgets

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.WIDGET_PENDING_INTENT_REQUEST_CODE
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter

/**
 * Implementation of App Widget functionality.
 */
class Nimaz : AppWidgetProvider()
{

	override fun onUpdate(
		context : Context ,
		appWidgetManager : AppWidgetManager ,
		appWidgetIds : IntArray ,
						 )
	{
		// Update each widget instance
		appWidgetIds.forEach { appWidgetId ->
			val views = RemoteViews(context.packageName , R.layout.nimaz)
			val intent = Intent(context , MainActivity::class.java)
			val pendingIntent = PendingIntent.getActivity(
					context ,
					WIDGET_PENDING_INTENT_REQUEST_CODE ,
					intent ,
					FLAG_IMMUTABLE
														 )

			// Set the random number text
			runBlocking {
				val repository = PrayerTimesRepository.getPrayerTimesForWidget(context)
				views.setTextViewText(R.id.Fajr_time , repository.data?.fajr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
				views.setTextViewText(R.id.Zuhar_time , repository.data?.dhuhr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
				views.setTextViewText(R.id.Asar_time , repository.data?.asr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
				views.setTextViewText(
						R.id.Maghrib_time ,
						repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				views.setTextViewText(R.id.Ishaa_time , repository.data?.isha?.format(DateTimeFormatter.ofPattern("hh:mm a")))
			}
			views.setOnClickPendingIntent(R.id.widget , pendingIntent)
			// Update the widget
			appWidgetManager.updateAppWidget(appWidgetId, views)
		}
	}

	override fun onEnabled(context : Context)
	{
		// Enter relevant functionality for when the first widget is created
		updateAppWidget(context, AppWidgetManager.getInstance(context), 0)
	}

	override fun onDisabled(context : Context)
	{
		// Enter relevant functionality for when the last widget is disabled
	}
}

internal fun updateAppWidget(
	context : Context ,
	appWidgetManager : AppWidgetManager ,
	appWidgetId : Int ,
							)
{

	Log.d("Nimaz: Widget" , "Updating Widget!")

	val intent = Intent(context , MainActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(
			context ,
			WIDGET_PENDING_INTENT_REQUEST_CODE ,
			intent ,
			FLAG_IMMUTABLE
												 )

	val views = RemoteViews(context.packageName , R.layout.nimaz)
	runBlocking {
		val repository = PrayerTimesRepository.getPrayerTimesForWidget(context)
		views.setTextViewText(R.id.Fajr_time , repository.data?.fajr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
		views.setTextViewText(R.id.Zuhar_time , repository.data?.dhuhr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
		views.setTextViewText(R.id.Asar_time , repository.data?.asr?.format(DateTimeFormatter.ofPattern("hh:mm a")))
		views.setTextViewText(
				R.id.Maghrib_time ,
				repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
							 )
		views.setTextViewText(R.id.Ishaa_time , repository.data?.isha?.format(DateTimeFormatter.ofPattern("hh:mm a")))
	}

	views.setOnClickPendingIntent(R.id.widget , pendingIntent)

	// Instruct the widget manager to update the widget
	appWidgetManager.updateAppWidget(appWidgetId , views)
}