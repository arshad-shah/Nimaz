package com.arshadshah.nimaz.presentation.screens.prophets

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
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProphetsListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ProphetViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val accent = NamesAccents.prophets()

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.prophets_title),
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
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(ProphetEvent.Search(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small),
                placeholder = stringResource(R.string.prophets_search_hint),
                showClearButton = state.searchQuery.isNotEmpty(),
                onClear = { viewModel.onEvent(ProphetEvent.ClearSearch) }
            )

            // Filter Chips
            NameFilterRow(
                showFavoritesOnly = state.showFavoritesOnly,
                onShowAll = { viewModel.onEvent(ProphetEvent.ToggleFavoritesFilter) },
                onShowFavorites = { viewModel.onEvent(ProphetEvent.ToggleFavoritesFilter) },
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
                val displayList = state.filteredProphets

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
                    ) { prophet ->
                        NameCard(
                            number = prophet.id,
                            arabicName = prophet.nameArabic,
                            primaryLabel = prophet.nameEnglish,
                            secondaryLabel = prophet.nameTransliteration,
                            isFavorite = prophet.isFavorite,
                            accent = accent,
                            onClick = { onNavigateToDetail(prophet.id) },
                            onFavoriteClick = {
                                viewModel.onEvent(ProphetEvent.ToggleFavorite(prophet.id))
                            },
                            titleLabel = prophet.titleEnglish,
                            eraChip = prophet.era
                        )
                    }

                    if (displayList.isEmpty()) {
                        item {
                            NimazEmptyState(
                                title = if (state.showFavoritesOnly) {
                                    stringResource(R.string.no_favorites_yet)
                                } else {
                                    stringResource(R.string.prophets_no_found)
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
