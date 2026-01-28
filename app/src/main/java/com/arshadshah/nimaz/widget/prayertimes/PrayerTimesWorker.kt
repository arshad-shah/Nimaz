package com.arshadshah.nimaz.widget.prayertimes

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
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Duration
import kotlin.time.Clock

@HiltWorker
class PrayerTimesWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "PrayerTimesWorker"
        private const val ONE_TIME_WORK_NAME = "PrayerTimesWorkerOneTime"

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<PrayerTimesWorker>(
                Duration.ofMinutes(15)
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
            val request = OneTimeWorkRequestBuilder<PrayerTimesWorker>().build()
            manager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: PrayerTimesWidgetState
    ) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = PrayerTimesStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }
        PrayerTimesWidget().updateAll(context)
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(PrayerTimesWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val latitude = preferencesDataStore.latitude.first().takeIf { it != 0.0 } ?: 53.3498
            val longitude = preferencesDataStore.longitude.first().takeIf { it != 0.0 } ?: -6.2603
            val locationName = preferencesDataStore.locationName.first()
                .takeIf { it.isNotBlank() }
                ?.split(",")
                ?.firstOrNull()
                ?.trim() ?: "Dublin"

            val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude)
            val currentTime = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val localTime = currentTime.toLocalDateTime(timeZone)

            val prayerMap = prayerTimes.associate { it.type to it }

            val fajr = prayerMap[PrayerType.FAJR]
            val dhuhr = prayerMap[PrayerType.DHUHR]
            val asr = prayerMap[PrayerType.ASR]
            val maghrib = prayerMap[PrayerType.MAGHRIB]
            val isha = prayerMap[PrayerType.ISHA]

            fun isPassed(prayerTime: com.arshadshah.nimaz.domain.model.PrayerTime?): Boolean {
                if (prayerTime == null) return false
                val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
                return prayerLocalTime.time < localTime.time
            }

            val nextPrayer = prayerTimes.firstOrNull { prayerTime ->
                prayerTime.type != PrayerType.SUNRISE &&
                prayerTime.time.toLocalDateTime(timeZone).time > localTime.time
            }

            val timeUntilNext = if (nextPrayer != null) {
                val diff: kotlin.time.Duration = nextPrayer.time - currentTime
                val totalMinutes = diff.inWholeMinutes
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                when {
                    hours > 0 -> "${hours}h ${minutes}m"
                    else -> "${minutes}m"
                }
            } else "—"

            val hijriDate = HijriDateCalculator.today()

            val data = PrayerTimesData(
                locationName = locationName,
                hijriDate = "${hijriDate.day} ${hijriDate.monthName}",
                nextPrayerName = nextPrayer?.type?.displayName ?: "—",
                timeUntilNext = timeUntilNext,
                fajrTime = fajr?.let { formatTime(it.time.toLocalDateTime(timeZone).hour, it.time.toLocalDateTime(timeZone).minute) } ?: "—",
                dhuhrTime = dhuhr?.let { formatTime(it.time.toLocalDateTime(timeZone).hour, it.time.toLocalDateTime(timeZone).minute) } ?: "—",
                asrTime = asr?.let { formatTime(it.time.toLocalDateTime(timeZone).hour, it.time.toLocalDateTime(timeZone).minute) } ?: "—",
                maghribTime = maghrib?.let { formatTime(it.time.toLocalDateTime(timeZone).hour, it.time.toLocalDateTime(timeZone).minute) } ?: "—",
                ishaTime = isha?.let { formatTime(it.time.toLocalDateTime(timeZone).hour, it.time.toLocalDateTime(timeZone).minute) } ?: "—",
                fajrPassed = isPassed(fajr),
                dhuhrPassed = isPassed(dhuhr),
                asrPassed = isPassed(asr),
                maghribPassed = isPassed(maghrib),
                ishaPassed = isPassed(isha)
            )

            setWidgetState(glanceIds, PrayerTimesWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            setWidgetState(glanceIds, PrayerTimesWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%d:%02d", h, minute)
    }
}
