package com.arshadshah.nimaz.presentation.screens.fasting

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.FastingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMakeup: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val makeupState by viewModel.makeupState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Fasting Tracker",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    }
                }
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
            // Today's Fast Card
            item {
                TodayFastCard(
                    isFasting = state.isFastingToday,
                    fastStatus = state.todayRecord?.status ?: FastStatus.NOT_FASTED,
                    fastType = state.selectedFastType,
                    onStartFast = { viewModel.onEvent(FastingEvent.ToggleTodayFast) },
                    onCompleteFast = { viewModel.onEvent(FastingEvent.CompleteFast(LocalDate.now())) },
                    onBreakFast = { viewModel.onEvent(FastingEvent.BreakFast(LocalDate.now())) }
                )
            }

            // Fast Type Selector
            item {
                Text(
                    text = "Fast Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FastType.entries.take(4).forEach { type ->
                        FilterChip(
                            selected = state.selectedFastType == type,
                            onClick = { viewModel.onEvent(FastingEvent.SetFastType(type)) },
                            label = {
                                Text(
                                    text = type.displayName(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            // Stats Summary
            item {
                StatsCard(
                    ramadanFasted = statsState.ramadanFastedCount,
                    voluntaryFasted = statsState.voluntaryFastCount,
                    pendingMakeup = makeupState.pendingCount
                )
            }

            // Makeup Fasts Card
            if (makeupState.pendingCount > 0) {
                item {
                    MakeupFastsCard(
                        pendingCount = makeupState.pendingCount,
                        onClick = onNavigateToMakeup
                    )
                }
            }

            // Quick Tips
            item {
                QuickTipsCard()
            }
        }
    }
}

@Composable
private fun TodayFastCard(
    isFasting: Boolean,
    fastStatus: FastStatus,
    fastType: FastType,
    onStartFast: () -> Unit,
    onCompleteFast: () -> Unit,
    onBreakFast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    val (statusColor, statusText) = when (fastStatus) {
        FastStatus.FASTED -> NimazColors.StatusColors.Prayed to "Fast Completed"
        FastStatus.NOT_FASTED -> MaterialTheme.colorScheme.surfaceVariant to "Not Fasting"
        FastStatus.EXEMPTED -> NimazColors.StatusColors.Pending to "Exempted"
        FastStatus.MAKEUP_DUE -> NimazColors.StatusColors.Missed to "Makeup Due"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NimazColors.FastingColors.Ramadan,
                            NimazColors.FastingColors.Ramadan.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = today.format(formatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (fastStatus) {
                            FastStatus.FASTED -> Icons.Default.Check
                            FastStatus.NOT_FASTED -> Icons.Default.PlayArrow
                            FastStatus.EXEMPTED -> Icons.Default.History
                            FastStatus.MAKEUP_DUE -> Icons.Default.Restaurant
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (isFasting) {
                    Text(
                        text = fastType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                when (fastStatus) {
                    FastStatus.NOT_FASTED -> {
                        Button(
                            onClick = onStartFast,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = NimazColors.FastingColors.Ramadan
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Fasting")
                        }
                    }
                    FastStatus.FASTED, FastStatus.EXEMPTED, FastStatus.MAKEUP_DUE -> {
                        // Already completed or exempted - no action needed
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(
    ramadanFasted: Int,
    voluntaryFasted: Int,
    pendingMakeup: Int,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                label = "Ramadan",
                value = ramadanFasted.toString(),
                color = NimazColors.FastingColors.Ramadan
            )
            StatColumn(
                label = "Voluntary",
                value = voluntaryFasted.toString(),
                color = NimazColors.FastingColors.Voluntary
            )
            StatColumn(
                label = "Makeup Due",
                value = pendingMakeup.toString(),
                color = NimazColors.StatusColors.Missed
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MakeupFastsCard(
    pendingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.StatusColors.Missed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NimazColors.StatusColors.Missed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pendingCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.StatusColors.Missed
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Makeup Fasts Due",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to view and manage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickTipsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Quick Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Fast on Mondays and Thursdays for extra rewards",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• The White Days (13th, 14th, 15th of each lunar month)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• Six days of Shawwal after Ramadan",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
