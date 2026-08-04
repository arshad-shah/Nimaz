package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
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

@HiltViewModel
class TafseerViewModel @Inject constructor(
    private val tafseerUseCases: TafseerUseCases,
    private val quranUseCases: QuranUseCases,
    private val telemetry: Telemetry,
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
            is TafseerEvent.LoadSurah -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.TAFSEER, "open_surah")
                loadSurah(event.surahNumber, event.ayahNumber)
            }
            is TafseerEvent.NavigateToAyah -> onAyahChanged(event.index)
            is TafseerEvent.NavigateToTafseerPage ->
                _state.value = _state.value.copy(currentTafseerPage = event.page)

            is TafseerEvent.SwitchSource -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.TAFSEER, "switch_source")
                switchSource(event.source)
            }
            is TafseerEvent.AddHighlight -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.TAFSEER, "add_highlight")
                addHighlight(
                    event.startOffset,
                    event.endOffset,
                    event.color,
                    event.note
                )
            }

            is TafseerEvent.DeleteHighlight -> {
                AppAnalytics.logFeatureUsed(
                    AppAnalytics.Feature.TAFSEER,
                    "delete_highlight"
                )
                deleteHighlight(event.highlightId)
            }
            is TafseerEvent.UpdateHighlight -> updateHighlight(
                event.highlightId,
                event.color,
                event.note
            )

            is TafseerEvent.AddNote -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.TAFSEER, "add_note")
                addNote(event.text)
            }
            is TafseerEvent.UpdateNote -> updateNote(event.note)
            is TafseerEvent.DeleteNote -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.TAFSEER, "delete_note")
                deleteNote(event.noteId)
            }
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
                availableSources = available,
                // Keyed on the verse, not the block: a block can span nine verses and the
                // subjects the corpus files 43:81 under are not the ones it files 43:89 under.
                topics = quranUseCases.getTopicsForAyah(ayah.id)
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
        val body = text.trim()
        // A blank note is a mis-tap, not a thought. Storing it puts an empty row in the
        // reader and on the notes screen, with nothing to read and nothing to explain it.
        if (body.isEmpty()) return

        val currentState = _state.value
        // `getOrNull`, not `[…]`. This ran outside the coroutine, so an index that had not
        // caught up with the ayah list — before the first load, or straight after switching
        // to a shorter surah — threw IndexOutOfBoundsException out of `onEvent`, on the UI
        // thread, from a tap. None of these three handlers has ever executed in production,
        // which is why nothing found it.
        val ayah = currentState.ayahs.getOrNull(currentState.currentAyahIndex) ?: return

        launchSafely(telemetry, DOMAIN, "add_note") {
            tafseerUseCases.addNote(
                ayahId = ayah.id,
                tafseerId = currentState.selectedSource.id,
                text = body
            )
        }
    }

    private fun updateNote(note: TafseerNote) {
        val body = note.text.trim()
        if (body.isEmpty()) return
        launchSafely(telemetry, DOMAIN, "update_note") {
            // The id carries through unchanged, so the DAO's @Update targets this row rather
            // than the insert-a-second-copy that an id of 0 would have produced.
            tafseerUseCases.updateNote(note.copy(text = body))
        }
    }

    private fun deleteNote(noteId: Long) {
        launchSafely(telemetry, DOMAIN, "delete_note") {
            tafseerUseCases.deleteNote(noteId)
        }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.TAFSEER
    }
}
