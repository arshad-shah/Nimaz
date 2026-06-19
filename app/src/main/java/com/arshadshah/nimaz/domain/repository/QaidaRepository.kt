package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCellProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.model.QaidaLetter
import kotlinx.coroutines.flow.Flow

/**
 * Surfaces the Noorani Qaida content and progress to the presentation layer,
 * fully decoupled from Room (no entity types leak past this interface).
 */
interface QaidaRepository {

    // ── Lessons ───────────────────────────────────────────────────────────
    fun getLessons(): Flow<List<QaidaLesson>>
    suspend fun getLesson(lessonId: Int): QaidaLesson?

    /** The full lesson page: lesson → ordered lines → ordered cells. */
    fun getLessonContent(lessonId: Int): Flow<QaidaLessonContent?>

    // ── Letters ───────────────────────────────────────────────────────────
    fun getLetters(): Flow<List<QaidaLetter>>
    suspend fun getLetter(letterId: Int): QaidaLetter?

    // ── Cells ─────────────────────────────────────────────────────────────
    suspend fun getCell(cellId: Int): QaidaCell?
    fun getCellsForLesson(lessonId: Int): Flow<List<QaidaCell>>

    /** Total number of seeded cells in a lesson (the denominator for progress). */
    suspend fun getCellCountForLesson(lessonId: Int): Int

    // ── Lesson progress ───────────────────────────────────────────────────
    fun getAllProgress(): Flow<List<QaidaLessonProgress>>
    fun observeLessonProgress(lessonId: Int): Flow<QaidaLessonProgress?>
    suspend fun getLessonProgress(lessonId: Int): QaidaLessonProgress?
    suspend fun upsertLessonProgress(progress: QaidaLessonProgress)

    // ── Cell progress ─────────────────────────────────────────────────────
    suspend fun getCellProgress(lessonId: Int, cellId: Int): QaidaCellProgress?
    fun observeCellProgressForLesson(lessonId: Int): Flow<List<QaidaCellProgress>>
    suspend fun getCompletedCellCount(lessonId: Int): Int
    suspend fun upsertCellProgress(progress: QaidaCellProgress)
}
