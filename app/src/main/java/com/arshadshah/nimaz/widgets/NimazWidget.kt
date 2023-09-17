package com.arshadshah.nimaz.widgets

import android.content.Context
import android.util.Log
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.text.Text
import com.arshadshah.nimaz.widgets.components.WidgetPrayerTimeRowList

class NimazWidget: GlanceAppWidget()
{

	override val stateDefinition = PrayerTimesStateDefinition

	override val sizeMode: SizeMode = SizeMode.Exact

	override suspend fun provideGlance(context : Context , id : GlanceId)
	{

		provideContent {
			val prayerTimes = currentState<PrayerTimesWidget>()
			GlanceTheme(colors = NimazWidgetColorScheme.colors) {

				when(prayerTimes)
				{
					is PrayerTimesWidget.Loading -> {
						Column {
							CircularProgressIndicator()
							Text(text = "Loading...")
						}
					}
					is PrayerTimesWidget.Success -> {
						Log.d("PrayerTimeWorker" , "provideGlance: ${prayerTimes.data}")
						WidgetPrayerTimeRowList(prayerTimes.data)
					}
					is PrayerTimesWidget.Error -> {
						Column {
							Log.d("PrayerTimeWorker" , "provideGlance: ${prayerTimes.message}")
							Text(text = "Data is not available")
							Button(text = "Retry", onClick = { PrayerTimeWorker.enqueue(context , true) })
						}
					}
				}
			}
		}
	}
}