package com.arshadshah.nimaz.presentation.screens.fasting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.FastingViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// Color constants for makeup fasts
private val OrangeAccent = Color(0xFFF97316)
private val OrangeDark = Color(0xFFEA580C)
private val GreenAccent = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val makeupState by viewModel.makeupState.collectAsState()
    val ramadanState by viewModel.ramadanState.collectAsState()
    val calendarState by viewModel.calendarState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ramadan", "Voluntary", "Makeup")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = "Fasting",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                when (index) {
                                    0 -> viewModel.onEvent(FastingEvent.LoadRamadan)
                                    1 -> viewModel.onEvent(FastingEvent.SetFastType(FastType.VOLUNTARY))
                                    2 -> viewModel.onEvent(FastingEvent.LoadMakeupFasts)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    // Ramadan Tab
                    if (ramadanState.isRamadan) {
                        // During Ramadan - show banner and stats
                        item {
                            RamadanBanner(
                                fastedDays = ramadanState.fastedDays,
                                totalDays = ramadanState.fastedDays + ramadanState.missedDays + ramadanState.remainingDays,
                                currentDay = ramadanState.currentDay
                            )
                        }

                        // Stats Grid
                        item {
                            StatsGrid(
                                fasted = ramadanState.fastedDays,
                                missed = ramadanState.missedDays,
                                remaining = ramadanState.remainingDays
                            )
                        }

                        // Missed Fasts Alert (if any days are missed/not logged)
                        item {
                            RamadanMissedFastsTracker(
                                currentDay = ramadanState.currentDay,
                                fastedDays = ramadanState.fastedDays,
                                records = calendarState.records
                            )
                        }
                    } else {
                        // Before/After Ramadan - show countdown
                        item {
                            RamadanCountdownCard()
                        }
                    }

                    // Today's Fast
                    item {
                        TodayFastSection(
                            isFasting = state.isFastingToday,
                            fastStatus = state.todayRecord?.status ?: FastStatus.NOT_FASTED,
                            fastType = state.selectedFastType,
                            selectedDate = state.selectedDate,
                            ramadanDay = if (ramadanState.isRamadan) ramadanState.currentDay else null,
                            suhoorTime = state.suhoorTime,
                            iftarTime = state.iftarTime,
                            timeUntilIftar = state.timeUntilIftar,
                            timeUntilSuhoor = state.timeUntilSuhoor,
                            isSuhoorTime = state.isSuhoorTime,
                            onToggleFast = { viewModel.onEvent(FastingEvent.ToggleTodayFast) }
                        )
                    }

                    // Calendar with Ramadan indicators
                    item {
                        FastingCalendarSection(
                            records = calendarState.records,
                            selectedMonth = calendarState.selectedMonth,
                            selectedYear = calendarState.selectedYear,
                            onPreviousMonth = {
                                val newMonth = if (calendarState.selectedMonth == 1) 12 else calendarState.selectedMonth - 1
                                val newYear = if (calendarState.selectedMonth == 1) calendarState.selectedYear - 1 else calendarState.selectedYear
                                viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                            },
                            onNextMonth = {
                                val newMonth = if (calendarState.selectedMonth == 12) 1 else calendarState.selectedMonth + 1
                                val newYear = if (calendarState.selectedMonth == 12) calendarState.selectedYear + 1 else calendarState.selectedYear
                                viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                            },
                            onSelectDate = { date -> viewModel.onEvent(FastingEvent.SelectDate(date)) },
                            showRamadanIndicators = true
                        )
                    }

                    // Log Fast Button
                    item {
                        LogFastButton(
                            onClick = { viewModel.onEvent(FastingEvent.ToggleTodayFast) }
                        )
                    }
                }
                1 -> {
                    // Voluntary Tab - Simplified view
                    // Today's Fast
                    item {
                        TodayFastSection(
                            isFasting = state.isFastingToday,
                            fastStatus = state.todayRecord?.status ?: FastStatus.NOT_FASTED,
                            fastType = FastType.VOLUNTARY,
                            selectedDate = state.selectedDate,
                            ramadanDay = null,
                            suhoorTime = state.suhoorTime,
                            iftarTime = state.iftarTime,
                            timeUntilIftar = state.timeUntilIftar,
                            timeUntilSuhoor = state.timeUntilSuhoor,
                            isSuhoorTime = state.isSuhoorTime,
                            onToggleFast = { viewModel.onEvent(FastingEvent.ToggleTodayFast) }
                        )
                    }

                    // Calendar
                    item {
                        FastingCalendarSection(
                            records = calendarState.records,
                            selectedMonth = calendarState.selectedMonth,
                            selectedYear = calendarState.selectedYear,
                            onPreviousMonth = {
                                val newMonth = if (calendarState.selectedMonth == 1) 12 else calendarState.selectedMonth - 1
                                val newYear = if (calendarState.selectedMonth == 1) calendarState.selectedYear - 1 else calendarState.selectedYear
                                viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                            },
                            onNextMonth = {
                                val newMonth = if (calendarState.selectedMonth == 12) 1 else calendarState.selectedMonth + 1
                                val newYear = if (calendarState.selectedMonth == 12) calendarState.selectedYear + 1 else calendarState.selectedYear
                                viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                            },
                            onSelectDate = { date -> viewModel.onEvent(FastingEvent.SelectDate(date)) }
                        )
                    }

                    // Recommended Fasts
                    item {
                        RecommendedFastsSection()
                    }

                    // Log Fast Button
                    item {
                        LogFastButton(
                            onClick = { viewModel.onEvent(FastingEvent.ToggleTodayFast) }
                        )
                    }
                }
                2 -> {
                    // Makeup Tab - Show makeup fasts inline
                    item {
                        MakeupFastsContent(
                            makeupState = makeupState,
                            onCompleteMakeupFast = { makeupFastId ->
                                viewModel.onEvent(FastingEvent.CompleteMakeupFast(makeupFastId))
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RamadanBanner(
    fastedDays: Int,
    totalDays: Int,
    currentDay: Int,
    modifier: Modifier = Modifier
) {
    val purpleGradient = Brush.linearGradient(
        colors = listOf(
            NimazColors.FastingColors.Ramadan,
            NimazColors.FastingColors.Ramadan.copy(alpha = 0.85f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(purpleGradient)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "CURRENT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ramadan - Day $currentDay",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { if (totalDays > 0) fastedDays.toFloat() / totalDays else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Text(
                    text = "$fastedDays/$totalDays",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RamadanCountdownCard(
    modifier: Modifier = Modifier
) {
    val daysUntilRamadan = HijriDateCalculator.daysUntilNextRamadan()
    val hijriToday = HijriDateCalculator.today()

    // Get the target Ramadan year
    val targetYear = if (hijriToday.month >= 9) hijriToday.year + 1 else hijriToday.year
    val ramadanStart = HijriDateCalculator.getFirstDayOfRamadan(targetYear)
    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        NimazColors.FastingColors.Ramadan,
                        NimazColors.FastingColors.Ramadan.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "RAMADAN STARTS IN",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$daysUntilRamadan",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = if (daysUntilRamadan == 1) "day" else "days",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = ramadanStart.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun RamadanMissedFastsTracker(
    currentDay: Int,
    fastedDays: Int,
    records: List<FastRecord>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    // Calculate how many past Ramadan days have no logged fast
    // currentDay is the current day of Ramadan (1-30)
    // fastedDays is the number of days logged as fasted
    // Past days without a record are considered missed

    val pastDaysInRamadan = currentDay - 1 // Days before today in Ramadan
    val recordedDays = records.count { record ->
        val recordDate = LocalDate.ofEpochDay(record.date / (24 * 60 * 60 * 1000))
        recordDate.isBefore(today) && HijriDateCalculator.isRamadan(recordDate)
    }

    val unloggedDays = (pastDaysInRamadan - recordedDays).coerceAtLeast(0)

    if (unloggedDays > 0) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFEF4444).copy(alpha = 0.1f),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$unloggedDays",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (unloggedDays == 1) "Unlogged Day" else "Unlogged Days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap on calendar dates to log your fasts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(
    fasted: Int,
    missed: Int,
    remaining: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = fasted.toString(),
            label = "Fasted",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = missed.toString(),
            label = "Missed",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = remaining.toString(),
            label = "Remaining",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(15.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayFastSection(
    isFasting: Boolean,
    fastStatus: FastStatus,
    fastType: FastType,
    selectedDate: LocalDate,
    ramadanDay: Int?,
    suhoorTime: String,
    iftarTime: String,
    timeUntilIftar: String,
    timeUntilSuhoor: String,
    isSuhoorTime: Boolean,
    onToggleFast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    Column(modifier = modifier) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Column {
                // Header with date and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = selectedDate.format(formatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (ramadanDay != null) {
                            Text(
                                text = "Ramadan - Day $ramadanDay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val (statusBg, statusColor, statusText) = when (fastStatus) {
                        FastStatus.FASTED -> Triple(
                            NimazColors.FastingColors.Fasted.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Fasted,
                            "Fasting"
                        )
                        FastStatus.NOT_FASTED -> Triple(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            "Not Fasting"
                        )
                        FastStatus.EXEMPTED -> Triple(
                            NimazColors.FastingColors.Exempted.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Exempted,
                            "Exempted"
                        )
                        FastStatus.MAKEUP_DUE -> Triple(
                            NimazColors.FastingColors.Makeup.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Makeup,
                            "Makeup Due"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(statusBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Suhoor and Iftar times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    // Suhoor card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(15.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Suhoor ends",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = suhoorTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF818CF8) // Indigo for suhoor
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (isSuhoorTime && timeUntilSuhoor.isNotEmpty()) timeUntilSuhoor else "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Iftar card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(15.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Iftar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = iftarTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NimazColors.FastingColors.Makeup // Orange
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (!isSuhoorTime && timeUntilIftar.isNotEmpty()) timeUntilIftar else if (isSuhoorTime) "Waiting" else "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastingCalendarSection(
    records: List<FastRecord>,
    selectedMonth: Int,
    selectedYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    showRamadanIndicators: Boolean = true,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val monthStart = LocalDate.of(selectedYear, selectedMonth, 1)
    val daysInMonth = monthStart.lengthOfMonth()
    val firstDayOfWeek = monthStart.dayOfWeek.value % 7 // Sunday = 0

    // Build a map of date -> status from records
    val recordMap = remember(records) {
        records.associateBy { record ->
            LocalDate.ofEpochDay(record.date / (24 * 60 * 60 * 1000))
        }
    }

    // Check which days are in Ramadan for indicators
    val ramadanDaysInMonth = remember(selectedMonth, selectedYear) {
        if (showRamadanIndicators) {
            (1..daysInMonth).filter { day ->
                val date = LocalDate.of(selectedYear, selectedMonth, day)
                HijriDateCalculator.isRamadan(date)
            }.toSet()
        } else {
            emptySet()
        }
    }

    Column(modifier = modifier) {
        // Calendar header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())} $selectedYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calendar grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(15.dp)
        ) {
            Column {
                // Weekday headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days grid
                val totalCells = firstDayOfWeek + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - firstDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val date = LocalDate.of(selectedYear, selectedMonth, dayNumber)
                                val record = recordMap[date]
                                val isToday = date == today
                                val isFuture = date.isAfter(today)
                                val isFasted = record?.status == FastStatus.FASTED
                                val isMissed = record?.status == FastStatus.MAKEUP_DUE
                                val isRamadanDay = dayNumber in ramadanDaysInMonth

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                isRamadanDay -> NimazColors.FastingColors.Ramadan.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onSelectDate(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isToday || isRamadanDay) FontWeight.SemiBold else FontWeight.Normal,
                                            color = when {
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isRamadanDay && !isFuture -> NimazColors.FastingColors.Ramadan
                                                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        // Status dot
                                        if (isFasted || isMissed) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isFasted) NimazColors.FastingColors.Fasted
                                                        else Color(0xFFEF4444)
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = NimazColors.FastingColors.Fasted, label = "Fasted")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color(0xFFEF4444), label = "Missed")
                    if (ramadanDaysInMonth.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(16.dp))
                        LegendItem(color = NimazColors.FastingColors.Ramadan, label = "Ramadan")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecommendedFastsSection(
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    // Calculate next Monday
    val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    val mondayText = if (nextMonday == today) "Today" else "Next: ${nextMonday.format(dateFormatter)}"

    // Calculate next Thursday
    val nextThursday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
    val thursdayText = if (nextThursday == today) "Today" else "Next: ${nextThursday.format(dateFormatter)}"

    // Calculate Ayyam al-Beed status (13th, 14th, 15th of lunar month)
    val ayyamText = calculateAyyamAlBeedStatus(today)

    Column(modifier = modifier) {
        Text(
            text = "Recommended Fasts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RecommendedFastCard(
                icon = "\uD83D\uDCC5",
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
                name = "Monday Fasting",
                description = "Sunnah of the Prophet \uFDFA",
                nextDate = mondayText
            )
            RecommendedFastCard(
                icon = "\uD83D\uDCC5",
                iconBgColor = Color(0xFFA855F7).copy(alpha = 0.2f),
                name = "Thursday Fasting",
                description = "Sunnah of the Prophet \uFDFA",
                nextDate = thursdayText
            )
            RecommendedFastCard(
                icon = "\uD83C\uDF15",
                iconBgColor = NimazColors.FastingColors.Makeup.copy(alpha = 0.2f),
                name = "Ayyam al-Beed",
                description = "13th, 14th, 15th of lunar month",
                nextDate = ayyamText
            )
        }
    }
}

private fun calculateAyyamAlBeedStatus(today: LocalDate): String {
    val hijriDate = HijriDateCalculator.toHijri(today)
    val hijriDay = hijriDate.day

    return when (hijriDay) {
        13, 14, 15 -> "Today" // Currently Ayyam al-Beed
        in 1..12 -> {
            // Calculate days until 13th
            val daysUntil = 13 - hijriDay
            if (daysUntil == 1) "Tomorrow" else "In $daysUntil days"
        }
        else -> {
            // After the 15th, calculate days until next month's 13th
            val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(hijriDate.year, hijriDate.month)
            val daysUntilNextMonth = daysInMonth - hijriDay
            val daysUntil = daysUntilNextMonth + 13
            "In $daysUntil days"
        }
    }
}

@Composable
private fun RecommendedFastCard(
    icon: String,
    iconBgColor: Color,
    name: String,
    description: String,
    nextDate: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = nextDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LogFastButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Log Today's Fast",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Makeup Fasts Content Components
@Composable
private fun MakeupFastsContent(
    makeupState: com.arshadshah.nimaz.presentation.viewmodel.MakeupFastsUiState,
    onCompleteMakeupFast: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (makeupState.allMakeupFasts.isEmpty()) {
            MakeupEmptyState()
        } else {
            val completedFasts = makeupState.allMakeupFasts.filter {
                it.status == MakeupFastStatus.COMPLETED || it.status == MakeupFastStatus.FIDYA_PAID
            }
            val completedCount = completedFasts.size
            val totalCount = makeupState.allMakeupFasts.size

            // Summary Card
            MakeupSummaryCard(pendingCount = makeupState.pendingCount)

            // Stats Grid
            MakeupStatsGrid(
                completedCount = completedCount,
                pendingCount = makeupState.pendingCount,
                totalCount = totalCount
            )

            // Info Banner
            MakeupInfoBanner()

            // Pending Section
            if (makeupState.pendingMakeupFasts.isNotEmpty()) {
                MakeupSectionHeader(
                    title = "Pending",
                    count = "${makeupState.pendingCount} pending"
                )

                makeupState.pendingMakeupFasts.forEach { makeupFast ->
                    MakeupPendingFastCard(
                        makeupFast = makeupFast,
                        onComplete = { onCompleteMakeupFast(makeupFast.id) },
                        onEdit = {
                            Toast.makeText(context, "Edit makeup fast dialog coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Completed Section
            if (completedFasts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                MakeupSectionHeader(
                    title = "Completed",
                    count = "${completedFasts.size} fasts"
                )

                completedFasts.forEach { makeupFast ->
                    MakeupCompletedFastItem(makeupFast = makeupFast)
                }
            }
        }
    }
}

@Composable
private fun MakeupSummaryCard(
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(OrangeAccent, OrangeDark)
                )
            )
            .padding(25.dp)
    ) {
        Column {
            Text(
                text = "Fasts to Make Up",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$pendingCount",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "remaining to complete",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun MakeupStatsGrid(
    completedCount: Int,
    pendingCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MakeupStatCard(
            value = "$completedCount",
            label = "Completed",
            valueColor = GreenAccent,
            modifier = Modifier.weight(1f)
        )
        MakeupStatCard(
            value = "$pendingCount",
            label = "Pending",
            valueColor = OrangeAccent,
            modifier = Modifier.weight(1f)
        )
        MakeupStatCard(
            value = "$totalCount",
            label = "Total",
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MakeupStatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = valueColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MakeupInfoBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Makeup fasts should ideally be completed before the next Ramadan. Fasting on Mondays and Thursdays is recommended.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun MakeupSectionHeader(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = count,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MakeupPendingFastCard(
    makeupFast: MakeupFast,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)

    val displayDate = makeupFast.originalHijriDate ?: missedDate

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Header: date + reason on left, status badge on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = makeupFast.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OrangeAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OrangeAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Edit button
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mark Complete button
                Surface(
                    onClick = onComplete,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark Complete",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MakeupCompletedFastItem(
    makeupFast: MakeupFast,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val missedDate = Instant.ofEpochMilli(makeupFast.originalDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)

    val completedDateText = makeupFast.completedDate?.let {
        val date = Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(formatter)
        if (makeupFast.status == MakeupFastStatus.FIDYA_PAID)
            "Fidya paid on $date"
        else
            "Made up on $date"
    } ?: if (makeupFast.status == MakeupFastStatus.FIDYA_PAID) "Fidya paid" else "Completed"

    val originalLabel = makeupFast.originalHijriDate?.let {
        "Originally: $it"
    } ?: "Originally: $missedDate"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            // Green check icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = completedDateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = originalLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MakeupEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Makeup Fasts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "All your fasts are up to date!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
