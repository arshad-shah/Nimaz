package com.arshadshah.nimaz.presentation.screens.prophets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailHeader
import com.arshadshah.nimaz.presentation.components.molecules.NameDetailSectionCard
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccent
import com.arshadshah.nimaz.presentation.components.molecules.NamesAccents
import com.arshadshah.nimaz.presentation.components.templates.NameDetailScaffold
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.ProphetEvent
import com.arshadshah.nimaz.presentation.viewmodel.ProphetViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProphetDetailScreen(
    prophetId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ProphetViewModel = hiltViewModel()
) {
    LaunchedEffect(prophetId) {
        viewModel.onEvent(ProphetEvent.LoadDetail(prophetId))
    }

    val state by viewModel.detailState.collectAsState()
    val accent = NamesAccents.prophets()

    NameDetailScaffold(
        item = state.prophet,
        isLoading = state.isLoading,
        title = state.prophet?.nameEnglish ?: stringResource(R.string.prophet_detail),
        accent = accent,
        isFavorite = { it.isFavorite },
        onNavigateBack = onNavigateBack,
        onToggleFavorite = { viewModel.onEvent(ProphetEvent.ToggleFavorite(it.id)) }
    ) { prophet ->
        // Calligraphic header (no number medallion for prophets)
        item {
            NameDetailHeader(
                arabicName = prophet.nameArabic,
                accent = accent,
                number = null,
                primaryLabel = prophet.nameEnglish,
                secondaryLabel = prophet.titleEnglish,
            )
        }

        // Story Section
        item {
            NameDetailSectionCard(
                title = stringResource(R.string.prophets_story),
                content = prophet.storySummary,
                titleColor = accent.contentTint
            )
        }

        // Key Lessons Section
        if (prophet.keyLessons.isNotEmpty()) {
            item {
                BulletListCard(
                    title = stringResource(R.string.prophets_key_lessons),
                    items = prophet.keyLessons,
                    accent = accent
                )
            }
        }

        // Quran Mentions
        if (prophet.quranMentions.isNotEmpty()) {
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
                            text = stringResource(R.string.prophets_quran_mentions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent.contentTint
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                        ) {
                            prophet.quranMentions.forEach { verse ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = verse,
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

        // Timeline Section
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
                    verticalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
                ) {
                    Text(
                        text = stringResource(R.string.prophets_timeline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent.contentTint
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimelineItem(
                            label = stringResource(R.string.prophets_era),
                            value = prophet.era,
                            modifier = Modifier.weight(1f)
                        )
                        TimelineItem(
                            label = stringResource(R.string.prophets_lineage),
                            value = prophet.lineage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimelineItem(
                            label = stringResource(R.string.prophets_years_lived),
                            value = prophet.yearsLived,
                            modifier = Modifier.weight(1f)
                        )
                        TimelineItem(
                            label = stringResource(R.string.prophets_place),
                            value = prophet.placeOfPreaching,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Miracles Section
        if (prophet.miracles.isNotEmpty()) {
            item {
                BulletListCard(
                    title = stringResource(R.string.prophets_miracles),
                    items = prophet.miracles,
                    accent = accent
                )
            }
        }
    }
}

@Composable
private fun BulletListCard(
    title: String,
    items: List<String>,
    accent: NamesAccent
) {
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
                color = accent.contentTint
            )
            items.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Circle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(8.dp)
                            .padding(top = 6.dp),
                        tint = accent.contentTint
                    )
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(NimazSpacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(NimazSpacing.ExtraSmall)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
