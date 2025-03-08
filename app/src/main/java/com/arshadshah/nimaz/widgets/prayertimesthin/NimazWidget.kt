package com.arshadshah.nimaz.widgets.prayertimesthin

import android.content.Context
import android.os.Build
import android.util.Log
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
import com.arshadshah.nimaz.widgets.NimazWidgetColorScheme
import com.arshadshah.nimaz.widgets.prayertimesthin.components.WidgetPrayerTimeRowList

class NimazWidget : GlanceAppWidget() {
    override val stateDefinition = PrayerTimesStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        provideContent {
            val prayerTimes = currentState<PrayerTimesWidget>()
            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    GlanceTheme.colors
                else
                    NimazWidgetColorScheme.colors
            ) {

                when (prayerTimes) {
                    is PrayerTimesWidget.Loading -> {
                        Column(

                            modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                                .background(GlanceTheme.colors.background).clickable(
                                    onClick = actionStartActivity<MainActivity>()
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading...",
                                modifier = GlanceModifier.padding(6.dp),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontSize = TextUnit(
                                        18F, TextUnitType.Sp
                                    )
                                )
                            )
                        }
                    }

                    is PrayerTimesWidget.Success -> {
                        Log.d("PrayerTimeWorker", "provideGlance: ${prayerTimes.data}")
                        WidgetPrayerTimeRowList(prayerTimes.data)
                    }

                    is PrayerTimesWidget.Error -> {
                        Column(

                            modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
                                .background(GlanceTheme.colors.background).clickable(
                                    onClick = actionStartActivity<MainActivity>()
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Log.d("PrayerTimeWorker", "provideGlance: ${prayerTimes.message}")
                            Text(
                                text = "Data is not available",
                                modifier = GlanceModifier.padding(6.dp),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onBackground,
                                    fontSize = TextUnit(
                                        18F, TextUnitType.Sp
                                    )
                                )
                            )
                            Button(text = "Retry", onClick = {
                                PrayerTimeWorker.enqueue(
                                    context,
                                    true
                                )
                            })
                        }
                    }
                }
            }
        }
    }
}