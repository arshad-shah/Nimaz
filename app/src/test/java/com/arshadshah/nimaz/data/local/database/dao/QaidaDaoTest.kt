package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.data.local.user.NimazUserDatabase
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
import com.arshadshah.nimaz.data.local.user.ProgressKind
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
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
    private lateinit var userDb: NimazUserDatabase
    private lateinit var progress: ProgressDao
    private lateinit var dao: QaidaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.qaidaDao()
        userDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NimazUserDatabase::class.java
        ).allowMainThreadQueries().build()
        progress = userDb.progressDao()
    }

    @After
    fun tearDown() {
        db.close()
        userDb.close()
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

    /**
     * A lesson's progress is a row in the user's `progress` table now, keyed by
     * (kind, target, date) — so an upsert of the same lesson replaces rather than duplicating.
     */
    @Test
    fun lessonProgress_insertsThenUpdates() = runTest {
        val initial = ProgressEntity(
            kind = ProgressKind.QAIDA_LESSON,
            targetId = 1,
            completed = 2,
            total = 10,
            isCompleted = false,
            state = "IN_PROGRESS",
            score = 0,
            createdAt = 100L,
            updatedAt = 100L,
        )
        progress.upsert(initial)
        assertThat(progress.find(ProgressKind.QAIDA_LESSON, 1)).isEqualTo(initial)

        val updated = initial.copy(
            state = "COMPLETED",
            score = 3,
            resumeId = 1009,
            completed = 10,
            isCompleted = true,
            updatedAt = 200L,
        )
        progress.upsert(updated)

        assertThat(progress.find(ProgressKind.QAIDA_LESSON, 1)).isEqualTo(updated)
        assertThat(progress.ofKind(ProgressKind.QAIDA_LESSON).first()).hasSize(1)
    }

    @Test
    fun cellProgress_replacesOnItsKey() = runTest {
        progress.upsert(
            ProgressEntity(
                kind = ProgressKind.QAIDA_CELL, targetId = 1001, contextId = 1,
                completed = 1, isCompleted = false, createdAt = 100L, updatedAt = 100L,
            )
        )
        progress.upsert(
            ProgressEntity(
                kind = ProgressKind.QAIDA_CELL, targetId = 1001, contextId = 1,
                completed = 3, isCompleted = true, createdAt = 100L, updatedAt = 300L,
            )
        )

        val rows = progress.inContext(ProgressKind.QAIDA_CELL, 1).first()
        assertThat(rows).hasSize(1)
        assertThat(rows.first().completed).isEqualTo(3)
        assertThat(rows.first().isCompleted).isTrue()
    }

    /**
     * Clearing a learner's progress cannot reach the lessons: they are in the other database.
     * Before the split this test had to assert that a `deleteAllUserData` on the same database
     * had stopped at the right tables.
     */
    @Test
    fun clearingProgress_keepsTheLessons() = runTest {
        dao.insertLessons(listOf(lesson(1)))
        progress.upsert(
            ProgressEntity(
                kind = ProgressKind.QAIDA_LESSON, targetId = 1, completed = 0, total = 5,
                isCompleted = false, state = "UNLOCKED", score = 1,
                createdAt = 1L, updatedAt = 1L,
            )
        )
        progress.upsert(
            ProgressEntity(
                kind = ProgressKind.QAIDA_CELL, targetId = 1001, contextId = 1,
                completed = 1, isCompleted = false, createdAt = 1L, updatedAt = 1L,
            )
        )

        progress.deleteKind(ProgressKind.QAIDA_LESSON)
        progress.deleteKind(ProgressKind.QAIDA_CELL)

        assertThat(progress.all()).isEmpty()
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
