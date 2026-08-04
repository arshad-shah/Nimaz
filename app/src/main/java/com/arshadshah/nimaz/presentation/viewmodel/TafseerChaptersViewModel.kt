package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.catchAndReport
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

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
    val error: String? = null,
)

@HiltViewModel
class TafseerChaptersViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val tafseerUseCases: TafseerUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(TafseerChaptersUiState())
    val state: StateFlow<TafseerChaptersUiState> = _state.asStateFlow()

    init {
        combine(
            quranUseCases.getSurahList(),
            tafseerUseCases.getTafseerNotes()
        ) { surahs, notes ->
            TafseerChaptersUiState(surahs = surahs, notes = notes, isLoading = false)
        }
            .onEach { loaded -> _state.update { loaded } }
            .catchAndReport(telemetry, DOMAIN, "load") { throwable ->
                _state.update { it.copy(isLoading = false, error = throwable.message) }
            }
            .launchIn(viewModelScope)
    }

    private companion object {
        const val DOMAIN = "tafseer_chapters"
    }
}
