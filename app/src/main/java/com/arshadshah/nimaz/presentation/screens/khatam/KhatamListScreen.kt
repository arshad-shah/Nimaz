package com.arshadshah.nimaz.presentation.screens.khatam

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamStatus
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.KhatamEvent
import com.arshadshah.nimaz.presentation.viewmodel.KhatamListUiState
import com.arshadshah.nimaz.presentation.viewmodel.KhatamViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: KhatamViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedKhatamForAction by remember { mutableStateOf<Khatam?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = "Khatam Quran",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Start New Khatam")
            }
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

        val displayList = when (selectedTab) {
            0 -> state.inProgressKhatams
            1 -> state.completedKhatams
            else -> state.abandonedKhatams
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats card
            if (state.hasAnyKhatam) {
                item {
                    KhatamStatsCard(state = state)
                }
            }

            // Active khatam summary
            state.activeKhatam?.let { active ->
                item {
                    ActiveKhatamCard(
                        khatam = active,
                        onClick = { onNavigateToDetail(active.id) }
                    )
                }
            }

            // Tabs for In Progress / Completed / Abandoned
            item {
                KhatamStatusTabs(
                    inProgressCount = state.inProgressKhatams.size,
                    completedCount = state.completedKhatams.size,
                    abandonedCount = state.abandonedKhatams.size,
                    selectedIndex = selectedTab,
                    onTabSelect = { selectedTab = it }
                )
            }

            if (displayList.isEmpty()) {
                item {
                    if (state.inProgressKhatams.isEmpty() && state.completedKhatams.isEmpty() && state.abandonedKhatams.isEmpty()) {
                        EmptyState(onCreateClick = onNavigateToCreate)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "No khatams in progress"
                                    1 -> "No completed khatams"
                                    else -> "No abandoned khatams"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(displayList, key = { it.id }) { khatam ->
                KhatamCard(
                    khatam = khatam,
                    onClick = { onNavigateToDetail(khatam.id) },
                    onLongClick = { selectedKhatamForAction = khatam }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
        }
    }

    // Bottom sheet for khatam actions
    selectedKhatamForAction?.let { khatam ->
        ModalBottomSheet(
            onDismissRequest = { selectedKhatamForAction = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = khatam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                if (!khatam.isActive && khatam.status != KhatamStatus.COMPLETED) {
                    ListItem(
                        headlineContent = { Text("Set as Active") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.onEvent(KhatamEvent.SetActiveKhatam(khatam.id))
                            selectedKhatamForAction = null
                        }
                    )
                }

                if (khatam.status != KhatamStatus.COMPLETED && khatam.status != KhatamStatus.ABANDONED) {
                    ListItem(
                        headlineContent = { Text("Abandon") },
                        leadingContent = {
                            Icon(
                                Icons.Default.DoNotDisturb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.onEvent(KhatamEvent.AbandonKhatam(khatam.id))
                            selectedKhatamForAction = null
                        }
                    )
                }

                if (khatam.status == KhatamStatus.ABANDONED) {
                    ListItem(
                        headlineContent = { Text("Reactivate") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.onEvent(KhatamEvent.ReactivateKhatam(khatam.id))
                            selectedKhatamForAction = null
                        }
                    )
                }

                ListItem(
                    headlineContent = {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable {
                        showDeleteConfirm = true
                    }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        val khatamToDelete = selectedKhatamForAction
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                selectedKhatamForAction = null
            },
            title = { Text("Delete Khatam") },
            text = {
                Text("Are you sure you want to delete \"${khatamToDelete?.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        khatamToDelete?.let {
                            viewModel.onEvent(KhatamEvent.DeleteKhatam(it.id))
                        }
                        showDeleteConfirm = false
                        selectedKhatamForAction = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        selectedKhatamForAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Custom Status Tabs ---

@Composable
private fun KhatamStatusTabs(
    inProgressCount: Int,
    completedCount: Int,
    abandonedCount: Int,
    selectedIndex: Int,
    onTabSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusTabCard(
            count = inProgressCount,
            label = "In Progress",
            isSelected = selectedIndex == 0,
            onClick = { onTabSelect(0) },
            modifier = Modifier.weight(1f)
        )
        StatusTabCard(
            count = completedCount,
            label = "Completed",
            isSelected = selectedIndex == 1,
            onClick = { onTabSelect(1) },
            modifier = Modifier.weight(1f)
        )
        StatusTabCard(
            count = abandonedCount,
            label = "Abandoned",
            isSelected = selectedIndex == 2,
            onClick = { onTabSelect(2) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusTabCard(
    count: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "tabBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 250),
        label = "tabContent"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// --- Stats Card ---

@Composable
private fun KhatamStatsCard(
    state: KhatamListUiState,
    modifier: Modifier = Modifier
) {
    val completedCount = state.completedKhatams.size
    val inProgressCount = state.inProgressKhatams.size
    val totalAyahsRead = (state.completedKhatams + state.inProgressKhatams + state.abandonedKhatams)
        .sumOf { it.totalAyahsRead }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            icon = Icons.Default.CheckCircle,
            value = completedCount.toString(),
            label = "Completed",
            tint = MaterialTheme.colorScheme.primary
        )
        StatItem(
            icon = Icons.Default.TrendingUp,
            value = inProgressCount.toString(),
            label = "In Progress",
            tint = MaterialTheme.colorScheme.tertiary
        )
        StatItem(
            icon = Icons.Default.AutoStories,
            value = formatAyahCount(totalAyahsRead),
            label = "Ayahs Read",
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun formatAyahCount(count: Int): String {
    return when {
        count >= 1000 -> String.format(Locale.US, "%.1fk", count / 1000f)
        else -> count.toString()
    }
}

// --- Active Khatam Card ---

@Composable
private fun ActiveKhatamCard(
    khatam: Khatam,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(brush = gradientBrush)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Star label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Active Khatam",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = khatam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${khatam.totalAyahsRead} / ${Khatam.TOTAL_QURAN_AYAHS} ayahs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            // Progress ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { khatam.progressPercent },
                    modifier = Modifier.size(72.dp),
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "${(khatam.progressPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Stats pills row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActiveKhatamPill(
                icon = Icons.Default.AutoStories,
                text = "${khatam.totalAyahsRead} Ayahs Read",
                modifier = Modifier.weight(1f)
            )
            ActiveKhatamPill(
                icon = Icons.Default.Timeline,
                text = "${khatam.dailyTarget} / day",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActiveKhatamPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

// --- Khatam List Card ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KhatamCard(
    khatam: Khatam,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = when (khatam.status) {
                            KhatamStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            KhatamStatus.ABANDONED -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (khatam.status) {
                    KhatamStatus.COMPLETED -> Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    KhatamStatus.ABANDONED -> Icon(
                        imageVector = Icons.Default.DoNotDisturb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )

                    else -> Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = khatam.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${khatam.totalAyahsRead} / ${Khatam.TOTAL_QURAN_AYAHS} ayahs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Created ${formatDate(khatam.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "${(khatam.progressPercent * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { khatam.progressPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = when (khatam.status) {
                KhatamStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                KhatamStatus.ABANDONED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.tertiary
            },
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
            strokeCap = StrokeCap.Round
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// --- Empty State ---

@Composable
private fun EmptyState(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "No Khatam Started",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Start your journey to complete the Quran",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

private val KhatamListUiState.hasAnyKhatam: Boolean
    get() = inProgressKhatams.isNotEmpty() || completedKhatams.isNotEmpty() || abandonedKhatams.isNotEmpty()
