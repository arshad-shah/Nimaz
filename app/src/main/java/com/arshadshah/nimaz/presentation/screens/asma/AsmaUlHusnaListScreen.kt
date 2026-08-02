package com.arshadshah.nimaz.presentation.screens.asma

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NameFilterRow
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUlHusnaEvent
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUlHusnaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmaUlHusnaListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AsmaUlHusnaViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val accent = NamesAccents.allah()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.asma_ul_husna_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            NimazSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(AsmaUlHusnaEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
                placeholder = stringResource(R.string.asma_ul_husna_search_hint),
                showClearButton = state.searchQuery.isNotEmpty(),
                onClear = { viewModel.onEvent(AsmaUlHusnaEvent.ClearSearch) },
                onSearch = { viewModel.onEvent(AsmaUlHusnaEvent.Search(it)) }
            )

            // Filter Chips
            NameFilterRow(
                showFavoritesOnly = state.showFavoritesOnly,
                onShowAll = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavoritesFilter) },
                onShowFavorites = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavoritesFilter) },
                accent = accent,
                allLabel = stringResource(R.string.all),
                favoritesLabel = stringResource(R.string.favorites),
                modifier = Modifier.padding(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.ExtraSmall
                )
            )

            // Content
            if (state.isLoading) {
                NimazLoadingState()
            } else {
                val displayList = state.filteredNames

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = NimazSpacing.Large,
                        vertical = NimazSpacing.Small
                    ),
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                ) {
                    items(
                        items = displayList,
                        key = { it.id }
                    ) { name ->
                        NameCard(
                            number = name.id,
                            arabicName = name.nameArabic,
                            primaryLabel = name.nameTransliteration,
                            secondaryLabel = name.nameEnglish,
                            isFavorite = name.isFavorite,
                            accent = accent,
                            onClick = { onNavigateToDetail(name.id) },
                            onFavoriteClick = {
                                viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavorite(name.id))
                            }
                        )
                    }

                    if (displayList.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = if (state.showFavoritesOnly) {
                                    stringResource(R.string.no_favorites_yet)
                                } else {
                                    stringResource(R.string.asma_ul_husna_no_names_found)
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
