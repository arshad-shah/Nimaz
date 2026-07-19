package com.arshadshah.nimaz.widget.hijridate

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel
import com.arshadshah.nimaz.widget.core.WidgetLoadingBox
import com.arshadshah.nimaz.widget.core.WidgetMessageBox

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
    val context = LocalContext.current
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is HijriDateWidgetState.Loading -> WidgetLoadingBox(
            background = backgroundColor,
            textSecondary = textSecondary,
            onClick = actionStartActivity<MainActivity>(),
        )

        is HijriDateWidgetState.Success -> {
            HijriDateSuccessContent(
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is HijriDateWidgetState.Error -> WidgetMessageBox(
            background = backgroundColor,
            onClick = actionStartActivity<MainActivity>(),
        ) {
            Text(
                text = context.getString(R.string.widget_tap_to_refresh),
                style = TextStyle(color = textSecondary, fontSize = 12.sp)
            )
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
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 14.dp,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.CenterVertically) {
                WidgetIcon(resId = R.drawable.ic_widget_crescent, tint = primaryColor, size = 13.dp)
                Spacer(modifier = GlanceModifier.width(5.dp))
                WidgetLabel(text = data.gregorianDayOfWeek.ifEmpty { "—" }, color = textSecondary)
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = data.hijriDay.toString(),
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold
                ),
            )
            Text(
                text = "${data.hijriMonth.ifEmpty { "—" }} ${data.hijriYear}",
                style = TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = data.gregorianDate.ifEmpty { "—" },
                style = TextStyle(color = textSecondary, fontSize = 11.sp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
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
