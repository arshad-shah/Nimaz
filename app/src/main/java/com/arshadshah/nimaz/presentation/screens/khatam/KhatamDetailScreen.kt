package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.JuzProgressInfo
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionTitle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.KhatamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamDetailScreen(
    khatamId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRead: (surahNumber: Int, ayahNumber: Int) -> Unit = { _, _ -> },
    viewModel: KhatamViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(khatamId) {
        viewModel.onEvent(KhatamEvent.LoadKhatamDetail(khatamId))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = state.khatam?.name ?: stringResource(R.string.khatam_detail),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val khatam = state.khatam ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress overview
            item {
                ProgressOverview(khatam = khatam)
            }

            // Continue Reading button
            val nextSurah = state.nextUnreadSurah
            val nextAyah = state.nextUnreadAyah
            if (nextSurah != null && nextAyah != null) {
                item {
                    Button(
                        onClick = { onNavigateToRead(nextSurah, nextAyah) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.khatam_continue_reading),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Stats grid
            item {
                StatsGrid(
                    daysActive = state.daysActive,
                    averagePace = state.averagePace,
                    dailyTarget = khatam.dailyTarget
                )
            }

            // Juz progress grid
            item {
                NimazSectionTitle(
                    text = stringResource(R.string.khatam_juz_progress),
                    modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                )
            }

            item {
                JuzProgressGrid(juzProgress = state.juzProgress)
            }

            // Daily reading chart placeholder
            if (state.dailyLogs.isNotEmpty()) {
                item {
                    NimazSectionTitle(
                        text = stringResource(R.string.khatam_daily_reading),
                        modifier = Modifier.padding(start = 5.dp, top = 4.dp, bottom = 0.dp)
                    )
                }

                item {
                    DailyReadingSection(dailyLogs = state.dailyLogs)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProgressOverview(
    khatam: Khatam,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { khatam.progressPercent },
                modifier = Modifier.size(120.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(khatam.progressPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.khatam_ayahs_count_format, khatam.totalAyahsRead),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = stringResource(R.string.khatam_of_ayahs_read, khatam.totalAyahsRead, Khatam.TOTAL_QURAN_AYAHS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsGrid(
    daysActive: Int,
    averagePace: Float,
    dailyTarget: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            icon = Icons.Default.CalendarToday,
            label = stringResource(R.string.khatam_days_active),
            value = "$daysActive",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Speed,
            label = stringResource(R.string.khatam_avg_pace),
            value = stringResource(R.string.khatam_pace_per_day, averagePace.toInt()),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Timer,
            label = stringResource(R.string.khatam_target),
            value = stringResource(R.string.khatam_pace_per_day, dailyTarget),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
private fun JuzProgressGrid(
    juzProgress: List<JuzProgressInfo>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(juzProgress, key = { it.juzNumber }) { juz ->
            JuzCell(juz = juz)
        }
    }
}

@Composable
private fun JuzCell(
    juz: JuzProgressInfo,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        juz.progressPercent >= 1f -> MaterialTheme.colorScheme.primary
        juz.progressPercent > 0f -> MaterialTheme.colorScheme.primary.copy(
            alpha = 0.2f + (juz.progressPercent * 0.6f)
        )
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        juz.progressPercent >= 1f -> MaterialTheme.colorScheme.onPrimary
        juz.progressPercent > 0.5f -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${juz.juzNumber}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun DailyReadingSection(
    dailyLogs: List<com.arshadshah.nimaz.domain.model.DailyLogEntry>,
    modifier: Modifier = Modifier
) {
    val recentLogs = dailyLogs.take(7)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        recentLogs.forEach { log ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                        .format(java.util.Date(log.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.khatam_ayahs_count_format, log.ayahsRead),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
