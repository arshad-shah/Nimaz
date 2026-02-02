package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.presentation.components.molecules.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.IndicatorPosition
import com.arshadshah.nimaz.presentation.components.molecules.NimazCalendar
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazQadaPrayerItem
import com.arshadshah.nimaz.presentation.components.molecules.SelectionStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    initialTab: Int = 0,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pagerState = rememberPagerState(initialPage = initialTab) { 2 }
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.tracker) to Icons.Default.Schedule,
        stringResource(R.string.qada) to Icons.Default.Restore
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.prayer_tracker_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.view_statistics)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = pagerState.currentPage
            ) {
                tabs.forEachIndexed { index, (title, icon) ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TrackerTabContent(viewModel = viewModel)
                    1 -> QadaTabContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun TrackerTabContent(viewModel: PrayerTrackerViewModel) {
    val state by viewModel.trackerState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()

    var displayedMonth by remember { mutableStateOf(YearMonth.from(state.selectedDate)) }

    LaunchedEffect(displayedMonth) {
        val startDate = displayedMonth.atDay(1)
        val endDate = displayedMonth.atEndOfMonth()
        viewModel.onEvent(PrayerTrackerEvent.LoadHistory(startDate, endDate))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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

@Composable
private fun QadaTabContent(viewModel: PrayerTrackerViewModel) {
    val qadaState by viewModel.qadaState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Card
        item {
            QadaSummaryCard(totalMissed = qadaState.totalMissed)
        }

        // Empty State
        if (qadaState.missedPrayers.isEmpty() && !qadaState.isLoading) {
            item {
                NimazEmptyState(
                    title = stringResource(R.string.all_caught_up),
                    message = stringResource(R.string.all_caught_up_message)
                )
            }
        }

        // Grouped by Month
        qadaState.groupedByMonth.forEach { (monthYear, prayers) ->
            item {
                Text(
                    text = monthYear,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(prayers, key = { it.id }) { prayer ->
                NimazQadaPrayerItem(
                    prayer = prayer,
                    onMarkCompleted = {
                        viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(prayer))
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Qada Tab Components ---

@Composable
private fun QadaSummaryCard(totalMissed: Int) {
    val warningOrange = Color(0xFFF97316)
    val warningOrangeDark = Color(0xFFEA580C)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(warningOrange, warningOrangeDark)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prayers_to_make_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (totalMissed == 0) {
                        stringResource(R.string.all_caught_up_short)
                    } else {
                        stringResource(
                            R.string.missed_prayers_pending,
                            totalMissed,
                            if (totalMissed != 1) stringResource(R.string.plural_s) else ""
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "$totalMissed",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
                        text = stringResource(R.string.current_streak),
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
                    text = stringResource(R.string.consecutive_days_all_prayers),
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
    val today = remember { LocalDate.now() }

    val prayerStatusMap = remember(historyRecords) {
        historyRecords
            .filter { it.prayerName != PrayerName.SUNRISE }
            .groupBy { it.date }
            .mapValues { (_, records) ->
                val completedCount = records.count { r ->
                    r.status == PrayerStatus.PRAYED ||
                            r.status == PrayerStatus.LATE ||
                            r.status == PrayerStatus.QADA
                }
                val missedCount = records.count { r -> r.status == PrayerStatus.MISSED }
                when {
                    completedCount == 5 -> NimazColors.StatusColors.Prayed
                    completedCount > 0 -> NimazColors.StatusColors.Partial
                    missedCount > 0 -> NimazColors.StatusColors.Missed
                    else -> null
                }
            }
    }

    NimazCalendar(
        displayedMonth = displayedMonth,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onPreviousMonth = { onMonthChange(displayedMonth.minusMonths(1)) },
        onNextMonth = { onMonthChange(displayedMonth.plusMonths(1)) },
        selectionStyle = SelectionStyle.BORDER,
        dayStateProvider = { date ->
            val epoch = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
            val badgeColor = prayerStatusMap[epoch]
            CalendarDayState(
                indicatorColor = if (date.isBefore(today) && badgeColor != null) badgeColor else null,
                indicatorPosition = IndicatorPosition.TOP_END
            )
        }
    )
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

    val now = LocalDateTime.now()
    val today = LocalDate.now()
    val isToday = selectedDate == today
    val isPastDate = selectedDate.isBefore(today)

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
                    text = stringResource(R.string.prayers_completed_format, prayedCount, prayers.size),
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

                    // Get prayer time as LocalDateTime for comparison
                    val prayerDateTime = prayerTimes?.let { times ->
                        when (prayerName) {
                            PrayerName.FAJR -> times.fajr
                            PrayerName.DHUHR -> times.dhuhr
                            PrayerName.ASR -> times.asr
                            PrayerName.MAGHRIB -> times.maghrib
                            PrayerName.ISHA -> times.isha
                            else -> null
                        }
                    }

                    val prayerTimeFormatted = prayerDateTime?.format(DateTimeFormatter.ofPattern("h:mm a"))

                    // Determine if prayer should show as missed based on time
                    val isPrayerTimePassed = when {
                        isPastDate -> true // All prayers on past dates have passed
                        isToday && prayerDateTime != null -> now.isAfter(prayerDateTime)
                        else -> false
                    }

                    // For visual styling, treat auto-detected missed as missed
                    val isMissed = status == PrayerStatus.MISSED ||
                            (isPrayerTimePassed && status == PrayerStatus.NOT_PRAYED)

                    // Determine the display status
                    val displayStatus = when {
                        status == PrayerStatus.PRAYED -> stringResource(R.string.on_time)
                        status == PrayerStatus.LATE -> stringResource(R.string.late)
                        status == PrayerStatus.MISSED -> stringResource(R.string.missed)
                        status == PrayerStatus.QADA -> stringResource(R.string.made_up)
                        isPrayerTimePassed && status == PrayerStatus.NOT_PRAYED -> stringResource(R.string.missed)
                        else -> stringResource(R.string.upcoming)
                    }

                    PrayerCheckItem(
                        name = prayerName.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        time = prayerTimeFormatted,
                        isCompleted = isCompleted,
                        isMissed = isMissed,
                        statusText = displayStatus,
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
    isMissed: Boolean = false,
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
                        when {
                            isCompleted -> Modifier.background(NimazColors.StatusColors.Prayed)
                            isMissed -> Modifier.background(NimazColors.StatusColors.Missed)
                            else -> Modifier.border(
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
                color = when {
                    isCompleted -> NimazColors.StatusColors.Prayed.copy(alpha = 0.2f)
                    isMissed -> NimazColors.StatusColors.Missed.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isCompleted -> NimazColors.StatusColors.Prayed
                        isMissed -> NimazColors.StatusColors.Missed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

