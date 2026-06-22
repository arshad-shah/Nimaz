package com.arshadshah.nimaz.widget.hijricalendar

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.updateWidgetState
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
        private val REFRESH_INTERVAL: Duration = Duration.ofHours(6)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<HijriCalendarWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<HijriCalendarWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: HijriCalendarWidgetState
    ) = updateWidgetState(context, HijriCalendarWidget(), HijriCalendarStateDefinition, glanceIds, newState)

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
            CrashReporter.log("HijriCalendarWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, HijriCalendarWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
