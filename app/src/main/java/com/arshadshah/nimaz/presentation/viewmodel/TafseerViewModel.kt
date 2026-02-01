package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.repository.QuranRepository
import com.arshadshah.nimaz.domain.repository.TafseerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TafseerUiState(
    val surahNumber: Int = 1,
    val ayahs: List<Ayah> = emptyList(),
    val currentAyahIndex: Int = 0,
    val selectedSource: TafseerSource = TafseerSource.IBN_KATHIR,
    val currentTafseer: TafseerText? = null,
    val highlights: List<TafseerHighlight> = emptyList(),
    val notes: List<TafseerNote> = emptyList(),
    val surahName: String = "",
    val isLoading: Boolean = true,
    val exportedText: String? = null
)

sealed interface TafseerEvent {
    data class LoadSurah(val surahNumber: Int, val ayahNumber: Int = 1) : TafseerEvent
    data class NavigateToAyah(val index: Int) : TafseerEvent
    data class SwitchSource(val source: TafseerSource) : TafseerEvent
    data class AddHighlight(
        val startOffset: Int,
        val endOffset: Int,
        val color: String
    ) : TafseerEvent
    data class DeleteHighlight(val highlightId: Long) : TafseerEvent
    data class UpdateHighlightNote(val highlightId: Long, val note: String?) : TafseerEvent
    data class AddNote(val text: String) : TafseerEvent
    data class UpdateNote(val note: TafseerNote) : TafseerEvent
    data class DeleteNote(val noteId: Long) : TafseerEvent
    data object ExportAnnotations : TafseerEvent
    data object ClearExport : TafseerEvent
}

@HiltViewModel
class TafseerViewModel @Inject constructor(
    private val tafseerRepository: TafseerRepository,
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TafseerUiState())
    val state: StateFlow<TafseerUiState> = _state.asStateFlow()

    fun onEvent(event: TafseerEvent) {
        when (event) {
            is TafseerEvent.LoadSurah -> loadSurah(event.surahNumber, event.ayahNumber)
            is TafseerEvent.NavigateToAyah -> onAyahChanged(event.index)
            is TafseerEvent.SwitchSource -> switchSource(event.source)
            is TafseerEvent.AddHighlight -> addHighlight(event.startOffset, event.endOffset, event.color)
            is TafseerEvent.DeleteHighlight -> deleteHighlight(event.highlightId)
            is TafseerEvent.UpdateHighlightNote -> updateHighlightNote(event.highlightId, event.note)
            is TafseerEvent.AddNote -> addNote(event.text)
            is TafseerEvent.UpdateNote -> updateNote(event.note)
            is TafseerEvent.DeleteNote -> deleteNote(event.noteId)
            is TafseerEvent.ExportAnnotations -> exportAnnotations()
            is TafseerEvent.ClearExport -> _state.value = _state.value.copy(exportedText = null)
        }
    }

    private fun loadSurah(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, surahNumber = surahNumber)

            val surah = quranRepository.getSurahByNumber(surahNumber)
            val ayahs = quranRepository.getAyahsBySurah(surahNumber).first()

            val initialIndex = ayahs.indexOfFirst { it.ayahNumber == ayahNumber }
                .coerceAtLeast(0)

            _state.value = _state.value.copy(
                ayahs = ayahs,
                currentAyahIndex = initialIndex,
                surahName = surah?.nameEnglish ?: "Surah $surahNumber",
                isLoading = false
            )

            loadTafseerForCurrentAyah()
        }
    }

    private fun onAyahChanged(index: Int) {
        val ayahs = _state.value.ayahs
        if (index < 0 || index >= ayahs.size) return
        _state.value = _state.value.copy(currentAyahIndex = index)
        loadTafseerForCurrentAyah()
    }

    private fun switchSource(source: TafseerSource) {
        _state.value = _state.value.copy(selectedSource = source)
        loadTafseerForCurrentAyah()
    }

    private fun loadTafseerForCurrentAyah() {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        val tafseerId = currentState.selectedSource.id

        viewModelScope.launch {
            val tafseer = tafseerRepository.getTafseerForAyah(ayah.id, tafseerId)
            _state.value = _state.value.copy(currentTafseer = tafseer)
        }

        viewModelScope.launch {
            tafseerRepository.getHighlightsForAyah(ayah.id, tafseerId).collectLatest { highlights ->
                _state.value = _state.value.copy(highlights = highlights)
            }
        }

        viewModelScope.launch {
            tafseerRepository.getNotesForAyah(ayah.id, tafseerId).collectLatest { notes ->
                _state.value = _state.value.copy(notes = notes)
            }
        }
    }

    private fun addHighlight(startOffset: Int, endOffset: Int, color: String) {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        viewModelScope.launch {
            tafseerRepository.addHighlight(
                ayahId = ayah.id,
                tafseerId = currentState.selectedSource.id,
                startOffset = startOffset,
                endOffset = endOffset,
                color = color
            )
        }
    }

    private fun deleteHighlight(highlightId: Long) {
        viewModelScope.launch {
            tafseerRepository.deleteHighlight(highlightId)
        }
    }

    private fun updateHighlightNote(highlightId: Long, note: String?) {
        val highlight = _state.value.highlights.find { it.id == highlightId } ?: return
        viewModelScope.launch {
            tafseerRepository.updateHighlight(highlight.copy(note = note, updatedAt = System.currentTimeMillis()))
        }
    }

    private fun addNote(text: String) {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        viewModelScope.launch {
            tafseerRepository.addNote(
                ayahId = ayah.id,
                tafseerId = currentState.selectedSource.id,
                text = text
            )
        }
    }

    private fun updateNote(note: TafseerNote) {
        viewModelScope.launch {
            tafseerRepository.updateNote(note)
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            tafseerRepository.deleteNote(noteId)
        }
    }

    private fun exportAnnotations() {
        viewModelScope.launch {
            val exported = tafseerRepository.exportAnnotations()
            _state.value = _state.value.copy(exportedText = exported)
        }
    }
}
