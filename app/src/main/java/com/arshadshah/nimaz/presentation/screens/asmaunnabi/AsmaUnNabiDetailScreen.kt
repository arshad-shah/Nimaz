package com.arshadshah.nimaz.presentation.screens.asmaunnabi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogDetailHeader
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogDetailScreen
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent

/** One of the Names of the Prophet ﷺ: meaning, explanation, source. */
@Composable
fun AsmaUnNabiDetailScreen(
    nameId: Int,
    onNavigateBack: () -> Unit,
    viewModel: AsmaUnNabiViewModel = hiltViewModel()
) {
    LaunchedEffect(nameId) {
        viewModel.onEvent(CatalogEvent.LoadDetail(nameId))
    }

    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val accent = NamesAccents.prophetNames()

    CatalogDetailScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onToggleFavorite = { viewModel.onEvent(CatalogEvent.ToggleFavorite(it.id)) },
        title = state.item?.nameTransliteration ?: stringResource(R.string.name_detail),
        accent = accent,
        isFavorite = { it.isFavorite },
        header = CatalogDetailHeader(
            number = { it.id },
            arabicName = { it.nameArabic },
            primaryLabel = { it.nameTransliteration },
            secondaryLabel = { it.nameEnglish },
        ),
    ) { name ->
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_meaning),
                content = name.meaning,
                titleColor = accent.contentTint
            )
        }
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_explanation),
                content = name.explanation,
                titleColor = accent.contentTint
            )
        }
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_un_nabi_source),
                content = name.source,
                titleColor = accent.contentTint
            )
        }
    }
}
