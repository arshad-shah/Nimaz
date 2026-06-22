package com.arshadshah.nimaz.presentation.screens.prophets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.templates.SearchableNameListScreen
import com.arshadshah.nimaz.presentation.viewmodel.ProphetEvent
import com.arshadshah.nimaz.presentation.viewmodel.ProphetViewModel

@Composable
fun ProphetsListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ProphetViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    val accent = NamesAccents.prophets()

    SearchableNameListScreen(
        title = stringResource(R.string.prophets_title),
        searchHint = stringResource(R.string.prophets_search_hint),
        searchQuery = state.searchQuery,
        showFavoritesOnly = state.showFavoritesOnly,
        isLoading = state.isLoading,
        items = state.filteredProphets,
        accent = accent,
        emptyTitle = stringResource(R.string.prophets_no_found),
        itemKey = { it.id },
        onNavigateBack = onNavigateBack,
        onSearch = { viewModel.onEvent(ProphetEvent.Search(it)) },
        onClearSearch = { viewModel.onEvent(ProphetEvent.ClearSearch) },
        onToggleFavoritesFilter = { viewModel.onEvent(ProphetEvent.ToggleFavoritesFilter) }
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
}
