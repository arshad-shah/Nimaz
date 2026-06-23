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
            val use24Hour = preferencesDataStore.use24HourFormat.first()

            val prayerTimes = prayerTimeCalculator.getPrayerTimes(latitude, longitude)
            val timeZone = TimeZone.currentSystemDefault()

            val prayerMap = prayerTimes.associate { it.type to it }

            val fajr = prayerMap[PrayerType.FAJR]
            val dhuhr = prayerMap[PrayerType.DHUHR]
            val asr = prayerMap[PrayerType.ASR]
            val maghrib = prayerMap[PrayerType.MAGHRIB]
            val isha = prayerMap[PrayerType.ISHA]

            fun formatPrayer(prayerTime: com.arshadshah.nimaz.domain.model.PrayerTime?): String =
                prayerTime?.let {
                    val local = it.time.toLocalDateTime(timeZone)
                    formatWidgetTime(local.hour, local.minute, use24Hour = use24Hour)
                } ?: "—"

            fun epochOf(prayerTime: com.arshadshah.nimaz.domain.model.PrayerTime?): Long =
                prayerTime?.time?.toEpochMilliseconds() ?: 0L

            val hijriDate = HijriDateCalculator.today()

            val data = PrayerTimesData(
                locationName = locationName,
                hijriDate = "${hijriDate.day} ${hijriDate.monthName}",
                fajrTime = formatPrayer(fajr),
                dhuhrTime = formatPrayer(dhuhr),
                asrTime = formatPrayer(asr),
                maghribTime = formatPrayer(maghrib),
                ishaTime = formatPrayer(isha),
                fajrEpochMillis = epochOf(fajr),
                dhuhrEpochMillis = epochOf(dhuhr),
                asrEpochMillis = epochOf(asr),
                maghribEpochMillis = epochOf(maghrib),
                ishaEpochMillis = epochOf(isha),
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
