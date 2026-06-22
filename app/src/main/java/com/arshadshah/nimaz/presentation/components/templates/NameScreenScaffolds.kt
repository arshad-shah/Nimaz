package com.arshadshah.nimaz.presentation.components.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.molecules.FavoriteFab
import com.arshadshah.nimaz.presentation.components.molecules.NameFilterRow
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccent
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing

/**
 * Shared scaffold for the three "names" list screens (Asma ul Husna, Asma un
 * Nabi, Prophets). These previously copy-pasted an identical search-bar +
 * favourites-filter + loading + list + empty-state structure; only the data
 * source, accent, strings and the per-item card differed.
 *
 * @param itemContent renders a single row (e.g. a [com.arshadshah.nimaz.presentation.components.molecules.NameCard]).
 * @param emptyTitle title shown when the (unfiltered) list is empty; the
 *        favourites-only empty state always uses [R.string.no_favorites_yet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableNameListScreen(
    title: String,
    searchHint: String,
    searchQuery: String,
    showFavoritesOnly: Boolean,
    isLoading: Boolean,
    items: List<T>,
    accent: NamesAccent,
    emptyTitle: String,
    itemKey: (T) -> Any,
    onNavigateBack: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = title,
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NimazSearchBar(
                query = searchQuery,
                onQueryChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
                placeholder = searchHint,
                showClearButton = searchQuery.isNotEmpty(),
                onClear = onClearSearch,
                onSearch = onSearch
            )

            NameFilterRow(
                showFavoritesOnly = showFavoritesOnly,
                onShowAll = onToggleFavoritesFilter,
                onShowFavorites = onToggleFavoritesFilter,
                accent = accent,
                allLabel = stringResource(R.string.all),
                favoritesLabel = stringResource(R.string.favorites),
                modifier = Modifier.padding(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.ExtraSmall
                )
            )

            if (isLoading) {
                NimazLoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = NimazSpacing.Large,
                        vertical = NimazSpacing.Small
                    ),
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                ) {
                    items(
                        items = items,
                        key = { itemKey(it) }
                    ) { item ->
                        itemContent(item)
                    }

                    if (items.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = if (showFavoritesOnly) {
                                    stringResource(R.string.no_favorites_yet)
                                } else {
                                    emptyTitle
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

/**
 * Shared scaffold for the three "names" detail screens. These previously
 * copy-pasted an identical top-bar + favourite FAB + loading + scrollable
 * section list. Only the loaded item, title, accent and the body sections
 * differed.
 *
 * The body [content] receives the non-null loaded [item]; a trailing spacer for
 * the FAB is appended automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> NameDetailScaffold(
    item: T?,
    isLoading: Boolean,
    title: String,
    accent: NamesAccent,
    isFavorite: (T) -> Boolean,
    onNavigateBack: () -> Unit,
    onToggleFavorite: (T) -> Unit,
    content: LazyListScope.(T) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = title,
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            item?.let { loaded ->
                FavoriteFab(
                    isFavorite = isFavorite(loaded),
                    accent = accent,
                    onClick = { onToggleFavorite(loaded) }
                )
            }
        }
    ) { paddingValues ->
        if (isLoading || item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.Small
                ),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
            ) {
                content(item)

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
