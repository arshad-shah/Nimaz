package com.arshadshah.nimaz.widget.prayertracker

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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.MainActivity
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class PrayerTrackerWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<PrayerTrackerWidgetState> =
        PrayerTrackerStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<PrayerTrackerWidgetState>()
                PrayerTrackerContent(context, state)
            }
        }
    }
}

@Composable
private fun PrayerTrackerContent(context: Context, state: PrayerTrackerWidgetState) {
    val backgroundColor = ColorProvider(R.color.widget_background)
    val textColor = ColorProvider(R.color.widget_text)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val primaryColor = ColorProvider(R.color.widget_primary)

    when (state) {
        is PrayerTrackerWidgetState.Loading -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "Loading...",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        is PrayerTrackerWidgetState.Success -> {
            PrayerTrackerSuccessContent(
                context = context,
                data = state.data,
                backgroundColor = backgroundColor,
                textColor = textColor,
                textSecondary = textSecondary,
                primaryColor = primaryColor
            )
        }

        is PrayerTrackerWidgetState.Error -> {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error loading data",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to retry",
                        style = TextStyle(
                            color = primaryColor,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTrackerSuccessContent(
    context: Context,
    data: PrayerTrackerData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    val checkedColor = ColorProvider(R.color.widget_checked)
    val uncheckedColor = ColorProvider(R.color.widget_unchecked)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Header row with title and count
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.dateLabel,
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${data.prayedCount}/${data.totalCount}",
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Prayer checkboxes row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val prayers = listOf(
                    Triple("Fajr", "F", data.fajr),
                    Triple("Dhuhr", "D", data.dhuhr),
                    Triple("Asr", "A", data.asr),
                    Triple("Maghrib", "M", data.maghrib),
                    Triple("Isha", "I", data.isha)
                )

                prayers.forEach { (name, shortName, isPrayed) ->
                    PrayerCheckbox(
                        prayerName = name,
                        shortName = shortName,
                        isPrayed = isPrayed,
                        context = context,
                        checkedColor = checkedColor,
                        uncheckedColor = uncheckedColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerCheckbox(
    prayerName: String,
    shortName: String,
    isPrayed: Boolean,
    context: Context,
    checkedColor: ColorProvider,
    uncheckedColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier.clickable {
            // Toggle prayer status
            togglePrayerStatus(context, prayerName.lowercase())
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Checkbox circle
        Box(
            modifier = GlanceModifier
                .size(28.dp)
                .cornerRadius(14.dp)
                .background(if (isPrayed) checkedColor else uncheckedColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPrayed) "\u2713" else "",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_background),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Prayer name
        Text(
            text = shortName,
            style = TextStyle(
                color = if (isPrayed) textColor else textSecondary,
                fontSize = 10.sp,
                fontWeight = if (isPrayed) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

private fun togglePrayerStatus(context: Context, prayerName: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
            val prayerDao = entryPoint.prayerDao()

            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay() * 86400000L

            // Get current record
            val currentRecord = prayerDao.getPrayerRecord(todayEpoch, prayerName)
            val currentStatus = currentRecord?.status ?: "not_prayed"
            val newStatus = if (currentStatus == "prayed") "not_prayed" else "prayed"
            val prayedAt = if (newStatus == "prayed") System.currentTimeMillis() else null

            if (currentRecord != null) {
                prayerDao.updatePrayerStatus(
                    date = todayEpoch,
                    prayerName = prayerName,
                    status = newStatus,
                    prayedAt = prayedAt,
                    isJamaah = false
                )
            } else {
                prayerDao.insertPrayerRecord(
                    com.arshadshah.nimaz.data.local.database.entity.PrayerRecordEntity(
                        date = todayEpoch,
                        prayerName = prayerName,
                        scheduledTime = System.currentTimeMillis(),
                        status = newStatus,
                        prayedAt = prayedAt,
                        isJamaah = false,
                        isQadaFor = null,
                        note = null
                    )
                )
            }

            // Trigger widget update
            PrayerTrackerWorker.enqueueImmediateWork(context)
        } catch (e: Exception) {
            android.util.Log.e("PrayerTrackerWidget", "Failed to toggle prayer status", e)
        }
    }
}

class PrayerTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTrackerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTrackerWorker.enqueuePeriodicWork(context, force = true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PrayerTrackerWorker.cancel(context)
    }
}
