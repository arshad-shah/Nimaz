package com.arshadshah.nimaz.presentation.viewmodel.content

data class CatalogListState<T>(
    val items: List<T> = emptyList(),
    val favorites: List<T> = emptyList(),
    val filteredItems: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
)

data class CatalogDetailState<T>(
    val item: T? = null,
    val isLoading: Boolean = true,
)
