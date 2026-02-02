package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AsmaUlHusnaListState(
    val names: List<AsmaUlHusna> = emptyList(),
    val favorites: List<AsmaUlHusna> = emptyList(),
    val filteredNames: List<AsmaUlHusna> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false
)

data class AsmaUlHusnaDetailState(
    val name: AsmaUlHusna? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface AsmaUlHusnaEvent {
    data class LoadDetail(val nameId: Int) : AsmaUlHusnaEvent
    data class ToggleFavorite(val nameId: Int) : AsmaUlHusnaEvent
    data class Search(val query: String) : AsmaUlHusnaEvent
    data object ClearSearch : AsmaUlHusnaEvent
    data object ToggleFavoritesFilter : AsmaUlHusnaEvent
}

@HiltViewModel
class AsmaUlHusnaViewModel @Inject constructor(
    private val asmaUlHusnaUseCases: AsmaUlHusnaUseCases
) : ViewModel() {

    private val _listState = MutableStateFlow(AsmaUlHusnaListState())
    val listState: StateFlow<AsmaUlHusnaListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(AsmaUlHusnaDetailState())
    val detailState: StateFlow<AsmaUlHusnaDetailState> = _detailState.asStateFlow()

    init {
        observeNames()
        observeFavorites()
    }

    fun onEvent(event: AsmaUlHusnaEvent) {
        when (event) {
            is AsmaUlHusnaEvent.LoadDetail -> loadDetail(event.nameId)
            is AsmaUlHusnaEvent.ToggleFavorite -> toggleFavorite(event.nameId)
            is AsmaUlHusnaEvent.Search -> search(event.query)
            AsmaUlHusnaEvent.ClearSearch -> clearSearch()
            AsmaUlHusnaEvent.ToggleFavoritesFilter -> toggleFavoritesFilter()
        }
    }

    private fun observeNames() {
        viewModelScope.launch {
            asmaUlHusnaUseCases.getAllNames().collect { names ->
                _listState.update {
                    it.copy(
                        names = names,
                        isLoading = false
                    )
                }
                applyFilters()
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            asmaUlHusnaUseCases.getFavorites().collect { favorites ->
                _listState.update { it.copy(favorites = favorites) }
                applyFilters()
            }
        }
    }

    private fun loadDetail(nameId: Int) {
        _detailState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val name = asmaUlHusnaUseCases.getNameById(nameId)
            _detailState.update {
                it.copy(
                    name = name,
                    isFavorite = name?.isFavorite ?: false,
                    isLoading = false
                )
            }
        }
    }

    private fun toggleFavorite(nameId: Int) {
        viewModelScope.launch {
            asmaUlHusnaUseCases.toggleFavorite(nameId)
            // Refresh detail if currently viewing this name
            val currentDetail = _detailState.value
            if (currentDetail.name?.id == nameId) {
                val updatedName = asmaUlHusnaUseCases.getNameById(nameId)
                _detailState.update {
                    it.copy(
                        name = updatedName,
                        isFavorite = updatedName?.isFavorite ?: false
                    )
                }
            }
        }
    }

    private fun search(query: String) {
        _listState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun clearSearch() {
        _listState.update { it.copy(searchQuery = "") }
        applyFilters()
    }

    private fun toggleFavoritesFilter() {
        _listState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _listState.value
        val source = if (state.showFavoritesOnly) state.favorites else state.names
        val filtered = if (state.searchQuery.isBlank()) {
            source
        } else {
            source.filter { name ->
                name.nameArabic.contains(state.searchQuery, ignoreCase = true) ||
                        name.nameTransliteration.contains(state.searchQuery, ignoreCase = true) ||
                        name.nameEnglish.contains(state.searchQuery, ignoreCase = true) ||
                        name.meaning.contains(state.searchQuery, ignoreCase = true)
            }
        }
        _listState.update { it.copy(filteredNames = filtered) }
    }
}
