package com.arshadshah.nimaz.widget.nextprayer

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.formatWidgetCountdown
import com.arshadshah.nimaz.widget.core.formatWidgetTime
import com.arshadshah.nimaz.widget.core.updateWidgetState
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

        // WorkManager minimum interval is 15 minutes; per-minute updates use AlarmManager.
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(15)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<NextPrayerWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<NextPrayerWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: NextPrayerWidgetState
    ) = updateWidgetState(
        context,
        NextPrayerWidget(),
        NextPrayerStateDefinition,
        glanceIds,
        newState
    )

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(NextPrayerWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val latitude = preferencesDataStore.latitude.first().takeIf { it != 0.0 } ?: 53.3498
            val longitude = preferencesDataStore.longitude.first().takeIf { it != 0.0 } ?: -6.2603
            val use24Hour = preferencesDataStore.use24HourFormat.first()

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
                val epochMillis = nextPrayer.time.toEpochMilliseconds()
                val countdown =
                    formatWidgetCountdown((nextPrayer.time - currentTime).inWholeSeconds)

                NextPrayerData(
                    prayerName = nextPrayer.type.displayName,
                    prayerTime = formatWidgetTime(
                        prayerLocalTime.hour,
                        prayerLocalTime.minute,
                        includeAmPm = true,
                        use24Hour = use24Hour
                    ),
                    countdown = countdown,
                    isValid = true,
                    nextPrayerEpochMillis = epochMillis
                )
            } else {
                // All prayers passed, show Fajr for tomorrow
                val tomorrowDate = java.time.LocalDate.now().plusDays(1)
                val tomorrowPrayers =
                    prayerTimeCalculator.getPrayerTimes(latitude, longitude, tomorrowDate)
                val tomorrowFajr = tomorrowPrayers.firstOrNull()

                if (tomorrowFajr != null) {
                    val epochMillis = tomorrowFajr.time.toEpochMilliseconds()
                    NextPrayerData(
                        prayerName = "Fajr",
                        prayerTime = "Tomorrow",
                        countdown = formatWidgetCountdown(
                            (tomorrowFajr.time - currentTime).inWholeSeconds
                        ),
                        isValid = true,
                        nextPrayerEpochMillis = epochMillis
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
            CrashReporter.log("NextPrayerWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, NextPrayerWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
