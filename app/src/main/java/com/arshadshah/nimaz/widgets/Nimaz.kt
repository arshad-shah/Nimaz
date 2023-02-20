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
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.utils.LocalDataStore
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
		// There may be multiple widgets active, so update all of them
		for (appWidgetId in appWidgetIds)
		{
			updateAppWidget(context , appWidgetManager , appWidgetId)
		}
	}

	override fun onEnabled(context : Context)
	{
		// Enter relevant functionality for when the first widget is created
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

	LocalDataStore.init(context)
	val dataStore = LocalDataStore.getDataStore()
	val prayertimes : PrayerTimes
	runBlocking {
		dataStore.getAllPrayerTimes().let {
			prayertimes = it
		}
	}
	// Construct the RemoteViews object
	val views = RemoteViews(context.packageName , R.layout.nimaz)


	val formatter = DateTimeFormatter.ofPattern("hh:mm a")

	//format the prayer times to show them in the widget as 00:00 using the formatTime function
	val fajrTime = prayertimes.fajr?.format(formatter)
	val dhuhrTime = prayertimes.dhuhr?.format(formatter)
	val asrTime = prayertimes.asr?.format(formatter)
	val maghribTime = prayertimes.maghrib?.format(formatter)
	val ishaTime = prayertimes.isha?.format(formatter)

	views.setTextViewText(R.id.Fajr_time , fajrTime)
	views.setTextViewText(R.id.Zuhar_time , dhuhrTime)
	views.setTextViewText(R.id.Asar_time , asrTime)
	views.setTextViewText(
			R.id.Maghrib_time ,
			maghribTime
						 )
	views.setTextViewText(R.id.Ishaa_time , ishaTime)

	views.setOnClickPendingIntent(R.id.widget , pendingIntent)

	// Instruct the widget manager to update the widget
	appWidgetManager.updateAppWidget(appWidgetId , views)
}