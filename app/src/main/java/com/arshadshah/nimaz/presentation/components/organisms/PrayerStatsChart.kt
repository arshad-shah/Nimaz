package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class for a summary stat item displayed below charts.
 */
data class ChartStatItem(
    val value: String,
    val label: String,
    val color: Color
)

/**
 * Prayer statistics chart with multiple visualization options.
 *
 * @param stats The prayer statistics data to display.
 * @param modifier Modifier for the card.
 * @param chartType The type of chart to render.
 * @param title The card title.
 * @param subtitle Optional subtitle displayed below the title.
 * @param summaryItems Controls the summary row below the chart:
 *   - null → show default summary (current streak, longest streak, jamaah)
 *   - empty list → show no summary row
 *   - non-empty list → render the provided items
 */
@Composable
fun PrayerStatsChart(
    stats: PrayerStats,
    modifier: Modifier = Modifier,
    chartType: PrayerChartType = PrayerChartType.DONUT,
    title: String = "Prayer Statistics",
    subtitle: String? = null,
    summaryItems: List<ChartStatItem>? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (chartType) {
                PrayerChartType.DONUT -> DonutChart(stats = stats)
                PrayerChartType.BAR -> BarChart(stats = stats)
                PrayerChartType.RADIAL -> RadialChart(stats = stats)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats summary
            when {
                summaryItems == null -> StatsSummary(stats = stats)
                summaryItems.isNotEmpty() -> StatsSummaryRow(items = summaryItems)
                // empty list → show nothing
            }
        }
    }
}

enum class PrayerChartType {
    DONUT,
    BAR,
    RADIAL
}

/**
 * Donut chart showing prayed vs missed ratio.
 */
@Composable
private fun DonutChart(
    stats: PrayerStats,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val totalPrayers = stats.totalPrayed + stats.totalMissed
    val prayedPercentage = if (totalPrayers > 0) {
        stats.totalPrayed.toFloat() / totalPrayers
    } else 0f

    val animatedPercentage by animateFloatAsState(
        targetValue = if (animationPlayed) prayedPercentage else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "donut_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset(
                (size.width - radius * 2) / 2,
                (size.height - radius * 2) / 2
            )

            // Background arc
            drawArc(
                color = Color.Gray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Prayed arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NimazColors.StatusColors.Prayed,
                        NimazColors.StatusColors.Prayed.copy(alpha = 0.7f)
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedPercentage * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedPercentage * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = NimazColors.StatusColors.Prayed
            )
            Text(
                text = "Completed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Legend
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LegendItem(
            color = NimazColors.StatusColors.Prayed,
            label = "Prayed",
            value = stats.totalPrayed
        )
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(
            color = NimazColors.StatusColors.Missed,
            label = "Missed",
            value = stats.totalMissed
        )
        if (stats.totalJamaah > 0) {
            Spacer(modifier = Modifier.width(24.dp))
            LegendItem(
                color = NimazColors.StatusColors.Jamaah,
                label = "Jamaah",
                value = stats.totalJamaah
            )
        }
    }
}

/**
 * Bar chart showing prayer breakdown by type.
 */
@Composable
private fun BarChart(
    stats: PrayerStats,
    modifier: Modifier = Modifier
) {
    val prayers = PrayerName.entries.filter { it != PrayerName.SUNRISE }
    val maxValue = (stats.prayedByPrayer.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        prayers.forEach { prayer ->
            val prayed = stats.prayedByPrayer[prayer] ?: 0
            val missed = stats.missedByPrayer[prayer] ?: 0
            val total = prayed + missed

            PrayerBar(
                prayerName = prayer.displayName(),
                prayedCount = prayed,
                totalCount = total,
                maxValue = maxValue,
                prayerColor = getPrayerColor(prayer)
            )
        }
    }
}

