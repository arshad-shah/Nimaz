package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.widgets.NimazWidgetColorScheme
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.components.PrayerTimesTrackerRowItems

class NimazWidgetPrayerTracker : GlanceAppWidget()
{

	override val stateDefinition = PrayerTimesTrackerStateDefinition

	override val sizeMode : SizeMode = SizeMode.Exact

	override suspend fun provideGlance(context : Context , id : GlanceId)
	{
		if (! LocalDataStore.isInitialized())
		{
			LocalDataStore.init(context)
		}

		provideContent {
			val prayerTimesTracker = currentState<PrayerTimesTrackerWidget>()
			val isFajrChecked = remember { mutableStateOf(false) }
			val isDhuhrChecked = remember { mutableStateOf(false) }
			val isAsrChecked = remember { mutableStateOf(false) }
			val isMaghribChecked = remember { mutableStateOf(false) }
			val isIshaChecked = remember { mutableStateOf(false) }

			val progress = remember { mutableFloatStateOf(0F) }
			GlanceTheme(
					 colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
						 GlanceTheme.colors
					 else
						 NimazWidgetColorScheme.colors
					   ) {

				when (prayerTimesTracker)
				{
					is PrayerTimesTrackerWidget.Loading ->
					{
						Column(

								 modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
									 .background(GlanceTheme.colors.background).clickable(
											  onClick = actionStartActivity<MainActivity>()
																						 ) ,
								 verticalAlignment = Alignment.CenterVertically ,
								 horizontalAlignment = Alignment.CenterHorizontally
							  ) {
							CircularProgressIndicator()
							Text(
									 text = "Loading..." ,
									 modifier = GlanceModifier.padding(6.dp) ,
									 style = TextStyle(
											  color = GlanceTheme.colors.onBackground ,
											  fontSize = TextUnit(
													   18F , TextUnitType.Sp
																 )
													  )
								)
						}
					}

					is PrayerTimesTrackerWidget.Success ->
					{
						Log.d(
								 "PrayerTimeTrackerWorker" ,
								 "provideGlance: ${prayerTimesTracker.data}"
							 )
						isFajrChecked.value = prayerTimesTracker.data.fajr
						isDhuhrChecked.value = prayerTimesTracker.data.dhuhr
						isAsrChecked.value = prayerTimesTracker.data.asr
						isMaghribChecked.value = prayerTimesTracker.data.maghrib
						isIshaChecked.value = prayerTimesTracker.data.isha

						progress.value = prayerTimesTracker.data.progress.toFloat()

						PrayerTimesTrackerRowItems(
								 fajr = isFajrChecked ,
								 dhuhr = isDhuhrChecked ,
								 asr = isAsrChecked ,
								 maghrib = isMaghribChecked ,
								 isha = isIshaChecked ,
								 progress = progress
												  )
					}

					is PrayerTimesTrackerWidget.Error ->
					{
						Column(
								 modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
									 .background(GlanceTheme.colors.background).clickable(
											  onClick = actionStartActivity<MainActivity>()
																						 ) ,
								 verticalAlignment = Alignment.CenterVertically ,
								 horizontalAlignment = Alignment.CenterHorizontally
							  ) {
							Log.d(
									 "PrayerTimeTrackerWorker" ,
									 "provideGlance: ${prayerTimesTracker.message}"
								 )
							Text(
									 text = "Data is not available" ,
									 modifier = GlanceModifier.padding(6.dp) ,
									 style = TextStyle(
											  color = GlanceTheme.colors.onBackground ,
											  fontSize = TextUnit(
													   18F , TextUnitType.Sp
																 )
													  )
								)
							Button(
									 text = "Retry" ,
									 onClick = { PrayerTimesTrackerWorker.enqueue(context , true) })
						}
					}
				}
			}
		}
	}
}