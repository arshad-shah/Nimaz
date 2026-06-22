package com.arshadshah.nimaz.presentation.screens.asmaunnabi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.templates.SearchableNameListScreen
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiEvent
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiViewModel

@Composable
fun AsmaUnNabiListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AsmaUnNabiViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    val accent = NamesAccents.prophetNames()

    SearchableNameListScreen(
        title = stringResource(R.string.asma_un_nabi_title),
        searchHint = stringResource(R.string.asma_un_nabi_search_hint),
        searchQuery = state.searchQuery,
        showFavoritesOnly = state.showFavoritesOnly,
        isLoading = state.isLoading,
        items = state.filteredNames,
        accent = accent,
        emptyTitle = stringResource(R.string.asma_un_nabi_no_names_found),
        itemKey = { it.id },
        onNavigateBack = onNavigateBack,
        onSearch = { viewModel.onEvent(AsmaUnNabiEvent.Search(it)) },
        onClearSearch = { viewModel.onEvent(AsmaUnNabiEvent.ClearSearch) },
        onToggleFavoritesFilter = { viewModel.onEvent(AsmaUnNabiEvent.ToggleFavoritesFilter) }
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
                viewModel.onEvent(AsmaUnNabiEvent.ToggleFavorite(name.id))
            }
        )
    }
}
