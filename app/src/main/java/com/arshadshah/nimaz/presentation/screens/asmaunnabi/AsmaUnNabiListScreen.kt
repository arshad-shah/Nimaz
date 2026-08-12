package com.arshadshah.nimaz.presentation.screens.asmaunnabi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogListScreen
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent

/** The Names of the Prophet ﷺ. See [CatalogListScreen] for the shape all three catalogs share. */
@Composable
fun AsmaUnNabiListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: AsmaUnNabiViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val accent = NamesAccents.prophetNames()

    CatalogListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        title = stringResource(R.string.asma_un_nabi_title),
        searchHint = stringResource(R.string.asma_un_nabi_search_hint),
        emptyMessage = stringResource(R.string.asma_un_nabi_no_names_found),
        accent = accent,
        itemKey = { it.id },
    ) { name ->
        NameCard(
            number = name.id,
            arabicName = name.nameArabic,
            primaryLabel = name.nameTransliteration,
            secondaryLabel = name.nameEnglish,
            isFavorite = name.isFavorite,
            accent = accent,
            onClick = { onNavigateToDetail(name.id) },
            onFavoriteClick = { viewModel.onEvent(CatalogEvent.ToggleFavorite(name.id)) },
        )
    }
}
