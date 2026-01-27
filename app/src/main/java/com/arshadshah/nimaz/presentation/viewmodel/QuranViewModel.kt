package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.ReadingProgress
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranHomeUiState(
    val surahs: List<Surah> = emptyList(),
    val filteredSurahs: List<Surah> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: QuranFilter = QuranFilter.ALL,
    val readingProgress: ReadingProgress? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class QuranReaderUiState(
    val surahWithAyahs: SurahWithAyahs? = null,
    val currentAyahIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showTranslation: Boolean = true,
    val selectedTranslatorId: String = "en.sahih",
    val fontSize: Float = 24f,
    val arabicFontSize: Float = 32f
)

data class QuranSearchUiState(
    val query: String = "",
    val results: List<QuranSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class QuranBookmarksUiState(
    val bookmarks: List<QuranBookmark> = emptyList(),
    val isLoading: Boolean = true
)

enum class QuranFilter {
    ALL, MECCAN, MEDINAN
}

sealed interface QuranEvent {
    data class LoadSurah(val surahNumber: Int) : QuranEvent
    data class Search(val query: String) : QuranEvent
    data class SetFilter(val filter: QuranFilter) : QuranEvent
    data class ToggleBookmark(val ayahId: Int, val surahNumber: Int, val ayahNumber: Int) : QuranEvent
    data class UpdateReadingPosition(val surah: Int, val ayah: Int, val page: Int, val juz: Int) : QuranEvent
    data class SetFontSize(val size: Float) : QuranEvent
    data class SetArabicFontSize(val size: Float) : QuranEvent
    data object ToggleTranslation : QuranEvent
    data object ClearSearch : QuranEvent
}

@HiltViewModel
class QuranViewModel @Inject constructor(
    private val quranUseCases: QuranUseCases
) : ViewModel() {

    private val _homeState = MutableStateFlow(QuranHomeUiState())
    val homeState: StateFlow<QuranHomeUiState> = _homeState.asStateFlow()

    private val _readerState = MutableStateFlow(QuranReaderUiState())
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(QuranSearchUiState())
    val searchState: StateFlow<QuranSearchUiState> = _searchState.asStateFlow()

    private val _bookmarksState = MutableStateFlow(QuranBookmarksUiState())
    val bookmarksState: StateFlow<QuranBookmarksUiState> = _bookmarksState.asStateFlow()

    init {
        loadSurahs()
        loadReadingProgress()
        loadBookmarks()
    }

    fun onEvent(event: QuranEvent) {
        when (event) {
            is QuranEvent.LoadSurah -> loadSurah(event.surahNumber)
            is QuranEvent.Search -> search(event.query)
            is QuranEvent.SetFilter -> setFilter(event.filter)
            is QuranEvent.ToggleBookmark -> toggleBookmark(event.ayahId, event.surahNumber, event.ayahNumber)
            is QuranEvent.UpdateReadingPosition -> updateReadingPosition(event.surah, event.ayah, event.page, event.juz)
            is QuranEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is QuranEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            QuranEvent.ToggleTranslation -> _readerState.update { it.copy(showTranslation = !it.showTranslation) }
            QuranEvent.ClearSearch -> {
                _searchState.update { QuranSearchUiState() }
                _homeState.update { it.copy(searchQuery = "", filteredSurahs = it.surahs) }
            }
        }
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            quranUseCases.getSurahList()
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { surahs ->
                    _homeState.update { state ->
                        state.copy(
                            surahs = surahs,
                            filteredSurahs = filterSurahs(surahs, state.selectedFilter, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loadReadingProgress() {
        viewModelScope.launch {
            quranUseCases.getReadingProgress()
                .collect { progress ->
                    _homeState.update { it.copy(readingProgress = progress) }
                }
        }
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            quranUseCases.getBookmarks()
                .collect { bookmarks ->
                    _bookmarksState.update { it.copy(bookmarks = bookmarks, isLoading = false) }
                }
        }
    }

    private fun loadSurah(surahNumber: Int) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            quranUseCases.getSurahWithAyahs(surahNumber, _readerState.value.selectedTranslatorId)
                .collect { surahWithAyahs ->
                    _readerState.update {
                        it.copy(
                            surahWithAyahs = surahWithAyahs,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun search(query: String) {
        _homeState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            _homeState.update { it.copy(filteredSurahs = filterSurahs(it.surahs, it.selectedFilter, "")) }
            _searchState.update { QuranSearchUiState() }
            return
        }

        // Filter surahs by name
        _homeState.update { state ->
            state.copy(filteredSurahs = filterSurahs(state.surahs, state.selectedFilter, query))
        }

        // Search in ayahs
        _searchState.update { it.copy(query = query, isSearching = true) }
        viewModelScope.launch {
            quranUseCases.searchQuran(query, _readerState.value.selectedTranslatorId)
                .collect { results ->
                    _searchState.update { it.copy(results = results, isSearching = false) }
                }
        }
    }

    private fun setFilter(filter: QuranFilter) {
        _homeState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredSurahs = filterSurahs(state.surahs, filter, state.searchQuery)
            )
        }
    }

    private fun filterSurahs(surahs: List<Surah>, filter: QuranFilter, query: String): List<Surah> {
        return surahs
            .filter { surah ->
                when (filter) {
                    QuranFilter.ALL -> true
                    QuranFilter.MECCAN -> surah.revelationType == RevelationType.MECCAN
                    QuranFilter.MEDINAN -> surah.revelationType == RevelationType.MEDINAN
                }
            }
            .filter { surah ->
                query.isBlank() ||
                surah.nameEnglish.contains(query, ignoreCase = true) ||
                surah.nameTransliteration.contains(query, ignoreCase = true) ||
                surah.nameArabic.contains(query)
            }
    }

    private fun toggleBookmark(ayahId: Int, surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranUseCases.toggleBookmark(ayahId, surahNumber, ayahNumber)
        }
    }

    private fun updateReadingPosition(surah: Int, ayah: Int, page: Int, juz: Int) {
        viewModelScope.launch {
            quranUseCases.updateReadingPosition(surah, ayah, page, juz)
        }
    }
}
