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
import androidx.compose.material3.TopAppBarDefaults
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
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
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
                onBackClick = onNavigateBack,
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = accent.contentTint,
                    navigationIconContentColor = accent.contentTint
                )
            )
        },
        floatingActionButton = {
            state.name?.let { name ->
                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(AsmaUnNabiEvent.ToggleFavorite(name.id))
                    },
                    containerColor = accent.chipContainer,
                    contentColor = accent.onChipContainer
                ) {
                    Icon(
                        imageVector = if (name.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = if (name.isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (name.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            accent.onChipContainer
                        }
                    )
                }
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
                    DetailSectionCard(
                        title = stringResource(R.string.asma_ul_husna_meaning),
                        content = name.meaning,
                        titleColor = accent.contentTint
                    )
                }

                // Explanation Section
                item {
                    DetailSectionCard(
                        title = stringResource(R.string.asma_ul_husna_explanation),
                        content = name.explanation,
                        titleColor = accent.contentTint
                    )
                }

                // Source Section
                item {
                    DetailSectionCard(
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

@Composable
private fun DetailSectionCard(
    title: String,
    content: String,
    titleColor: androidx.compose.ui.graphics.Color
) {
    if (content.isNotBlank()) {
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.FILLED,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(NimazSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
