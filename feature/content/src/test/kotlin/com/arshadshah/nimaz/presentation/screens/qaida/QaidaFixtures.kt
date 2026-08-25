package com.arshadshah.nimaz.presentation.screens.qaida

import com.arshadshah.nimaz.domain.model.LessonStatus
import com.arshadshah.nimaz.domain.model.LineType
import com.arshadshah.nimaz.domain.model.MakhrajArea
import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaCourseProgress
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLessonState
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.domain.model.QaidaLine
import com.arshadshah.nimaz.domain.model.QaidaLineContent
import com.arshadshah.nimaz.domain.model.TokenType

/** The Qaida course, as the screens receive it. */
internal fun qaidaLesson(
    id: Int = 1,
    titleEnglish: String = "The Arabic Letters",
    lessonNumber: Int = id,
) = QaidaLesson(
    id = id,
    lessonNumber = lessonNumber,
    titleEnglish = titleEnglish,
    titleArabic = "الحروف",
    titleTransliteration = "Al-Huruf",
    description = "Recognise each letter on its own",
    conceptTags = listOf("letters"),
    icon = "alif",
    displayOrder = id,
)

internal fun qaidaLessonState(
    id: Int = 1,
    titleEnglish: String = "The Arabic Letters",
    status: LessonStatus = LessonStatus.IN_PROGRESS,
    stars: Int = 0,
    completedCells: Int = 0,
    totalCells: Int = 10,
) = QaidaLessonState(
    lesson = qaidaLesson(id = id, titleEnglish = titleEnglish),
    status = status,
    stars = stars,
    completedCells = completedCells,
    totalCells = totalCells,
    completionFraction = if (totalCells == 0) 0f else completedCells.toFloat() / totalCells,
    lastCellId = null,
)

internal fun qaidaCourse(
    lessons: List<QaidaLessonState> = listOf(qaidaLessonState()),
    completedLessons: Int = 0,
    totalStars: Int = 0,
    nextLessonId: Int? = lessons.firstOrNull()?.lesson?.id,
) = QaidaCourseProgress(
    lessons = lessons,
    completedLessons = completedLessons,
    totalLessons = lessons.size,
    totalStars = totalStars,
    maxStars = lessons.size * 3,
    totalCellsHeard = lessons.sumOf { it.completedCells },
    overallFraction = if (lessons.isEmpty()) 0f else completedLessons.toFloat() / lessons.size,
    nextLessonId = nextLessonId,
)

internal fun qaidaCell(
    id: Int = 1,
    lineId: Int = 1,
    lessonId: Int = 1,
    position: Int = id,
    textArabic: String = "ا",
    transliteration: String = "a",
) = QaidaCell(
    id = id,
    lineId = lineId,
    lessonId = lessonId,
    position = position,
    textArabic = textArabic,
    transliteration = transliteration,
    tokenType = TokenType.LETTER,
    audioKey = "cell_$id",
    audioPath = "qaida/cell_$id.mp3",
    highlightGroup = null,
    letterId = null,
    notes = null,
)

internal fun qaidaLessonContent(
    lessonId: Int = 1,
    titleEnglish: String = "The Arabic Letters",
    cells: List<QaidaCell> = listOf(
        qaidaCell(id = 1, lessonId = lessonId, textArabic = "ا", transliteration = "alif"),
        qaidaCell(id = 2, lessonId = lessonId, textArabic = "ب", transliteration = "ba"),
    ),
    instructionEnglish: String? = "Tap each letter to hear it",
) = QaidaLessonContent(
    lesson = qaidaLesson(id = lessonId, titleEnglish = titleEnglish),
    lines = listOf(
        QaidaLineContent(
            line = QaidaLine(
                id = 1,
                lessonId = lessonId,
                lineNumber = 1,
                lineType = LineType.EXAMPLE,
                instructionEnglish = instructionEnglish,
                instructionArabic = null,
                displayOrder = 1,
            ),
            cells = cells,
        ),
    ),
)

internal fun qaidaLetter(
    id: Int = 1,
    letterArabic: String = "ا",
    nameTransliteration: String = "Alif",
    nameArabic: String = "ألف",
    isConnecting: Boolean = false,
    phoneticHint: String? = "Like the 'a' in father",
) = QaidaLetter(
    id = id,
    letterArabic = letterArabic,
    nameArabic = nameArabic,
    nameTransliteration = nameTransliteration,
    isolatedForm = letterArabic,
    initialForm = if (isConnecting) letterArabic else null,
    medialForm = if (isConnecting) letterArabic else null,
    finalForm = letterArabic,
    isConnecting = isConnecting,
    makhrajArea = MakhrajArea.JAWF,
    makhrajDetail = "From the empty space of the mouth",
    phoneticHint = phoneticHint,
    audioKey = "letter_$id",
    audioPath = "qaida/letter_$id.mp3",
    displayOrder = id,
)
