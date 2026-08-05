package com.arshadshah.nimaz.presentation.viewmodel.quran

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranTopic
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.domain.model.TafseerNote
import com.arshadshah.nimaz.domain.model.TafseerSource
import com.arshadshah.nimaz.presentation.viewmodel.UiError
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
import kotlinx.coroutines.flow.update
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
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "open_surah")
                loadSurah(event.surahNumber, event.ayahNumber)
            }
            // Swiping between ayahs and turning a commentary page are how this screen is read;
            // only opening it and switching source were counted, so "opened tafseer" looked
            // like the whole of the engagement.
            is TafseerEvent.NavigateToAyah -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "navigate_ayah")
                onAyahChanged(event.index)
            }
            is TafseerEvent.NavigateToTafseerPage -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "navigate_page")
                _state.update { it.copy(currentTafseerPage = event.page) }
            }

            is TafseerEvent.SwitchSource -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "switch_source")
                switchSource(event.source)
            }
            is TafseerEvent.AddHighlight -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "add_highlight")
                addHighlight(
                    event.startOffset,
                    event.endOffset,
                    event.color,
                    event.note
                )
            }

            is TafseerEvent.DeleteHighlight -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "delete_highlight")
                deleteHighlight(event.highlightId)
            }
            is TafseerEvent.UpdateHighlight -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "update_highlight")
                updateHighlight(
                    event.highlightId,
                    event.color,
                    event.note
                )
            }

            is TafseerEvent.AddNote -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "add_note")
                addNote(event.text)
            }
            is TafseerEvent.UpdateNote -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "update_note")
                updateNote(event.note)
            }
            is TafseerEvent.DeleteNote -> {
                telemetry.featureUsed(AppAnalytics.Feature.TAFSEER, "delete_note")
                deleteNote(event.noteId)
            }

            TafseerEvent.DismissNoteError -> dismissNoteError()
        }
    }

    private fun loadSurah(surahNumber: Int, ayahNumber: Int) {
        launchSafely(
            telemetry,
            AppAnalytics.Feature.TAFSEER,
            "load_surah",
            onFailure = { _state.update { it.copy(isLoading = false) } },
        ) {
            _state.update { it.copy(isLoading = true, surahNumber = surahNumber) }

            val surah = quranUseCases.getSurahByNumber(surahNumber)
            val ayahs = quranUseCases.getAyahsBySurah(surahNumber).first()

            val initialIndex = ayahs.indexOfFirst { it.ayahNumber == ayahNumber }
                .coerceAtLeast(0)

            _state.update {
                it.copy(
                    ayahs = ayahs,
                    currentAyahIndex = initialIndex,
                    surahName = surah?.nameEnglish ?: "Surah $surahNumber",
                    isLoading = false
                )
            }

            loadTafseerForCurrentAyah()
        }
    }

    private fun onAyahChanged(index: Int) {
        val ayahs = _state.value.ayahs
        if (index < 0 || index >= ayahs.size) return
        _state.update { it.copy(currentAyahIndex = index) }
        loadTafseerForCurrentAyah()
    }

    private fun switchSource(source: TafseerSource) {
        _state.update { it.copy(selectedSource = source) }
        loadTafseerForCurrentAyah()
    }

    private fun loadTafseerForCurrentAyah() {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        val tafseerId = currentState.selectedSource.id

        ayahAnnotationsJob?.cancel()
        ayahAnnotationsJob = launchSafely(telemetry, AppAnalytics.Feature.TAFSEER, "load_tafseer_for_current_ayah") {
            val tafseer =
                tafseerUseCases.getTafseerForAyah(ayah.surahNumber, ayah.ayahNumber, tafseerId)

            // Probed **only when the selected source came back empty**, which is the one time
            // the answer is used: `availableSources` reaches exactly one consumer,
            // `TafseerEmptyState`, and that is rendered only on an empty selection.
            //
            // It used to run unconditionally — one read per `TafseerSource` on top of the one
            // above, on every ayah swipe *and* every source switch. With five sources, swiping
            // through Al-Baqarah issued 286 × 6 = 1,716 reads to populate a set that all but a
            // handful of those verses never looked at.
            val available = if (tafseer?.text.isNullOrBlank()) {
                TafseerSource.entries.filter { source ->
                    source.id != tafseerId &&
                        tafseerUseCases
                            .getTafseerForAyah(ayah.surahNumber, ayah.ayahNumber, source.id)
                            ?.text?.isNotBlank() == true
                }.toSet()
            } else {
                emptySet()
            }
            val topics = quranUseCases.getTopicsForAyah(ayah.id)

            _state.update {
                // Only reset the reading position when the block itself changed —
                // swiping to the next ayah of the same block should hold position,
                // not reopen the commentary from page 1.
                val sameBlock = tafseer != null && tafseer.id == it.currentTafseer?.id
                it.copy(
                    currentTafseer = tafseer,
                    currentTafseerPage = if (sameBlock) it.currentTafseerPage else 0,
                    availableSources = available,
                    // Keyed on the verse, not the block: a block can span nine verses and the
                    // subjects the corpus files 43:81 under are not the ones it files 43:89
                    // under.
                    topics = topics
                )
            }

            if (tafseer != null) {
                launch {
                    tafseerUseCases.getHighlightsForRange(
                        tafseer.surahNumber, tafseer.ayahStart, tafseer.ayahEnd, tafseerId
                    ).collectLatest { highlights ->
                        _state.update { it.copy(highlights = highlights) }
                    }
                }
                launch {
                    tafseerUseCases.getNotesForRange(
                        tafseer.surahNumber, tafseer.ayahStart, tafseer.ayahEnd, tafseerId
                    ).collectLatest { notes ->
                        _state.update { it.copy(notes = notes) }
                    }
                }
            } else {
                _state.update { it.copy(highlights = emptyList(), notes = emptyList()) }
            }
        }
    }

    private fun addHighlight(startOffset: Int, endOffset: Int, color: String, note: String?) {
        val currentState = _state.value
        val ayahs = currentState.ayahs
        if (ayahs.isEmpty()) return

        val ayah = ayahs[currentState.currentAyahIndex]
        launchSafely(telemetry, AppAnalytics.Feature.TAFSEER, "add_highlight") {
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
        launchSafely(telemetry, AppAnalytics.Feature.TAFSEER, "delete_highlight") {
            tafseerUseCases.deleteHighlight(highlightId)
        }
    }

    private fun updateHighlight(highlightId: Long, color: String, note: String?) {
        val highlight = _state.value.highlights.find { it.id == highlightId } ?: return
        launchSafely(telemetry, AppAnalytics.Feature.TAFSEER, "update_highlight") {
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

        launchSafely(
            telemetry, DOMAIN, "add_note",
            onFailure = { noteWriteFailed(R.string.tafseer_note_save_failed, it) },
        ) {
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
        launchSafely(
            telemetry, DOMAIN, "update_note",
            onFailure = { noteWriteFailed(R.string.tafseer_note_save_failed, it) },
        ) {
            // The id carries through unchanged, so the DAO's @Update targets this row rather
            // than the insert-a-second-copy that an id of 0 would have produced.
            tafseerUseCases.updateNote(note.copy(text = body))
        }
    }

    private fun deleteNote(noteId: Long) {
        launchSafely(
            telemetry, DOMAIN, "delete_note",
            onFailure = { noteWriteFailed(R.string.tafseer_note_delete_failed, it) },
        ) {
            tafseerUseCases.deleteNote(noteId)
        }
    }

    /**
     * A note write that did not land.
     *
     * Not droppable, unlike most write failures: from where the reader is standing, a note
     * that silently failed to save is a note they wrote and lost. Not full-screen either —
     * it must not take away the commentary they are reading. So: a snackbar, and the
     * commentary stays.
     */
    private fun noteWriteFailed(@StringRes message: Int, throwable: Throwable) {
        _state.update {
            it.copy(noteError = UiError(message = message, details = throwable.message))
        }
    }

    private fun dismissNoteError() {
        _state.update { it.copy(noteError = null) }
    }

    private companion object {
        private const val DOMAIN = AppAnalytics.Feature.TAFSEER
    }
}
