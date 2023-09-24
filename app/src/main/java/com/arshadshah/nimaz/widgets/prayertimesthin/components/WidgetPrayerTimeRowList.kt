package com.arshadshah.nimaz.widgets.prayertimesthin.components

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import java.time.format.DateTimeFormatter

@Composable
fun WidgetPrayerTimeRowList(data : PrayerTimes)
{
	val newIshaTime = if (data.isha !!.hour >= 22)
	{
		data.maghrib?.plusMinutes(60)
	} else
	{
		data.isha
	}
	Row(
			 modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
				 .background(GlanceTheme.colors.background).clickable(
						  onClick = actionStartActivity<MainActivity>()
																	 )
	   ) {
		val modifier = GlanceModifier.defaultWeight()
		WidgetPrayerTimeColumn(
				 name = "Fajr" ,
				 time = data.fajr !!.format(DateTimeFormatter.ofPattern("hh:mm")) ,
				 modifier
							  )
		WidgetPrayerTimeColumn(
				 name = "Dhuhr" ,
				 time = data.dhuhr !!.format(DateTimeFormatter.ofPattern("hh:mm")) ,
				 modifier = modifier
							  )
		WidgetPrayerTimeColumn(
				 name = "Asr" ,
				 time = data.asr !!.format(DateTimeFormatter.ofPattern("hh:mm")) ,
				 modifier = modifier
							  )
		WidgetPrayerTimeColumn(
				 name = "Maghrib" ,
				 time = data.maghrib !!.format(DateTimeFormatter.ofPattern("hh:mm")) ,
				 modifier = modifier
							  )
		WidgetPrayerTimeColumn(
				 name = "Isha" ,
				 time = newIshaTime !!.format(DateTimeFormatter.ofPattern("hh:mm")) ,
				 modifier = modifier
							  )
	}
}