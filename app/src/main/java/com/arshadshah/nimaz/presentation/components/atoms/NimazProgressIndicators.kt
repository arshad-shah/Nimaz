package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * Indeterminate circular loading indicator.
 */
@Composable
fun NimazLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = strokeWidth
    )
}

/**
 * Indeterminate linear loading indicator.
 */
@Composable
fun NimazLinearLoading(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Determinate linear progress indicator.
 */
@Composable
fun NimazLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
    animated: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val displayProgress = if (animated) animatedProgress else progress.coerceIn(0f, 1f)

    LinearProgressIndicator(
        progress = { displayProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
        color = color,
        trackColor = trackColor
    )
}

/**
 * Determinate circular progress indicator with center text.
 */
@Composable
fun NimazCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = true,
    animated: Boolean = true
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val displayProgress = if (animated) animatedProgress else progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = 360 * displayProgress
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )

            // Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx()),
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)
            )

            // Progress
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke,
                size = Size(size.toPx() - strokeWidth.toPx(), size.toPx() - strokeWidth.toPx()),
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2)
            )
        }

        if (showPercentage) {
            Text(
                text = "${(displayProgress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Progress with label and percentage.
 */
@Composable
fun LabeledProgress(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    showPercentage: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (showPercentage) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        NimazLinearProgress(
            progress = progress,
            color = color
        )
    }
}

/**
 * Step progress indicator for multi-step processes.
 */
@Composable
fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    completedColor: Color = MaterialTheme.colorScheme.primary,
    stepSize: Dp = 24.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..totalSteps) {
            val color = when {
                step < currentStep -> completedColor
                step == currentStep -> activeColor
                else -> inactiveColor
            }

            Box(
                modifier = Modifier
                    .size(stepSize)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (step <= currentStep) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (step < totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            if (step < currentStep) completedColor else inactiveColor
                        )
                )
            }
        }
    }
}

/**
 * Prayer completion progress ring.
 */
@Composable
fun PrayerProgressRing(
    completedPrayers: Int,
    totalPrayers: Int = 5,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 10.dp,
    completedColor: Color = MaterialTheme.colorScheme.primary,
    remainingColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val progress = completedPrayers.toFloat() / totalPrayers

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        NimazCircularProgress(
            progress = progress,
            size = size,
            strokeWidth = strokeWidth,
            color = completedColor,
            trackColor = remainingColor,
            showPercentage = false
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedPrayers/$totalPrayers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = completedColor
            )
            Text(
                text = "prayers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Fasting streak progress bar.
 */
@Composable
fun StreakProgress(
    currentStreak: Int,
    targetStreak: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    val progress = (currentStreak.toFloat() / targetStreak).coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = currentStreak.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "day streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Target: $targetStreak days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        NimazLinearProgress(
            progress = progress,
            color = color,
            height = 12.dp
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Loading Spinner")
@Composable
private fun NimazLoadingSpinnerPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NimazLoadingSpinner()
        }
    }
}

@Preview(showBackground = true, name = "Linear Loading")
@Composable
private fun NimazLinearLoadingPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            NimazLinearLoading()
        }
    }
}

@Preview(showBackground = true, name = "Linear Progress")
@Composable
private fun NimazLinearProgressPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazLinearProgress(progress = 0.25f)
            NimazLinearProgress(progress = 0.5f)
            NimazLinearProgress(progress = 0.75f)
            NimazLinearProgress(progress = 1.0f)
        }
    }
}

@Preview(showBackground = true, name = "Circular Progress")
@Composable
private fun NimazCircularProgressPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazCircularProgress(progress = 0.25f)
            NimazCircularProgress(progress = 0.75f)
        }
    }
}

@Preview(showBackground = true, name = "Labeled Progress")
@Composable
private fun LabeledProgressPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LabeledProgress(label = "Fajr Completion", progress = 0.8f)
            LabeledProgress(label = "Reading Progress", progress = 0.45f)
        }
    }
}

@Preview(showBackground = true, name = "Step Progress")
@Composable
private fun StepProgressIndicatorPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StepProgressIndicator(currentStep = 1, totalSteps = 4)
            StepProgressIndicator(currentStep = 2, totalSteps = 4)
            StepProgressIndicator(currentStep = 4, totalSteps = 4)
        }
    }
}

@Preview(showBackground = true, name = "Prayer Progress Ring")
@Composable
private fun PrayerProgressRingPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrayerProgressRing(completedPrayers = 2)
            PrayerProgressRing(completedPrayers = 5)
        }
    }
}

@Preview(showBackground = true, name = "Streak Progress")
@Composable
private fun StreakProgressPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StreakProgress(currentStreak = 7, targetStreak = 30)
        }
    }
}
