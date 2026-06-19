package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.BeadDesignPickerSheet
import com.arshadshah.nimaz.presentation.components.molecules.tasbih.CurrentTasbihSheet
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.TasbihCounterStyle
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

@Composable
fun TasbihScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToChooseDhikr: () -> Unit = {},
    onNavigateToAddPreset: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val counterState by viewModel.counterState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()

    var showDesignSheet by remember { mutableStateOf(false) }
    var showCurrentSheet by remember { mutableStateOf(false) }

    val beadsMode = counterState.counterStyle == TasbihCounterStyle.BEADS
    val liveTotalToday = statsState.baseTotalToday +
        (counterState.count + counterState.laps * counterState.targetCount)

    if (showDesignSheet) {
        BeadDesignPickerSheet(
            selectedKey = counterState.beadDesignKey,
            onSelect = { viewModel.onEvent(TasbihEvent.SetBeadDesign(it)) },
            leftHanded = counterState.leftHanded,
            onToggleHanded = { viewModel.onEvent(TasbihEvent.SetLeftHanded(it)) },
            onDismiss = { showDesignSheet = false }
        )
    }
    if (showCurrentSheet) {
        CurrentTasbihSheet(
            preset = counterState.selectedPreset,
            targetCount = counterState.targetCount,
            totalToday = liveTotalToday,
            laps = counterState.laps,
            onChangeDhikr = {
                showCurrentSheet = false
                onNavigateToChooseDhikr()
            },
            onDismiss = { showCurrentSheet = false }
        )
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: mode toggle + actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazPillTabs(
                    tabs = listOf(
                        stringResource(R.string.tasbih_mode_beads),
                        stringResource(R.string.tasbih_mode_classic)
                    ),
                    selectedIndex = if (beadsMode) 0 else 1,
                    onTabSelect = { index ->
                        viewModel.onEvent(
                            TasbihEvent.SetCounterStyle(
                                if (index == 0) TasbihCounterStyle.BEADS else TasbihCounterStyle.CLASSIC
                            )
                        )
                    }
                )
                Spacer(Modifier.weight(1f))
                if (beadsMode) {
                    IconButton(onClick = { showDesignSheet = true }) {
                        Icon(Icons.Default.Palette, stringResource(R.string.tasbih_bead_design))
                    }
                }
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, stringResource(R.string.history))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Pill count capsule
            TasbihCountCapsule(
                count = counterState.count,
                target = counterState.targetCount,
                laps = counterState.laps,
                autoLap = counterState.autoLap
            )

            // Counter — beads strand (wide band) or classic ring, crossfaded
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Crossfade(targetState = beadsMode, animationSpec = tween(400), label = "counter_mode") { beads ->
                    if (beads) {
                        TasbihBeads(
                            count = counterState.count + counterState.laps * counterState.targetCount,
                            onIncrement = { viewModel.onEvent(TasbihEvent.Increment) },
                            targetCount = counterState.targetCount,
                            design = BeadDesigns.byKey(counterState.beadDesignKey),
                            leftHanded = counterState.leftHanded,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    } else {
                        CounterCircle(
                            count = counterState.count,
                            targetCount = counterState.targetCount,
                            laps = counterState.laps,
                            onIncrement = { viewModel.onEvent(TasbihEvent.Increment) }
                        )
                    }
                }
            }

            // Controls
            ControlButtons(
                soundEnabled = counterState.soundEnabled,
                vibrationEnabled = counterState.vibrationEnabled,
                onReset = { viewModel.onEvent(TasbihEvent.Reset) },
                onToggleSound = { viewModel.onEvent(TasbihEvent.ToggleSound(!counterState.soundEnabled)) },
                onToggleVibration = { viewModel.onEvent(TasbihEvent.ToggleVibration(!counterState.vibrationEnabled)) }
            )

            Spacer(Modifier.height(12.dp))

            // Current-tasbih peek card → opens the detail sheet
            CurrentTasbihPeek(
                arabic = counterState.selectedPreset?.arabicText,
                name = counterState.selectedPreset?.name ?: stringResource(R.string.free_count_label),
                translation = counterState.selectedPreset?.translation,
                target = counterState.targetCount,
                onClick = { showCurrentSheet = true }
            )
        }
    }
}

@Composable
private fun TasbihCountCapsule(
    count: Int,
    target: Int,
    laps: Int,
    autoLap: Boolean,
    modifier: Modifier = Modifier
) {
    val goalReached = !autoLap && count >= target
    val accent = if (goalReached) NimazColors.TasbihColors.Complete
    else NimazColors.TasbihColors.Milestone

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(7.dp).background(accent, CircleShape))
            Text(
                text = "$count / $target",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            if (laps > 0) {
                Text(
                    text = stringResource(R.string.laps_format, laps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrentTasbihPeek(
    arabic: String?,
    name: String,
    translation: String?,
    target: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!arabic.isNullOrEmpty()) {
                    ArabicText(
                        text = arabic,
                        size = ArabicTextSize.SMALL,
                        color = NimazColors.TasbihColors.Milestone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = translation?.let { "$it · ${stringResource(R.string.target_format, target)}" }
                            ?: stringResource(R.string.target_format, target),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CounterCircle(
    count: Int,
    targetCount: Int,
    laps: Int,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press_scale"
    )
    val progress = if (targetCount > 0) count.toFloat() / targetCount.toFloat() else 0f
    val animatedProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "progress")

    val isComplete = count >= targetCount
    val progressColor by animateColorAsState(
        targetValue = if (isComplete) NimazColors.TasbihColors.Complete else MaterialTheme.colorScheme.primary,
        label = "progress_color"
    )
    val borderAlpha by animateFloatAsState(if (isPressed) 0.8f else 0.4f, label = "border_alpha")

    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val goldColor = NimazColors.TasbihColors.Milestone
    val circleSize = 260.dp

    Box(modifier = modifier.size(circleSize), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxSize().drawBehind {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                val arcSize = Size(radius * 2, radius * 2)
                drawArc(ringTrackColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(progressColor, goldColor, progressColor)),
                        startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        )
        Surface(
            modifier = Modifier
                .size(circleSize - 36.dp)
                .scale(scale)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onIncrement),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    brush = Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.02f)
                        )
                    ),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (laps > 0) stringResource(R.string.laps_format, laps)
                        else stringResource(R.string.tap_to_count),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (laps > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onReset, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Refresh, stringResource(R.string.reset_action),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onToggleSound, modifier = Modifier.size(40.dp)) {
            Icon(
                if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                stringResource(R.string.toggle_sound),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (soundEnabled) 0.8f else 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onToggleVibration, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.PhoneAndroid, stringResource(R.string.toggle_vibration),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (vibrationEnabled) 0.8f else 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
