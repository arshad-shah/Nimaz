package com.arshadshah.nimaz.presentation.screens.asma

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.templates.NameDetailScaffold
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUlHusnaEvent
import com.arshadshah.nimaz.presentation.viewmodel.AsmaUlHusnaViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsmaUlHusnaDetailScreen(
    nameId: Int,
    onNavigateBack: () -> Unit,
    viewModel: AsmaUlHusnaViewModel = hiltViewModel()
) {
    LaunchedEffect(nameId) {
        viewModel.onEvent(AsmaUlHusnaEvent.LoadDetail(nameId))
    }

    val state by viewModel.detailState.collectAsState()
    val accent = NamesAccents.allah()

    NameDetailScaffold(
        item = state.name,
        isLoading = state.isLoading,
        title = state.name?.nameTransliteration ?: stringResource(R.string.name_detail),
        accent = accent,
        isFavorite = { it.isFavorite },
        onNavigateBack = onNavigateBack,
        onToggleFavorite = { viewModel.onEvent(AsmaUlHusnaEvent.ToggleFavorite(it.id)) }
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
                content = name.meaning
            )
        }

        // Explanation Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_explanation),
                content = name.explanation
            )
        }

        // Benefits Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_benefits),
                content = name.benefits
            )
        }

        // Quran References
        if (name.quranReferences.isNotEmpty()) {
            item {
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
                            text = stringResource(R.string.asma_ul_husna_quran_references),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent.contentTint
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                        ) {
                            name.quranReferences.forEach { reference ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = reference,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = accent.chipContainer,
                                        labelColor = accent.onChipContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Usage in Dua Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_usage_in_dua),
                content = name.usageInDua
            )
        }
    }
}
