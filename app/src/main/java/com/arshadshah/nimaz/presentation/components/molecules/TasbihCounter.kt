package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.CounterTextStyles
import com.arshadshah.nimaz.presentation.theme.NimazColors
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Main tasbih counter with circular progress and tap interaction.
 */
@Composable
fun TasbihCounter(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    lapsCompleted: Int = 0,
    size: Dp = 280.dp,
    progressColor: Color = NimazColors.TasbihColors.Counter,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    milestoneColor: Color = NimazColors.TasbihColors.Milestone,
    onTap: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    val progress = (currentCount.toFloat() / targetCount).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main counter circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                ),
            contentAlignment = Alignment.Center
        ) {
            // Progress ring
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.toPx() - strokeWidth) / 2

                // Track
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                drawArc(
                    color = if (progress >= 1f) milestoneColor else progressColor,
                    startAngle = -90f,
                    sweepAngle = 360 * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            }

            // Count display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = currentCount,
                    transitionSpec = {
                        (scaleIn(initialScale = 1.2f) + fadeIn()) togetherWith
                                (scaleOut(targetScale = 0.8f) + fadeOut())
                    },
                    label = "count"
                ) { count ->
                    Text(
                        text = count.toString(),
                        style = CounterTextStyles.counterLarge,
                        color = if (progress >= 1f) milestoneColor else progressColor
                    )
                }

                Text(
                    text = "of $targetCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (lapsCompleted > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LapsBadge(laps = lapsCompleted)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onUndo != null) {
                IconButton(
                    onClick = onUndo,
                    enabled = currentCount > 0 || lapsCompleted > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Undo",
                        tint = if (currentCount > 0 || lapsCompleted > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                text = "Tap to count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(32.dp))

            if (onReset != null) {
                IconButton(
                    onClick = onReset,
                    enabled = currentCount > 0 || lapsCompleted > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = if (currentCount > 0 || lapsCompleted > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Laps completed badge.
 */
@Composable
private fun LapsBadge(
    laps: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NimazColors.TasbihColors.Complete.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$laps ${if (laps == 1) "lap" else "laps"} completed",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = NimazColors.TasbihColors.Complete
        )
    }
}

/**
 * Tasbih counter with dhikr text display.
 */
@Composable
fun TasbihCounterWithDhikr(
    arabicText: String,
    transliteration: String,
    translation: String,
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    lapsCompleted: Int = 0,
    onTap: () -> Unit,
    onUndo: (() -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dhikr text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArabicText(
                    text = arabicText,
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Counter
        TasbihCounter(
            currentCount = currentCount,
            targetCount = targetCount,
            lapsCompleted = lapsCompleted,
            onTap = onTap,
            onUndo = onUndo,
            onReset = onReset
        )
    }
}

/**
 * Compact tasbih counter for smaller displays.
 */
@Composable
fun CompactTasbihCounter(
    currentCount: Int,
    targetCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onTap: () -> Unit
) {
    val progress = (currentCount.toFloat() / targetCount).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 6.dp.toPx()
            val radius = (size.toPx() - strokeWidth) / 2

            drawCircle(
                color = Color.Gray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = NimazColors.TasbihColors.Counter,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )
        }

        Text(
            text = "$currentCount",
            style = CounterTextStyles.counterMedium,
            color = NimazColors.TasbihColors.Counter
        )
    }
}

/**
 * Tasbih session stats display.
 */
@Composable
fun TasbihSessionStats(
    totalCount: Int,
    lapsCompleted: Int,
    duration: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "Total", value = totalCount.toString())
        StatItem(label = "Laps", value = lapsCompleted.toString())
        StatItem(label = "Duration", value = duration)
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Tasbih Counter")
@Composable
private fun TasbihCounterPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihCounter(
                currentCount = 33,
                targetCount = 100,
                lapsCompleted = 0,
                onTap = {},
                onUndo = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tasbih Counter with Laps")
@Composable
private fun TasbihCounterWithLapsPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihCounter(
                currentCount = 100,
                targetCount = 100,
                lapsCompleted = 2,
                onTap = {},
                onUndo = {},
                onReset = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Tasbih Counter")
@Composable
private fun CompactTasbihCounterPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactTasbihCounter(
                currentCount = 25,
                targetCount = 33,
                onTap = {}
            )
            CompactTasbihCounter(
                currentCount = 33,
                targetCount = 33,
                onTap = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tasbih Session Stats")
@Composable
private fun TasbihSessionStatsPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihSessionStats(
                totalCount = 300,
                lapsCompleted = 3,
                duration = "5m 32s"
            )
        }
    }
}

@Preview(showBackground = true, name = "Tasbih Counter with Dhikr")
@Composable
private fun TasbihCounterWithDhikrPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TasbihCounterWithDhikr(
                arabicText = "سُبْحَانَ اللَّهِ",
                transliteration = "SubhanAllah",
                translation = "Glory be to Allah",
                currentCount = 21,
                targetCount = 33,
                lapsCompleted = 1,
                onTap = {},
                onUndo = {},
                onReset = {}
            )
        }
    }
}
