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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.FastingViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeupFastsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.makeupState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Makeup Fasts",
                subtitle = "${state.pendingCount} pending",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add makeup fast dialog */ },
                containerColor = NimazColors.FastingColors.Ramadan
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Makeup Fast",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (state.allMakeupFasts.isEmpty()) {
            EmptyMakeupState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary Card
                item {
                    MakeupSummaryCard(
                        pendingCount = state.pendingCount,
                        completedCount = state.allMakeupFasts.count { it.status == MakeupFastStatus.COMPLETED },
                        totalFidyaPaid = state.totalFidyaPaid
                    )
                }

                // Pending Section
                if (state.pendingMakeupFasts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(
                        items = state.pendingMakeupFasts,
                        key = { it.id }
                    ) { makeupFast ->
                        MakeupFastCard(
                            makeupFast = makeupFast,
                            onComplete = {
                                viewModel.onEvent(FastingEvent.CompleteMakeupFast(makeupFast.id))
                            },
                            onPayFidya = { amount ->
                                viewModel.onEvent(FastingEvent.PayFidya(makeupFast.id, amount))
                            }
                        )
                    }
                }

                // Completed Section
                val completedFasts = state.allMakeupFasts.filter {
                    it.status == MakeupFastStatus.COMPLETED || it.status == MakeupFastStatus.FIDYA_PAID
                }
                if (completedFasts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = completedFasts,
                        key = { "completed_${it.id}" }
                    ) { makeupFast ->
                        CompletedMakeupFastCard(makeupFast = makeupFast)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMakeupState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(NimazColors.StatusColors.Prayed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = NimazColors.StatusColors.Prayed,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Makeup Fasts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All your fasts are up to date!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MakeupSummaryCard(
    pendingCount: Int,
    completedCount: Int,
    totalFidyaPaid: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.FastingColors.Ramadan.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn(
                label = "Pending",
                value = pendingCount.toString(),
                color = NimazColors.StatusColors.Missed
            )
            SummaryColumn(
                label = "Completed",
                value = completedCount.toString(),
                color = NimazColors.StatusColors.Prayed
            )
            SummaryColumn(
                label = "Fidya Paid",
                value = "$${totalFidyaPaid.toInt()}",
                color = NimazColors.ZakatColors.Cash
            )
        }
    }
}

@Composable
private fun SummaryColumn(
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
            style = MaterialTheme.typography.headlineSmall,
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

@Composable
private fun MakeupFastCard(
    makeupFast: MakeupFast,
    onComplete: () -> Unit,
    onPayFidya: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Missed on $missedDate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    makeupFast.reason?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NimazColors.StatusColors.Missed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.StatusColors.Missed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { onPayFidya(10.0) }, // Default fidya amount
                    shape = RoundedCornerShape(8.dp),
                    color = NimazColors.ZakatColors.Cash.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = NimazColors.ZakatColors.Cash,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pay Fidya",
                            style = MaterialTheme.typography.labelMedium,
                            color = NimazColors.ZakatColors.Cash
                        )
                    }
                }

                Surface(
                    onClick = onComplete,
                    shape = RoundedCornerShape(8.dp),
                    color = NimazColors.StatusColors.Prayed.copy(alpha = 0.1f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = NimazColors.StatusColors.Prayed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fasted",
                            style = MaterialTheme.typography.labelMedium,
                            color = NimazColors.StatusColors.Prayed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedMakeupFastCard(
    makeupFast: MakeupFast,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)

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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NimazColors.StatusColors.Prayed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Missed on $missedDate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (makeupFast.status == MakeupFastStatus.FIDYA_PAID)
                            "Fidya paid: $${makeupFast.fidyaAmount?.toInt() ?: 0}" else "Made up",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
