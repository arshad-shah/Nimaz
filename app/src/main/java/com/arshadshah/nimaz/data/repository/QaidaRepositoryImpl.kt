package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.util.mapItems
import com.arshadshah.nimaz.data.local.user.ProgressDao
import com.arshadshah.nimaz.data.local.user.ProgressEntity
import com.arshadshah.nimaz.data.local.user.ProgressKind
import kotlinx.coroutines.flow.first
import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonWithContent
import com.arshadshah.nimaz.data.local.database.entity.QaidaLetterEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLineEntity
import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCellProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonProgress
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.TokenType
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QaidaRepositoryImpl @Inject constructor(
    private val qaidaDao: QaidaDao,
    private val progressDao: ProgressDao
) : QaidaRepository {

    // ── Lessons ───────────────────────────────────────────────────────────
    override fun getLessons(): Flow<List<QaidaLesson>> {
        return qaidaDao.getAllLessons().mapItems { it.toDomain() }
    }

    override suspend fun getLesson(lessonId: Int): QaidaLesson? {
        return qaidaDao.getLesson(lessonId)?.toDomain()
    }

    override fun getLessonContent(lessonId: Int): Flow<QaidaLessonContent?> {
        return qaidaDao.getLessonWithLinesAndCells(lessonId).map { it?.toDomain() }
    }

    // ── Letters ───────────────────────────────────────────────────────────
    override fun getLetters(): Flow<List<QaidaLetter>> {
        return qaidaDao.getAllLetters().mapItems { it.toDomain() }
    }

    override suspend fun getLetter(letterId: Int): QaidaLetter? {
        return qaidaDao.getLetter(letterId)?.toDomain()
    }

    // ── Cells ─────────────────────────────────────────────────────────────
    override suspend fun getCell(cellId: Int): QaidaCell? {
        return qaidaDao.getCell(cellId)?.toDomain()
    }

    override fun getCellsForLesson(lessonId: Int): Flow<List<QaidaCell>> {
        return qaidaDao.getCellsForLesson(lessonId)
            .mapItems { it.toDomain() }
    }

    override suspend fun getCellCountForLesson(lessonId: Int): Int {
        return qaidaDao.countCellsForLesson(lessonId)
    }

    // ── Lesson progress ─────────────────────────────────────────────────────
    override fun getAllProgress(): Flow<List<QaidaLessonProgress>> {
        return progressDao.ofKind(ProgressKind.QAIDA_LESSON).mapItems { it.toLessonProgress() }
    }

    override fun observeLessonProgress(lessonId: Int): Flow<QaidaLessonProgress?> {
        return progressDao.ofKind(ProgressKind.QAIDA_LESSON)
            .map { rows -> rows.firstOrNull { it.targetId == lessonId }?.toLessonProgress() }
    }

    override suspend fun getLessonProgress(lessonId: Int): QaidaLessonProgress? {
        return progressDao.find(ProgressKind.QAIDA_LESSON, lessonId)?.toLessonProgress()
    }

    override suspend fun upsertLessonProgress(progress: QaidaLessonProgress) {
        progressDao.upsert(progress.toProgressEntity())
    }

    // ── Cell progress ───────────────────────────────────────────────────────
    override suspend fun getCellProgress(lessonId: Int, cellId: Int): QaidaCellProgress? {
        return progressDao.find(ProgressKind.QAIDA_CELL, cellId)?.toCellProgress()
    }

    override fun observeCellProgressForLesson(lessonId: Int): Flow<List<QaidaCellProgress>> {
        return progressDao.inContext(ProgressKind.QAIDA_CELL, lessonId)
            .mapItems { it.toCellProgress() }
    }

    override suspend fun getCompletedCellCount(lessonId: Int): Int {
        return progressDao.inContext(ProgressKind.QAIDA_CELL, lessonId).first()
            .count { it.isCompleted }
    }

    override suspend fun upsertCellProgress(progress: QaidaCellProgress) {
        progressDao.upsert(progress.toProgressEntity())
    }

    override suspend fun resetProgress() {
        // Both kinds, and only the learner's rows — the lessons themselves are content.
        progressDao.deleteKind(ProgressKind.QAIDA_LESSON)
        progressDao.deleteKind(ProgressKind.QAIDA_CELL)
    }

    // ── Mapping ───────────────────────────────────────────────────────────

    /**
     * Resolves a bundled `audio_key` to a playable asset URI. Audio clips ship
     * inside the APK under `assets/qaida/audio/` (sub-issue B's delivery
     * decision), so the key maps directly to an `android_asset` file URI.
     */
    private fun resolveAudioPath(audioKey: String): String =
        "$ASSET_AUDIO_URI_PREFIX$audioKey.mp3"

    private fun parseJsonArray(json: String): List<String> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            emptyList()
        }
    }

    private fun QaidaLessonEntity.toDomain(): QaidaLesson {
        return QaidaLesson(
            id = id,
            lessonNumber = lessonNumber,
            titleEnglish = titleEnglish,
            titleArabic = titleArabic,
            titleTransliteration = titleTransliteration,
            description = description,
            conceptTags = parseJsonArray(conceptTags),
            icon = icon,
            displayOrder = displayOrder
        )
    }

    private fun QaidaLetterEntity.toDomain(): QaidaLetter {
        return QaidaLetter(
            id = id,
            letterArabic = letterArabic,
            nameArabic = nameArabic,
            nameTransliteration = nameTransliteration,
            isolatedForm = isolatedForm,
            initialForm = initialForm,
            medialForm = medialForm,
            finalForm = finalForm,
            isConnecting = isConnecting,
            makhrajArea = MakhrajArea.fromString(makhrajArea),
            makhrajDetail = makhrajDetail,
            phoneticHint = phoneticHint,
            audioKey = audioKey,
            audioPath = resolveAudioPath(audioKey),
            displayOrder = displayOrder
        )
    }

    private fun QaidaLineEntity.toDomain(): QaidaLine {
        return QaidaLine(
            id = id,
            lessonId = lessonId,
            lineNumber = lineNumber,
            lineType = LineType.fromString(lineType),
            instructionEnglish = instructionEnglish,
            instructionArabic = instructionArabic,
            displayOrder = displayOrder
        )
    }

    private fun QaidaCellEntity.toDomain(): QaidaCell {
        return QaidaCell(
            id = id,
            lineId = lineId,
            lessonId = lessonId,
            position = position,
            textArabic = textArabic,
            transliteration = transliteration,
            tokenType = TokenType.fromString(tokenType),
            audioKey = audioKey,
            audioPath = resolveAudioPath(audioKey),
            highlightGroup = highlightGroup,
            letterId = letterId,
            notes = notes
        )
    }

    private fun QaidaLessonWithContent.toDomain(): QaidaLessonContent {
        return QaidaLessonContent(
            lesson = lesson.toDomain(),
            lines = lines
                .sortedBy { it.line.displayOrder }
                .map { lineWithCells ->
                    QaidaLineContent(
                        line = lineWithCells.line.toDomain(),
                        cells = lineWithCells.cells
                            .sortedBy { it.position }
                            .map { it.toDomain() }
                    )
                }
        )
    }

    // A lesson's progress and a cell's are two kinds of row in one table now. `state` carries
    // the lesson's status, `score` its stars, `resume_id` its last cell; a cell's `context_id`
    // is the lesson it belongs to. The domain types are unchanged.

    private fun ProgressEntity.toLessonProgress(): QaidaLessonProgress {
        return QaidaLessonProgress(
            lessonId = targetId,
            status = LessonStatus.fromString(state ?: ""),
            stars = score ?: 0,
            lastCellId = resumeId,
            completedCells = completed,
            totalCells = total ?: 0,
            updatedAt = updatedAt
        )
    }

    private fun QaidaLessonProgress.toProgressEntity(): ProgressEntity {
        return ProgressEntity(
            kind = ProgressKind.QAIDA_LESSON,
            targetId = lessonId,
            completed = completedCells,
            total = totalCells,
            isCompleted = status == LessonStatus.COMPLETED,
            state = status.name,
            score = stars,
            resumeId = lastCellId,
            createdAt = updatedAt,
            updatedAt = updatedAt
        )
    }

    private fun ProgressEntity.toCellProgress(): QaidaCellProgress {
        return QaidaCellProgress(
            lessonId = contextId ?: 0,
            cellId = targetId,
            heardCount = completed,
            isCompleted = isCompleted,
            lastPracticedAt = updatedAt
        )
    }

    private fun QaidaCellProgress.toProgressEntity(): ProgressEntity {
        return ProgressEntity(
            kind = ProgressKind.QAIDA_CELL,
            targetId = cellId,
            contextId = lessonId,
            completed = heardCount,
            isCompleted = isCompleted,
            createdAt = lastPracticedAt,
            updatedAt = lastPracticedAt
        )
    }

    companion object {
        private const val ASSET_AUDIO_URI_PREFIX = "file:///android_asset/qaida/audio/"
    }
}
