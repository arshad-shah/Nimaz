package com.arshadshah.nimaz.presentation.screens.prophets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogListScreen
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.ProphetViewModel

/**
 * The Prophets. See [CatalogListScreen] for the shape all three catalogs share — this is the
 * one whose card differs: the English name leads, and a title and era chip ride along.
 */
@Composable
fun ProphetsListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ProphetViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()
    val accent = NamesAccents.prophets()

    CatalogListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        title = stringResource(R.string.prophets_title),
        searchHint = stringResource(R.string.prophets_search_hint),
        emptyMessage = stringResource(R.string.prophets_no_found),
        accent = accent,
        itemKey = { it.id },
    ) { prophet ->
        NameCard(
            number = prophet.id,
            arabicName = prophet.nameArabic,
            primaryLabel = prophet.nameEnglish,
            secondaryLabel = prophet.nameTransliteration,
            isFavorite = prophet.isFavorite,
            accent = accent,
            onClick = { onNavigateToDetail(prophet.id) },
            onFavoriteClick = { viewModel.onEvent(CatalogEvent.ToggleFavorite(prophet.id)) },
            titleLabel = prophet.titleEnglish,
            eraChip = prophet.era,
        )
    }
}
