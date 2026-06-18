package com.arshadshah.nimaz.presentation.screens.tasbih

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TasbihPreset
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.TasbihCounterUiState
import com.arshadshah.nimaz.presentation.viewmodel.TasbihEvent
import com.arshadshah.nimaz.presentation.viewmodel.TasbihPresetsUiState
import com.arshadshah.nimaz.presentation.viewmodel.TasbihStatsUiState
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
                title = stringResource(R.string.tasbih_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.history)
                        )
                    }
                    IconButton(onClick = onNavigateToAddPreset) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_preset)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = currentWindowSizeClass()

        if (windowSizeClass.isCompact) {
            // Phone layout: horizontal preset row + counter below
            TasbihCompactContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                presetsState = presetsState,
                counterState = counterState,
                statsState = statsState,
                viewModel = viewModel,
            )
        } else {
            // Tablet layout: presets sidebar left, enlarged counter right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Left sidebar: presets list (vertical)
                Column(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tasbih_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "free_count") {
                            FreeCountChip(
                                isSelected = counterState.selectedPreset == null,
                                onClick = { viewModel.onEvent(TasbihEvent.ClearPreset) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(
                            items = presetsState.defaultPresets,
                            key = { it.id }
                        ) { preset ->
                            PresetChip(
                                preset = preset,
                                isSelected = counterState.selectedPreset?.id == preset.id,
                                onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(
                            items = presetsState.customPresets,
                            key = { it.id }
                        ) { preset ->
                            PresetChip(
                                preset = preset,
                                isSelected = counterState.selectedPreset?.id == preset.id,
                                onClick = { viewModel.onEvent(TasbihEvent.SelectPreset(preset)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Right side: enlarged counter
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                ) {
                    val availableForCircle = maxHeight - 200.dp
                    val circleSize = availableForCircle.coerceIn(220.dp, 380.dp)
                    val counterFontSize = if (circleSize < 280.dp) 64.sp
                        else if (circleSize < 340.dp) 76.sp
                        else 88.sp

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Dhikr context
                        DhikrDisplay(
                            selectedPreset = counterState.selectedPreset,
                            targetCount = counterState.targetCount,
                            onTargetCountChange = { viewModel.onEvent(TasbihEvent.SetTargetCount(it)) },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Counter circle — enlarged on tablet
                        CounterCircle(
                            count = counterState.count,
                            targetCount = counterState.targetCount,
                            laps = counterState.laps,
                            onIncrement = { viewModel.onEvent(TasbihEvent.Increment) },
                            circleSize = circleSize,
                            counterFontSize = counterFontSize
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Inline stats
                        val currentSessionCount =
                            counterState.count + (counterState.laps * counterState.targetCount)
                        val liveTotalToday = statsState.baseTotalToday + currentSessionCount

                        InlineStats(
                            totalToday = liveTotalToday,
                            laps = counterState.laps,
                            sessions = statsState.completedSessions
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Control buttons
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
                    }
                }
            }
        }
    }
}

@Composable
private fun TasbihCompactContent(
    modifier: Modifier = Modifier,
    presetsState: TasbihPresetsUiState,
    counterState: TasbihCounterUiState,
    statsState: TasbihStatsUiState,
    viewModel: TasbihViewModel,
) {
    BoxWithConstraints(modifier = modifier) {
        val isSmallHeight = maxHeight < 600.dp
        val availableForCircle = maxHeight - 240.dp
        val circleSize = availableForCircle.coerceIn(180.dp, 300.dp)
        val counterFontSize = if (circleSize < 220.dp) 52.sp
            else if (circleSize < 270.dp) 64.sp
            else 76.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Presets row — compact, no label
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = if (isSmallHeight) 4.dp else 8.dp)
            ) {
                item(key = "free_count") {
                    FreeCountChip(
                        isSelected = counterState.selectedPreset == null,
                        onClick = { viewModel.onEvent(TasbihEvent.ClearPreset) }
                    )
                }
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

            // Push everything below to center/bottom
            Spacer(modifier = Modifier.weight(1f))

            // Dhikr context — sits just above the counter
            DhikrDisplay(
                selectedPreset = counterState.selectedPreset,
                targetCount = counterState.targetCount,
                onTargetCountChange = { viewModel.onEvent(TasbihEvent.SetTargetCount(it)) },
                modifier = Modifier.padding(bottom = if (isSmallHeight) 8.dp else 12.dp)
            )

            // Counter circle — the hero
            CounterCircle(
                count = counterState.count,
                targetCount = counterState.targetCount,
                laps = counterState.laps,
                onIncrement = { viewModel.onEvent(TasbihEvent.Increment) },
                circleSize = circleSize,
                counterFontSize = counterFontSize
            )

            Spacer(modifier = Modifier.height(if (isSmallHeight) 10.dp else 16.dp))

            // Inline stats — minimal text row
            val currentSessionCount =
                counterState.count + (counterState.laps * counterState.targetCount)
            val liveTotalToday = statsState.baseTotalToday + currentSessionCount

            InlineStats(
                totalToday = liveTotalToday,
                laps = counterState.laps,
                sessions = statsState.completedSessions
            )

            Spacer(modifier = Modifier.height(if (isSmallHeight) 10.dp else 16.dp))

            // Control buttons
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

            Spacer(modifier = Modifier.height(if (isSmallHeight) 8.dp else 16.dp))
        }
    }
}

@Composable
private fun DhikrDisplay(
    selectedPreset: TasbihPreset?,
    targetCount: Int,
    onTargetCountChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isFreeCount = selectedPreset == null
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(targetCount) { mutableStateOf(targetCount.toString()) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!selectedPreset?.arabicText.isNullOrEmpty()) {
            ArabicText(
                text = selectedPreset.arabicText,
                size = ArabicTextSize.LARGE,
                color = NimazColors.TasbihColors.Milestone,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Name / translation
        val displayText = selectedPreset?.translation ?: selectedPreset?.name
            ?: stringResource(R.string.free_count_label)
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Target — editable in free count mode
        if (isFreeCount && isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            editText = value
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val newTarget = editText.toIntOrNull()?.coerceIn(1, 9999) ?: targetCount
                            onTargetCountChange(newTarget)
                            isEditing = false
                            focusManager.clearFocus()
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                IconButton(
                    onClick = {
                        val newTarget = editText.toIntOrNull()?.coerceIn(1, 9999) ?: targetCount
                        onTargetCountChange(newTarget)
                        isEditing = false
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (isFreeCount) Modifier.clickable { isEditing = true } else Modifier
            ) {
                Text(
                    text = stringResource(R.string.target_format, targetCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                if (isFreeCount) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                }
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
    circleSize: Dp = 260.dp,
    counterFontSize: TextUnit = 72.sp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )
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
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0.4f,
        label = "border_alpha"
    )

    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val goldColor = NimazColors.TasbihColors.Milestone
    val innerSize = circleSize - 36.dp

    Box(
        modifier = modifier
            .size(circleSize),
        contentAlignment = Alignment.Center
    ) {
        // Progress Ring — does NOT animate with press
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeWidth = 6.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2f
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2f,
                        (size.height - radius * 2) / 2f
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    drawArc(
                        color = ringTrackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

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

        // Tappable counter — animates on press
        Surface(
            modifier = Modifier
                .size(innerSize)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onIncrement
                ),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
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
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = counterFontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (laps > 0) {
                        Text(
                            text = stringResource(R.string.laps_format, laps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tap_to_count),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineStats(
    totalToday: Int,
    laps: Int,
    sessions: Int,
    modifier: Modifier = Modifier
) {
    val separator = "  ·  "
    val statsText = buildString {
        append(stringResource(R.string.today_upper))
        append(" ")
        append(totalToday)
        append(separator)
        append(stringResource(R.string.sessions_upper))
        append(" ")
        append(sessions)
    }

    Text(
        text = statsText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeCountChip(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Text(
            text = stringResource(R.string.free_count),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!preset.arabicText.isNullOrEmpty()) {
                ArabicText(
                    text = preset.arabicText,
                    size = ArabicTextSize.SMALL,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${preset.targetCount}x",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
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
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onReset, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.reset_action),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onToggleSound, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(R.string.toggle_sound),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (soundEnabled) 0.8f else 0.3f
                ),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onToggleVibration, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = stringResource(R.string.toggle_vibration),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (vibrationEnabled) 0.8f else 0.3f
                ),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
