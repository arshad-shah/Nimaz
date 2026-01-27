package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.ContainedIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazCircularProgress
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.theme.NimazColors

// ==================== PREVIEWS ====================

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Mosque


/**
 * Statistics card with icon and value.
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    trend: StatTrend? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ContainedIcon(
                    imageVector = icon,
                    size = NimazIconSize.MEDIUM,
                    containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                    backgroundColor = iconColor.copy(alpha = 0.15f),
                    iconColor = iconColor
                )

                if (trend != null) {
                    TrendIndicator(trend = trend)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )
            }
        }
    }
}

/**
 * Stat trend direction.
 */
enum class StatTrend(val label: String, val icon: ImageVector, val color: Color) {
    UP("Increase", Icons.Default.TrendingUp, NimazColors.StatusColors.Prayed),
    DOWN("Decrease", Icons.Default.TrendingDown, NimazColors.StatusColors.Missed),
    FLAT("No change", Icons.Default.TrendingFlat, NimazColors.StatusColors.Pending)
}

/**
 * Trend indicator component.
 */
@Composable
private fun TrendIndicator(
    trend: StatTrend,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = trend.icon,
            contentDescription = trend.label,
            tint = trend.color,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Stat card with circular progress indicator.
 */
@Composable
fun ProgressStatCard(
    title: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress circle
            NimazCircularProgress(
                progress = progress,
                size = 60.dp,
                strokeWidth = 6.dp,
                color = progressColor,
                showPercentage = false
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor
                    )
                }
            }

            // Percentage
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
    }
}

/**
 * Compact stat display.
 */
@Composable
fun CompactStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Stats row with multiple values.
 */
@Composable
fun StatsRow(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEach { (title, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Prayer stats summary card.
 */
@Composable
fun PrayerStatsCard(
    prayedCount: Int,
    missedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val progress = if (totalCount > 0) prayedCount.toFloat() / totalCount else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress ring
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                NimazCircularProgress(
                    progress = progress,
                    size = 80.dp,
                    strokeWidth = 8.dp,
                    color = NimazColors.StatusColors.Prayed,
                    trackColor = NimazColors.StatusColors.Missed.copy(alpha = 0.3f),
                    showPercentage = false
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$prayedCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.StatusColors.Prayed
                    )
                    Text(
                        text = "/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Stats
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Prayer Completion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBadge(
                        label = "Prayed",
                        value = prayedCount,
                        color = NimazColors.StatusColors.Prayed
                    )
                    StatBadge(
                        label = "Missed",
                        value = missedCount,
                        color = NimazColors.StatusColors.Missed
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$value $label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Stat Card")
@Composable
private fun StatCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StatCard(
                title = "Prayers This Week",
                value = "28",
                icon = Icons.Default.Star,
                subtitle = "+5 from last week",
                trend = StatTrend.UP
            )
        }
    }
}

@Preview(showBackground = true, name = "Progress Stat Card")
@Composable
private fun ProgressStatCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ProgressStatCard(
                title = "Weekly Goal",
                value = "35/50",
                progress = 0.7f,
                subtitle = "Keep going!"
            )
        }
    }
}

@Preview(showBackground = true, name = "Compact Stat")
@Composable
private fun CompactStatPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CompactStat(title = "Streak", value = "7 days", icon = Icons.Default.Star)
            CompactStat(title = "Total", value = "245")
        }
    }
}

@Preview(showBackground = true, name = "Stats Row")
@Composable
private fun StatsRowPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StatsRow(
                stats = listOf(
                    "Prayed" to "28",
                    "Missed" to "2",
                    "Qada" to "1"
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Prayer Stats Card")
@Composable
private fun PrayerStatsCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PrayerStatsCard(
                prayedCount = 4,
                missedCount = 1,
                totalCount = 5
            )
        }
    }
}
