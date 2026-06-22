package com.arshadshah.nimaz.presentation.screens.asmaunnabi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.templates.NameDetailScaffold
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiEvent
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiViewModel

@Composable
fun AsmaUnNabiDetailScreen(
    nameId: Int,
    onNavigateBack: () -> Unit,
    viewModel: AsmaUnNabiViewModel = hiltViewModel()
) {
    LaunchedEffect(nameId) {
        viewModel.onEvent(AsmaUnNabiEvent.LoadDetail(nameId))
    }

    val state by viewModel.detailState.collectAsState()
    val accent = NamesAccents.prophetNames()

    NameDetailScaffold(
        item = state.name,
        isLoading = state.isLoading,
        title = state.name?.nameTransliteration ?: stringResource(R.string.name_detail),
        accent = accent,
        isFavorite = { it.isFavorite },
        onNavigateBack = onNavigateBack,
        onToggleFavorite = { viewModel.onEvent(AsmaUnNabiEvent.ToggleFavorite(it.id)) }
    ) { name ->
        // Calligraphic header
        item {
            NameDetailHeader(
                arabicName = name.nameArabic,
                accent = accent,
                number = name.id,
                primaryLabel = name.nameTransliteration,
                secondaryLabel = name.nameEnglish,
            )
        }

        // Meaning Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_meaning),
                content = name.meaning,
                titleColor = accent.contentTint
            )
        }

        // Explanation Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_explanation),
                content = name.explanation,
                titleColor = accent.contentTint
            )
        }

        // Source Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_un_nabi_source),
                content = name.source,
                titleColor = accent.contentTint
            )
        }
    }
}
