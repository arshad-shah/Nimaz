package com.arshadshah.nimaz.presentation.viewmodel.content

/**
 * Shared by all three catalogues. Ids are plain `Int`s, so this needs no type parameter and
 * the three features keep distinct `typealias`es without three copies of the hierarchy.
 */
sealed interface CatalogEvent {
    data class LoadDetail(val itemId: Int) : CatalogEvent
    data class ToggleFavorite(val itemId: Int) : CatalogEvent
    data class Search(val query: String) : CatalogEvent
    data object ClearSearch : CatalogEvent
}
