package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCellProgress
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.domain.model.QaidaProgressRules
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The Qaida "learning loop" use cases (epic #171, sub-issue E of #176): the
 * testable domain logic for persisted progress, lesson gating, stars and
 * resume. All thresholds live in [QaidaProgressRules]; these classes only
 * orchestrate reads/writes through the repository.
 */

/**
 * Records that a cell's audio was tapped/played, then recomputes and persists
 * the lesson's progress (completed cells, status, stars, resume pointer). When
 * the lesson reaches completion it unlocks the following lesson.
 *
 * [now] is injectable so unit tests can assert deterministic timestamps.
 */
class MarkCellHeardUseCase @Inject constructor(
    private val repository: QaidaRepository,
    private val unlockNextLesson: UnlockNextLessonUseCase
) {
    suspend operator fun invoke(
        lessonId: Int,
        cellId: Int,
        now: Long = System.currentTimeMillis()
    ) {
        // 1. Mark the individual cell as heard (incrementing replay count).
        val existingCell = repository.getCellProgress(lessonId, cellId)
        repository.upsertCellProgress(
            QaidaCellProgress(
                lessonId = lessonId,
                cellId = cellId,
                heardCount = (existingCell?.heardCount ?: 0) + 1,
                isCompleted = true,
                lastPracticedAt = now
            )
        )

        // 2. Recompute the lesson rollup from the data-driven rules.
        val totalCells = repository.getCellCountForLesson(lessonId)
        val completedCells = repository.getCompletedCellCount(lessonId)
        val stars = QaidaProgressRules.starsFor(completedCells, totalCells)
        val status = QaidaProgressRules.statusFor(completedCells, totalCells, isUnlocked = true)
        val existingProgress = repository.getLessonProgress(lessonId)

        repository.upsertLessonProgress(
            QaidaLessonProgress(
                lessonId = lessonId,
                status = status,
                // Stars never regress once earned.
                stars = maxOf(stars, existingProgress?.stars ?: 0),
                lastCellId = cellId,
                completedCells = completedCells,
                totalCells = totalCells,
                updatedAt = now
            )
        )

        // 3. Completing this lesson unlocks the next one.
        if (QaidaProgressRules.isComplete(completedCells, totalCells)) {
            unlockNextLesson(lessonId, now)
        }
    }
}

/**
 * Persists an `UNLOCKED` progress row for the lesson that follows [lessonId]
 * (by display order), if there is one and it has not already been started.
 * Returns the newly-unlocked lesson id, or null if nothing changed.
 */
class UnlockNextLessonUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    suspend operator fun invoke(
        lessonId: Int,
        now: Long = System.currentTimeMillis()
    ): Int? {
        val lessons = repository.getLessons().first().sortedBy { it.displayOrder }
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index == -1 || index == lessons.lastIndex) return null

        val next = lessons[index + 1]
        // Don't clobber progress the learner has already made on the next lesson.
        if (repository.getLessonProgress(next.id) != null) return null

        repository.upsertLessonProgress(
            QaidaLessonProgress(
                lessonId = next.id,
                status = LessonStatus.UNLOCKED,
                stars = 0,
                lastCellId = null,
                completedCells = 0,
                totalCells = repository.getCellCountForLesson(next.id),
                updatedAt = now
            )
        )
        return next.id
    }
}

/**
 * Observes the derived display state ([QaidaLessonState]) for a single lesson,
 * with status correctly gated against the preceding lessons. Emits null if the
 * lesson id is unknown.
 */
class GetLessonProgressUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    operator fun invoke(lessonId: Int): Flow<QaidaLessonState?> =
        combine(repository.getLessons(), repository.getAllProgress()) { lessons, progress ->
            QaidaProgressRules.deriveCourseProgress(
                lessons = lessons,
                progressByLessonId = progress.associateBy { it.lessonId }
            ).lessons.firstOrNull { it.lesson.id == lessonId }
        }
}

/**
 * Observes the whole-course rollup ([QaidaCourseProgress]): every lesson's
 * gated status, overall completion %, total stars and the "continue where you
 * left off" pointer. Recomputes reactively as progress is written.
 */
class GetCourseProgressUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    operator fun invoke(): Flow<QaidaCourseProgress> =
        combine(repository.getLessons(), repository.getAllProgress()) { lessons, progress ->
            QaidaProgressRules.deriveCourseProgress(
                lessons = lessons,
                progressByLessonId = progress.associateBy { it.lessonId }
            )
        }
}
