package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QaidaDaoTest {

    private lateinit var db: NimazDatabase
    private lateinit var dao: QaidaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.qaidaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun lesson(id: Int, order: Int = id) = QaidaLessonEntity(
        id = id,
        lessonNumber = id,
        titleEnglish = "Lesson $id",
        titleArabic = "درس $id",
        titleTransliteration = "Dars $id",
        description = "desc $id",
        conceptTags = "[\"tag\"]",
        icon = "🔤",
        displayOrder = order
    )

    private fun letter(id: Int) = QaidaLetterEntity(
        id = id,
        letterArabic = "ب",
        nameArabic = "بَاء",
        nameTransliteration = "baa",
        isolatedForm = "ب",
        initialForm = "بـ",
        medialForm = "ـبـ",
        finalForm = "ـب",
        isConnecting = true,
        makhrajArea = "SHAFATAIN",
        makhrajDetail = "The inner part of both lips.",
        phoneticHint = "like 'b'",
        audioKey = "letter_ba_$id",
        displayOrder = id
    )

    private fun line(id: Int, lessonId: Int, order: Int = id) = QaidaLineEntity(
        id = id,
        lessonId = lessonId,
        lineNumber = order,
        lineType = "PRACTICE",
        instructionEnglish = null,
        instructionArabic = null,
        displayOrder = order
    )

    private fun cell(id: Int, lineId: Int, lessonId: Int, position: Int, letterId: Int?) =
        QaidaCellEntity(
            id = id,
            lineId = lineId,
            lessonId = lessonId,
            position = position,
            textArabic = "ب",
            transliteration = "baa",
            tokenType = "LETTER",
            audioKey = "letter_ba_$id",
            highlightGroup = null,
            letterId = letterId,
            notes = null
        )

    @Test
    fun getAllLessons_returnsContentOrderedByDisplayOrder() = runTest {
        dao.insertLessons(listOf(lesson(2, order = 2), lesson(1, order = 1)))

        val lessons = dao.getAllLessons().first()

        assertThat(lessons.map { it.id }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun getLessonWithLinesAndCells_buildsFullHierarchy() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        dao.insertLetters(listOf(letter(1)))
        dao.insertLines(listOf(line(101, lessonId = 1), line(102, lessonId = 1, order = 2)))
        dao.insertCells(
            listOf(
                cell(1001, lineId = 101, lessonId = 1, position = 1, letterId = 1),
                cell(1002, lineId = 101, lessonId = 1, position = 2, letterId = null),
                cell(1003, lineId = 102, lessonId = 1, position = 1, letterId = 1),
            )
        )

        val full = dao.getLessonWithLinesAndCells(1).first()

        assertThat(full).isNotNull()
        assertThat(full!!.lesson.id).isEqualTo(1)
        assertThat(full.lines).hasSize(2)
        val allCells = full.lines.flatMap { it.cells }
        assertThat(allCells.map { it.id }).containsExactly(1001, 1002, 1003)
    }

    @Test
    fun getCellsForLesson_isOrderedByLineThenPosition() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        dao.insertLines(listOf(line(101, lessonId = 1), line(102, lessonId = 1, order = 2)))
        dao.insertCells(
            listOf(
                cell(1003, lineId = 102, lessonId = 1, position = 1, letterId = null),
                cell(1002, lineId = 101, lessonId = 1, position = 2, letterId = null),
                cell(1001, lineId = 101, lessonId = 1, position = 1, letterId = null),
            )
        )

        val cells = dao.getCellsForLesson(1).first()

        assertThat(cells.map { it.id }).containsExactly(1001, 1002, 1003).inOrder()
    }

    @Test
    fun getCell_andGetLetter_returnSingleRows() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        dao.insertLetters(listOf(letter(7)))
        dao.insertLines(listOf(line(101, lessonId = 1)))
        dao.insertCells(listOf(cell(1001, lineId = 101, lessonId = 1, position = 1, letterId = 7)))

        assertThat(dao.getCell(1001)?.letterId).isEqualTo(7)
        assertThat(dao.getCell(9999)).isNull()
        assertThat(dao.getLetter(7)?.nameTransliteration).isEqualTo("baa")
        assertThat(dao.getLetter(123)).isNull()
    }

    @Test
    fun upsertLessonProgress_insertsThenUpdates() = runTest {
        val initial = QaidaLessonProgressEntity(
            lessonId = 1,
            status = "IN_PROGRESS",
            stars = 0,
            lastCellId = null,
            completedCells = 2,
            totalCells = 10,
            updatedAt = 100L
        )
        dao.upsertLessonProgress(initial)
        assertThat(dao.getLessonProgress(1)).isEqualTo(initial)

        val updated = initial.copy(
            status = "COMPLETED",
            stars = 3,
            lastCellId = 1009,
            completedCells = 10,
            updatedAt = 200L
        )
        dao.upsertLessonProgress(updated)

        val stored = dao.getLessonProgress(1)
        assertThat(stored).isEqualTo(updated)
        assertThat(dao.getAllProgress().first()).hasSize(1)
    }

    @Test
    fun upsertCellProgress_replacesOnCompositeKey() = runTest {
        dao.upsertCellProgress(
            QaidaCellProgressEntity(
                lessonId = 1, cellId = 1001, heardCount = 1,
                isCompleted = false, lastPracticedAt = 100L
            )
        )
        dao.upsertCellProgress(
            QaidaCellProgressEntity(
                lessonId = 1, cellId = 1001, heardCount = 3,
                isCompleted = true, lastPracticedAt = 300L
            )
        )

        val progress = dao.getCellProgressForLesson(1).first()
        assertThat(progress).hasSize(1)
        assertThat(progress.first().heardCount).isEqualTo(3)
        assertThat(progress.first().isCompleted).isTrue()
        assertThat(dao.getCellProgress(1, 1001)?.heardCount).isEqualTo(3)
    }

    @Test
    fun deleteAllUserData_clearsProgressButKeepsContent() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        dao.upsertLessonProgress(
            QaidaLessonProgressEntity(
                lessonId = 1, status = "UNLOCKED", stars = 1, lastCellId = null,
                completedCells = 0, totalCells = 5, updatedAt = 1L
            )
        )
        dao.upsertCellProgress(
            QaidaCellProgressEntity(
                lessonId = 1, cellId = 1001, heardCount = 1,
                isCompleted = false, lastPracticedAt = 1L
            )
        )

        dao.deleteAllUserData()

        assertThat(dao.getAllProgress().first()).isEmpty()
        assertThat(dao.getCellProgressForLesson(1).first()).isEmpty()
        assertThat(dao.getAllLessons().first()).hasSize(1)
    }

    @Test
    fun deletingLine_cascadesToCells_andDeletingLetter_nullsCellLetterId() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        dao.insertLetters(listOf(letter(1)))
        dao.insertLines(listOf(line(101, lessonId = 1)))
        dao.insertCells(listOf(cell(1001, lineId = 101, lessonId = 1, position = 1, letterId = 1)))

        // Deleting the referenced letter sets the cell's letter_id to NULL (SET_NULL).
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")
        db.openHelper.writableDatabase.execSQL("DELETE FROM qaida_letters WHERE id = 1")
        assertThat(dao.getCell(1001)?.letterId).isNull()

        // Deleting the parent line cascades to its cells.
        db.openHelper.writableDatabase.execSQL("DELETE FROM qaida_lines WHERE id = 101")
        assertThat(dao.getCell(1001)).isNull()
    }
}
