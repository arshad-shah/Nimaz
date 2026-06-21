package com.arshadshah.nimaz.presentation.screens.asmaunnabi

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.molecules.FavoriteFab
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiEvent
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUnNabiViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = state.name?.nameTransliteration ?: stringResource(R.string.name_detail),
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            state.name?.let { name ->
                FavoriteFab(
                    isFavorite = name.isFavorite,
                    accent = accent,
                    onClick = { viewModel.onEvent(AsmaUnNabiEvent.ToggleFavorite(name.id)) }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading || state.name == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val name = state.name!!
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
