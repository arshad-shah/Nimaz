package com.arshadshah.nimaz.domain.model

/**
 * Single source of truth for every Qaida "learning loop" threshold (epic #171,
 * sub-issue E of #176): completion gating, star awards, unlock rules and the
 * derived status / course-progress mappers.
 *
 * All magic numbers live here so they are trivially tunable and the pure logic
 * is unit-testable in isolation from Room, DataStore and the UI.
 */
object QaidaProgressRules {

    /** Stars awarded per lesson range from [MIN_STARS]..[MAX_STARS]. */
    const val MIN_STARS: Int = 1
    const val MAX_STARS: Int = 3

    /**
     * Fraction of a lesson's cells that must be heard for it to count as
     * COMPLETED — and therefore unlock the next lesson. Default = 100%.
     */
    const val COMPLETION_THRESHOLD: Float = 1.0f

    /** Fraction of cells heard required to earn the first star ("finished once"). */
    const val ONE_STAR_THRESHOLD: Float = 0.5f

    /** Fraction of cells heard required to earn the second star ("all cells heard"). */
    const val TWO_STAR_THRESHOLD: Float = 1.0f

    /**
     * Alternative, star-based unlock gate: a previous lesson with at least this
     * many stars also unlocks the next one (see [unlocksNext]). Defaults to the
     * "all cells heard" tier so the cell-fraction and star gates agree.
     */
    const val UNLOCK_MIN_STARS: Int = 2

    /**
     * Completion fraction in 0f..1f. Guards against a zero/empty total so an
     * un-seeded lesson reports 0% rather than NaN.
     */
    fun completionFraction(completedCells: Int, totalCells: Int): Float {
        if (totalCells <= 0) return 0f
        return (completedCells.toFloat() / totalCells).coerceIn(0f, 1f)
    }

    /** Whether a lesson has reached the [COMPLETION_THRESHOLD]. */
    fun isComplete(completedCells: Int, totalCells: Int): Boolean =
        completionFraction(completedCells, totalCells) >= COMPLETION_THRESHOLD

    /**
     * Stars earned for a lesson. The third star requires the optional
     * practice/quiz pass on top of having heard every cell.
     */
    fun starsFor(
        completedCells: Int,
        totalCells: Int,
        practicePassed: Boolean = false
    ): Int {
        val fraction = completionFraction(completedCells, totalCells)
        return when {
            fraction >= TWO_STAR_THRESHOLD && practicePassed -> MAX_STARS
            fraction >= TWO_STAR_THRESHOLD -> 2
            fraction >= ONE_STAR_THRESHOLD -> MIN_STARS
            else -> 0
        }
    }

    /**
     * Derived status for a lesson given its raw progress and whether its unlock
     * gate (the previous lesson) is satisfied. A locked lesson is always LOCKED
     * regardless of any stale stored progress.
     */
    fun statusFor(
        completedCells: Int,
        totalCells: Int,
        isUnlocked: Boolean
    ): LessonStatus {
        if (!isUnlocked) return LessonStatus.LOCKED
        return when {
            isComplete(completedCells, totalCells) -> LessonStatus.COMPLETED
            completedCells > 0 -> LessonStatus.IN_PROGRESS
            else -> LessonStatus.UNLOCKED
        }
    }

    /**
     * Whether finishing [previous] unlocks the lesson that follows it. The very
     * first lesson has no predecessor and is unlocked by [deriveCourseProgress].
     */
    fun unlocksNext(previous: QaidaLessonProgress?): Boolean {
        if (previous == null) return false
        return previous.status == LessonStatus.COMPLETED ||
            isComplete(previous.completedCells, previous.totalCells) ||
            previous.stars >= UNLOCK_MIN_STARS
    }

    /**
     * Pure derived-state mapper: combines the seeded [lessons] (ordered by
     * [QaidaLesson.displayOrder]) with stored [progressByLessonId] into a fully
     * gated [QaidaCourseProgress].
     *
     * Lesson 1 is always unlocked; each subsequent lesson unlocks only once the
     * previous one is COMPLETED. [QaidaCourseProgress.nextLessonId] points at
     * the first unlocked-but-incomplete lesson (the global resume pointer).
     */
    fun deriveCourseProgress(
        lessons: List<QaidaLesson>,
        progressByLessonId: Map<Int, QaidaLessonProgress>
    ): QaidaCourseProgress {
        val ordered = lessons.sortedBy { it.displayOrder }

        var previousCompleted = true // lesson 1 is always unlocked
        var completedLessons = 0
        var totalStars = 0
        var totalCellsHeard = 0
        var nextLessonId: Int? = null

        val states = ordered.map { lesson ->
            val progress = progressByLessonId[lesson.id]
            val completedCells = progress?.completedCells ?: 0
            val totalCells = progress?.totalCells ?: 0
            val isUnlocked = previousCompleted
            val status = statusFor(completedCells, totalCells, isUnlocked)
            val stars = if (isUnlocked) progress?.stars ?: 0 else 0

            if (status == LessonStatus.COMPLETED) completedLessons++
            totalStars += stars
            totalCellsHeard += completedCells
            if (nextLessonId == null &&
                (status == LessonStatus.UNLOCKED || status == LessonStatus.IN_PROGRESS)
            ) {
                nextLessonId = lesson.id
            }

            previousCompleted = status == LessonStatus.COMPLETED

            QaidaLessonState(
                lesson = lesson,
                status = status,
                stars = stars,
                completedCells = completedCells,
                totalCells = totalCells,
                completionFraction = completionFraction(completedCells, totalCells),
                lastCellId = progress?.lastCellId
            )
        }

        val totalLessons = ordered.size
        return QaidaCourseProgress(
            lessons = states,
            completedLessons = completedLessons,
            totalLessons = totalLessons,
            totalStars = totalStars,
            maxStars = totalLessons * MAX_STARS,
            totalCellsHeard = totalCellsHeard,
            overallFraction = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons,
            nextLessonId = nextLessonId
        )
    }
}
