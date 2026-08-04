package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The browse-a-catalogue-of-things feature, once.
 *
 * `AsmaUlHusnaViewModel`, `AsmaUnNabiViewModel` and `ProphetViewModel` were **byte-identical
 * after identifier substitution** — 171, 171 and 172 lines of the same two states, the same
 * five events, the same six handlers and the same filter, differing only in what the thing is
 * called. Three copies means a fix lands in one and rots in the other two, and it already had:
 * the search use case each of them injects was invoked by none of them, and the `isFavorite`
 * field each of them maintains at four sites is read by no screen (the screens read
 * `item.isFavorite` off the domain model).
 *
 * What genuinely differs per feature is small enough to be a parameter: where the rows come
 * from, and which of their fields a search should look at. That is [CatalogSource].
 */
interface CatalogSource<T : Any> {
    fun all(): Flow<List<T>>
    fun favourites(): Flow<List<T>>
    suspend fun byId(id: Int): T?
    suspend fun toggleFavourite(id: Int)
    fun idOf(item: T): Int

    /** Whether [item] matches [query] — the only per-feature part of the filter. */
    fun matches(item: T, query: String): Boolean
}

abstract class CatalogViewModel<T : Any>(
    private val source: CatalogSource<T>,
    private val telemetry: Telemetry,
    private val feature: String,
) : ViewModel() {

    private val _listState = MutableStateFlow(CatalogListState<T>())
    val listState: StateFlow<CatalogListState<T>> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(CatalogDetailState<T>())
    val detailState: StateFlow<CatalogDetailState<T>> = _detailState.asStateFlow()

    private var detailJob: Job? = null

    init {
        observeItems()
        observeFavourites()
    }

    fun onEvent(event: CatalogEvent) = when (event) {
        is CatalogEvent.LoadDetail -> {
            telemetry.featureUsed(feature, AppAnalytics.Action.OPEN_DETAIL)
            loadDetail(event.itemId)
        }

        is CatalogEvent.ToggleFavorite -> {
            telemetry.featureUsed(feature, AppAnalytics.Action.TOGGLE_FAVORITE)
            toggleFavourite(event.itemId)
        }

        is CatalogEvent.Search -> {
            telemetry.featureUsed(feature, AppAnalytics.Action.SEARCH)
            updateList { it.copy(searchQuery = event.query) }
        }

        CatalogEvent.ClearSearch -> updateList { it.copy(searchQuery = "") }

        CatalogEvent.ToggleFavoritesFilter -> {
            telemetry.featureUsed(feature, AppAnalytics.Action.TOGGLE_FAVORITES_FILTER)
            updateList { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        }
    }

    private fun observeItems() {
        launchSafely(
            telemetry,
            feature,
            "load_items",
            onFailure = { _listState.update { it.copy(isLoading = false) } },
        ) {
            source.all().collect { items ->
                updateList { it.copy(items = items, isLoading = false) }
            }
        }
    }

    private fun observeFavourites() {
        launchSafely(telemetry, feature, "load_favourites") {
            source.favourites().collect { favourites ->
                updateList { it.copy(favorites = favourites) }
            }
        }
    }

    private fun loadDetail(itemId: Int) {
        _detailState.update { it.copy(isLoading = true) }
        detailJob?.cancel()
        detailJob = launchSafely(
            telemetry,
            feature,
            "load_detail",
            onFailure = { _detailState.update { it.copy(isLoading = false) } },
        ) {
            _detailState.update { it.copy(item = source.byId(itemId), isLoading = false) }
        }
    }

    private fun toggleFavourite(itemId: Int) {
        launchSafely(telemetry, feature, "toggle_favourite") {
            source.toggleFavourite(itemId)
            // Re-read only when the detail screen is showing this very item; the list is
            // driven by the two flows above and needs no help.
            val open = _detailState.value.item
            if (open != null && source.idOf(open) == itemId) {
                _detailState.update { it.copy(item = source.byId(itemId)) }
            }
        }
    }

    /**
     * The single place the filter is applied.
     *
     * Every mutation of the list state goes through here, so the filtered view can never fall
     * out of step with its inputs — which is what happened when each copy had to remember to
     * call `applyFilters()` after every `update`.
     */
    private fun updateList(mutate: (CatalogListState<T>) -> CatalogListState<T>) {
        _listState.update { current ->
            val next = mutate(current)
            val pool = if (next.showFavoritesOnly) next.favorites else next.items
            next.copy(
                filteredItems = if (next.searchQuery.isBlank()) {
                    pool
                } else {
                    pool.filter { source.matches(it, next.searchQuery) }
                },
            )
        }
    }
}
