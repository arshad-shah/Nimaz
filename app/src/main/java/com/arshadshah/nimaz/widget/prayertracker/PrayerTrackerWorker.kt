package com.arshadshah.nimaz.widget.prayertracker

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.updateWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate

@HiltWorker
class PrayerTrackerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerDao: PrayerDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "PrayerTrackerWorker"
        private const val ONE_TIME_WORK_NAME = "PrayerTrackerWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(30)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<PrayerTrackerWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<PrayerTrackerWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: PrayerTrackerWidgetState
    ) = updateWidgetState(context, PrayerTrackerWidget(), PrayerTrackerStateDefinition, glanceIds, newState)

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(PrayerTrackerWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val today = LocalDate.now()
            val todayEpoch = today.toEpochDay() * 86400000L
            val records = prayerDao.getPrayerRecordsForDateSync(todayEpoch)
            val recordMap = records.associate { it.prayerName to it.status }

            val data = PrayerTrackerData(
                dateLabel = today.dayOfWeek.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                fajr = recordMap["fajr"] == "prayed",
                dhuhr = recordMap["dhuhr"] == "prayed",
                asr = recordMap["asr"] == "prayed",
                maghrib = recordMap["maghrib"] == "prayed",
                isha = recordMap["isha"] == "prayed",
                prayedCount = listOf(
                    recordMap["fajr"] == "prayed",
                    recordMap["dhuhr"] == "prayed",
                    recordMap["asr"] == "prayed",
                    recordMap["maghrib"] == "prayed",
                    recordMap["isha"] == "prayed"
                ).count { it },
                totalCount = 5
            )

            setWidgetState(glanceIds, PrayerTrackerWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            setWidgetState(glanceIds, PrayerTrackerWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
