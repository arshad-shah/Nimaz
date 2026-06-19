package com.arshadshah.nimaz.data.repository

import app.cash.turbine.test
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonWithContent
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineWithCells
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaCellProgress
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.model.TokenType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Runs under Robolectric so the real org.json implementation backs concept-tag
// parsing (plain JUnit tests use the stubbed android.jar, which would yield empty lists).
@RunWith(RobolectricTestRunner::class)
class QaidaRepositoryImplTest {

    private lateinit var dao: QaidaDao
    private lateinit var repository: QaidaRepositoryImpl

    private val now = System.currentTimeMillis()

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = QaidaRepositoryImpl(dao)
    }

    // ── Helper factories ────────────────────────────────────────────

    private fun lessonEntity(
        id: Int = 1,
        displayOrder: Int = 1,
        conceptTags: String = """["alphabet","letters"]"""
    ) = QaidaLessonEntity(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "الدرس",
        titleTransliteration = "ad-dars",
        description = "Intro",
        conceptTags = conceptTags,
        icon = "book",
        displayOrder = displayOrder
    )

    private fun letterEntity(
        id: Int = 1,
        audioKey: String = "alif",
        makhrajArea: String = "JAWF"
    ) = QaidaLetterEntity(
        id = id,
        letterArabic = "ا",
        nameArabic = "ألف",
        nameTransliteration = "alif",
        isolatedForm = "ا",
        initialForm = null,
        medialForm = null,
        finalForm = "ـا",
        isConnecting = false,
        makhrajArea = makhrajArea,
        makhrajDetail = "throat",
        phoneticHint = "a",
        audioKey = audioKey,
        displayOrder = id
    )

    private fun lineEntity(
        id: Int = 1,
        lessonId: Int = 1,
        lineType: String = "EXAMPLE",
        displayOrder: Int = 1
    ) = QaidaLineEntity(
        id = id,
        lessonId = lessonId,
        lineNumber = id,
        lineType = lineType,
        instructionEnglish = "Read",
        instructionArabic = null,
        displayOrder = displayOrder
    )

    private fun cellEntity(
        id: Int = 1,
        lineId: Int = 1,
        lessonId: Int = 1,
        position: Int = 1,
        tokenType: String = "LETTER",
        audioKey: String = "l1_alif"
    ) = QaidaCellEntity(
        id = id,
        lineId = lineId,
        lessonId = lessonId,
        position = position,
        textArabic = "ا",
        transliteration = "a",
        tokenType = tokenType,
        audioKey = audioKey,
        highlightGroup = null,
        letterId = 1,
        notes = null
    )

    private fun progressEntity(
        lessonId: Int = 1,
        status: String = "IN_PROGRESS",
        stars: Int = 2
    ) = QaidaLessonProgressEntity(
        lessonId = lessonId,
        status = status,
        stars = stars,
        lastCellId = 5,
        completedCells = 3,
        totalCells = 10,
        updatedAt = now
    )

    // ── getLessons ──────────────────────────────────────────────────

    @Test
    fun `getLessons emits mapped lessons with parsed concept tags`() = runTest {
        every { dao.getAllLessons() } returns flowOf(listOf(lessonEntity()))

        repository.getLessons().test {
            val lessons = awaitItem()
            assertThat(lessons).hasSize(1)
            assertThat(lessons[0].id).isEqualTo(1)
            assertThat(lessons[0].conceptTags).containsExactly("alphabet", "letters").inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `getLessons maps malformed concept tags JSON to empty list`() = runTest {
        every { dao.getAllLessons() } returns flowOf(listOf(lessonEntity(conceptTags = "not json")))

        repository.getLessons().test {
            assertThat(awaitItem()[0].conceptTags).isEmpty()
            awaitComplete()
        }
    }

    @Test
    fun `getLessons emits empty list when no lessons`() = runTest {
        every { dao.getAllLessons() } returns flowOf(emptyList())

        repository.getLessons().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }
    }

    // ── getLetters ──────────────────────────────────────────────────

    @Test
    fun `getLetters maps makhraj area enum and resolves audio path`() = runTest {
        every { dao.getAllLetters() } returns flowOf(listOf(letterEntity(makhrajArea = "halq", audioKey = "alif")))

        repository.getLetters().test {
            val letters = awaitItem()
            assertThat(letters[0].makhrajArea).isEqualTo(MakhrajArea.HALQ)
            assertThat(letters[0].audioKey).isEqualTo("alif")
            assertThat(letters[0].audioPath).isEqualTo("file:///android_asset/qaida/audio/alif.mp3")
            awaitComplete()
        }
    }

    @Test
    fun `getLetter returns mapped domain or null`() = runTest {
        coEvery { dao.getLetter(1) } returns letterEntity(id = 1)
        coEvery { dao.getLetter(999) } returns null

        assertThat(repository.getLetter(1)).isNotNull()
        assertThat(repository.getLetter(999)).isNull()
    }

    // ── getCell ─────────────────────────────────────────────────────

    @Test
    fun `getCell maps token type and resolves audio path`() = runTest {
        coEvery { dao.getCell(7) } returns cellEntity(id = 7, tokenType = "syllable", audioKey = "l1_ba")

        val cell = repository.getCell(7)
        assertThat(cell).isNotNull()
        assertThat(cell!!.tokenType).isEqualTo(TokenType.SYLLABLE)
        assertThat(cell.audioPath).isEqualTo("file:///android_asset/qaida/audio/l1_ba.mp3")
    }

    @Test
    fun `getCell returns null when missing`() = runTest {
        coEvery { dao.getCell(999) } returns null
        assertThat(repository.getCell(999)).isNull()
    }

    @Test
    fun `getCellsForLesson emits mapped cells`() = runTest {
        every { dao.getCellsForLesson(1) } returns flowOf(
            listOf(cellEntity(id = 1, position = 1), cellEntity(id = 2, position = 2))
        )

        repository.getCellsForLesson(1).test {
            val cells = awaitItem()
            assertThat(cells).hasSize(2)
            assertThat(cells.map { it.id }).containsExactly(1, 2).inOrder()
            awaitComplete()
        }
    }

    // ── getLessonContent ────────────────────────────────────────────

    @Test
    fun `getLessonContent maps hierarchy and orders lines and cells`() = runTest {
        val content = QaidaLessonWithContent(
            lesson = lessonEntity(id = 1),
            lines = listOf(
                // Deliberately out of display order to verify sorting
                QaidaLineWithCells(
                    line = lineEntity(id = 2, lineType = "EXERCISE", displayOrder = 2),
                    cells = listOf(
                        cellEntity(id = 21, lineId = 2, position = 2),
                        cellEntity(id = 20, lineId = 2, position = 1)
                    )
                ),
                QaidaLineWithCells(
                    line = lineEntity(id = 1, lineType = "HEADING", displayOrder = 1),
                    cells = listOf(cellEntity(id = 10, lineId = 1, position = 1))
                )
            )
        )
        every { dao.getLessonWithLinesAndCells(1) } returns flowOf(content)

        repository.getLessonContent(1).test {
            val result = awaitItem()
            assertThat(result).isNotNull()
            assertThat(result!!.lesson.id).isEqualTo(1)
            // Lines sorted by display order
            assertThat(result.lines.map { it.line.id }).containsExactly(1, 2).inOrder()
            assertThat(result.lines[0].line.lineType).isEqualTo(LineType.HEADING)
            assertThat(result.lines[1].line.lineType).isEqualTo(LineType.EXERCISE)
            // Cells within the second line sorted by position
            assertThat(result.lines[1].cells.map { it.id }).containsExactly(20, 21).inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `getLessonContent emits null when lesson missing`() = runTest {
        every { dao.getLessonWithLinesAndCells(404) } returns flowOf(null)

        repository.getLessonContent(404).test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    // ── Progress ────────────────────────────────────────────────────

    @Test
    fun `observeLessonProgress maps status enum`() = runTest {
        every { dao.observeLessonProgress(1) } returns flowOf(progressEntity(status = "COMPLETED"))

        repository.observeLessonProgress(1).test {
            val progress = awaitItem()
            assertThat(progress).isNotNull()
            assertThat(progress!!.status).isEqualTo(LessonStatus.COMPLETED)
            assertThat(progress.stars).isEqualTo(2)
            awaitComplete()
        }
    }

    @Test
    fun `getAllProgress emits mapped progress list`() = runTest {
        every { dao.getAllProgress() } returns flowOf(listOf(progressEntity(lessonId = 1), progressEntity(lessonId = 2)))

        repository.getAllProgress().test {
            assertThat(awaitItem()).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun `getLessonProgress returns mapped domain or null`() = runTest {
        coEvery { dao.getLessonProgress(1) } returns progressEntity()
        coEvery { dao.getLessonProgress(999) } returns null

        assertThat(repository.getLessonProgress(1)).isNotNull()
        assertThat(repository.getLessonProgress(999)).isNull()
    }

    // ── Cell progress ───────────────────────────────────────────────

    @Test
    fun `getCellProgress maps entity to domain`() = runTest {
        coEvery { dao.getCellProgress(1, 5) } returns QaidaCellProgressEntity(
            lessonId = 1,
            cellId = 5,
            heardCount = 3,
            isCompleted = true,
            lastPracticedAt = now
        )
        coEvery { dao.getCellProgress(1, 999) } returns null

        val progress = repository.getCellProgress(1, 5)
        assertThat(progress).isNotNull()
        assertThat(progress!!.heardCount).isEqualTo(3)
        assertThat(progress.isCompleted).isTrue()
        assertThat(repository.getCellProgress(1, 999)).isNull()
    }

    @Test
    fun `getCellCountForLesson and getCompletedCellCount delegate to DAO`() = runTest {
        coEvery { dao.countCellsForLesson(1) } returns 12
        coEvery { dao.countCompletedCells(1) } returns 5

        assertThat(repository.getCellCountForLesson(1)).isEqualTo(12)
        assertThat(repository.getCompletedCellCount(1)).isEqualTo(5)
    }

    @Test
    fun `upsertCellProgress converts domain to entity and calls DAO`() = runTest {
        repository.upsertCellProgress(
            QaidaCellProgress(
                lessonId = 2,
                cellId = 7,
                heardCount = 1,
                isCompleted = true,
                lastPracticedAt = now
            )
        )

        coVerify {
            dao.upsertCellProgress(match<QaidaCellProgressEntity> { entity ->
                entity.lessonId == 2 &&
                    entity.cellId == 7 &&
                    entity.heardCount == 1 &&
                    entity.isCompleted
            })
        }
    }

    @Test
    fun `upsertLessonProgress converts domain to entity and calls DAO`() = runTest {
        val progress = QaidaLessonProgress(
            lessonId = 3,
            status = LessonStatus.IN_PROGRESS,
            stars = 1,
            lastCellId = 8,
            completedCells = 4,
            totalCells = 12,
            updatedAt = now
        )

        repository.upsertLessonProgress(progress)

        coVerify {
            dao.upsertLessonProgress(match<QaidaLessonProgressEntity> { entity ->
                entity.lessonId == 3 &&
                    entity.status == "IN_PROGRESS" &&
                    entity.stars == 1 &&
                    entity.completedCells == 4 &&
                    entity.totalCells == 12
            })
        }
    }
}
