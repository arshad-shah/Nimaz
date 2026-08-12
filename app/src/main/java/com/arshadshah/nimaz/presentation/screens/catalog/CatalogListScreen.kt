package com.arshadshah.nimaz.presentation.screens.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccent
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NameFilterRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogListState

/**
 * A searchable, favourite-filterable list of catalog items.
 *
 * The Names of Allah, the Names of the Prophet ﷺ and the Prophets are three catalogs with the
 * same shape: a title, a search box, an all/favourites filter, and a list of cards. The
 * *ViewModel* layer already knew this — [com.arshadshah.nimaz.presentation.viewmodel.content.CatalogViewModel]
 * is generic in the item type and all three share [CatalogEvent] and [CatalogListState]. Only
 * the screens had been copied, three times, 136 lines each and differing in a ViewModel, an
 * accent, three string resources and how a card labels itself (audit §1.3).
 *
 * So this is the screen and those are the differences. A fourth catalog is a call to this
 * function, not a fourth file.
 *
 * The card is a slot rather than a config field because it is the one difference that is not a
 * value: the Prophets list puts the English name first and adds a title and an era chip, and a
 * `NameCard(...)` call expresses that better than five more parameters would.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> CatalogListScreen(
    state: CatalogListState<T>,
    onEvent: (CatalogEvent) -> Unit,
    onNavigateBack: () -> Unit,
    title: String,
    searchHint: String,
    emptyMessage: String,
    accent: NamesAccent,
    itemKey: (T) -> Any,
    card: @Composable (T) -> Unit,
) {
    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(title = title, onBackClick = onNavigateBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NimazSearchBar(
                query = state.searchQuery,
                onQueryChange = { onEvent(CatalogEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
                placeholder = searchHint,
                showClearButton = state.searchQuery.isNotEmpty(),
                onClear = { onEvent(CatalogEvent.ClearSearch) },
                // The Prophets copy left this off, so its keyboard's search key did nothing
                // while the other two searched. One screen, one answer.
                onSearch = { onEvent(CatalogEvent.Search(it)) },
            )

            NameFilterRow(
                showFavoritesOnly = state.showFavoritesOnly,
                onShowAll = { onEvent(CatalogEvent.ToggleFavoritesFilter) },
                onShowFavorites = { onEvent(CatalogEvent.ToggleFavoritesFilter) },
                accent = accent,
                allLabel = stringResource(R.string.all),
                favoritesLabel = stringResource(R.string.favorites),
                modifier = Modifier.padding(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.ExtraSmall
                )
            )

            if (state.isLoading) {
                NimazLoadingState()
            } else {
                val displayList = state.filteredItems

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = NimazSpacing.Large,
                        vertical = NimazSpacing.Small
                    ),
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                ) {
                    items(items = displayList, key = itemKey) { item -> card(item) }

                    if (displayList.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = if (state.showFavoritesOnly) {
                                    stringResource(R.string.no_favorites_yet)
                                } else {
                                    emptyMessage
                                },
                                message = "",
                                icon = Icons.Filled.Favorite,
                                iconTint = accent.contentTint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
