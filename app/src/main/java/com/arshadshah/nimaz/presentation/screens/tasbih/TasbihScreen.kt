package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToAddPreset: () -> Unit = {},
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
            NimazTopAppBar(
                title = "Tasbih",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                    IconButton(onClick = onNavigateToAddPreset) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Preset"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dhikr Display
            DhikrDisplay(
                selectedPreset = counterState.selectedPreset,
                targetCount = counterState.targetCount,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            // Counter Circle with Progress Ring
            Spacer(modifier = Modifier.weight(0.3f))

            CounterCircle(
                count = counterState.count,
                targetCount = counterState.targetCount,
                onIncrement = { viewModel.onEvent(TasbihEvent.Increment) }
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Stats Row - compute live "Today" total
            val currentSessionCount = counterState.count + (counterState.laps * counterState.targetCount)
            val liveTotalToday = statsState.baseTotalToday + currentSessionCount

            NimazStatsGrid(
                stats = listOf(
                    NimazStatData(value = liveTotalToday.toString(), label = "TODAY"),
                    NimazStatData(value = counterState.laps.toString(), label = "ROUNDS"),
                    NimazStatData(value = statsState.completedSessions.toString(), label = "SESSIONS")
                ),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Section
            Text(
                text = "QUICK PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Default Presets
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = presetsState.defaultPresets,
                    key = { it.id }
                ) { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = counterState.selectedPreset?.id == preset.id,
                        onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) }
                    )
                }

                // Custom presets in the same row
                items(
                    items = presetsState.customPresets,
                    key = { it.id }
                ) { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = counterState.selectedPreset?.id == preset.id,
                        onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Buttons
            ControlButtons(
                soundEnabled = counterState.soundEnabled,
                vibrationEnabled = counterState.vibrationEnabled,
                onReset = { viewModel.onEvent(TasbihEvent.Reset) },
                onToggleSound = {
                    viewModel.onEvent(TasbihEvent.ToggleSound(!counterState.soundEnabled))
                },
                onToggleVibration = {
                    viewModel.onEvent(TasbihEvent.ToggleVibration(!counterState.vibrationEnabled))
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DhikrDisplay(
    selectedPreset: TasbihPreset?,
    targetCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Arabic text in gold with Amiri font
        if (!selectedPreset?.arabicText.isNullOrEmpty()) {
            ArabicText(
                text = selectedPreset?.arabicText ?: "",
                size = ArabicTextSize.EXTRA_LARGE,
                color = NimazColors.TasbihColors.Milestone,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Translation
        Text(
            text = selectedPreset?.translation ?: selectedPreset?.name ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Target count
        Text(
            text = "Target: $targetCount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CounterCircle(
    count: Int,
    targetCount: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val progress = if (targetCount > 0) count.toFloat() / targetCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progress"
    )

    val isComplete = count >= targetCount
    val progressColor by animateColorAsState(
        targetValue = if (isComplete) NimazColors.TasbihColors.Complete
        else MaterialTheme.colorScheme.primary,
        label = "progress_color"
    )

    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val goldColor = NimazColors.TasbihColors.Milestone

    Box(
        modifier = modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Progress Ring (drawn behind)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 8.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2f,
                        (size.height - radius * 2) / 2f
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background ring
                    drawArc(
                        color = ringTrackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc with gradient effect
                    if (animatedProgress > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(progressColor, goldColor, progressColor)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
        )

        // Tappable Counter Button
        Surface(
            modifier = Modifier
                .size(220.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onIncrement
                ),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to count",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetChip(
    preset: TasbihPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(25.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!preset.arabicText.isNullOrEmpty()) {
                ArabicText(
                    text = preset.arabicText ?: "",
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${preset.targetCount}x",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ControlButtons(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onReset: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        Surface(
            onClick = onReset,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Sound toggle
        Surface(
            onClick = onToggleSound,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Sound",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (soundEnabled) 1f else 0.4f
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Vibration toggle
        Surface(
            onClick = onToggleVibration,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Toggle Vibration",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (vibrationEnabled) 1f else 0.4f
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
