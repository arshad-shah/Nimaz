package com.arshadshah.nimaz.widget.prayertimes

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.widget.core.WidgetWork
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
class PrayerTimesWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimeCalculator: PrayerTimeCalculator,
    private val preferencesDataStore: PreferencesDataStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "PrayerTimesWorker"
        private const val ONE_TIME_WORK_NAME = "PrayerTimesWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(15)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<PrayerTimesWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<PrayerTimesWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: PrayerTimesWidgetState
    ) = updateWidgetState(context, PrayerTimesWidget(), PrayerTimesStateDefinition, glanceIds, newState)

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

            fun formatPrayer(prayerTime: com.arshadshah.nimaz.domain.model.PrayerTime?): String =
                prayerTime?.let {
                    val local = it.time.toLocalDateTime(timeZone)
                    formatWidgetTime(local.hour, local.minute)
                } ?: "—"

            val nextPrayer = prayerTimes.firstOrNull { prayerTime ->
                prayerTime.type != PrayerType.SUNRISE &&
                        prayerTime.time.toLocalDateTime(timeZone).time > localTime.time
            }

            val nextPrayerEpochMillis = nextPrayer?.time?.toEpochMilliseconds() ?: 0L
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
                fajrTime = formatPrayer(fajr),
                dhuhrTime = formatPrayer(dhuhr),
                asrTime = formatPrayer(asr),
                maghribTime = formatPrayer(maghrib),
                ishaTime = formatPrayer(isha),
                fajrPassed = isPassed(fajr),
                dhuhrPassed = isPassed(dhuhr),
                asrPassed = isPassed(asr),
                maghribPassed = isPassed(maghrib),
                ishaPassed = isPassed(isha),
                nextPrayerEpochMillis = nextPrayerEpochMillis
            )

            setWidgetState(glanceIds, PrayerTimesWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            CrashReporter.log("PrayerTimesWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, PrayerTimesWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
