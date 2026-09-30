package com.arshadshah.nimaz.domain.usecase

import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.arshadshah.nimaz.domain.model.toNoteItem
import javax.inject.Inject

data class TafseerUseCases(
    val getTafseerForAyah: GetTafseerForAyahUseCase,
    val getHighlightsForRange: GetHighlightsForRangeUseCase,
    val addHighlight: AddHighlightUseCase,
    val updateHighlight: UpdateHighlightUseCase,
    val deleteHighlight: DeleteHighlightUseCase,
    val getNotesForRange: GetNotesForRangeUseCase,
    val addNote: AddNoteUseCase,
    val updateNote: UpdateNoteUseCase,
    val deleteNote: DeleteNoteUseCase,
    val getTafseerNotes: GetTafseerNotesUseCase
)

/** One index for both standalone reflections and notes attached to highlighted text. */
class GetTafseerNotesUseCase @Inject constructor(
    private val tafseerRepository: TafseerRepository,
    private val quranRepository: QuranRepository
) {
    operator fun invoke(): Flow<List<TafseerNoteItem>> = combine(
        tafseerRepository.getAllHighlights(), tafseerRepository.getAllNotes(),
    ) { highlights, notes ->
        val result = mutableListOf<TafseerNoteItem>()
        val ayahs = (highlights.map { it.ayahId } + notes.map { it.ayahId }).distinct()
            .associateWith { quranRepository.getAyahById(it) }
        // Resolve once per source/verse per emission, including when several notes share it.
        val texts = mutableMapOf<Pair<Int, String>, String?>()
        for (highlight in highlights.filter { !it.note.isNullOrBlank() }) {
            val ayah = ayahs[highlight.ayahId] ?: continue
            val key = highlight.ayahId to highlight.tafseerId
            if (!texts.containsKey(key)) texts[key] = tafseerRepository.getTafseerForAyah(
                ayah.surahNumber, ayah.ayahNumber, highlight.tafseerId,
            )?.text
            result += highlight.toNoteItem(ayah.surahNumber, ayah.ayahNumber, texts[key])
        }
        for (note in notes) {
            val ayah = ayahs[note.ayahId] ?: continue
            result += note.toNoteItem(ayah.surahNumber, ayah.ayahNumber)
        }
        result.sortedByDescending { it.createdAt }
    }
}

/** Shared mutation semantics for the reader and the notes index. */
class TafseerNoteActions @Inject constructor(private val repository: TafseerRepository) {
    suspend fun save(item: TafseerNoteItem, text: String) {
        require(text.isNotBlank())
        item.reflection?.let {
            if (it.id == 0L) repository.addNote(it.ayahId, it.tafseerId, text.trim())
            else repository.updateNote(it.copy(text = text.trim()))
        }
            ?: item.highlight?.let { repository.updateHighlight(it.copy(note = text.trim())) }
    }

    suspend fun delete(item: TafseerNoteItem) {
        item.reflection?.let { repository.deleteNote(it.id) }
            ?: item.highlight?.let { repository.deleteHighlight(it.id) }
    }

    suspend fun restore(item: TafseerNoteItem) {
        item.reflection?.let { repository.restoreNote(it) }
            ?: item.highlight?.let { repository.restoreHighlight(it) }
    }
}

class GetTafseerForAyahUseCase @Inject constructor(private val repository: TafseerRepository) {
    suspend operator fun invoke(surahNumber: Int, ayahNumber: Int, tafseerId: String): TafseerText? =
        repository.getTafseerForAyah(surahNumber, ayahNumber, tafseerId)
}

class GetHighlightsForRangeUseCase @Inject constructor(private val repository: TafseerRepository) {
    operator fun invoke(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerHighlight>> =
        repository.getHighlightsForRange(surahNumber, ayahStart, ayahEnd, tafseerId)
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

class GetNotesForRangeUseCase @Inject constructor(private val repository: TafseerRepository) {
    operator fun invoke(
        surahNumber: Int,
        ayahStart: Int,
        ayahEnd: Int,
        tafseerId: String
    ): Flow<List<TafseerNote>> =
        repository.getNotesForRange(surahNumber, ayahStart, ayahEnd, tafseerId)
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
