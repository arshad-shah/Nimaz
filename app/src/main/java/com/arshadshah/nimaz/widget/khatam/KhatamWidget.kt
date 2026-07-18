package com.arshadshah.nimaz.widget.khatam

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetLabel
import com.arshadshah.nimaz.widget.core.WidgetLoadingBox
import com.arshadshah.nimaz.widget.core.WidgetMessageBox

class KhatamWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<KhatamWidgetState> =
        KhatamStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<KhatamWidgetState>()
                KhatamContent(context, state)
            }
        }
    }
}

@Composable
private fun KhatamContent(context: Context, state: KhatamWidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is KhatamWidgetState.Loading -> WidgetLoadingBox(
            background = backgroundColor,
            textSecondary = textSecondary,
            onClick = actionStartActivity<MainActivity>(),
        )

        is KhatamWidgetState.Success -> {
            if (state.data.hasActiveKhatam) {
                KhatamProgressContent(
                    context = context,
                    data = state.data,
                    backgroundColor = backgroundColor,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    primaryColor = primaryColor
                )
            } else {
                KhatamEmptyContent(
                    context = context,
                    backgroundColor = backgroundColor,
                    textSecondary = textSecondary,
                    primaryColor = primaryColor
                )
            }
        }

        is KhatamWidgetState.Error -> WidgetMessageBox(
            background = backgroundColor,
            onClick = actionStartActivity<MainActivity>(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.widget_error_loading),
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_tap_to_retry),
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun KhatamProgressContent(
    context: Context,
    data: KhatamWidgetData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.name,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${data.progressPercent}%",
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            // Glance cannot draw a Compose canvas, so the app's serpentine juz
            // trail degrades to a plain bar plus the juz number below it.
            LinearProgressIndicator(
                progress = data.progressPercent / 100f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = primaryColor,
                backgroundColor = ColorProvider(R.color.widget_primary_dim),
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetLabel(
                    text = context.getString(R.string.khatam_juz_position, data.currentJuz),
                    color = textColor,
                    fontSize = 12.sp,
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                WidgetLabel(
                    text = context.resources.getQuantityString(
                        R.plurals.khatam_ayahs_remaining,
                        data.remainingAyahs,
                        data.remainingAyahs
                    ),
                    color = textSecondary,
                )
            }
        }
    }
}

@Composable
private fun KhatamEmptyContent(
    context: Context,
    backgroundColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    // The whole card is the tap target — tapping opens the app so the reader can
    // start a khatam, which is the only action worth offering here.
    WidgetMessageBox(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getString(R.string.khatam_widget_no_active),
                style = TextStyle(color = textSecondary, fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.khatam_widget_start),
                style = TextStyle(
                    color = primaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
            )
        }
    }
}

class KhatamWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KhatamWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        KhatamWorker.enqueuePeriodicWork(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        KhatamWorker.cancel(context)
    }
}
