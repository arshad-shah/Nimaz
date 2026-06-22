package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.TafseerNoteItem
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.TafseerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Immutable UI state for the Tafseer chapters page — a surah picker plus the
 * "My notes" tab listing the user's annotated tafseer.
 */
data class TafseerChaptersUiState(
    val surahs: List<Surah> = emptyList(),
    val notes: List<TafseerNoteItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TafseerChaptersViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases,
    private val tafseerUseCases: TafseerUseCases
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
            .onEach { _state.value = it }
            .catch { _state.value = _state.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }
}
