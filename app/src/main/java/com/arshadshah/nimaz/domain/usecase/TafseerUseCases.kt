package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class TafseerUseCases(
    val getTafseerForAyah: GetTafseerForAyahUseCase,
    val getHighlightsForAyah: GetHighlightsForAyahUseCase,
    val addHighlight: AddHighlightUseCase,
    val updateHighlight: UpdateHighlightUseCase,
    val deleteHighlight: DeleteHighlightUseCase,
    val getNotesForAyah: GetNotesForAyahUseCase,
    val addNote: AddNoteUseCase,
    val updateNote: UpdateNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val exportAnnotations: ExportAnnotationsUseCase
)

class GetTafseerForAyahUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(ayahId: Int, tafseerId: String): TafseerText? =
        repository.getTafseerForAyah(ayahId, tafseerId)
}

class GetHighlightsForAyahUseCase @Inject constructor(private val repository: TafseerRepository) {
    operator fun invoke(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlight>> =
        repository.getHighlightsForAyah(ayahId, tafseerId)
}

class AddHighlightUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(
        ayahId: Int,
        tafseerId: String,
        startOffset: Int,
        endOffset: Int,
        color: String,
        note: String? = null
    ): Long = repository.addHighlight(ayahId, tafseerId, startOffset, endOffset, color, note)
}

class UpdateHighlightUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(highlight: TafseerHighlight) = repository.updateHighlight(highlight)
}

class DeleteHighlightUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(highlightId: Long) = repository.deleteHighlight(highlightId)
}

class GetNotesForAyahUseCase @Inject constructor(private val repository: TafseerRepository) {
    operator fun invoke(ayahId: Int, tafseerId: String): Flow<List<TafseerNote>> =
        repository.getNotesForAyah(ayahId, tafseerId)
}

class AddNoteUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(ayahId: Int, tafseerId: String, text: String): Long =
        repository.addNote(ayahId, tafseerId, text)
}

class UpdateNoteUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(note: TafseerNote) = repository.updateNote(note)
}

class DeleteNoteUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(noteId: Long) = repository.deleteNote(noteId)
}

class ExportAnnotationsUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(): String = repository.exportAnnotations()
}
