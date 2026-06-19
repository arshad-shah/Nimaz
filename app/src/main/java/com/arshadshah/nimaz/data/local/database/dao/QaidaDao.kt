package com.arshadshah.nimaz.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonWithContent
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QaidaDao {

    // ── Lessons ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM qaida_lessons ORDER BY display_order ASC")
    fun getAllLessons(): Flow<List<QaidaLessonEntity>>

    @Query("SELECT * FROM qaida_lessons WHERE id = :lessonId")
    suspend fun getLesson(lessonId: Int): QaidaLessonEntity?

    @Query("SELECT COUNT(*) FROM qaida_lessons")
    suspend fun lessonCount(): Int

    /**
     * The full lesson page: lesson → lines → cells. Use @Transaction so the
     * three reads stay consistent. Reactive so the UI updates if content or
     * (in tests) seeding changes.
     */
    @Transaction
    @Query("SELECT * FROM qaida_lessons WHERE id = :lessonId")
    fun getLessonWithLinesAndCells(lessonId: Int): Flow<QaidaLessonWithContent?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<QaidaLessonEntity>)

    // ── Lines ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM qaida_lines WHERE lesson_id = :lessonId ORDER BY display_order ASC")
    fun getLinesForLesson(lessonId: Int): Flow<List<QaidaLineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<QaidaLineEntity>)

    // ── Letters ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM qaida_letters ORDER BY display_order ASC")
    fun getAllLetters(): Flow<List<QaidaLetterEntity>>

    @Query("SELECT * FROM qaida_letters WHERE id = :letterId")
    suspend fun getLetter(letterId: Int): QaidaLetterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetters(letters: List<QaidaLetterEntity>)

    // ── Cells ─────────────────────────────────────────────────────────────
    @Query("SELECT * FROM qaida_cells WHERE id = :cellId")
    suspend fun getCell(cellId: Int): QaidaCellEntity?

    @Query("SELECT * FROM qaida_cells WHERE lesson_id = :lessonId ORDER BY line_id ASC, position ASC")
    fun getCellsForLesson(lessonId: Int): Flow<List<QaidaCellEntity>>

    @Query("SELECT * FROM qaida_cells WHERE line_id = :lineId ORDER BY position ASC")
    suspend fun getCellsForLine(lineId: Int): List<QaidaCellEntity>

    @Query("SELECT COUNT(*) FROM qaida_cells WHERE lesson_id = :lessonId")
    suspend fun countCellsForLesson(lessonId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<QaidaCellEntity>)

    // ── Content seeding ───────────────────────────────────────────────────
    @Query("DELETE FROM qaida_cells")
    suspend fun deleteAllCells()

    @Query("DELETE FROM qaida_lines")
    suspend fun deleteAllLines()

    @Query("DELETE FROM qaida_letters")
    suspend fun deleteAllLetters()

    @Query("DELETE FROM qaida_lessons")
    suspend fun deleteAllLessons()

    /**
     * Atomically replaces the four Qaida content tables. Used by the content
     * seeder so an interrupted refresh never leaves the content half-populated.
     *
     * Rows are deleted children-first and inserted parents-first to respect the
     * foreign keys (cells → lines → lessons, cells → letters). The progress
     * tables have no foreign key into the content tables, so this never touches
     * the user's learning progress.
     */
    @Transaction
    suspend fun replaceAllContent(
        lessons: List<QaidaLessonEntity>,
        letters: List<QaidaLetterEntity>,
        lines: List<QaidaLineEntity>,
        cells: List<QaidaCellEntity>
    ) {
        deleteAllCells()
        deleteAllLines()
        deleteAllLetters()
        deleteAllLessons()
        insertLessons(lessons)
        insertLetters(letters)
        insertLines(lines)
        insertCells(cells)
    }

    // ── Lesson progress (user data) ───────────────────────────────────────
    @Query("SELECT * FROM qaida_lesson_progress WHERE lesson_id = :lessonId")
    suspend fun getLessonProgress(lessonId: Int): QaidaLessonProgressEntity?

    @Query("SELECT * FROM qaida_lesson_progress WHERE lesson_id = :lessonId")
    fun observeLessonProgress(lessonId: Int): Flow<QaidaLessonProgressEntity?>

    @Query("SELECT * FROM qaida_lesson_progress ORDER BY lesson_id ASC")
    fun getAllProgress(): Flow<List<QaidaLessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: QaidaLessonProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: List<QaidaLessonProgressEntity>)

    @Query("DELETE FROM qaida_lesson_progress")
    suspend fun deleteAllLessonProgress()

    // ── Cell progress (optional fine-grained user data) ───────────────────
    @Query("SELECT * FROM qaida_cell_progress WHERE lesson_id = :lessonId AND cell_id = :cellId")
    suspend fun getCellProgress(lessonId: Int, cellId: Int): QaidaCellProgressEntity?

    @Query("SELECT * FROM qaida_cell_progress WHERE lesson_id = :lessonId")
    fun getCellProgressForLesson(lessonId: Int): Flow<List<QaidaCellProgressEntity>>

    @Query("SELECT COUNT(*) FROM qaida_cell_progress WHERE lesson_id = :lessonId AND is_completed = 1")
    suspend fun countCompletedCells(lessonId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCellProgress(progress: QaidaCellProgressEntity)

    @Query("DELETE FROM qaida_cell_progress")
    suspend fun deleteAllCellProgress()

    @Transaction
    suspend fun deleteAllUserData() {
        deleteAllLessonProgress()
        deleteAllCellProgress()
    }
}
