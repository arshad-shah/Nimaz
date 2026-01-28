package com.arshadshah.nimaz.widget.hijridate

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R

class HijriDateWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<HijriDateWidgetState> =
        HijriDateStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<HijriDateWidgetState>()
                HijriDateContent(state)
            }
        }
    }
}

@Composable
private fun HijriDateContent(state: HijriDateWidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is HijriDateWidgetState.Loading -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Loading...",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            }
        }

        is HijriDateWidgetState.Success -> {
            HijriDateSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is HijriDateWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to refresh",
                    style = TextStyle(color = textSecondary, fontSize = 12.sp)
                )
            }
        }
    }
}

@Composable
private fun HijriDateSuccessContent(
    data: HijriDateData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.gregorianDayOfWeek.ifEmpty { "—" },
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = data.hijriDay.toString(),
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = "${data.hijriMonth.ifEmpty { "—" }} ${data.hijriYear}",
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = data.gregorianDate.ifEmpty { "—" },
                style = TextStyle(color = textSecondary, fontSize = 11.sp)
            )
        }
    }
}

class HijriDateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        HijriDateWorker.enqueuePeriodicWork(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        HijriDateWorker.cancel(context)
    }
}
