package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val presetsState by viewModel.presetsState.collectAsState()
    val counterState by viewModel.counterState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Tasbih",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Counter Section
            item {
                TasbihCounterSection(
                    count = counterState.count,
                    laps = counterState.laps,
                    targetCount = counterState.targetCount,
                    selectedPreset = counterState.selectedPreset,
                    isActive = counterState.isActive,
                    onIncrement = { viewModel.onEvent(TasbihEvent.Increment) },
                    onReset = { viewModel.onEvent(TasbihEvent.Reset) },
                    onStartSession = { viewModel.onEvent(TasbihEvent.StartSession) },
                    onPauseSession = { viewModel.onEvent(TasbihEvent.PauseSession) },
                    onCompleteSession = { viewModel.onEvent(TasbihEvent.CompleteSession) }
                )
            }

            // Today's Stats
            item {
                TodayStatsCard(
                    totalToday = statsState.totalToday,
                    completedSessions = statsState.completedSessions,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Presets Section
            item {
                Text(
                    text = "Dhikr Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = presetsState.defaultPresets,
                        key = { it.id }
                    ) { preset ->
                        PresetCard(
                            preset = preset,
                            isSelected = counterState.selectedPreset?.id == preset.id,
                            onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) }
                        )
                    }
                }
            }

            // Custom Presets
            if (presetsState.customPresets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Custom Presets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = presetsState.customPresets,
                            key = { it.id }
                        ) { preset ->
                            PresetCard(
                                preset = preset,
                                isSelected = counterState.selectedPreset?.id == preset.id,
                                onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasbihCounterSection(
    count: Int,
    laps: Int,
    targetCount: Int,
    selectedPreset: TasbihPreset?,
    isActive: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onCompleteSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(100),
        label = "counter_scale"
    )

    val progress = count.toFloat() / targetCount.toFloat()
    val progressColor by animateColorAsState(
        targetValue = if (count >= targetCount) NimazColors.StatusColors.Prayed else NimazColors.TasbihColors.Counter,
        label = "progress_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                NimazColors.TasbihColors.Counter,
                                NimazColors.TasbihColors.Counter.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Selected Preset Name
                    selectedPreset?.let {
                        Text(
                            text = it.arabicText ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Counter Circle
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onIncrement
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Progress Ring
                        CircularProgressRing(
                            progress = progress.coerceIn(0f, 1f),
                            color = progressColor,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 64.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (laps > 0) {
                                Text(
                                    text = "Lap $laps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Target: $targetCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Text(
                        text = "Tap to count",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Control Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset Button
                        Surface(
                            onClick = onReset,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = Color.White
                                )
                            }
                        }

                        // Play/Pause Button
                        Surface(
                            onClick = if (isActive) onPauseSession else onStartSession,
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isActive) "Pause" else "Start",
                                    tint = NimazColors.TasbihColors.Counter,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Complete Button
                        Surface(
                            onClick = onCompleteSession,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Complete",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Simplified progress ring - in production would use Canvas
    Box(modifier = modifier)
}

@Composable
private fun TodayStatsCard(
    totalToday: Int,
    completedSessions: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Today's Count",
                value = totalToday.toString()
            )
            StatItem(
                label = "Sessions",
                value = completedSessions.toString()
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = NimazColors.TasbihColors.Counter
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetCard(
    preset: TasbihPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                NimazColors.TasbihColors.Counter.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, NimazColors.TasbihColors.Counter)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.arabicText ?: "",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "× ${preset.targetCount}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.TasbihColors.Counter
            )
        }
    }
}
