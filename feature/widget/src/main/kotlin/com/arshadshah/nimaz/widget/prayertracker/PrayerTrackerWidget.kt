package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.widget.core.launchAppComponent

import com.arshadshah.nimaz.feature.widget.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerName

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.LocalContext
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arshadshah.nimaz.widget.core.WidgetError
import com.arshadshah.nimaz.widget.core.WidgetLoading
import com.arshadshah.nimaz.widget.core.WidgetPalette
import com.arshadshah.nimaz.widget.core.WidgetWorkReceiver
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.common.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.widget.WidgetEntryPoint
import com.arshadshah.nimaz.widget.core.WidgetCard
import com.arshadshah.nimaz.widget.core.WidgetIcon
import com.arshadshah.nimaz.widget.core.WidgetMessageBox
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val palette = WidgetPalette()

    when (state) {
        is PrayerTrackerWidgetState.Loading -> WidgetLoading(palette)

        is PrayerTrackerWidgetState.Success -> {
            PrayerTrackerSuccessContent(
                context = context,
                data = state.data,
                backgroundColor = palette.background,
                textColor = palette.text,
                textSecondary = palette.textSecondary,
                primaryColor = palette.primary
            )
        }

        is PrayerTrackerWidgetState.Error -> WidgetMessageBox(
            background = palette.background,
            onClick = actionStartActivity(LocalContext.current.launchAppComponent()),
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

@Composable
private fun PrayerTrackerSuccessContent(
    context: Context,
    data: PrayerTrackerData,
    backgroundColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    primaryColor: ColorProvider
) {
    WidgetCard(
        background = backgroundColor,
        onClick = actionStartActivity(LocalContext.current.launchAppComponent()),
        padding = 12.dp,
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
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
                    ),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${data.prayedCount} / ${data.totalCount}",
                    style = TextStyle(
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                // Two enums, deliberately both. `PrayerType` carries the label the tile shows;
                // `PrayerName` is what a prayer record is keyed on. They were bridged by
                // `type.displayName.lowercase()` until PR 13 of #551 — a string round-trip that
                // happened to work because the names matched, and would have broken silently the
                // first time a display name was reworded.
                val prayers = listOf(
                    Triple(PrayerType.FAJR, PrayerName.FAJR, data.fajr),
                    Triple(PrayerType.DHUHR, PrayerName.DHUHR, data.dhuhr),
                    Triple(PrayerType.ASR, PrayerName.ASR, data.asr),
                    Triple(PrayerType.MAGHRIB, PrayerName.MAGHRIB, data.maghrib),
                    Triple(PrayerType.ISHA, PrayerName.ISHA, data.isha),
                )
                prayers.forEach { (type, prayer, isPrayed) ->
                    PrayerCheckbox(
                        label = type.displayName,
                        prayer = prayer,
                        isPrayed = isPrayed,
                        context = context,
                        backgroundColor = backgroundColor,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerCheckbox(
    label: String,
    prayer: PrayerName,
    isPrayed: Boolean,
    context: Context,
    backgroundColor: ColorProvider,
    primaryColor: ColorProvider,
    textColor: ColorProvider,
    textSecondary: ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    val uncheckedColor = ColorProvider(R.color.widget_unchecked)
    val onPrimary = ColorProvider(R.color.widget_on_primary)
    Column(
        modifier = modifier.clickable { togglePrayerStatus(context, prayer) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isPrayed) {
            // Filled teal disc + tinted check vector.
            Box(
                modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp).background(primaryColor),
                contentAlignment = Alignment.Center,
            ) {
                WidgetIcon(resId = R.drawable.ic_widget_check, tint = onPrimary, size = 16.dp)
            }
        } else {
            // Outline ring built from two discs (Glance has no stroke modifier).
            Box(
                modifier = GlanceModifier.size(28.dp).cornerRadius(14.dp)
                    .background(uncheckedColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = GlanceModifier.size(24.dp).cornerRadius(12.dp)
                        .background(backgroundColor)
                ) {}
            }
        }
        Spacer(modifier = GlanceModifier.height(5.dp))
        Text(
            text = label,
            style = TextStyle(
                color = if (isPrayed) textColor else textSecondary,
                fontSize = 9.sp,
                fontWeight = if (isPrayed) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
        )
    }
}

/**
 * Flips today's status for [prayerName] between prayed and not-prayed, then refreshes the tile.
 *
 * Went through `PrayerDao` until `widget/` became `:feature:widget` in PR 13 of #551 — reading a
 * row, comparing `"prayed"` against a `String` status, and constructing a `PrayerRecordEntity` to
 * insert. A widget writing database entities is the coupling the epic exists to remove, and
 * nothing objected while both lived in `:app`; the module boundary turned it into an unresolved
 * reference.
 *
 * `PrayerRepository` already declared every operation this needs, typed. The rewrite is a
 * narrowing: [PrayerName] and [PrayerStatus] replace the string literals, so a typo stops
 * compiling, and `"not_prayed"` — which the enum spells [PrayerStatus.NOT_PRAYED] — can no longer
 * drift from whatever the DAO happened to store.
 */
private fun togglePrayerStatus(context: Context, prayerName: PrayerName) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val prayerRepository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            ).prayerRepository()

            val todayEpoch = LocalDate.now().toUtcMidnightMillis()
            val current = prayerRepository.getPrayerRecord(todayEpoch, prayerName)
            val newStatus =
                if (current?.status == PrayerStatus.PRAYED) PrayerStatus.NOT_PRAYED
                else PrayerStatus.PRAYED
            val prayedAt =
                if (newStatus == PrayerStatus.PRAYED) System.currentTimeMillis() else null

            if (current != null) {
                prayerRepository.updatePrayerStatus(
                    date = todayEpoch,
                    prayerName = prayerName,
                    status = newStatus,
                    prayedAt = prayedAt,
                    isJamaah = false,
                )
            } else {
                prayerRepository.insertPrayerRecord(
                    PrayerRecord(
                        id = 0L,
                        date = todayEpoch,
                        prayerName = prayerName,
                        status = newStatus,
                        prayedAt = prayedAt,
                        scheduledTime = System.currentTimeMillis(),
                        isJamaah = false,
                        isQadaFor = null,
                        note = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }

            PrayerTrackerWorker.enqueueImmediateWork(context)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            android.util.Log.e("PrayerTrackerWidget", "Failed to toggle prayer status", e)
        }
    }
}

class PrayerTrackerWidgetReceiver : WidgetWorkReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTrackerWidget()

    override fun enqueueWork(context: Context, force: Boolean) =
        PrayerTrackerWorker.enqueuePeriodicWork(context, force = force)

    override fun refreshNow(context: Context) = PrayerTrackerWorker.enqueueImmediateWork(context)

    override fun cancelWork(context: Context) = PrayerTrackerWorker.cancel(context)
}
