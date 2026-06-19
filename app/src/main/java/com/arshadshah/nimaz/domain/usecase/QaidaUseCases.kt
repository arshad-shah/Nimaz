package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.QaidaCell
import com.arshadshah.nimaz.domain.model.QaidaLesson
import com.arshadshah.nimaz.domain.model.QaidaLessonContent
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.domain.repository.QaidaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetQaidaLessonsUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    operator fun invoke(): Flow<List<QaidaLesson>> = repository.getLessons()
}

class GetQaidaLessonContentUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    operator fun invoke(lessonId: Int): Flow<QaidaLessonContent?> =
        repository.getLessonContent(lessonId)
}

class GetQaidaLettersUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    operator fun invoke(): Flow<List<QaidaLetter>> = repository.getLetters()
}

class GetQaidaCellUseCase @Inject constructor(
    private val repository: QaidaRepository
) {
    suspend operator fun invoke(cellId: Int): QaidaCell? = repository.getCell(cellId)
}

// Wrapper class for all Qaida use cases
data class QaidaUseCases(
    val getLessons: GetQaidaLessonsUseCase,
    val getLessonContent: GetQaidaLessonContentUseCase,
    val getLetters: GetQaidaLettersUseCase,
    val getCell: GetQaidaCellUseCase
)
