package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-logic tests for the centralized Qaida rules (#176): completion gating,
 * star thresholds, unlock transitions and the derived course-progress mapper.
 * No Android dependencies — runs on plain JUnit.
 */
class QaidaProgressRulesTest {

    private fun lesson(id: Int, order: Int = id) = QaidaLesson(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "درس",
        titleTransliteration = "dars",
        description = "",
        conceptTags = emptyList(),
        icon = "book",
        displayOrder = order
    )

    private fun progress(
        lessonId: Int,
        completed: Int,
        total: Int,
        stars: Int = QaidaProgressRules.starsFor(completed, total),
        status: LessonStatus = LessonStatus.IN_PROGRESS,
        lastCellId: Int? = null
    ) = QaidaLessonProgress(
        lessonId = lessonId,
        status = status,
        stars = stars,
        lastCellId = lastCellId,
        completedCells = completed,
        totalCells = total,
        updatedAt = 0L
    )

    // ── completionFraction ──────────────────────────────────────────

    @Test
    fun `completionFraction guards against zero total`() {
        assertThat(QaidaProgressRules.completionFraction(0, 0)).isEqualTo(0f)
        assertThat(QaidaProgressRules.completionFraction(3, 0)).isEqualTo(0f)
    }

    @Test
    fun `completionFraction is clamped to one`() {
        assertThat(QaidaProgressRules.completionFraction(5, 10)).isEqualTo(0.5f)
        assertThat(QaidaProgressRules.completionFraction(12, 10)).isEqualTo(1f)
    }

    // ── stars ───────────────────────────────────────────────────────

    @Test
    fun `starsFor follows documented thresholds`() {
        assertThat(QaidaProgressRules.starsFor(0, 10)).isEqualTo(0)
        // below the first-star threshold
        assertThat(QaidaProgressRules.starsFor(4, 10)).isEqualTo(0)
        // first star at >= 50%
        assertThat(QaidaProgressRules.starsFor(5, 10)).isEqualTo(1)
        assertThat(QaidaProgressRules.starsFor(9, 10)).isEqualTo(1)
        // second star at 100% of cells heard
        assertThat(QaidaProgressRules.starsFor(10, 10)).isEqualTo(2)
        // third star needs all cells heard AND the practice/quiz pass
        assertThat(QaidaProgressRules.starsFor(10, 10, practicePassed = true)).isEqualTo(3)
        // practice pass without full cells does not grant the third star
        assertThat(QaidaProgressRules.starsFor(5, 10, practicePassed = true)).isEqualTo(1)
    }

    // ── isComplete ──────────────────────────────────────────────────

    @Test
    fun `isComplete only at the completion threshold`() {
        assertThat(QaidaProgressRules.isComplete(9, 10)).isFalse()
        assertThat(QaidaProgressRules.isComplete(10, 10)).isTrue()
    }

    // ── statusFor ───────────────────────────────────────────────────

    @Test
    fun `statusFor reports LOCKED regardless of stored progress when gated`() {
        assertThat(QaidaProgressRules.statusFor(10, 10, isUnlocked = false))
            .isEqualTo(LessonStatus.LOCKED)
    }

    @Test
    fun `statusFor maps unlocked progress to UNLOCKED, IN_PROGRESS, COMPLETED`() {
        assertThat(QaidaProgressRules.statusFor(0, 10, isUnlocked = true))
            .isEqualTo(LessonStatus.UNLOCKED)
        assertThat(QaidaProgressRules.statusFor(3, 10, isUnlocked = true))
            .isEqualTo(LessonStatus.IN_PROGRESS)
        assertThat(QaidaProgressRules.statusFor(10, 10, isUnlocked = true))
            .isEqualTo(LessonStatus.COMPLETED)
    }

    // ── unlocksNext ─────────────────────────────────────────────────

    @Test
    fun `unlocksNext requires completion or enough stars`() {
        assertThat(QaidaProgressRules.unlocksNext(null)).isFalse()
        assertThat(QaidaProgressRules.unlocksNext(progress(1, 9, 10, stars = 1))).isFalse()
        // 100% cells heard
        assertThat(QaidaProgressRules.unlocksNext(progress(1, 10, 10))).isTrue()
        // status already COMPLETED
        assertThat(
            QaidaProgressRules.unlocksNext(
                progress(1, 0, 0, stars = 0, status = LessonStatus.COMPLETED)
            )
        ).isTrue()
        // star-based gate
        assertThat(QaidaProgressRules.unlocksNext(progress(1, 0, 0, stars = 2))).isTrue()
    }

    // ── deriveCourseProgress ────────────────────────────────────────

    @Test
    fun `deriveCourseProgress unlocks only lesson one when no progress exists`() {
        val lessons = listOf(lesson(1), lesson(2), lesson(3))

        val course = QaidaProgressRules.deriveCourseProgress(lessons, emptyMap())

        assertThat(course.lessons.map { it.status }).containsExactly(
            LessonStatus.UNLOCKED,
            LessonStatus.LOCKED,
            LessonStatus.LOCKED
        ).inOrder()
        assertThat(course.completedLessons).isEqualTo(0)
        assertThat(course.totalLessons).isEqualTo(3)
        assertThat(course.maxStars).isEqualTo(9)
        // Resume pointer = first unlocked-incomplete lesson
        assertThat(course.nextLessonId).isEqualTo(1)
        assertThat(course.overallFraction).isEqualTo(0f)
    }

    @Test
    fun `deriveCourseProgress unlocks the next lesson when the previous is completed`() {
        val lessons = listOf(lesson(1), lesson(2), lesson(3))
        val progressMap = mapOf(
            1 to progress(1, 10, 10, status = LessonStatus.COMPLETED, lastCellId = 99)
        )

        val course = QaidaProgressRules.deriveCourseProgress(lessons, progressMap)

        assertThat(course.lessons.map { it.status }).containsExactly(
            LessonStatus.COMPLETED,
            LessonStatus.UNLOCKED,
            LessonStatus.LOCKED
        ).inOrder()
        assertThat(course.completedLessons).isEqualTo(1)
        assertThat(course.totalStars).isEqualTo(2)
        assertThat(course.totalCellsHeard).isEqualTo(10)
        // Lesson 1 done → continue at lesson 2
        assertThat(course.nextLessonId).isEqualTo(2)
        assertThat(course.lessons[0].lastCellId).isEqualTo(99)
    }

    @Test
    fun `deriveCourseProgress resume pointer prefers an in-progress lesson`() {
        val lessons = listOf(lesson(1), lesson(2))
        val progressMap = mapOf(1 to progress(1, 4, 10))

        val course = QaidaProgressRules.deriveCourseProgress(lessons, progressMap)

        assertThat(course.lessons[0].status).isEqualTo(LessonStatus.IN_PROGRESS)
        assertThat(course.nextLessonId).isEqualTo(1)
    }

    @Test
    fun `deriveCourseProgress reports null resume pointer when every lesson is complete`() {
        val lessons = listOf(lesson(1), lesson(2))
        val progressMap = mapOf(
            1 to progress(1, 10, 10, status = LessonStatus.COMPLETED),
            2 to progress(2, 8, 8, status = LessonStatus.COMPLETED)
        )

        val course = QaidaProgressRules.deriveCourseProgress(lessons, progressMap)

        assertThat(course.completedLessons).isEqualTo(2)
        assertThat(course.overallFraction).isEqualTo(1f)
        assertThat(course.nextLessonId).isNull()
    }

    @Test
    fun `deriveCourseProgress respects display order over id`() {
        // id order and display order disagree; gating must follow display order.
        val lessons = listOf(lesson(id = 2, order = 1), lesson(id = 1, order = 2))
        val progressMap = mapOf(
            2 to progress(2, 10, 10, status = LessonStatus.COMPLETED)
        )

        val course = QaidaProgressRules.deriveCourseProgress(lessons, progressMap)

        assertThat(course.lessons.map { it.lesson.id }).containsExactly(2, 1).inOrder()
        assertThat(course.lessons[1].status).isEqualTo(LessonStatus.UNLOCKED)
    }

    @Test
    fun `deriveCourseProgress ignores stale stars on a locked lesson`() {
        // Lesson 2 has stored stars but lesson 1 is incomplete, so 2 stays LOCKED.
        val lessons = listOf(lesson(1), lesson(2))
        val progressMap = mapOf(
            1 to progress(1, 3, 10),
            2 to progress(2, 0, 0, stars = 3, status = LessonStatus.COMPLETED)
        )

        val course = QaidaProgressRules.deriveCourseProgress(lessons, progressMap)

        assertThat(course.lessons[1].status).isEqualTo(LessonStatus.LOCKED)
        assertThat(course.lessons[1].stars).isEqualTo(0)
        // Only lesson 1's (zero) stars count toward the total.
        assertThat(course.totalStars).isEqualTo(0)
    }
}