@Composable
private fun PrayerBar(
    prayerName: String,
    prayedCount: Int,
    totalCount: Int,
    maxValue: Int,
    prayerColor: Color,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val percentage = if (totalCount > 0) prayedCount.toFloat() / totalCount else 0f
    val barWidth = if (maxValue > 0) totalCount.toFloat() / maxValue else 0f

    val animatedWidth by animateFloatAsState(
        targetValue = if (animationPlayed) barWidth else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar_animation"
    )

    val animatedPercentage by animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "percentage_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prayerName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(prayerColor.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPercentage)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(prayerColor)
                )
            }
        }

        Text(
            text = "$prayedCount/$totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

/**
 * Radial chart showing all prayers in a radar-like view.
 */
@Composable
private fun RadialChart(
    stats: PrayerStats,
    modifier: Modifier = Modifier
) {
    val prayers = PrayerName.entries.filter { it != PrayerName.SUNRISE }

    var animationPlayed by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "radial_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        val chartPrimaryColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 20.dp.toPx()
            val angleStep = 360f / prayers.size

            // Draw grid circles
            for (i in 1..4) {
                val radius = maxRadius * i / 4
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Draw lines to each prayer
            prayers.forEachIndexed { index, _ ->
                val angle = Math.toRadians((index * angleStep - 90).toDouble())
                val endX = center.x + maxRadius * cos(angle).toFloat()
                val endY = center.y + maxRadius * sin(angle).toFloat()

                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw data polygon
            val path = Path()
            prayers.forEachIndexed { index, prayer ->
                val prayed = stats.prayedByPrayer[prayer] ?: 0
                val missed = stats.missedByPrayer[prayer] ?: 0
                val total = prayed + missed
                val percentage = if (total > 0) prayed.toFloat() / total else 0f
                val radius = maxRadius * percentage * animatedProgress

                val angle = Math.toRadians((index * angleStep - 90).toDouble())
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            // Fill
            drawPath(
                path = path,
                color = chartPrimaryColor.copy(alpha = 0.3f)
            )

            // Stroke
            drawPath(
                path = path,
                color = chartPrimaryColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw points
            prayers.forEachIndexed { index, prayer ->
                val prayed = stats.prayedByPrayer[prayer] ?: 0
                val missed = stats.missedByPrayer[prayer] ?: 0
                val total = prayed + missed
                val percentage = if (total > 0) prayed.toFloat() / total else 0f
                val radius = maxRadius * percentage * animatedProgress

                val angle = Math.toRadians((index * angleStep - 90).toDouble())
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()

                drawCircle(
                    color = getPrayerColor(prayer),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val totalPrayers = stats.totalPrayed + stats.totalMissed
            val percentage = if (totalPrayers > 0) {
                (stats.totalPrayed.toFloat() / totalPrayers * 100).toInt()
            } else 0
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Prayer labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        prayers.forEach { prayer ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getPrayerColor(prayer))
                )
                Text(
                    text = prayer.displayName().take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Default statistics summary row (shown when summaryItems is null).
 */
@Composable
private fun StatsSummary(
    stats: PrayerStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = stats.currentStreak.toString(),
            label = "Current\nStreak",
            color = NimazColors.StatusColors.Prayed
        )
        StatItem(
            value = stats.longestStreak.toString(),
            label = "Longest\nStreak",
            color = MaterialTheme.colorScheme.secondary
        )
        StatItem(
            value = stats.totalJamaah.toString(),
            label = "In\nJamaah",
            color = NimazColors.StatusColors.Jamaah
        )
    }
}

/**
 * Custom summary row driven by ChartStatItem list.
 */
@Composable
private fun StatsSummaryRow(
    items: List<ChartStatItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { item ->
            StatItem(
                value = item.value,
                label = item.label,
                color = item.color
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getPrayerColor(prayerName: PrayerName) = when (prayerName) {
    PrayerName.FAJR -> NimazColors.PrayerColors.Fajr
    PrayerName.SUNRISE -> NimazColors.PrayerColors.Sunrise
    PrayerName.DHUHR -> NimazColors.PrayerColors.Dhuhr
    PrayerName.ASR -> NimazColors.PrayerColors.Asr
    PrayerName.MAGHRIB -> NimazColors.PrayerColors.Maghrib
    PrayerName.ISHA -> NimazColors.PrayerColors.Isha
}

private val samplePrayerStats = PrayerStats(
    totalPrayed = 120,
    totalMissed = 30,
    totalJamaah = 45,
    prayedByPrayer = mapOf(
        PrayerName.FAJR to 20,
        PrayerName.SUNRISE to 0,
        PrayerName.DHUHR to 28,
        PrayerName.ASR to 25,
        PrayerName.MAGHRIB to 27,
        PrayerName.ISHA to 20
    ),
    missedByPrayer = mapOf(
        PrayerName.FAJR to 10,
        PrayerName.SUNRISE to 0,
        PrayerName.DHUHR to 2,
        PrayerName.ASR to 5,
        PrayerName.MAGHRIB to 3,
        PrayerName.ISHA to 10
    ),
    currentStreak = 7,
    longestStreak = 21,
    perfectDays = 15,
    startDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
    endDate = System.currentTimeMillis()
)

@Preview(showBackground = true)
@Composable
private fun PrayerStatsChartDonutPreview() {
    NimazTheme {
        PrayerStatsChart(
            stats = samplePrayerStats,
            chartType = PrayerChartType.DONUT,
            title = "Prayer Completion",
            subtitle = "January 2026",
            summaryItems = listOf(
                ChartStatItem("120", "Prayed", NimazColors.StatusColors.Prayed),
                ChartStatItem("30", "Missed", NimazColors.StatusColors.Missed),
                ChartStatItem("15", "Perfect\nDays", NimazColors.PrayerColors.Maghrib),
                ChartStatItem("7", "Current\nStreak", NimazColors.StatusColors.Prayed),
                ChartStatItem("21", "Longest\nStreak", Color(0xFF6366F1))
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerStatsChartBarPreview() {
    NimazTheme {
        PrayerStatsChart(
            stats = samplePrayerStats,
            chartType = PrayerChartType.BAR,
            title = "Prayer Breakdown",
            summaryItems = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
