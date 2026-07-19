package com.arshadshah.nimaz.widget.khatam

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.widget.core.WidgetWork
import com.arshadshah.nimaz.widget.core.updateWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration

@HiltWorker
class KhatamWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val khatamRepository: KhatamRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val UNIQUE_WORK_NAME = "KhatamWorker"
        private const val ONE_TIME_WORK_NAME = "KhatamWorkerOneTime"
        private val REFRESH_INTERVAL: Duration = Duration.ofMinutes(30)

        fun enqueuePeriodicWork(context: Context, force: Boolean = false) =
            WidgetWork.enqueuePeriodic<KhatamWorker>(
                context, UNIQUE_WORK_NAME, REFRESH_INTERVAL, force
            )

        fun enqueueImmediateWork(context: Context) =
            WidgetWork.enqueueImmediate<KhatamWorker>(context, ONE_TIME_WORK_NAME)

        fun cancel(context: Context) =
            WidgetWork.cancel(context, UNIQUE_WORK_NAME, ONE_TIME_WORK_NAME)
    }

    private suspend fun setWidgetState(
        glanceIds: List<GlanceId>,
        newState: KhatamWidgetState
    ) = updateWidgetState(context, KhatamWidget(), KhatamStateDefinition, glanceIds, newState)

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(KhatamWidget::class.java)

        if (glanceIds.isEmpty()) {
            return Result.success()
        }

        return try {
            val khatam = khatamRepository.observeActiveKhatam().first()
            val data = if (khatam == null) {
                // No active khatam — the widget renders its empty state.
                KhatamWidgetData(hasActiveKhatam = false)
            } else {
                // The detail snapshot is the only place insights (juz completed)
                // are computed, so read it rather than recomputing here.
                val insights = khatamRepository.observeKhatamDetail(khatam.id).first()?.insights
                KhatamWidgetData(
                    hasActiveKhatam = true,
                    name = khatam.name,
                    progressPercent = (khatam.progressPercent * 100).toInt().coerceIn(0, 100),
                    // The reader is *inside* the juz after the completed ones;
                    // cap at 30 for a finished khatam.
                    currentJuz = insights?.currentJuz ?: 1,
                    juzCompleted = insights?.juzCompleted ?: 0,
                    remainingAyahs = insights?.remainingAyahs ?: khatam.remainingAyahs,
                    dailyTarget = khatam.dailyTarget,
                    currentStreak = insights?.currentStreak ?: 0
                )
            }

            setWidgetState(glanceIds, KhatamWidgetState.Success(data))
            Result.success()
        } catch (e: Exception) {
            CrashReporter.log("KhatamWorker failed")
            CrashReporter.recordException(e)
            setWidgetState(glanceIds, KhatamWidgetState.Error(e.message))
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
