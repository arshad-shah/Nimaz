package com.arshadshah.nimaz.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.usecase.AsmaUnNabiUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AsmaUnNabiListState(
    val names: List<AsmaUnNabi> = emptyList(),
    val favorites: List<AsmaUnNabi> = emptyList(),
    val filteredNames: List<AsmaUnNabi> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false
)

data class AsmaUnNabiDetailState(
    val name: AsmaUnNabi? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true
)

sealed interface AsmaUnNabiEvent {
    data class LoadDetail(val nameId: Int) : AsmaUnNabiEvent
    data class ToggleFavorite(val nameId: Int) : AsmaUnNabiEvent
    data class Search(val query: String) : AsmaUnNabiEvent
    data object ClearSearch : AsmaUnNabiEvent
    data object ToggleFavoritesFilter : AsmaUnNabiEvent
}

@HiltViewModel
class AsmaUnNabiViewModel @Inject constructor(
    private val asmaUnNabiUseCases: AsmaUnNabiUseCases
) : ViewModel() {

    private val _listState = MutableStateFlow(AsmaUnNabiListState())
    val listState: StateFlow<AsmaUnNabiListState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(AsmaUnNabiDetailState())
    val detailState: StateFlow<AsmaUnNabiDetailState> = _detailState.asStateFlow()

    init {
        observeNames()
        observeFavorites()
    }

    fun onEvent(event: AsmaUnNabiEvent) {
        when (event) {
            is AsmaUnNabiEvent.LoadDetail -> loadDetail(event.nameId)
            is AsmaUnNabiEvent.ToggleFavorite -> toggleFavorite(event.nameId)
            is AsmaUnNabiEvent.Search -> search(event.query)
            AsmaUnNabiEvent.ClearSearch -> clearSearch()
            AsmaUnNabiEvent.ToggleFavoritesFilter -> toggleFavoritesFilter()
        }
    }

    private fun observeNames() {
        viewModelScope.launch {
            asmaUnNabiUseCases.getAllNames().collect { names ->
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
            asmaUnNabiUseCases.getFavorites().collect { favorites ->
                _listState.update { it.copy(favorites = favorites) }
                applyFilters()
            }
        }
    }

    private fun loadDetail(nameId: Int) {
        _detailState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val name = asmaUnNabiUseCases.getNameById(nameId)
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
            asmaUnNabiUseCases.toggleFavorite(nameId)
            // Refresh detail if currently viewing this name
            val currentDetail = _detailState.value
            if (currentDetail.name?.id == nameId) {
                val updatedName = asmaUnNabiUseCases.getNameById(nameId)
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
