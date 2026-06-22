package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    val exportAnnotations: ExportAnnotationsUseCase,
    val getTafseerNotes: GetTafseerNotesUseCase
)

/**
 * All saved tafseer notes (highlights that have a note), resolved to their
 * surah/ayah so the list can deep-link into the reader. Newest first (the DAO
 * orders by created_at DESC).
 */
class GetTafseerNotesUseCase @Inject constructor(
    private val tafseerRepository: TafseerRepository,
    private val quranRepository: QuranRepository
) {
    operator fun invoke(): Flow<List<TafseerNoteItem>> =
        tafseerRepository.getAllHighlights().map { highlights ->
            highlights
                .filter { !it.note.isNullOrBlank() }
                .mapNotNull { highlight ->
                    val ayah = quranRepository.getAyahById(highlight.ayahId)
                        ?: return@mapNotNull null
                    TafseerNoteItem(
                        highlightId = highlight.id,
                        surahNumber = ayah.surahNumber,
                        ayahNumber = ayah.ayahNumber,
                        sourceLabel = TafseerSource.entries
                            .firstOrNull { it.id == highlight.tafseerId }?.displayName
                            ?: highlight.tafseerId,
                        color = highlight.color,
                        note = highlight.note.orEmpty()
                    )
                }
        }
}

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
