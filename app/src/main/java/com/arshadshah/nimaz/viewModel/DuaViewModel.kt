package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalCategory
import com.arshadshah.nimaz.data.local.models.LocalChapter
import com.arshadshah.nimaz.data.local.models.LocalDua
import com.arshadshah.nimaz.repositories.DuaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuaViewModel @Inject constructor(
    private val duaRepository: DuaRepository
) : ViewModel() {

    // UI States
    sealed class UiState {
        object Loading : UiState()
        data class Success<T>(val data: T) : UiState()
        data class Error(val message: String) : UiState()
    }

    // States
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<LocalCategory>>(emptyList())
    val categories: StateFlow<List<LocalCategory>> = _categories.asStateFlow()

    private val _chapters = MutableStateFlow<List<LocalChapter>>(emptyList())
    val chapters: StateFlow<List<LocalChapter>> = _chapters.asStateFlow()

    private val _duas = MutableStateFlow<List<LocalDua>>(emptyList())
    val duas: StateFlow<List<LocalDua>> = _duas.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _favorites = MutableStateFlow<List<LocalDua>>(emptyList())
    val favorites: StateFlow<List<LocalDua>> = _favorites.asStateFlow()

    // Filtered Duas based on search query
    val filteredDuas: StateFlow<List<LocalDua>> = combine(
        _duas,
        _searchQuery
    ) { duas, query ->
        if (query.isBlank()) duas
        else duas.filter {
            it.english_translation.contains(query, ignoreCase = true) ||
                    it.arabic_dua.contains(query, ignoreCase = true) ||
                    it.english_reference.contains(query, ignoreCase = true)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        getCategories()
        getFavorites()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getCategories() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = duaRepository.getCategories()
                _categories.value = response.data ?: emptyList()
                _uiState.value = UiState.Success(_categories.value)
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting categories", e)
                _uiState.value = UiState.Error("Failed to load categories: ${e.message}")
            }
        }
    }

    fun getChapters(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = duaRepository.getChaptersByCategory(categoryId)
                _chapters.value = response.data ?: emptyList()
                _uiState.value = UiState.Success(_chapters.value)
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting chapters", e)
                _uiState.value = UiState.Error("Failed to load chapters: ${e.message}")
            }
        }
    }

    fun getDuas(chapterId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = duaRepository.getDuasOfChapter(chapterId)
                _duas.value = response.data ?: emptyList()
                _uiState.value = UiState.Success(_duas.value)
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting duas", e)
                _uiState.value = UiState.Error("Failed to load duas: ${e.message}")
            }
        }
    }

    fun toggleFavorite(dua: LocalDua) {
        viewModelScope.launch {
            try {
                val updatedDua = dua.copy(favourite = if (dua.favourite == 1) 0 else 1)
                duaRepository.updateDua(updatedDua)
                // Update the duas list
                _duas.value = _duas.value.map { if (it._id == dua._id) updatedDua else it }
                // Update favorites if necessary
                if (updatedDua.favourite == 1) {
                    _favorites.value += updatedDua
                } else {
                    _favorites.value = _favorites.value.filter { it._id != dua._id }
                }
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error toggling favorite", e)
                _uiState.value = UiState.Error("Failed to update favorite: ${e.message}")
            }
        }
    }

    fun getFavorites() {
        viewModelScope.launch {
            try {
                val favoriteDuas = duaRepository.getFavoriteDuas()
                _favorites.value = favoriteDuas.data ?: emptyList()
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting favorites", e)
            }
        }
    }

    // Get chapter by ID
    suspend fun getChapterById(chapterId: Int): LocalChapter? {
        return duaRepository.getChapterById(chapterId).data
    }

    // Get category by ID
    fun getCategoryById(categoryId: Int): LocalCategory? {
        return _categories.value.find { it.id == categoryId }
    }

    // Get related duas (duas from the same chapter)
    fun getRelatedDuas(dua: LocalDua): List<LocalDua> {
        return _duas.value.filter { it.chapter_id == dua.chapter_id && it._id != dua._id }
    }

    // Clear search query
    fun clearSearch() {
        _searchQuery.value = ""
        //remove search results
        _duas.value = emptyList()
    }

    //ghet search duas
    fun getSearchDuas(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = duaRepository.searchDuas(query)
                _duas.value = response.data ?: emptyList()
                _uiState.value = UiState.Success(_duas.value)
            } catch (e: Exception) {
                Log.e("DuaViewModel", "Error getting search duas", e)
                _uiState.value = UiState.Error("Failed to load search duas: ${e.message}")
            }
        }
    }

    // Refresh all data
    fun refreshData() {
        getCategories()
        getFavorites()
        // If there's a current chapter selected, refresh its duas
        if (_chapters.value.isNotEmpty()) {
            getDuas(_chapters.value.first()._id)
        }
    }
}