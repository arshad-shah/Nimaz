package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerText
import kotlinx.coroutines.flow.Flow

interface TafseerRepository {
    // Tafseer text
    suspend fun getTafseerForAyah(ayahId: Int, tafseerId: String): TafseerText?
    fun getTafseerForSurah(surahNumber: Int, tafseerId: String): Flow<List<TafseerText>>

    // Highlights
    fun getHighlightsForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerHighlight>>
    suspend fun addHighlight(ayahId: Int, tafseerId: String, startOffset: Int, endOffset: Int, color: String, note: String? = null): Long
    suspend fun updateHighlight(highlight: TafseerHighlight)
    suspend fun deleteHighlight(highlightId: Long)

    // Notes
    fun getNotesForAyah(ayahId: Int, tafseerId: String): Flow<List<TafseerNote>>
    suspend fun addNote(ayahId: Int, tafseerId: String, text: String): Long
    suspend fun updateNote(note: TafseerNote)
    suspend fun deleteNote(noteId: Long)

    // Export
    suspend fun exportAnnotations(): String
}
