package com.arshadshah.nimaz.presentation.screens.asmaunnabi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.FavoriteFab
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUnNabiViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = state.item?.nameTransliteration ?: stringResource(R.string.name_detail),
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            state.item?.let { name ->
                FavoriteFab(
                    isFavorite = name.isFavorite,
                    accent = accent,
                    onClick = { viewModel.onEvent(CatalogEvent.ToggleFavorite(name.id)) }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading || state.item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                NimazLoadingState()
            }
        } else {
            val name = state.item!!
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

                // Bottom spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
