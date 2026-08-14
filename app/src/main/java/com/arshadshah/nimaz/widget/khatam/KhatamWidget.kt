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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
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
    val palette = WidgetPalette()
    val gold = ColorProvider(R.color.widget_gold)
    val goldContainer = ColorProvider(R.color.widget_gold_container)
    val onGoldContainer = ColorProvider(R.color.widget_on_gold_container)

    when (state) {
        is KhatamWidgetState.Loading -> WidgetLoading(palette)

        is KhatamWidgetState.Success -> {
            if (state.data.hasActiveKhatam) {
                KhatamProgressContent(
                    context = context,
                    data = state.data,
                    backgroundColor = palette.background,
                    textColor = palette.text,
                    textSecondary = palette.textSecondary,
                    primaryColor = palette.primary,
                    gold = gold,
                    goldContainer = goldContainer,
                    onGoldContainer = onGoldContainer,
                )
            } else {
                KhatamEmptyContent(
                    context = context,
                    backgroundColor = palette.background,
                    textSecondary = palette.textSecondary,
                    primaryColor = palette.primary
                )
            }
        }

        is KhatamWidgetState.Error -> WidgetMessageBox(
            background = palette.background,
            onClick = actionStartActivity<MainActivity>(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.widget_error_loading),
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = context.getString(R.string.widget_tap_to_retry),
                    style = TextStyle(
                        color = palette.primary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * The active-khatam layout — an editorial stat card: an eyebrow with the khatam's
 * name, a gold juz medallion paired with the ayahs-remaining and pace line, and a
 * thin progress rule closing the card.
 *
 * Glance cannot draw a Compose canvas, so the app's serpentine juz trail is not
 * reproduced here; the medallion carries the "where am I" glance instead, and the
 * numbers (juz, remaining, pace, streak) fill what used to be an empty card.
 */
@Composable
private fun KhatamProgressContent(
    context: Context,
    data: KhatamWidgetData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider,
    gold: ColorProvider,
    goldContainer: ColorProvider,
    onGoldContainer: ColorProvider,
) {
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity<MainActivity>(),
        padding = 16.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Eyebrow — the khatam's name.
            Text(
                text = data.name,
                style = TextStyle(
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(12.dp))

            // Hero row: the gold juz medallion beside the supporting stats.
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(60.dp)
                        .background(goldContainer)
                        .cornerRadius(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = data.currentJuz.toString(),
                            style = TextStyle(
                                color = onGoldContainer,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = context.getString(R.string.khatam_widget_juz_caption),
                            style = TextStyle(
                                color = onGoldContainer,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.width(14.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = context.resources.getQuantityString(
                            R.plurals.khatam_ayahs_remaining,
                            data.remainingAyahs,
                            data.remainingAyahs,
                        ),
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = paceLine(context, data),
                        style = TextStyle(color = textSecondary, fontSize = 12.sp),
                        maxLines = 1,
                    )
                }
            }

            // Push the rule to the bottom edge so the card fills its height.
            Spacer(modifier = GlanceModifier.defaultWeight())

            // Progress rule with the percentage in gold.
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = data.progressPercent / 100f,
                    modifier = GlanceModifier.defaultWeight().height(6.dp),
                    color = primaryColor,
                    backgroundColor = ColorProvider(R.color.widget_primary_dim),
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Text(
                    text = "${data.progressPercent}%",
                    style = TextStyle(
                        color = gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/**
 * Builds the "20/day · 12-day streak" line, dropping either part when it is zero
 * (a khatam with no daily target, or a reader with no current streak). Falls back
 * to the plain juz position when there is nothing to say about pace yet.
 */
private fun paceLine(context: Context, data: KhatamWidgetData): String {
    val parts = buildList {
        if (data.dailyTarget > 0) {
            add(context.getString(R.string.khatam_widget_daily_target, data.dailyTarget))
        }
        if (data.currentStreak > 0) {
            add(
                context.resources.getQuantityString(
                    R.plurals.khatam_widget_streak,
                    data.currentStreak,
                    data.currentStreak,
                )
            )
        }
    }
    return if (parts.isEmpty()) {
        context.getString(R.string.khatam_juz_position, data.currentJuz)
    } else {
        parts.joinToString(" · ")
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

class KhatamWidgetReceiver : WidgetWorkReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KhatamWidget()

    override fun enqueueWork(context: Context, force: Boolean) =
        KhatamWorker.enqueuePeriodicWork(context, force = force)

    override fun refreshNow(context: Context) = KhatamWorker.enqueueImmediateWork(context)

    override fun cancelWork(context: Context) = KhatamWorker.cancel(context)
}
