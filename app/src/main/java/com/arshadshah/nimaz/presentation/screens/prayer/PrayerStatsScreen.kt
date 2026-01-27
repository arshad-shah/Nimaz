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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerStatsDonutChart
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import com.arshadshah.nimaz.presentation.viewmodel.StatsPeriod

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
                title = "Prayer Statistics",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Streak Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(
                        label = "Current Streak",
                        value = state.currentStreak,
                        modifier = Modifier.weight(1f)
                    )
                    StreakCard(
                        label = "Longest Streak",
                        value = state.longestStreak,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Stats Chart
            item {
                state.stats?.let { stats ->
                    PrayerStatsDonutChart(
                        prayed = stats.totalPrayed,
                        late = 0, // Late tracking would need additional field in PrayerStats
                        missed = stats.totalMissed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Detailed Stats
            item {
                state.stats?.let { stats ->
                    val totalPrayers = stats.totalPrayed + stats.totalMissed
                    val completionRate = if (totalPrayers > 0) {
                        stats.totalPrayed.toFloat() / totalPrayers
                    } else 0f

                    DetailedStatsCard(
                        totalPrayers = totalPrayers,
                        prayed = stats.totalPrayed,
                        late = 0,
                        missed = stats.totalMissed,
                        jamaahCount = stats.totalJamaah,
                        completionRate = completionRate
                    )
                }
            }

            // Prayer Breakdown
            item {
                state.stats?.let { stats ->
                    val prayedMap = stats.prayedByPrayer
                    val missedMap = stats.missedByPrayer

                    fun getCompletion(prayer: com.arshadshah.nimaz.domain.model.PrayerName): Float {
                        val prayed = prayedMap[prayer] ?: 0
                        val missed = missedMap[prayer] ?: 0
                        val total = prayed + missed
                        return if (total > 0) prayed.toFloat() / total else 0f
                    }

                    PrayerBreakdownCard(
                        fajrCompletion = getCompletion(com.arshadshah.nimaz.domain.model.PrayerName.FAJR),
                        dhuhrCompletion = getCompletion(com.arshadshah.nimaz.domain.model.PrayerName.DHUHR),
                        asrCompletion = getCompletion(com.arshadshah.nimaz.domain.model.PrayerName.ASR),
                        maghribCompletion = getCompletion(com.arshadshah.nimaz.domain.model.PrayerName.MAGHRIB),
                        ishaCompletion = getCompletion(com.arshadshah.nimaz.domain.model.PrayerName.ISHA)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = NimazColors.Primary
            )
            Text(
                text = if (value == 1) "Day" else "Days",
                style = MaterialTheme.typography.labelSmall,
                color = NimazColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailedStatsCard(
    totalPrayers: Int,
    prayed: Int,
    late: Int,
    missed: Int,
    jamaahCount: Int,
    completionRate: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatRow(label = "Total Prayers", value = totalPrayers.toString())
            StatRow(label = "Prayed On Time", value = prayed.toString(), color = NimazColors.StatusColors.Prayed)
            StatRow(label = "Prayed Late", value = late.toString(), color = NimazColors.StatusColors.Late)
            StatRow(label = "Missed", value = missed.toString(), color = NimazColors.StatusColors.Missed)
            StatRow(label = "With Jama'ah", value = jamaahCount.toString(), color = NimazColors.Primary)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completion Rate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        completionRate >= 0.9f -> NimazColors.StatusColors.Prayed
                        completionRate >= 0.7f -> NimazColors.StatusColors.Late
                        else -> NimazColors.StatusColors.Missed
                    }
                )
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PrayerBreakdownCard(
    fajrCompletion: Float,
    dhuhrCompletion: Float,
    asrCompletion: Float,
    maghribCompletion: Float,
    ishaCompletion: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "By Prayer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrayerCompletionRow("Fajr", fajrCompletion, NimazColors.PrayerColors.Fajr)
            PrayerCompletionRow("Dhuhr", dhuhrCompletion, NimazColors.PrayerColors.Dhuhr)
            PrayerCompletionRow("Asr", asrCompletion, NimazColors.PrayerColors.Asr)
            PrayerCompletionRow("Maghrib", maghribCompletion, NimazColors.PrayerColors.Maghrib)
            PrayerCompletionRow("Isha", ishaCompletion, NimazColors.PrayerColors.Isha)
        }
    }
}

@Composable
private fun PrayerCompletionRow(
    prayer: String,
    completion: Float,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prayer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(completion * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(completion)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
