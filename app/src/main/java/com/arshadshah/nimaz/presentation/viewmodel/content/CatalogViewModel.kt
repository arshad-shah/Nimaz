package com.arshadshah.nimaz.presentation.viewmodel.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    /**
     * The item the detail surface is currently *for*, set synchronously when it is asked for.
     *
     * [detailJob] alone does not close the race: a coroutine cancelled after its last suspension
     * point still runs to the end of its block, and `source.byId` *is* that suspension point
     * with the state write immediately after it. Open Adam, back, open Muhammad quickly enough
     * — the detail screen fires `LoadDetail` from a `LaunchedEffect(id)` and the instance is
     * shared through the list screen's back-stack entry — and Adam's read resolving second put
     * **Adam's story under Muhammad's route**, with `isLoading = false` to say it was ready.
     */
    private var requestedItemId: Int? = null

    /**
     * The live search box, for **analytics only** — the filter itself stays synchronous.
     *
     * Filtering a catalogue is an in-memory `List.filter` over a few hundred rows, so it should
     * and does happen on every keystroke. Recording it should not: the screens dispatch
     * [CatalogEvent.Search] from `onQueryChange`, so typing "Ibrahim" logged seven events and
     * two backspaces logged two more. Debouncing this flow separates the two rates — the list
     * still tracks the finger, and one settled query is one `search` event.
     */
    private val searchQueries = MutableStateFlow("")

    init {
        observeItems()
        observeFavourites()
        observeSearchQueries()
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
            searchQueries.value = event.query
            updateList { it.copy(searchQuery = event.query) }
        }

        CatalogEvent.ClearSearch -> {
            searchQueries.value = ""
            updateList { it.copy(searchQuery = "") }
        }

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

    /**
     * One [Telemetry.search] per settled, non-blank query.
     *
     * `logFeatureUsed(feature, "search")` was the wrong helper as well as the wrong rate: it
     * threw away the query length, which is the one thing `search` records and the only way to
     * tell "people type a name" from "people type a letter and give up".
     */
    @OptIn(FlowPreview::class)
    private fun observeSearchQueries() {
        viewModelScope.launch {
            searchQueries
                .debounce(SEARCH_DEBOUNCE_MS)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { query -> telemetry.search(feature, query.length) }
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
        requestedItemId = itemId
        detailJob?.cancel()
        detailJob = launchSafely(
            telemetry,
            feature,
            "load_detail",
            onFailure = { _detailState.update { it.copy(isLoading = false) } },
        ) {
            val item = source.byId(itemId)
            if (requestedItemId != itemId) return@launchSafely
            _detailState.update { it.copy(item = item, isLoading = false) }
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

    private companion object {
        /** Long enough that a word typed at speed is one event, short enough to feel live. */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
