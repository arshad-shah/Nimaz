package com.arshadshah.nimaz.widget.hijridate

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
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
import com.arshadshah.nimaz.widget.core.WidgetError
import com.arshadshah.nimaz.widget.core.WidgetLoading
import com.arshadshah.nimaz.widget.core.WidgetPalette
import com.arshadshah.nimaz.widget.core.WidgetWorkReceiver
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetLabel

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
    val palette = WidgetPalette()

    when (state) {
        is HijriDateWidgetState.Loading -> WidgetLoading(palette)

        is HijriDateWidgetState.Success -> {
            HijriDateSuccessContent(
                data = state.data,
                backgroundColor = palette.background,
                textColor = palette.text,
                textSecondary = palette.textSecondary,
                primaryColor = palette.primary
            )
        }

        is HijriDateWidgetState.Error -> WidgetError(palette)
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

class HijriDateWidgetReceiver : WidgetWorkReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HijriDateWidget()

    override fun enqueueWork(context: Context, force: Boolean) =
        HijriDateWorker.enqueuePeriodicWork(context, force = force)

    override fun refreshNow(context: Context) = HijriDateWorker.enqueueImmediateWork(context)

    override fun cancelWork(context: Context) = HijriDateWorker.cancel(context)
}
