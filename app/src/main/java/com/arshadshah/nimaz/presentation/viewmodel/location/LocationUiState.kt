package com.arshadshah.nimaz.presentation.viewmodel.location

data class LocationUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchLocation> = emptyList(),
    val currentLocation: CurrentLocationState = CurrentLocationState.NotSet,
    val recentLocations: List<SearchLocation> = emptyList(),
    val popularCities: List<SearchLocation> = defaultPopularCities,
    val selectedRegion: CityRegion? = null,
    val isSearching: Boolean = false,
    val isLoadingGps: Boolean = false,
    val error: String? = null
)

sealed interface CurrentLocationState {
    data object NotSet : CurrentLocationState
    data object Loading : CurrentLocationState
    data class Set(
        val name: String,
        val latitude: Double,
        val longitude: Double
    ) : CurrentLocationState
}
