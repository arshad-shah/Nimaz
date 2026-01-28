package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.arshadshah.nimaz.domain.model.DuaProgress
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.repository.DuaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class DuaCollectionUiState(
    val categories: List<DuaCategory> = emptyList(),
    val filteredCategories: List<DuaCategory> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DuaCategoryUiState(
    val category: DuaCategory? = null,
    val duas: List<Dua> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class DuaReaderUiState(
    val dua: Dua? = null,
    val progress: DuaProgress? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val fontSize: Float = 16f,
    val arabicFontSize: Float = 28f
)

data class DuaSearchUiState(
    val query: String = "",
    val results: List<DuaSearchResult> = emptyList(),
    val isSearching: Boolean = false
)

data class DuaFavoritesUiState(
    val favorites: List<DuaBookmark> = emptyList(),
    val isLoading: Boolean = true
)

data class DuaDailyProgressUiState(
    val progressList: List<DuaProgress> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true
)

sealed interface DuaEvent {
    data class LoadCategory(val categoryId: String) : DuaEvent
    data class LoadDua(val duaId: String) : DuaEvent
    data class LoadDuasByOccasion(val occasion: DuaOccasion) : DuaEvent
    data class Search(val query: String) : DuaEvent
    data class SearchCategories(val query: String) : DuaEvent
    data class ToggleFavorite(val duaId: String, val categoryId: String) : DuaEvent
    data class IncrementProgress(val duaId: String, val targetCount: Int) : DuaEvent
    data class DecrementProgress(val duaId: String) : DuaEvent
    data class SetFontSize(val size: Float) : DuaEvent
    data class SetArabicFontSize(val size: Float) : DuaEvent
    data class LoadProgressForDate(val date: Long) : DuaEvent
    data object ToggleArabic : DuaEvent
    data object ToggleTransliteration : DuaEvent
    data object ToggleTranslation : DuaEvent
    data object ClearSearch : DuaEvent
    data object LoadAllCategories : DuaEvent
    data object LoadFavorites : DuaEvent
    data object LoadTodayProgress : DuaEvent
}

@HiltViewModel
class DuaViewModel @Inject constructor(
    private val duaRepository: DuaRepository
) : ViewModel() {

    private val _collectionState = MutableStateFlow(DuaCollectionUiState())
    val collectionState: StateFlow<DuaCollectionUiState> = _collectionState.asStateFlow()

    private val _categoryState = MutableStateFlow(DuaCategoryUiState())
    val categoryState: StateFlow<DuaCategoryUiState> = _categoryState.asStateFlow()

    private val _readerState = MutableStateFlow(DuaReaderUiState())
    val readerState: StateFlow<DuaReaderUiState> = _readerState.asStateFlow()

    private val _searchState = MutableStateFlow(DuaSearchUiState())
    val searchState: StateFlow<DuaSearchUiState> = _searchState.asStateFlow()

    private val _favoritesState = MutableStateFlow(DuaFavoritesUiState())
    val favoritesState: StateFlow<DuaFavoritesUiState> = _favoritesState.asStateFlow()

    private val _dailyProgressState = MutableStateFlow(DuaDailyProgressUiState())
    val dailyProgressState: StateFlow<DuaDailyProgressUiState> = _dailyProgressState.asStateFlow()

    init {
        loadAllCategories()
        loadFavorites()
        loadTodayProgress()
    }

    fun onEvent(event: DuaEvent) {
        when (event) {
            is DuaEvent.LoadCategory -> loadCategory(event.categoryId)
            is DuaEvent.LoadDua -> loadDua(event.duaId)
            is DuaEvent.LoadDuasByOccasion -> loadDuasByOccasion(event.occasion)
            is DuaEvent.Search -> search(event.query)
            is DuaEvent.SearchCategories -> searchCategories(event.query)
            is DuaEvent.ToggleFavorite -> toggleFavorite(event.duaId, event.categoryId)
            is DuaEvent.IncrementProgress -> incrementProgress(event.duaId, event.targetCount)
            is DuaEvent.DecrementProgress -> decrementProgress(event.duaId)
            is DuaEvent.SetFontSize -> _readerState.update { it.copy(fontSize = event.size) }
            is DuaEvent.SetArabicFontSize -> _readerState.update { it.copy(arabicFontSize = event.size) }
            is DuaEvent.LoadProgressForDate -> loadProgressForDate(event.date)
            DuaEvent.ToggleArabic -> _readerState.update { it.copy(showArabic = !it.showArabic) }
            DuaEvent.ToggleTransliteration -> _readerState.update { it.copy(showTransliteration = !it.showTransliteration) }
            DuaEvent.ToggleTranslation -> _readerState.update { it.copy(showTranslation = !it.showTranslation) }
            DuaEvent.ClearSearch -> {
                _searchState.update { DuaSearchUiState() }
                _collectionState.update { it.copy(searchQuery = "", filteredCategories = it.categories) }
            }
            DuaEvent.LoadAllCategories -> loadAllCategories()
            DuaEvent.LoadFavorites -> loadFavorites()
            DuaEvent.LoadTodayProgress -> loadTodayProgress()
        }
    }

    private fun loadAllCategories() {
        viewModelScope.launch {
            duaRepository.getAllCategories().collect { categories ->
                _collectionState.update {
                    it.copy(
                        categories = categories,
                        filteredCategories = categories,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadCategory(categoryId: String) {
        _categoryState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val category = duaRepository.getCategoryById(categoryId)
                _categoryState.update { it.copy(category = category) }

                duaRepository.getDuasByCategory(categoryId).collect { duas ->
                    _categoryState.update {
                        it.copy(duas = duas, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _categoryState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadDua(duaId: String) {
        _readerState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val dua = duaRepository.getDuaById(duaId)
                val todayEpoch = getTodayEpoch()
                val progress = duaRepository.getProgressForDuaOnDate(duaId, todayEpoch)

                _readerState.update {
                    it.copy(dua = dua, progress = progress, isLoading = false)
                }

                // Load favorite status
                duaRepository.isDuaFavorite(duaId).collect { isFav ->
                    _readerState.update { it.copy(isFavorite = isFav) }
                }
            } catch (e: Exception) {
                _readerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadDuasByOccasion(occasion: DuaOccasion) {
        _categoryState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            duaRepository.getDuasByOccasion(occasion).collect { duas ->
                _categoryState.update {
                    it.copy(duas = duas, isLoading = false)
                }
            }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            _searchState.update { DuaSearchUiState() }
            return
        }

        _searchState.update { it.copy(query = query, isSearching = true) }
        viewModelScope.launch {
            duaRepository.searchDuas(query).collect { results ->
                _searchState.update { it.copy(results = results, isSearching = false) }
            }
        }
    }

    private fun searchCategories(query: String) {
        _collectionState.update { state ->
            val filtered = if (query.isBlank()) {
                state.categories
            } else {
                state.categories.filter { category ->
                    category.nameEnglish.contains(query, ignoreCase = true) ||
                    category.nameArabic.contains(query) ||
                    category.description?.contains(query, ignoreCase = true) == true
                }
            }
            state.copy(searchQuery = query, filteredCategories = filtered)
        }
    }

    private fun toggleFavorite(duaId: String, categoryId: String) {
        viewModelScope.launch {
            duaRepository.toggleFavorite(duaId, categoryId)
        }
    }

    private fun incrementProgress(duaId: String, targetCount: Int) {
        viewModelScope.launch {
            val todayEpoch = getTodayEpoch()
            duaRepository.incrementDuaProgress(duaId, todayEpoch, targetCount)
            // Reload progress
            val progress = duaRepository.getProgressForDuaOnDate(duaId, todayEpoch)
            _readerState.update { it.copy(progress = progress) }
        }
    }

    private fun decrementProgress(duaId: String) {
        viewModelScope.launch {
            val currentCount = _readerState.value.progress?.completedCount ?: 0
            if (currentCount > 0) {
                val todayEpoch = getTodayEpoch()
                duaRepository.decrementDuaProgress(duaId, todayEpoch)
                val progress = duaRepository.getProgressForDuaOnDate(duaId, todayEpoch)
                _readerState.update { it.copy(progress = progress) }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            duaRepository.getFavoriteDuas().collect { favorites ->
                _favoritesState.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
    }

    private fun loadTodayProgress() {
        val todayEpoch = getTodayEpoch()
        loadProgressForDate(todayEpoch)
    }

    private fun loadProgressForDate(date: Long) {
        _dailyProgressState.update { it.copy(isLoading = true, date = date) }
        viewModelScope.launch {
            duaRepository.getProgressForDate(date).collect { progressList ->
                _dailyProgressState.update {
                    it.copy(progressList = progressList, isLoading = false)
                }
            }
        }
    }

    private fun getTodayEpoch(): Long {
        return LocalDate.now().atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
    }

    fun isDuaFavorite(duaId: String) = duaRepository.isDuaFavorite(duaId)
}
