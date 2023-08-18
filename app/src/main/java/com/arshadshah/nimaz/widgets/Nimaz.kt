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
import com.arshadshah.nimaz.activities.RoutingActivity
import com.arshadshah.nimaz.constants.AppConstants.WIDGET_PENDING_INTENT_REQUEST_CODE
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.api.ApiResponse
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
		Log.d("Nimaz: Widget" , "onUpdate")
		// Update each widget instance
		appWidgetIds.forEach { appWidgetId ->
			val views = RemoteViews(context.packageName , R.layout.nimaz)
			val intent = Intent(context , RoutingActivity::class.java)
			val pendingIntent = PendingIntent.getActivity(
					 context ,
					 WIDGET_PENDING_INTENT_REQUEST_CODE ,
					 intent ,
					 FLAG_IMMUTABLE
														 )

			Log.d("Nimaz: Widget" , "onUpdate: Setting prayer times")
			//set prayer times text on a separate thread
			runBlocking {
				val repository = PrayerTimesRepository.getPrayerTimes(context)
				views.setTextViewText(
						 R.id.Fajr_time ,
						 repository.data?.fajr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				views.setTextViewText(
						 R.id.Zuhar_time ,
						 repository.data?.dhuhr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				views.setTextViewText(
						 R.id.Asar_time ,
						 repository.data?.asr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				views.setTextViewText(
						 R.id.Maghrib_time ,
						 repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
									 )
				val ishaTime = repository.data?.isha?.toLocalTime()?.hour
				val newIshaTime = if (ishaTime !! >= 22)
				{
					repository.data.maghrib?.plusMinutes(60)
				} else
				{
					repository.data.isha
				}
				views.setTextViewText(
						 R.id.Ishaa_time , newIshaTime?.format(
						 DateTimeFormatter.ofPattern("hh:mm a")
															  )
									 )
			}
			Log.d("Nimaz: Widget" , "onUpdate: Setting click listener")
			views.setOnClickPendingIntent(R.id.widget , pendingIntent)
			// Update the widget
			appWidgetManager.updateAppWidget(appWidgetId , views)
			Log.d("Nimaz: Widget" , "onUpdate: Finished updating widget")
		}
	}

	override fun onEnabled(context : Context)
	{
		// Enter relevant functionality for when the first widget is created
		updateAppWidget(context , AppWidgetManager.getInstance(context) , 0)
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

	val intent = Intent(context , RoutingActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(
			 context ,
			 WIDGET_PENDING_INTENT_REQUEST_CODE ,
			 intent ,
			 FLAG_IMMUTABLE
												 )

	val views = RemoteViews(context.packageName , R.layout.nimaz)
	var repository : ApiResponse<PrayerTimes> = ApiResponse.Loading()

	Log.d("Nimaz: Widget" , "Starting Coroutine to get Prayer Times!")
	runBlocking {
		repository = PrayerTimesRepository.getPrayerTimes(context)
	}
	Log.d("Nimaz: Widget" , "Finished Coroutine to get Prayer Times!")

	Log.d("Nimaz: Widget" , "Setting Prayer Times!")
	views.setTextViewText(
			 R.id.Fajr_time ,
			 repository.data?.fajr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
						 )
	views.setTextViewText(
			 R.id.Zuhar_time ,
			 repository.data?.dhuhr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
						 )
	views.setTextViewText(
			 R.id.Asar_time ,
			 repository.data?.asr?.format(DateTimeFormatter.ofPattern("hh:mm a"))
						 )
	views.setTextViewText(
			 R.id.Maghrib_time ,
			 repository.data?.maghrib?.format(DateTimeFormatter.ofPattern("hh:mm a"))
						 )
	val ishaTime = repository.data?.isha?.toLocalTime()?.hour
	val newIshaTime = if (ishaTime !! >= 22)
	{
		repository.data?.maghrib?.plusMinutes(60)
	} else
	{
		repository.data?.isha
	}
	views.setTextViewText(
			 R.id.Ishaa_time , newIshaTime?.format(
			 DateTimeFormatter.ofPattern("hh:mm a")
												  )
						 )

	views.setOnClickPendingIntent(R.id.widget , pendingIntent)

	Log.d("Nimaz: Widget" , "Updating Widget!")

	// Instruct the widget manager to update the widget
	appWidgetManager.updateAppWidget(appWidgetId , views)

	Log.d("Nimaz: Widget" , "Finished Updating Widget!")
}