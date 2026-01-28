package com.arshadshah.nimaz.widget.nextprayer

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import kotlin.time.Clock

@HiltWorker
class NextPrayerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "NextPrayerWorker"
        private const val ONE_TIME_WORK_NAME = "NextPrayerWorkerOneTime"

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<NextPrayerWorker>(
                Duration.ofMinutes(1)
            ).build()

            val policy = if (force) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            manager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
        }

        fun enqueueImmediateWork(context: Context) {
            val manager = WorkManager.getInstance(context)
            val request = OneTimeWorkRequestBuilder<NextPrayerWorker>().build()
            manager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: NextPrayerWidgetState
    ) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = NextPrayerStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }
        NextPrayerWidget().updateAll(context)
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NextPrayerWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val latitude = preferencesDataStore.latitude.first().takeIf { it != 0.0 } ?: 53.3498
            val longitude = preferencesDataStore.longitude.first().takeIf { it != 0.0 } ?: -6.2603

            val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude)
            val currentTime = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val localTime = currentTime.toLocalDateTime(timeZone)

            val nextPrayer = prayerTimes.firstOrNull { prayerTime ->
                val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
                prayerLocalTime.time > localTime.time
            }

            val data = if (nextPrayer != null) {
                val prayerLocalTime = nextPrayer.time.toLocalDateTime(timeZone)
                val diff: kotlin.time.Duration = nextPrayer.time - currentTime
                val totalSeconds = diff.inWholeSeconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val countdown = when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    minutes > 0 -> "${minutes}m ${seconds}s"
                    else -> "${seconds}s"
                }

                NextPrayerData(
                    prayerName = nextPrayer.type.displayName,
                    prayerTime = formatTime(prayerLocalTime.hour, prayerLocalTime.minute),
                    countdown = countdown,
                    isValid = true
                )
            } else {
                // All prayers passed, show Fajr for tomorrow
                val tomorrowDate = java.time.LocalDate.now().plusDays(1)
                val tomorrowPrayers = prayerTimeCalculator.getPrayerTimes(latitude, longitude, tomorrowDate)
                val tomorrowFajr = tomorrowPrayers.firstOrNull()

                if (tomorrowFajr != null) {
                    val diff: kotlin.time.Duration = tomorrowFajr.time - currentTime
                    val totalSeconds = diff.inWholeSeconds
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60

                    NextPrayerData(
                        prayerName = "Fajr",
                        prayerTime = "Tomorrow",
                        countdown = "${hours}h ${minutes}m",
                        isValid = true
                    )
                } else {
                    NextPrayerData(
                        prayerName = "Fajr",
                        prayerTime = "Tomorrow",
                        countdown = "—",
                        isValid = true
                    )
                }
            }

            setWidgetState(glanceIds, NextPrayerWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            setWidgetState(glanceIds, NextPrayerWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        val amPm = if (hour >= 12) "PM" else "AM"
        return String.format("%d:%02d %s", h, minute, amPm)
    }
}
