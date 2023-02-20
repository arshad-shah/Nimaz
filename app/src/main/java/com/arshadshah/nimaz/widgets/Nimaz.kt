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
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.WIDGET_PENDING_INTENT_REQUEST_CODE
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import java.time.LocalDateTime
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

	val views = RemoteViews(context.packageName , R.layout.nimaz)


	val sharedPreferences = PrivateSharedPreferences(context)
	//get the prayer times from the shared preferences
	val fajr = LocalDateTime.parse(
			sharedPreferences.getData(
					AppConstants.FAJR ,
					LocalDateTime.now().toString()
									 )
								  )
	val dhuhr = LocalDateTime.parse(
			sharedPreferences.getData(
					AppConstants.DHUHR ,
					LocalDateTime.now().toString()
									 )
								   )
	val asr = LocalDateTime.parse(
			sharedPreferences.getData(
					AppConstants.ASR ,
					LocalDateTime.now().toString()
									 )
								 )
	val maghrib = LocalDateTime.parse(
			sharedPreferences.getData(
					AppConstants.MAGHRIB ,
					LocalDateTime.now().toString()
									 )
									 )
	val isha = LocalDateTime.parse(
			sharedPreferences.getData(
					AppConstants.ISHA ,
					LocalDateTime.now().toString()
									 )
								  )


	val formatter = DateTimeFormatter.ofPattern("hh:mm a")

	//format the prayer times to show them in the widget as 00:00 using the formatTime function
	val fajrTime = fajr.format(formatter)
	val dhuhrTime = dhuhr.format(formatter)
	val asrTime = asr.format(formatter)
	val maghribTime = maghrib.format(formatter)
	val ishaTime = isha.format(formatter)

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