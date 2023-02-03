package com.arshadshah.nimaz.widgets

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants
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

	val intent = Intent(context , MainActivity::class.java)
	val pendingIntent = PendingIntent.getActivity(context , 9 , intent , FLAG_IMMUTABLE)

	val sharedPreferences = PrivateSharedPreferences(context)
	//get the prayer times from the shared preferences
	val fajr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.FAJR , "00:00"))
	val sunrise = LocalDateTime.parse(sharedPreferences.getData(AppConstants.SUNRISE , "00:00"))
	val dhuhr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.DHUHR , "00:00"))
	val asr = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ASR , "00:00"))
	val maghrib = LocalDateTime.parse(sharedPreferences.getData(AppConstants.MAGHRIB , "00:00"))
	val isha = LocalDateTime.parse(sharedPreferences.getData(AppConstants.ISHA , "00:00"))
	// Construct the RemoteViews object
	val views = RemoteViews(context.packageName , R.layout.nimaz)


	val formatter = DateTimeFormatter.ofPattern("hh:mm a")

	//format the prayer times to show them in the widget as 00:00 using the formatTime function
	val fajrTime = fajr.format(formatter)
	val sunriseTime = sunrise.format(formatter)
	val dhuhrTime = dhuhr.format(formatter)
	val asrTime = asr.format(formatter)
	val maghribTime = maghrib.format(formatter)
	val ishaTime = isha.format(formatter)

	views.setTextViewText(R.id.Fajr_time , fajrTime.format(DateTimeFormatter.ofPattern("HH:mm")))
	views.setTextViewText(R.id.Zuhar_time , dhuhrTime.format(DateTimeFormatter.ofPattern("HH:mm")))
	views.setTextViewText(R.id.Asar_time , asrTime.format(DateTimeFormatter.ofPattern("HH:mm")))
	views.setTextViewText(
			R.id.Maghrib_time ,
			maghribTime.format(DateTimeFormatter.ofPattern("HH:mm"))
						 )
	views.setTextViewText(R.id.Ishaa_time , ishaTime.format(DateTimeFormatter.ofPattern("HH:mm")))

	views.setOnClickPendingIntent(R.id.widget , pendingIntent)

	// Instruct the widget manager to update the widget
	appWidgetManager.updateAppWidget(appWidgetId , views)
}