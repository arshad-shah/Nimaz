package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.data.local.database.dao.QaidaDao
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaCellProgressEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonEntity
import com.arshadshah.nimaz.data.local.database.entity.QaidaLessonProgressEntity
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
import com.arshadshah.nimaz.core.util.mapItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QaidaRepositoryImpl @Inject constructor(
    private val qaidaDao: QaidaDao
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
        return qaidaDao.getAllProgress().mapItems { it.toDomain() }
    }

    override fun observeLessonProgress(lessonId: Int): Flow<QaidaLessonProgress?> {
        return qaidaDao.observeLessonProgress(lessonId).map { it?.toDomain() }
    }

    override suspend fun getLessonProgress(lessonId: Int): QaidaLessonProgress? {
        return qaidaDao.getLessonProgress(lessonId)?.toDomain()
    }

    override suspend fun upsertLessonProgress(progress: QaidaLessonProgress) {
        qaidaDao.upsertLessonProgress(progress.toEntity())
    }

    // ── Cell progress ───────────────────────────────────────────────────────
    override suspend fun getCellProgress(lessonId: Int, cellId: Int): QaidaCellProgress? {
        return qaidaDao.getCellProgress(lessonId, cellId)?.toDomain()
    }

    override fun observeCellProgressForLesson(lessonId: Int): Flow<List<QaidaCellProgress>> {
        return qaidaDao.getCellProgressForLesson(lessonId)
            .mapItems { it.toDomain() }
    }

    override suspend fun getCompletedCellCount(lessonId: Int): Int {
        return qaidaDao.countCompletedCells(lessonId)
    }

    override suspend fun upsertCellProgress(progress: QaidaCellProgress) {
        qaidaDao.upsertCellProgress(progress.toEntity())
    }

    override suspend fun resetProgress() {
        qaidaDao.deleteAllUserData()
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

    private fun QaidaLessonProgressEntity.toDomain(): QaidaLessonProgress {
        return QaidaLessonProgress(
            lessonId = lessonId,
            status = LessonStatus.fromString(status),
            stars = stars,
            lastCellId = lastCellId,
            completedCells = completedCells,
            totalCells = totalCells,
            updatedAt = updatedAt
        )
    }

    private fun QaidaLessonProgress.toEntity(): QaidaLessonProgressEntity {
        return QaidaLessonProgressEntity(
            lessonId = lessonId,
            status = status.name,
            stars = stars,
            lastCellId = lastCellId,
            completedCells = completedCells,
            totalCells = totalCells,
            updatedAt = updatedAt
        )
    }

    private fun QaidaCellProgressEntity.toDomain(): QaidaCellProgress {
        return QaidaCellProgress(
            lessonId = lessonId,
            cellId = cellId,
            heardCount = heardCount,
            isCompleted = isCompleted,
            lastPracticedAt = lastPracticedAt
        )
    }

    private fun QaidaCellProgress.toEntity(): QaidaCellProgressEntity {
        return QaidaCellProgressEntity(
            lessonId = lessonId,
            cellId = cellId,
            heardCount = heardCount,
            isCompleted = isCompleted,
            lastPracticedAt = lastPracticedAt
        )
    }

    companion object {
        private const val ASSET_AUDIO_URI_PREFIX = "file:///android_asset/qaida/audio/"
    }
}
