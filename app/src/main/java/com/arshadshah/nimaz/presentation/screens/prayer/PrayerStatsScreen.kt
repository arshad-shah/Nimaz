package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.components.organisms.ChartStatItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerChartType
import com.arshadshah.nimaz.presentation.components.organisms.PrayerStatsChart
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.StatsPeriod
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val state by viewModel.statsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Statistics",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Period Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.period == period,
                            onClick = { viewModel.onEvent(PrayerTrackerEvent.SetStatsPeriod(period)) },
                            label = {
                                Text(
                                    text = when (period) {
                                        StatsPeriod.WEEK -> "Week"
                                        StatsPeriod.MONTH -> "Month"
                                        StatsPeriod.YEAR -> "Year"
                                        StatsPeriod.ALL_TIME -> "All Time"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Donut Chart — replaces OverviewCard + StreakCard
            item {
                state.stats?.let { stats ->
                    val periodLabel = try {
                        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
                        val startDate = Instant.ofEpochMilli(stats.startDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        startDate.format(formatter)
                    } catch (_: Exception) {
                        when (state.period) {
                            StatsPeriod.WEEK -> "This Week"
                            StatsPeriod.MONTH -> "This Month"
                            StatsPeriod.YEAR -> "This Year"
                            StatsPeriod.ALL_TIME -> "All Time"
                        }
                    }

                    PrayerStatsChart(
                        stats = stats,
                        chartType = PrayerChartType.DONUT,
                        title = "Prayer Completion",
                        subtitle = periodLabel,
                        summaryItems = listOf(
                            ChartStatItem(
                                "${stats.totalPrayed}",
                                "Prayed",
                                NimazColors.StatusColors.Prayed
                            ),
                            ChartStatItem(
                                "${stats.totalMissed}",
                                "Missed",
                                NimazColors.StatusColors.Missed
                            ),
                            ChartStatItem(
                                "${stats.perfectDays}",
                                "Perfect\nDays",
                                NimazColors.PrayerColors.Maghrib
                            ),
                            ChartStatItem(
                                "${state.currentStreak}",
                                "Current\nStreak",
                                NimazColors.StatusColors.Prayed
                            ),
                            ChartStatItem(
                                "${state.longestStreak}",
                                "Longest\nStreak",
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                }
            }

            // Bar Chart — replaces PrayerBreakdownSection
            item {
                state.stats?.let { stats ->
                    PrayerStatsChart(
                        stats = stats,
                        chartType = PrayerChartType.BAR,
                        title = "Prayer Breakdown",
                        summaryItems = emptyList()
                    )
                }
            }

            // Insights
            item {
                state.stats?.let { stats ->
                    InsightsSection(stats = stats)
                }
            }
        }
    }
}

// --- Insights ---

@Composable
private fun InsightsSection(
    stats: com.arshadshah.nimaz.domain.model.PrayerStats,
    modifier: Modifier = Modifier
) {
    // Determine insights from data
    val prayerNames = listOf(
        PrayerName.FAJR to "Fajr",
        PrayerName.DHUHR to "Dhuhr",
        PrayerName.ASR to "Asr",
        PrayerName.MAGHRIB to "Maghrib",
        PrayerName.ISHA to "Isha"
    )

    // Find weakest prayer
    val weakest = prayerNames.minByOrNull { (prayer, _) ->
        val prayed = stats.prayedByPrayer[prayer] ?: 0
        val missed = stats.missedByPrayer[prayer] ?: 0
        val total = prayed + missed
        if (total > 0) prayed.toFloat() / total else 1f
    }

    // Find strongest prayer
    val strongest = prayerNames.maxByOrNull { (prayer, _) ->
        val prayed = stats.prayedByPrayer[prayer] ?: 0
        val missed = stats.missedByPrayer[prayer] ?: 0
        val total = prayed + missed
        if (total > 0) prayed.toFloat() / total else 0f
    }

    val totalPrayers = stats.totalPrayed + stats.totalMissed

    Column(modifier = modifier) {
        Text(
            text = "Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Weakest prayer insight
        weakest?.let { (prayer, name) ->
            val prayed = stats.prayedByPrayer[prayer] ?: 0
            val missed = stats.missedByPrayer[prayer] ?: 0
            val total = prayed + missed
            val percent = if (total > 0) (prayed.toFloat() / total * 100).toInt() else 0
            if (percent < 90 && totalPrayers > 0) {
                InsightCard(
                    icon = Icons.Default.Warning,
                    iconBackgroundColor = Color(0xFFF97316).copy(alpha = 0.2f),
                    iconTint = Color(0xFFF97316),
                    title = "$name needs attention",
                    description = "Your $name completion is at $percent%. Try setting an alarm to improve."
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Overall completion insight
        if (totalPrayers > 0) {
            val overallPercent = (stats.totalPrayed.toFloat() / totalPrayers * 100).toInt()
            InsightCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconBackgroundColor = Color(0xFF22C55E).copy(alpha = 0.2f),
                iconTint = Color(0xFF22C55E),
                title = "Overall completion: $overallPercent%",
                description = "You've completed ${stats.totalPrayed} out of $totalPrayers prayers. Keep going!"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Strongest prayer insight
        strongest?.let { (prayer, name) ->
            val prayed = stats.prayedByPrayer[prayer] ?: 0
            val missed = stats.missedByPrayer[prayer] ?: 0
            val total = prayed + missed
            val percent = if (total > 0) (prayed.toFloat() / total * 100).toInt() else 0
            if (totalPrayers > 0) {
                InsightCard(
                    icon = Icons.Default.Lightbulb,
                    iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Best prayer: $name",
                    description = "You consistently complete $name at $percent%."
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "InsightCard - Warning")
@Composable
private fun InsightCardWarningPreview() {
    NimazTheme {
        InsightCard(
            icon = Icons.Default.Warning,
            iconBackgroundColor = Color(0xFFF97316).copy(alpha = 0.2f),
            iconTint = Color(0xFFF97316),
            title = "Fajr needs attention",
            description = "Your Fajr completion is at 65%. Try setting an alarm to improve."
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "InsightCard - Trend")
@Composable
private fun InsightCardTrendPreview() {
    NimazTheme {
        InsightCard(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconBackgroundColor = Color(0xFF22C55E).copy(alpha = 0.2f),
            iconTint = Color(0xFF22C55E),
            title = "Overall completion: 85%",
            description = "You've completed 120 out of 140 prayers. Keep going!"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "InsightCard - Best")
@Composable
private fun InsightCardBestPreview() {
    NimazTheme {
        InsightCard(
            icon = Icons.Default.Lightbulb,
            iconBackgroundColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
            iconTint = Color(0xFF3B82F6),
            title = "Best prayer: Maghrib",
            description = "You consistently complete Maghrib at 98%."
        )
    }
}
