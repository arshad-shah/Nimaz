package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.domain.model.TafseerText
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    // Hoisted out of the reader composable so it survives an ayah-by-ayah swipe
    // within the same commentary block: it only resets to 0 when the block
    // itself changes, not on every ayah navigation.
    val currentTafseerPage: Int = 0,
    val highlights: List<TafseerHighlight> = emptyList(),
    val notes: List<TafseerNote> = emptyList(),
    val surahName: String = "",
    val isLoading: Boolean = true,
    // Sources whose seed data actually has non-empty text for the current ayah.
    // Used to recommend an alternate source when the selected one has no content.
    val availableSources: Set<TafseerSource> = emptySet()
)

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

@HiltViewModel
class TafseerViewModel @Inject constructor(
    private val tafseerUseCases: TafseerUseCases,
    private val quranUseCases: QuranUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(TafseerUiState())
    val state: StateFlow<TafseerUiState> = _state.asStateFlow()

    /**
     * The in-flight load for the ayah on screen — the tafseer text, the source probe, and the
     * two Room flows carrying that block's highlights and notes.
     *
     * [loadTafseerForCurrentAyah] runs on every swipe and every source switch. Room flows never
     * complete, so without a handle to cancel, each run left its annotation collectors alive:
     * one pair per ayah visited, all writing the same `highlights`/`notes`. Room then re-emits
     * to *every* live collector when the table changes, so annotating one ayah woke the
     * collectors for all the others and the last to land won — the reader showed a different
     * ayah's highlights over the one being read. Cancelling here also settles the source probe,
     * which is two suspend reads per source and could otherwise land after the next ayah's.
     *
     * One handle is right because the reader shows one ayah at a time (contrast the Quran
     * pager, which holds several pages live and needs one handle per page).
     */
    private var ayahAnnotationsJob: Job? = null

    fun onEvent(event: TafseerEvent) {
        when (event) {
            is TafseerEvent.LoadSurah -> AppAnalytics.logFeatureUsed("tafseer", "open_surah")
            is TafseerEvent.SwitchSource -> AppAnalytics.logFeatureUsed("tafseer", "switch_source")
            is TafseerEvent.AddHighlight -> AppAnalytics.logFeatureUsed("tafseer", "add_highlight")
            is TafseerEvent.DeleteHighlight -> AppAnalytics.logFeatureUsed(
                "tafseer",
                "delete_highlight"
            )

            is TafseerEvent.AddNote -> AppAnalytics.logFeatureUsed("tafseer", "add_note")
            is TafseerEvent.DeleteNote -> AppAnalytics.logFeatureUsed("tafseer", "delete_note")
            else -> {}
        }
        when (event) {
            is TafseerEvent.LoadSurah -> loadSurah(event.surahNumber, event.ayahNumber)
            is TafseerEvent.NavigateToAyah -> onAyahChanged(event.index)
            is TafseerEvent.NavigateToTafseerPage ->
                _state.value = _state.value.copy(currentTafseerPage = event.page)

            is TafseerEvent.SwitchSource -> switchSource(event.source)
            is TafseerEvent.AddHighlight -> addHighlight(
                event.startOffset,
                event.endOffset,
                event.color,
                event.note
            )

            is TafseerEvent.DeleteHighlight -> deleteHighlight(event.highlightId)
            is TafseerEvent.UpdateHighlight -> updateHighlight(
                event.highlightId,
                event.color,
                event.note
            )

            is TafseerEvent.AddNote -> addNote(event.text)
            is TafseerEvent.UpdateNote -> updateNote(event.note)
            is TafseerEvent.DeleteNote -> deleteNote(event.noteId)
        }
    }

    private fun loadSurah(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, surahNumber = surahNumber)

            val surah = quranUseCases.getSurahByNumber(surahNumber)
            val ayahs = quranUseCases.getAyahsBySurah(surahNumber).first()

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

        ayahAnnotationsJob?.cancel()
        ayahAnnotationsJob = viewModelScope.launch {
            val tafseer =
                tafseerUseCases.getTafseerForAyah(ayah.surahNumber, ayah.ayahNumber, tafseerId)
            // Probe every source so the UI can suggest an alternate one when the
            // selected source has no content for this ayah (seed coverage is partial).
            val available = TafseerSource.entries.filter { source ->
                tafseerUseCases.getTafseerForAyah(ayah.surahNumber, ayah.ayahNumber, source.id)
                    ?.text?.isNotBlank() == true
            }.toSet()
            // Only reset the reading position when the block itself changed —
            // swiping to the next ayah of the same block should hold position,
            // not reopen the commentary from page 1.
            val sameBlock = tafseer != null && tafseer.id == _state.value.currentTafseer?.id
            _state.value = _state.value.copy(
                currentTafseer = tafseer,
                currentTafseerPage = if (sameBlock) _state.value.currentTafseerPage else 0,
                availableSources = available
            )

            if (tafseer != null) {
                launch {
                    tafseerUseCases.getHighlightsForRange(
                        tafseer.surahNumber, tafseer.ayahStart, tafseer.ayahEnd, tafseerId
                    ).collectLatest { highlights ->
                        _state.value = _state.value.copy(highlights = highlights)
                    }
                }
                launch {
                    tafseerUseCases.getNotesForRange(
                        tafseer.surahNumber, tafseer.ayahStart, tafseer.ayahEnd, tafseerId
                    ).collectLatest { notes ->
                        _state.value = _state.value.copy(notes = notes)
                    }
                }
            } else {
                _state.value = _state.value.copy(highlights = emptyList(), notes = emptyList())
            }
        }
    }

    private fun addHighlight(startOffset: Int, endOffset: Int, color: String, note: String?) {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        viewModelScope.launch {
            tafseerUseCases.addHighlight(
                ayahId = ayah.id,
                tafseerId = currentState.selectedSource.id,
                startOffset = startOffset,
                endOffset = endOffset,
                color = color,
                note = note?.takeIf { it.isNotBlank() }
            )
        }
    }

    private fun deleteHighlight(highlightId: Long) {
        viewModelScope.launch {
            tafseerUseCases.deleteHighlight(highlightId)
        }
    }

    private fun updateHighlight(highlightId: Long, color: String, note: String?) {
        val highlight = _state.value.highlights.find { it.id == highlightId } ?: return
        viewModelScope.launch {
            tafseerUseCases.updateHighlight(
                highlight.copy(
                    color = color,
                    note = note?.takeIf { it.isNotBlank() },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun addNote(text: String) {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        viewModelScope.launch {
            tafseerUseCases.addNote(
                ayahId = ayah.id,
                tafseerId = currentState.selectedSource.id,
                text = text
            )
        }
    }

    private fun updateNote(note: TafseerNote) {
        viewModelScope.launch {
            tafseerUseCases.updateNote(note)
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            tafseerUseCases.deleteNote(noteId)
        }
    }

}
