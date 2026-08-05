package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource

sealed interface TafseerEvent {
    data class LoadSurah(val surahNumber: Int, val ayahNumber: Int = 1) : TafseerEvent
    data class NavigateToAyah(val index: Int) : TafseerEvent
    data class NavigateToTafseerPage(val page: Int) : TafseerEvent
    data class SwitchSource(val source: TafseerSource) : TafseerEvent
    data class AddHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val color: String,
        val note: String? = null
    ) : TafseerEvent

    data class DeleteHighlight(val highlightId: Long) : TafseerEvent

    /** Update an existing highlight's colour and/or note in a single step. */
    data class UpdateHighlight(
        val highlightId: Long,
        val color: String,
        val note: String?
    ) : TafseerEvent

    data class AddNote(val text: String) : TafseerEvent
    data class UpdateNote(val note: TafseerNote) : TafseerEvent
    data class DeleteNote(val noteId: Long) : TafseerEvent
}
