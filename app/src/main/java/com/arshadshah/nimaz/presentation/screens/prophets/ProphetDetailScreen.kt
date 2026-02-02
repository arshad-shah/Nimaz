package com.arshadshah.nimaz.presentation.screens.prophets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.ProphetEvent
import com.arshadshah.nimaz.presentation.viewmodel.ProphetViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = state.prophet?.nameEnglish ?: stringResource(R.string.prophet_detail),
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            state.prophet?.let { prophet ->
                FloatingActionButton(
                    onClick = {
                        viewModel.onEvent(ProphetEvent.ToggleFavorite(prophet.id))
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = if (prophet.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = if (prophet.isFavorite) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(R.string.add_to_favorites)
                        },
                        tint = if (prophet.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (state.isLoading || state.prophet == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val prophet = state.prophet!!
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
                // Gradient Header
                item {
                    NimazCard(
                        modifier = Modifier.fillMaxWidth(),
                        style = NimazCardStyle.FILLED,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    shape = RoundedCornerShape(NimazSpacing.Large)
                                )
                                .padding(NimazSpacing.ExtraLarge),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
                            ) {
                                ArabicText(
                                    text = prophet.nameArabic,
                                    size = ArabicTextSize.LARGE,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = prophet.nameEnglish,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = prophet.titleEnglish,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Story Section
                item {
                    DetailSectionCard(
                        title = stringResource(R.string.prophets_story),
                        content = prophet.storySummary
                    )
                }

                // Key Lessons Section
                if (prophet.keyLessons.isNotEmpty()) {
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
                                    text = stringResource(R.string.prophets_key_lessons),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                prophet.keyLessons.forEach { lesson ->
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
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = lesson,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
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
                                    color = MaterialTheme.colorScheme.onSurface
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
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                                color = MaterialTheme.colorScheme.onSurface
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
                                    text = stringResource(R.string.prophets_miracles),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                prophet.miracles.forEach { miracle ->
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
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = miracle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
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

@Composable
private fun DetailSectionCard(
    title: String,
    content: String
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
                    color = MaterialTheme.colorScheme.onSurface
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
