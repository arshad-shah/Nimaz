package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.MONTH_YEAR_FORMATTER
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.organisms.ChartStatItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerChartType
import com.arshadshah.nimaz.presentation.components.organisms.PrayerStatsChart
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.StatsPeriod
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val state by viewModel.statsState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.statistics),
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
                                        StatsPeriod.WEEK -> stringResource(R.string.stats_period_week)
                                        StatsPeriod.MONTH -> stringResource(R.string.stats_period_month)
                                        StatsPeriod.YEAR -> stringResource(R.string.stats_period_year)
                                        StatsPeriod.ALL_TIME -> stringResource(R.string.all_time)
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
                    val weekLabel = stringResource(R.string.this_week)
                    val monthLabel = stringResource(R.string.stats_this_month)
                    val yearLabel = stringResource(R.string.stats_this_year)
                    val allTimeLabel = stringResource(R.string.all_time)
                    val periodLabel = try {
                        val formatter = MONTH_YEAR_FORMATTER
                        val startDate = Instant.ofEpochMilli(stats.startDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        startDate.format(formatter)
                    } catch (_: Exception) {
                        when (state.period) {
                            StatsPeriod.WEEK -> weekLabel
                            StatsPeriod.MONTH -> monthLabel
                            StatsPeriod.YEAR -> yearLabel
                            StatsPeriod.ALL_TIME -> allTimeLabel
                        }
                    }

                    PrayerStatsChart(
                        stats = stats,
                        chartType = PrayerChartType.DONUT,
                        title = stringResource(R.string.prayer_completion),
                        subtitle = periodLabel,
                        summaryItems = listOf(
                            ChartStatItem(
                                "${stats.totalPrayed}",
                                stringResource(R.string.prayed),
                                NimazColors.StatusColors.Prayed
                            ),
                            ChartStatItem(
                                "${stats.totalMissed}",
                                stringResource(R.string.missed),
                                NimazColors.StatusColors.Missed
                            ),
                            ChartStatItem(
                                "${stats.perfectDays}",
                                stringResource(R.string.stat_perfect_days),
                                NimazColors.PrayerColors.Maghrib
                            ),
                            ChartStatItem(
                                "${state.currentStreak}",
                                stringResource(R.string.stat_current_streak),
                                NimazColors.StatusColors.Prayed
                            ),
                            ChartStatItem(
                                "${state.longestStreak}",
                                stringResource(R.string.stat_longest_streak),
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
                        title = stringResource(R.string.prayer_breakdown),
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
        PrayerName.FAJR to stringResource(R.string.prayer_fajr),
        PrayerName.DHUHR to stringResource(R.string.prayer_dhuhr),
        PrayerName.ASR to stringResource(R.string.prayer_asr),
        PrayerName.MAGHRIB to stringResource(R.string.prayer_maghrib),
        PrayerName.ISHA to stringResource(R.string.prayer_isha)
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
            text = stringResource(R.string.insights),
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
                    iconBackgroundColor = NimazColors.PrayerColors.Asr.copy(alpha = 0.2f),
                    iconTint = NimazColors.PrayerColors.Asr,
                    title = stringResource(R.string.prayer_insight_needs_attention, name),
                    description = stringResource(
                        R.string.prayer_insight_needs_attention_desc,
                        name,
                        percent
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Overall completion insight
        if (totalPrayers > 0) {
            val overallPercent = (stats.totalPrayed.toFloat() / totalPrayers * 100).toInt()
            InsightCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconBackgroundColor = NimazColors.Success.copy(alpha = 0.2f),
                iconTint = NimazColors.Success,
                title = stringResource(R.string.prayer_insight_overall, overallPercent),
                description = stringResource(
                    R.string.prayer_insight_overall_desc,
                    stats.totalPrayed,
                    totalPrayers
                )
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
                    title = stringResource(R.string.prayer_insight_best, name),
                    description = stringResource(R.string.prayer_insight_best_desc, name, percent)
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
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(14.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = iconTint,
                containerColor = iconBackgroundColor,
                containerSize = 40.dp,
                iconSize = 20.dp,
                cornerRadius = 10.dp,
            )
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
            iconBackgroundColor = NimazColors.PrayerColors.Asr.copy(alpha = 0.2f),
            iconTint = NimazColors.PrayerColors.Asr,
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
            iconBackgroundColor = NimazColors.Success.copy(alpha = 0.2f),
            iconTint = NimazColors.Success,
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
            iconBackgroundColor = NimazColors.Info.copy(alpha = 0.2f),
            iconTint = NimazColors.Info,
            title = "Best prayer: Maghrib",
            description = "You consistently complete Maghrib at 98%."
        )
    }
}
