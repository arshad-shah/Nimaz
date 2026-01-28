package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var displayedMonth by remember { mutableStateOf(YearMonth.from(state.selectedDate)) }

    LaunchedEffect(displayedMonth) {
        val startDate = displayedMonth.atDay(1)
        val endDate = displayedMonth.atEndOfMonth()
        viewModel.onEvent(PrayerTrackerEvent.LoadHistory(startDate, endDate))
    }

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
                            contentDescription = "View Statistics"
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Streak Card
            item {
                StreakCard(currentStreak = statsState.currentStreak)
            }

            // Calendar Section
            item {
                Spacer(modifier = Modifier.height(25.dp))
                CalendarSection(
                    displayedMonth = displayedMonth,
                    selectedDate = state.selectedDate,
                    onMonthChange = { displayedMonth = it },
                    onDateSelected = { date ->
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(date))
                    },
                    historyRecords = historyState.records
                )
            }

            // Selected Day Detail
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedDayDetail(
                    selectedDate = state.selectedDate,
                    prayerRecords = state.prayerRecords,
                    prayerTimes = state.prayerTimes,
                    onTogglePrayer = { prayerName, currentStatus ->
                        if (currentStatus == PrayerStatus.PRAYED || currentStatus == PrayerStatus.LATE) {
                            // Already prayed - could toggle off, but for now mark missed
                            viewModel.onEvent(PrayerTrackerEvent.MarkPrayerMissed(prayerName))
                        } else {
                            viewModel.onEvent(
                                PrayerTrackerEvent.MarkPrayerPrayed(
                                    prayerName,
                                    false
                                )
                            )
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// --- Streak Card ---

@Composable
private fun StreakCard(currentStreak: Int) {
    val goldDark = Color(0xFFCA8A04)
    val goldLight = Color(0xFFEAB308)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(goldLight, goldDark)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFF1C1917),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1917)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$currentStreak",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917),
                    lineHeight = 48.sp
                )
                Text(
                    text = "consecutive days with all prayers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1917).copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- Calendar Section ---

@Composable
private fun CalendarSection(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    historyRecords: List<PrayerRecord>
) {
    // Section header with month navigation
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { onMonthChange(displayedMonth.minusMonths(1)) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(120.dp)
            )
            Surface(
                onClick = { onMonthChange(displayedMonth.plusMonths(1)) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(15.dp))

    // Calendar grid
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            // Day name headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                dayNames.forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar days
            val calendarDays = remember(displayedMonth) {
                buildCalendarDays(displayedMonth)
            }

            val today = LocalDate.now()

            // Render rows of 7
            calendarDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { day ->
                        val isCurrentMonth = day.month == displayedMonth.month
                        val isToday = day == today
                        val isSelected = day == selectedDate

                        // Calculate completion status for the day using historyRecords
                        val dayEpoch = day.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
                        val dayRecords = historyRecords.filter { it.date == dayEpoch && it.prayerName != PrayerName.SUNRISE }
                        val completedCount = dayRecords.count { record ->
                            record.status == PrayerStatus.PRAYED ||
                            record.status == PrayerStatus.LATE ||
                            record.status == PrayerStatus.QADA
                        }
                        val missedCount = dayRecords.count { record ->
                            record.status == PrayerStatus.MISSED
                        }
                        val totalExpected = 5 // 5 daily prayers (excluding sunrise)
                        val hasRecords = dayRecords.isNotEmpty()

                        val badgeColor = when {
                            !hasRecords -> null // No records for this day
                            completedCount == totalExpected -> NimazColors.StatusColors.Prayed // Green - all completed
                            completedCount > 0 -> NimazColors.StatusColors.Partial // Orange - partial completion
                            missedCount > 0 -> NimazColors.StatusColors.Missed // Red - all missed
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(10.dp)
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (isToday) {
                                        Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    } else Modifier
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDateSelected(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${day.dayOfMonth}",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.25f
                                    )

                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            // Day badge for past days based on actual completion status
                            if (day.isBefore(today) && badgeColor != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildCalendarDays(yearMonth: YearMonth): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstOfMonth.dayOfWeek
    // Sunday = 0 offset
    val offset = if (firstDayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfWeek.value
    val startDate = firstOfMonth.minusDays(offset.toLong())

    val days = mutableListOf<LocalDate>()
    val totalDays = 42 // 6 rows x 7
    for (i in 0 until totalDays) {
        days.add(startDate.plusDays(i.toLong()))
    }
    return days
}

// --- Selected Day Detail ---

@Composable
private fun SelectedDayDetail(
    selectedDate: LocalDate,
    prayerRecords: List<PrayerRecord>,
    prayerTimes: com.arshadshah.nimaz.domain.model.PrayerTimes?,
    onTogglePrayer: (PrayerName, PrayerStatus) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    val prayers = PrayerName.entries.filter { it != PrayerName.SUNRISE }
    val prayedCount = prayerRecords.count {
        it.status == PrayerStatus.PRAYED || it.status == PrayerStatus.LATE || it.status == PrayerStatus.QADA
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$prayedCount of ${prayers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Prayer checklist
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                prayers.forEach { prayerName ->
                    val record = prayerRecords.find { it.prayerName == prayerName }
                    val status = record?.status ?: PrayerStatus.NOT_PRAYED
                    val isCompleted =
                        status == PrayerStatus.PRAYED || status == PrayerStatus.LATE || status == PrayerStatus.QADA
                    val prayerTime = prayerTimes?.let { times ->
                        when (prayerName) {
                            PrayerName.FAJR -> times.fajr
                            PrayerName.DHUHR -> times.dhuhr
                            PrayerName.ASR -> times.asr
                            PrayerName.MAGHRIB -> times.maghrib
                            PrayerName.ISHA -> times.isha
                            else -> null
                        }
                    }?.format(DateTimeFormatter.ofPattern("h:mm a"))

                    PrayerCheckItem(
                        name = prayerName.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        time = prayerTime,
                        isCompleted = isCompleted,
                        statusText = when (status) {
                            PrayerStatus.PRAYED -> "On time"
                            PrayerStatus.LATE -> "Late"
                            PrayerStatus.MISSED -> "Missed"
                            PrayerStatus.QADA -> "Made up"
                            else -> "Upcoming"
                        },
                        onClick = { onTogglePrayer(prayerName, status) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerCheckItem(
    name: String,
    time: String?,
    isCompleted: Boolean,
    statusText: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .then(
                        if (isCompleted) {
                            Modifier.background(NimazColors.StatusColors.Prayed)
                        } else {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                CircleShape
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Prayer info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                time?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isCompleted) {
                    NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) {
                        NimazColors.StatusColors.Prayed
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
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
