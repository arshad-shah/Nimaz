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

    /** Clears a failed note-write message once its snackbar has been shown. */
    data object DismissNoteError : TafseerEvent

    /** A tafseer PDF export threw. Reported, never shown: the commentary is still readable. */
    data class ExportFailed(val throwable: Throwable) : TafseerEvent

    /**
     * A tafseer PDF finished rendering, in [durationMs]. Measured by the screen, which is where
     * the render runs, and reported through the ViewModel, which is where the seam is.
     */
    data class ExportCompleted(val durationMs: Long) : TafseerEvent
}
