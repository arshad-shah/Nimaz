package com.arshadshah.nimaz.presentation.screens.tasbih

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.TasbihSession
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.TasbihViewModel

private enum class HistoryTab { TODAY, THIS_WEEK, ALL_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TasbihViewModel = hiltViewModel()
) {
    val historyState by viewModel.historyState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val counterState by viewModel.counterState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var selectedTab by remember { mutableStateOf(HistoryTab.TODAY) }

    // Compute live "Today" total
    val currentSessionCount = counterState.count + (counterState.laps * counterState.targetCount)
    val liveTotalToday = statsState.baseTotalToday + currentSessionCount

    // Sessions to display based on selected tab.
    val visibleSessions: List<TasbihSession> = when (selectedTab) {
        HistoryTab.TODAY -> historyState.todaySessions
        HistoryTab.THIS_WEEK -> historyState.weekSessions
        // No dedicated all-time source exists yet; fall back to the widest set we have.
        HistoryTab.ALL_TIME -> historyState.weekSessions
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.tasbih_history),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        if (historyState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stats Summary
                item(key = "stats_summary") {
                    StatsSummaryCard(
                        totalToday = liveTotalToday,
                        completedSessions = statsState.completedSessions,
                        totalThisWeek = statsState.totalThisWeek
                    )
                }

                // Filter tabs
                item(key = "tabs") {
                    HistoryTabRow(
                        selected = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }

                if (visibleSessions.isNotEmpty()) {
                    items(
                        items = visibleSessions,
                        key = { "${selectedTab.name}_${it.id}" }
                    ) { session ->
                        SessionCard(session = session)
                    }
                } else {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.no_sessions_yet),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.start_counting_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(
    totalToday: Int,
    completedSessions: Int,
    totalThisWeek: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = totalToday.toString(), label = stringResource(R.string.today))
            StatItem(value = completedSessions.toString(), label = stringResource(R.string.sessions))
            StatItem(value = totalThisWeek.toString(), label = stringResource(R.string.this_week))
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
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
            color = NimazColors.TasbihColors.Milestone
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HistoryTabRow(
    selected: HistoryTab,
    onSelect: (HistoryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabPill(
            label = stringResource(R.string.today),
            selected = selected == HistoryTab.TODAY,
            onClick = { onSelect(HistoryTab.TODAY) }
        )
        TabPill(
            label = stringResource(R.string.this_week),
            selected = selected == HistoryTab.THIS_WEEK,
            onClick = { onSelect(HistoryTab.THIS_WEEK) }
        )
        TabPill(
            label = stringResource(R.string.all_time),
            selected = selected == HistoryTab.ALL_TIME,
            onClick = { onSelect(HistoryTab.ALL_TIME) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SessionCard(
    session: TasbihSession,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Completion indicator
            Icon(
                imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Default.History,
                contentDescription = null,
                tint = if (session.isCompleted) NimazColors.TasbihColors.Complete
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.presetName ?: stringResource(R.string.custom),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildSessionSubtitle(session),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (session.isCompleted) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = NimazColors.TasbihColors.Complete.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.done).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.TasbihColors.Complete,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun buildSessionSubtitle(session: TasbihSession): String {
    val parts = mutableListOf("${session.currentCount}/${session.targetCount}")
    if (session.totalLaps > 0) {
        parts.add(stringResource(R.string.laps_format, session.totalLaps))
    }
    session.duration?.let { durationMs ->
        val minutes = durationMs / 60000
        val seconds = (durationMs % 60000) / 1000
        parts.add("%d:%02d".format(minutes, seconds))
    }
    return parts.joinToString(" · ")
}
