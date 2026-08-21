package com.arshadshah.nimaz.domain.usecase

import app.cash.turbine.test
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.QaidaCellProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Use-case tests for the Qaida learning loop (#176): cell-heard writes, lesson
 * gating/unlock, resume pointer and derived course progress, all over a mocked
 * repository.
 */
class QaidaProgressUseCasesTest {

    private lateinit var repository: QaidaRepository
    private lateinit var unlockNextLesson: UnlockNextLessonUseCase
    private lateinit var markCellHeard: MarkCellHeardUseCase

    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        unlockNextLesson = UnlockNextLessonUseCase(repository)
        markCellHeard = MarkCellHeardUseCase(repository, unlockNextLesson)
    }

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

    // ── MarkCellHeardUseCase ────────────────────────────────────────

    @Test
    fun `markCellHeard records cell and persists in-progress lesson without unlocking`() = runTest {
        coEvery { repository.getCellProgress(1, 5) } returns null
        coEvery { repository.getCellCountForLesson(1) } returns 10
        coEvery { repository.getCompletedCellCount(1) } returns 3
        coEvery { repository.getLessonProgress(1) } returns null

        val cellSlot = slot<QaidaCellProgress>()
        val lessonSlot = slot<QaidaLessonProgress>()
        coEvery { repository.upsertCellProgress(capture(cellSlot)) } just Runs
        coEvery { repository.upsertLessonProgress(capture(lessonSlot)) } just Runs

        markCellHeard(lessonId = 1, cellId = 5, now = now)

        assertThat(cellSlot.captured.cellId).isEqualTo(5)
        assertThat(cellSlot.captured.heardCount).isEqualTo(1)
        assertThat(cellSlot.captured.isCompleted).isTrue()
        assertThat(cellSlot.captured.lastPracticedAt).isEqualTo(now)

        assertThat(lessonSlot.captured.status).isEqualTo(LessonStatus.IN_PROGRESS)
        assertThat(lessonSlot.captured.completedCells).isEqualTo(3)
        assertThat(lessonSlot.captured.totalCells).isEqualTo(10)
        assertThat(lessonSlot.captured.lastCellId).isEqualTo(5)
        assertThat(lessonSlot.captured.updatedAt).isEqualTo(now)

        // Nothing unlocked: getLessons (used only by unlock) is never read.
        coVerify(exactly = 0) { repository.getLessons() }
    }

    @Test
    fun `markCellHeard increments replay count for an already-heard cell`() = runTest {
        coEvery { repository.getCellProgress(1, 5) } returns
            QaidaCellProgress(1, 5, heardCount = 2, isCompleted = true, lastPracticedAt = 1L)
        coEvery { repository.getCellCountForLesson(1) } returns 10
        coEvery { repository.getCompletedCellCount(1) } returns 4

        val cellSlot = slot<QaidaCellProgress>()
        coEvery { repository.upsertCellProgress(capture(cellSlot)) } just Runs

        markCellHeard(lessonId = 1, cellId = 5, now = now)

        assertThat(cellSlot.captured.heardCount).isEqualTo(3)
    }

    @Test
    fun `markCellHeard completes lesson, awards stars and unlocks the next`() = runTest {
        coEvery { repository.getCellProgress(1, 9) } returns null
        coEvery { repository.getCellCountForLesson(1) } returns 10
        coEvery { repository.getCompletedCellCount(1) } returns 10
        coEvery { repository.getLessonProgress(1) } returns null
        // Unlock path
        every { repository.getLessons() } returns flowOf(listOf(lesson(1), lesson(2)))
        coEvery { repository.getLessonProgress(2) } returns null
        coEvery { repository.getCellCountForLesson(2) } returns 8

        val lessonSlots = mutableListOf<QaidaLessonProgress>()
        coEvery { repository.upsertLessonProgress(capture(lessonSlots)) } just Runs

        markCellHeard(lessonId = 1, cellId = 9, now = now)

        val lesson1 = lessonSlots.first { it.lessonId == 1 }
        assertThat(lesson1.status).isEqualTo(LessonStatus.COMPLETED)
        assertThat(lesson1.stars).isEqualTo(2)

        val lesson2 = lessonSlots.first { it.lessonId == 2 }
        assertThat(lesson2.status).isEqualTo(LessonStatus.UNLOCKED)
        assertThat(lesson2.totalCells).isEqualTo(8)
        assertThat(lesson2.completedCells).isEqualTo(0)
    }

    @Test
    fun `markCellHeard never regresses earned stars`() = runTest {
        coEvery { repository.getCellProgress(1, 2) } returns null
        coEvery { repository.getCellCountForLesson(1) } returns 10
        coEvery { repository.getCompletedCellCount(1) } returns 2 // would be 0 stars
        coEvery { repository.getLessonProgress(1) } returns QaidaLessonProgress(
            lessonId = 1,
            status = LessonStatus.IN_PROGRESS,
            stars = 2,
            lastCellId = 1,
            completedCells = 9,
            totalCells = 10,
            updatedAt = 1L
        )

        val lessonSlot = slot<QaidaLessonProgress>()
        coEvery { repository.upsertLessonProgress(capture(lessonSlot)) } just Runs

        markCellHeard(lessonId = 1, cellId = 2, now = now)

        assertThat(lessonSlot.captured.stars).isEqualTo(2)
    }

    // ── UnlockNextLessonUseCase ─────────────────────────────────────

    @Test
    fun `unlockNextLesson unlocks the following lesson by display order`() = runTest {
        every { repository.getLessons() } returns flowOf(
            listOf(lesson(id = 1, order = 1), lesson(id = 2, order = 2))
        )
        coEvery { repository.getLessonProgress(2) } returns null
        coEvery { repository.getCellCountForLesson(2) } returns 6

        val slot = slot<QaidaLessonProgress>()
        coEvery { repository.upsertLessonProgress(capture(slot)) } just Runs

        val unlocked = unlockNextLesson(lessonId = 1, now = now)

        assertThat(unlocked).isEqualTo(2)
        assertThat(slot.captured.lessonId).isEqualTo(2)
        assertThat(slot.captured.status).isEqualTo(LessonStatus.UNLOCKED)
    }

    @Test
    fun `unlockNextLesson returns null at the last lesson`() = runTest {
        every { repository.getLessons() } returns flowOf(listOf(lesson(1), lesson(2)))

        val unlocked = unlockNextLesson(lessonId = 2, now = now)

        assertThat(unlocked).isNull()
        coVerify(exactly = 0) { repository.upsertLessonProgress(any<QaidaLessonProgress>()) }
    }

    @Test
    fun `unlockNextLesson does not clobber existing progress on the next lesson`() = runTest {
        every { repository.getLessons() } returns flowOf(listOf(lesson(1), lesson(2)))
        coEvery { repository.getLessonProgress(2) } returns QaidaLessonProgress(
            lessonId = 2,
            status = LessonStatus.IN_PROGRESS,
            stars = 1,
            lastCellId = 3,
            completedCells = 4,
            totalCells = 8,
            updatedAt = 1L
        )

        val unlocked = unlockNextLesson(lessonId = 1, now = now)

        assertThat(unlocked).isNull()
        coVerify(exactly = 0) { repository.upsertLessonProgress(any<QaidaLessonProgress>()) }
    }

    // ── GetCourseProgressUseCase / GetLessonProgressUseCase ─────────

    @Test
    fun `getCourseProgress derives gated statuses and resume pointer`() = runTest {
        every { repository.getLessons() } returns flowOf(listOf(lesson(1), lesson(2), lesson(3)))
        every { repository.getAllProgress() } returns flowOf(
            listOf(
                QaidaLessonProgress(1, LessonStatus.COMPLETED, 2, 9, 10, 10, now)
            )
        )

        GetCourseProgressUseCase(repository)().test {
            val course = awaitItem()
            assertThat(course.lessons.map { it.status }).containsExactly(
                LessonStatus.COMPLETED,
                LessonStatus.UNLOCKED,
                LessonStatus.LOCKED
            ).inOrder()
            assertThat(course.nextLessonId).isEqualTo(2)
            assertThat(course.totalStars).isEqualTo(2)
            awaitComplete()
        }
    }

    @Test
    fun `getLessonProgress restores exact position and status for a lesson`() = runTest {
        every { repository.getLessons() } returns flowOf(listOf(lesson(1), lesson(2)))
        every { repository.getAllProgress() } returns flowOf(
            listOf(
                QaidaLessonProgress(1, LessonStatus.IN_PROGRESS, 1, 7, 6, 10, now)
            )
        )

        GetLessonProgressUseCase(repository)(lessonId = 1).test {
            val state = awaitItem()
            assertThat(state).isNotNull()
            assertThat(state!!.status).isEqualTo(LessonStatus.IN_PROGRESS)
            assertThat(state.lastCellId).isEqualTo(7)
            assertThat(state.completedCells).isEqualTo(6)
            awaitComplete()
        }
    }

    @Test
    fun `getLessonProgress emits null for an unknown lesson`() = runTest {
        every { repository.getLessons() } returns flowOf(listOf(lesson(1)))
        every { repository.getAllProgress() } returns flowOf(emptyList())

        GetLessonProgressUseCase(repository)(lessonId = 99).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }
}
