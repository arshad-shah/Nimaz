package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.StatsPeriod
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

            // Overview Card
            item {
                state.stats?.let { stats ->
                    val totalPrayers = stats.totalPrayed + stats.totalMissed
                    val completionRate = if (totalPrayers > 0) {
                        (stats.totalPrayed.toFloat() / totalPrayers * 100).toInt()
                    } else 0

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

                    // Count perfect days (days where all 5 prayers were completed)
                    val perfectDays = stats.prayedByPrayer.values.minOrNull() ?: 0

                    OverviewCard(
                        completionPercent = completionRate,
                        completionText = "${stats.totalPrayed} of $totalPrayers prayers completed",
                        periodLabel = periodLabel,
                        prayed = stats.totalPrayed,
                        missed = stats.totalMissed,
                        perfectDays = perfectDays
                    )
                }
            }

            // Streak Card
            item {
                StreakCard(
                    currentStreak = state.currentStreak,
                    longestStreak = state.longestStreak
                )
            }

            // Prayer Breakdown
            item {
                state.stats?.let { stats ->
                    val prayedMap = stats.prayedByPrayer
                    val missedMap = stats.missedByPrayer

                    fun getStats(prayer: PrayerName): Pair<Int, Int> {
                        val prayed = prayedMap[prayer] ?: 0
                        val missed = missedMap[prayer] ?: 0
                        return Pair(prayed, prayed + missed)
                    }

                    PrayerBreakdownSection(
                        fajrStats = getStats(PrayerName.FAJR),
                        dhuhrStats = getStats(PrayerName.DHUHR),
                        asrStats = getStats(PrayerName.ASR),
                        maghribStats = getStats(PrayerName.MAGHRIB),
                        ishaStats = getStats(PrayerName.ISHA)
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

// --- Overview Card ---

@Composable
private fun OverviewCard(
    completionPercent: Int,
    completionText: String,
    periodLabel: String,
    prayed: Int,
    missed: Int,
    perfectDays: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, primaryDark)
                )
            )
            .padding(25.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Prayer Completion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big percentage
            Text(
                text = "$completionPercent%",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = completionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OverviewStatItem(value = prayed.toString(), label = "Prayed")
                OverviewStatItem(value = missed.toString(), label = "Missed")
                OverviewStatItem(value = perfectDays.toString(), label = "Perfect Days")
            }
        }
    }
}

@Composable
private fun OverviewStatItem(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    }
}

// --- Streak Card ---

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    val streakGold = Color(0xFFEAB308)
    val streakGoldDark = Color(0xFFCA8A04)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(streakGold, streakGoldDark)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color(0xFF1C1917)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$currentStreak Day Streak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917)
                )
                Text(
                    text = "Keep it going! Best: $longestStreak days",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1C1917).copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- Prayer Breakdown ---

@Composable
private fun PrayerBreakdownSection(
    fajrStats: Pair<Int, Int>,
    dhuhrStats: Pair<Int, Int>,
    asrStats: Pair<Int, Int>,
    maghribStats: Pair<Int, Int>,
    ishaStats: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Prayer Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                PrayerStatRow("Fajr", fajrStats, NimazColors.PrayerColors.Fajr)
                PrayerStatRow("Dhuhr", dhuhrStats, NimazColors.PrayerColors.Dhuhr)
                PrayerStatRow("Asr", asrStats, NimazColors.PrayerColors.Asr)
                PrayerStatRow("Maghrib", maghribStats, NimazColors.PrayerColors.Maghrib)
                PrayerStatRow("Isha", ishaStats, NimazColors.PrayerColors.Isha, showDivider = false)
            }
        }
    }
}

@Composable
private fun PrayerStatRow(
    name: String,
    stats: Pair<Int, Int>, // (prayed, total)
    color: Color,
    showDivider: Boolean = true
) {
    val percent = if (stats.second > 0) (stats.first.toFloat() / stats.second * 100).toInt() else 0
    val fraction = if (stats.second > 0) stats.first.toFloat() / stats.second else 0f

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )

            // Prayer name and count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${stats.first} of ${stats.second} days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }

            // Percentage
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(45.dp),
                textAlign = TextAlign.End
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
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
