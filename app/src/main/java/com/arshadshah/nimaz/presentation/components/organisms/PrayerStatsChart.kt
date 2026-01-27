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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStats
import com.arshadshah.nimaz.presentation.theme.NimazColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class for daily prayer completion.
 */
data class DailyPrayerData(
    val dayLabel: String,
    val prayedCount: Int,
    val totalPrayers: Int = 5
)

/**
 * Prayer statistics chart with multiple visualization options.
 */
@Composable
fun PrayerStatsChart(
    stats: PrayerStats,
    modifier: Modifier = Modifier,
    chartType: PrayerChartType = PrayerChartType.DONUT
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
                text = "Prayer Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (chartType) {
                PrayerChartType.DONUT -> DonutChart(stats = stats)
                PrayerChartType.BAR -> BarChart(stats = stats)
                PrayerChartType.RADIAL -> RadialChart(stats = stats)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats summary
            StatsSummary(stats = stats)
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
    val maxValue = (stats.prayedByPrayer.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PrayerName.entries.forEach { prayer ->
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
            val prayers = PrayerName.entries
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
        PrayerName.entries.forEach { prayer ->
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
 * Statistics summary row.
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

/**
 * Weekly prayer completion chart.
 */
@Composable
fun WeeklyPrayerChart(
    weekData: List<DailyPrayerData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Text(
                text = "This Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekData.forEach { dayData ->
                    DayColumn(
                        dayLabel = dayData.dayLabel,
                        prayedCount = dayData.prayedCount,
                        totalPrayers = dayData.totalPrayers
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    dayLabel: String,
    prayedCount: Int,
    totalPrayers: Int,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val percentage = prayedCount.toFloat() / totalPrayers

    val animatedHeight by animateFloatAsState(
        targetValue = if (animationPlayed) percentage else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "day_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$prayedCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(24.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(animatedHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            percentage >= 1f -> NimazColors.StatusColors.Prayed
                            percentage >= 0.6f -> NimazColors.StatusColors.Pending
                            else -> NimazColors.StatusColors.Missed.copy(alpha = 0.5f)
                        }
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = dayLabel,
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

/**
 * Standalone donut chart for prayer stats screen.
 */
@Composable
fun PrayerStatsDonutChart(
    prayed: Int,
    late: Int,
    missed: Int,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val total = prayed + late + missed
    val prayedPercentage = if (total > 0) prayed.toFloat() / total else 0f
    val latePercentage = if (total > 0) late.toFloat() / total else 0f

    val animatedPrayed by animateFloatAsState(
        targetValue = if (animationPlayed) prayedPercentage else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "prayed_animation"
    )

    val animatedLate by animateFloatAsState(
        targetValue = if (animationPlayed) latePercentage else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "late_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

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
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 24.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )

                    // Background arc (missed)
                    drawArc(
                        color = NimazColors.StatusColors.Missed.copy(alpha = 0.3f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Late arc
                    drawArc(
                        color = NimazColors.StatusColors.Late,
                        startAngle = -90f,
                        sweepAngle = (animatedPrayed + animatedLate) * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Prayed arc
                    drawArc(
                        color = NimazColors.StatusColors.Prayed,
                        startAngle = -90f,
                        sweepAngle = animatedPrayed * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val completionRate = if (total > 0) {
                        ((prayed + late).toFloat() / total * 100).toInt()
                    } else 0
                    Text(
                        text = "$completionRate%",
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
                    label = "On Time",
                    value = prayed
                )
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(
                    color = NimazColors.StatusColors.Late,
                    label = "Late",
                    value = late
                )
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(
                    color = NimazColors.StatusColors.Missed,
                    label = "Missed",
                    value = missed
                )
            }
        }
    }
}
