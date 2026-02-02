package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.usecase.ProphetUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProphetListState(
    val prophets: List<Prophet> = emptyList(),
    val favorites: List<Prophet> = emptyList(),
    val filteredProphets: List<Prophet> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false
)

data class ProphetDetailState(
    val prophet: Prophet? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface ProphetEvent {
    data class LoadDetail(val prophetId: Int) : ProphetEvent
    data class ToggleFavorite(val prophetId: Int) : ProphetEvent
    data class Search(val query: String) : ProphetEvent
    data object ClearSearch : ProphetEvent
    data object ToggleFavoritesFilter : ProphetEvent
}

@HiltViewModel
class ProphetViewModel @Inject constructor(
    private val prophetUseCases: ProphetUseCases
) : ViewModel() {

    private val _listState = MutableStateFlow(ProphetListState())
    val listState: StateFlow<ProphetListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(ProphetDetailState())
    val detailState: StateFlow<ProphetDetailState> = _detailState.asStateFlow()

    init {
        observeProphets()
        observeFavorites()
    }

    fun onEvent(event: ProphetEvent) {
        when (event) {
            is ProphetEvent.LoadDetail -> loadDetail(event.prophetId)
            is ProphetEvent.ToggleFavorite -> toggleFavorite(event.prophetId)
            is ProphetEvent.Search -> search(event.query)
            ProphetEvent.ClearSearch -> clearSearch()
            ProphetEvent.ToggleFavoritesFilter -> toggleFavoritesFilter()
        }
    }

    private fun observeProphets() {
        viewModelScope.launch {
            prophetUseCases.getAllProphets().collect { prophets ->
                _listState.update {
                    it.copy(
                        prophets = prophets,
                        isLoading = false
                    )
                }
                applyFilters()
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            prophetUseCases.getFavorites().collect { favorites ->
                _listState.update { it.copy(favorites = favorites) }
                applyFilters()
            }
        }
    }

    private fun loadDetail(prophetId: Int) {
        _detailState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val prophet = prophetUseCases.getProphetById(prophetId)
            _detailState.update {
                it.copy(
                    prophet = prophet,
                    isFavorite = prophet?.isFavorite ?: false,
                    isLoading = false
                )
            }
        }
    }

    private fun toggleFavorite(prophetId: Int) {
        viewModelScope.launch {
            prophetUseCases.toggleFavorite(prophetId)
            // Refresh detail if currently viewing this prophet
            val currentDetail = _detailState.value
            if (currentDetail.prophet?.id == prophetId) {
                val updatedProphet = prophetUseCases.getProphetById(prophetId)
                _detailState.update {
                    it.copy(
                        prophet = updatedProphet,
                        isFavorite = updatedProphet?.isFavorite ?: false
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
        val source = if (state.showFavoritesOnly) state.favorites else state.prophets
        val filtered = if (state.searchQuery.isBlank()) {
            source
        } else {
            source.filter { prophet ->
                prophet.nameArabic.contains(state.searchQuery, ignoreCase = true) ||
                        prophet.nameEnglish.contains(state.searchQuery, ignoreCase = true) ||
                        prophet.nameTransliteration.contains(state.searchQuery, ignoreCase = true) ||
                        prophet.titleEnglish.contains(state.searchQuery, ignoreCase = true) ||
                        prophet.era.contains(state.searchQuery, ignoreCase = true)
            }
        }
        _listState.update { it.copy(filteredProphets = filtered) }
    }
}
