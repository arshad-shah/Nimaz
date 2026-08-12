package com.arshadshah.nimaz.widget.khatam

import com.arshadshah.nimaz.domain.repository.KhatamRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Computes what the khatam widget shows.
 *
 * Split out of [KhatamWorker] so it can be tested. The worker's `doWork()` cannot: it opens by
 * asking `GlanceAppWidgetManager` for the widget's glance ids, and a test device has none placed
 * on its home screen, so the whole body is skipped by the `glanceIds.isEmpty()` early return.
 * That is why `WidgetWorkersTest` was green for a year while asserting nothing about the data —
 * see #474. With the computation here, `doWork()` is *get ids, load, write*, and the part that
 * can be wrong has tests.
 */
class KhatamWidgetDataSource @Inject constructor(
    private val khatamRepository: KhatamRepository,
) {

    suspend fun load(): KhatamWidgetData {
        val khatam = khatamRepository.observeActiveKhatam().first()
            ?: return KhatamWidgetData(hasActiveKhatam = false)

        // The detail snapshot is the only place insights (juz completed) are computed, so read
        // it rather than recomputing here. It can be absent — a khatam that exists but has no
        // detail row yet — which is why every field below has a fallback.
        val insights = khatamRepository.observeKhatamDetail(khatam.id).first()?.insights

        return KhatamWidgetData(
            hasActiveKhatam = true,
            name = khatam.name,
            // Clamped because a khatam recorded past its own total would otherwise render a
            // progress bar wider than its track.
            progressPercent = (khatam.progressPercent * 100).toInt().coerceIn(0, 100),
            // The reader is *inside* the juz after the completed ones.
            currentJuz = insights?.currentJuz ?: 1,
            juzCompleted = insights?.juzCompleted ?: 0,
            remainingAyahs = insights?.remainingAyahs ?: khatam.remainingAyahs,
            dailyTarget = khatam.dailyTarget,
            currentStreak = insights?.currentStreak ?: 0,
        )
    }
}
