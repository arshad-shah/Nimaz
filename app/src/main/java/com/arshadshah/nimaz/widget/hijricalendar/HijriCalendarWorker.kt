package com.arshadshah.nimaz.widget.hijricalendar

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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@HiltWorker
class HijriCalendarWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "HijriCalendarWorker"
        private const val ONE_TIME_WORK_NAME = "HijriCalendarWorkerOneTime"

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) {
            val manager = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<HijriCalendarWorker>(
                Duration.ofHours(6)
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
            val request = OneTimeWorkRequestBuilder<HijriCalendarWorker>().build()
            manager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
        }
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: HijriCalendarWidgetState
    ) {
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = HijriCalendarStateDefinition,
                glanceId = glanceId,
                updateState = { newState }
            )
        }
        HijriCalendarWidget().updateAll(context)
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(HijriCalendarWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val hijriDate = HijriDateCalculator.today()
            val today = LocalDate.now()
            val gregorianDate = "${today.dayOfMonth} ${
                today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            }"

            val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(
                hijriDate.year, hijriDate.month
            )

            // Get the Gregorian date of the 1st of the current Hijri month
            val firstOfMonth = HijriDateCalculator.toGregorian(1, hijriDate.month, hijriDate.year)
            // dayOfWeek: MONDAY=1 .. SUNDAY=7, convert to 0=Sun..6=Sat
            val javaDow = firstOfMonth.dayOfWeek.value // 1=Mon..7=Sun
            val firstDayOfWeekOffset = if (javaDow == 7) 0 else javaDow // Sun=0, Mon=1..Sat=6

            // Get today's events
            val allEvents = HijriDateCalculator.getIslamicEvents(hijriDate.year)
            val todayEvents = allEvents.filter { event ->
                event.day == hijriDate.day && event.month == hijriDate.month
            }.map { event ->
                HijriCalendarEventData(
                    name = event.name,
                    nameArabic = event.nameArabic,
                    type = event.type.name
                )
            }

            val data = HijriCalendarData(
                hijriMonth = hijriDate.month,
                hijriMonthName = hijriDate.monthName,
                hijriYear = hijriDate.year,
                gregorianDate = gregorianDate,
                daysInMonth = daysInMonth,
                firstDayOfWeekOffset = firstDayOfWeekOffset,
                todayHijriDay = hijriDate.day,
                events = todayEvents
            )

            setWidgetState(glanceIds, HijriCalendarWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            setWidgetState(glanceIds, HijriCalendarWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
