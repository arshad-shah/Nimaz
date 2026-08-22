package com.arshadshah.nimaz.presentation.screens.asma

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogDetailHeader
import com.arshadshah.nimaz.presentation.screens.catalog.CatalogDetailScreen
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.content.AsmaUlHusnaViewModel
import com.arshadshah.nimaz.presentation.viewmodel.content.CatalogEvent

/**
 * One of the ninety-nine Names: meaning, explanation, benefits, the verses it appears in, and
 * its use in duʿāʾ.
 *
 * The Qur'an references are why [CatalogDetailScreen] takes a `LazyListScope` slot rather than
 * a list of (title, body) pairs — they are a `FlowRow` of chips, not prose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsmaUlHusnaDetailScreen(
    nameId: Int,
    onNavigateBack: () -> Unit,
    viewModel: AsmaUlHusnaViewModel = hiltViewModel()
) {
    LaunchedEffect(nameId) {
        viewModel.onEvent(CatalogEvent.LoadDetail(nameId))
    }

    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val accent = NamesAccents.allah()

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
                content = name.meaning
            )
        }
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_explanation),
                content = name.explanation
            )
        }
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_benefits),
                content = name.benefits
            )
        }

        if (name.quranReferences.isNotEmpty()) {
            item {
                NimazCard(
                    modifier = Modifier.fillMaxWidth(),
                    style = NimazCardStyle.ELEVATED,
                    tone = NimazTone.NEUTRAL
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

        item {
            NameDetailSectionCard(
                title = stringResource(R.string.asma_ul_husna_usage_in_dua),
                content = name.usageInDua
            )
        }
    }
}
