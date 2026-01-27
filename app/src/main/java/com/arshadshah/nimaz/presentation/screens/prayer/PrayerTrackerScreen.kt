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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val isToday = state.selectedDate == LocalDate.now()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Prayer Tracker",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Statistics"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Navigator
            item {
                DateNavigator(
                    selectedDate = state.selectedDate,
                    onPreviousDay = { viewModel.onEvent(PrayerTrackerEvent.NavigateToPreviousDay) },
                    onNextDay = { viewModel.onEvent(PrayerTrackerEvent.NavigateToNextDay) },
                    canNavigateForward = !isToday
                )
            }

            // Day Summary Card
            item {
                DaySummaryCard(
                    prayerRecords = state.prayerRecords,
                    isToday = isToday
                )
            }

            // Prayer List
            item {
                Text(
                    text = "Prayers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(PrayerName.entries.filter { it != PrayerName.SUNRISE }) { prayerName ->
                val record = state.prayerRecords.find { it.prayerName == prayerName }
                val prayerTime = state.prayerTimes?.let { times ->
                    when (prayerName) {
                        PrayerName.FAJR -> times.fajr
                        PrayerName.DHUHR -> times.dhuhr
                        PrayerName.ASR -> times.asr
                        PrayerName.MAGHRIB -> times.maghrib
                        PrayerName.ISHA -> times.isha
                        else -> null
                    }
                }

                PrayerTrackerCard(
                    prayerName = prayerName,
                    prayerTime = prayerTime?.format(DateTimeFormatter.ofPattern("hh:mm a")),
                    status = record?.status ?: PrayerStatus.NOT_PRAYED,
                    isJamaah = record?.isJamaah ?: false,
                    onMarkPrayed = {
                        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerPrayed(prayerName, false))
                    },
                    onMarkWithJamaah = {
                        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerPrayed(prayerName, true))
                    },
                    onMarkMissed = {
                        viewModel.onEvent(PrayerTrackerEvent.MarkPrayerMissed(prayerName))
                    }
                )
            }
        }
    }
}

@Composable
private fun DateNavigator(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    canNavigateForward: Boolean,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    val isToday = selectedDate == LocalDate.now()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isToday) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNextDay,
                enabled = canNavigateForward
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Day",
                    tint = if (canNavigateForward) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    prayerRecords: List<PrayerRecord>,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val prayed = prayerRecords.count { it.status == PrayerStatus.PRAYED || it.status == PrayerStatus.LATE }
    val jamaah = prayerRecords.count { it.isJamaah }
    val missed = prayerRecords.count { it.status == PrayerStatus.MISSED }
    val total = 5

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
                text = if (isToday) "Today's Progress" else "Day Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Prayed",
                    value = "$prayed/$total",
                    color = NimazColors.StatusColors.Prayed
                )
                SummaryItem(
                    label = "Jama'ah",
                    value = jamaah.toString(),
                    color = NimazColors.Primary
                )
                SummaryItem(
                    label = "Missed",
                    value = missed.toString(),
                    color = NimazColors.StatusColors.Missed
                )
            }

            // Progress Bar
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(prayed.toFloat() / total)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NimazColors.StatusColors.Prayed)
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerTrackerCard(
    prayerName: PrayerName,
    prayerTime: String?,
    status: PrayerStatus,
    isJamaah: Boolean,
    onMarkPrayed: () -> Unit,
    onMarkWithJamaah: () -> Unit,
    onMarkMissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prayerColor = getPrayerColor(prayerName)
    val statusColor = when (status) {
        PrayerStatus.PRAYED -> NimazColors.StatusColors.Prayed
        PrayerStatus.LATE -> NimazColors.StatusColors.Late
        PrayerStatus.MISSED -> NimazColors.StatusColors.Missed
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prayer Icon/Status
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (status == PrayerStatus.NOT_PRAYED) {
                            prayerColor.copy(alpha = 0.1f)
                        } else {
                            statusColor.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (status == PrayerStatus.PRAYED || status == PrayerStatus.LATE) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Prayed",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (status == PrayerStatus.MISSED) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Missed",
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Not Prayed",
                        tint = prayerColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayerName.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isJamaah) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NimazColors.Primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Jama'ah",
                                    tint = NimazColors.Primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Jama'ah",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NimazColors.Primary
                                )
                            }
                        }
                    }
                }
                prayerTime?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            if (status == PrayerStatus.NOT_PRAYED) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onMarkMissed,
                        shape = CircleShape,
                        color = NimazColors.StatusColors.Missed.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Mark Missed",
                                tint = NimazColors.StatusColors.Missed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onMarkWithJamaah,
                        shape = CircleShape,
                        color = NimazColors.Primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Mark with Jama'ah",
                                tint = NimazColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onMarkPrayed,
                        shape = CircleShape,
                        color = NimazColors.StatusColors.Prayed.copy(alpha = 0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark Prayed",
                                tint = NimazColors.StatusColors.Prayed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getPrayerColor(prayerName: PrayerName): Color {
    return when (prayerName) {
        PrayerName.FAJR -> NimazColors.PrayerColors.Fajr
        PrayerName.SUNRISE -> NimazColors.PrayerColors.Sunrise
        PrayerName.DHUHR -> NimazColors.PrayerColors.Dhuhr
        PrayerName.ASR -> NimazColors.PrayerColors.Asr
        PrayerName.MAGHRIB -> NimazColors.PrayerColors.Maghrib
        PrayerName.ISHA -> NimazColors.PrayerColors.Isha
    }
}
