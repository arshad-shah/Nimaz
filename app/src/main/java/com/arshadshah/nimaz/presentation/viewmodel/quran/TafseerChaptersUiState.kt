package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.presentation.viewmodel.UiError

/**
 * Immutable UI state for the Tafseer chapters page — a surah picker plus the
 * "My notes" tab listing the user's annotated tafseer.
 */
data class TafseerChaptersUiState(
    val surahs: List<Surah> = emptyList(),
    val notes: List<TafseerNoteItem> = emptyList(),
    val isLoading: Boolean = true,
    /**
     * Set when the surah list or the notes fail to load. Without it a content-database
     * failure rendered as an empty picker with the spinner turned off — indistinguishable
     * from "you have no notes yet".
     */
    val error: UiError? = null,
)
