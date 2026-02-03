package com.arshadshah.nimaz.presentation.screens.fasting

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLegendItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.molecules.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazCalendar
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.FastingViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
    val makeupState by viewModel.makeupState.collectAsState()
    val ramadanState by viewModel.ramadanState.collectAsState()
    val calendarState by viewModel.calendarState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.fasting_tab_ramadan), stringResource(R.string.fasting_tab_voluntary), stringResource(R.string.fasting_tab_makeup))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.fasting_title),
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
                            NimazStatsGrid(
                                stats = listOf(
                                    NimazStatData(ramadanState.fastedDays.toString(), stringResource(R.string.fasting_fasted)),
                                    NimazStatData(ramadanState.missedDays.toString(), stringResource(R.string.fasting_missed)),
                                    NimazStatData(ramadanState.remainingDays.toString(), stringResource(R.string.fasting_remaining))
                                )
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
                text = stringResource(R.string.fasting_current),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.fasting_ramadan_day, currentDay),
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
                text = stringResource(R.string.fasting_ramadan_starts_in),
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
                text = if (daysUntilRamadan == 1) stringResource(R.string.fasting_day) else stringResource(R.string.fasting_days),
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
                        text = if (unloggedDays == 1) stringResource(R.string.fasting_unlogged_day) else stringResource(R.string.fasting_unlogged_days),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.fasting_log_calendar_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            text = stringResource(R.string.fasting_today),
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
                                text = stringResource(R.string.fasting_ramadan_day, ramadanDay),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val fastingStatusText = stringResource(R.string.fasting_status_fasting)
                    val notFastingStatusText = stringResource(R.string.fasting_status_not_fasting)
                    val exemptedStatusText = stringResource(R.string.fasting_status_exempted)
                    val makeupDueStatusText = stringResource(R.string.fasting_status_makeup_due)

                    val (statusBg, statusColor, statusText) = when (fastStatus) {
                        FastStatus.FASTED -> Triple(
                            NimazColors.FastingColors.Fasted.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Fasted,
                            fastingStatusText
                        )
                        FastStatus.NOT_FASTED -> Triple(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            notFastingStatusText
                        )
                        FastStatus.EXEMPTED -> Triple(
                            NimazColors.FastingColors.Exempted.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Exempted,
                            exemptedStatusText
                        )
                        FastStatus.MAKEUP_DUE -> Triple(
                            NimazColors.FastingColors.Makeup.copy(alpha = 0.2f),
                            NimazColors.FastingColors.Makeup,
                            makeupDueStatusText
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
                                text = stringResource(R.string.fasting_suhoor_ends),
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
                                text = if (isSuhoorTime && timeUntilSuhoor.isNotEmpty()) timeUntilSuhoor else stringResource(R.string.fasting_completed),
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
                                text = stringResource(R.string.fasting_iftar),
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
                                text = if (!isSuhoorTime && timeUntilIftar.isNotEmpty()) timeUntilIftar else if (isSuhoorTime) stringResource(R.string.fasting_waiting) else stringResource(R.string.fasting_completed),
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
    val today = remember { LocalDate.now() }
    val displayedMonth = remember(selectedMonth, selectedYear) {
        YearMonth.of(selectedYear, selectedMonth)
    }
    val daysInMonth = displayedMonth.lengthOfMonth()

    val recordMap = remember(records) {
        records.associateBy { record ->
            LocalDate.ofEpochDay(record.date / (24 * 60 * 60 * 1000))
        }
    }

    val ramadanDaysInMonth = remember(selectedMonth, selectedYear) {
        if (showRamadanIndicators) {
            (1..daysInMonth).filter { day ->
                HijriDateCalculator.isRamadan(LocalDate.of(selectedYear, selectedMonth, day))
            }.toSet()
        } else emptySet()
    }

    val futureTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)

    val fastedLabel = stringResource(R.string.fasting_fasted)
    val missedLabel = stringResource(R.string.fasting_missed)
    val ramadanLabel = stringResource(R.string.fasting_tab_ramadan)
    val legendItems = remember(ramadanDaysInMonth, fastedLabel, missedLabel, ramadanLabel) {
        buildList {
            add(CalendarLegendItem(NimazColors.FastingColors.Fasted, fastedLabel))
            add(CalendarLegendItem(Color(0xFFEF4444), missedLabel))
            if (ramadanDaysInMonth.isNotEmpty()) {
                add(CalendarLegendItem(NimazColors.FastingColors.Ramadan, ramadanLabel))
            }
        }
    }

    NimazCalendar(
        displayedMonth = displayedMonth,
        selectedDate = null,
        onDateSelected = onSelectDate,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        modifier = modifier,
        dayStateProvider = { date ->
            if (date.monthValue == selectedMonth && date.year == selectedYear) {
                val record = recordMap[date]
                val isFuture = date.isAfter(today)
                val isRamadanDay = date.dayOfMonth in ramadanDaysInMonth
                CalendarDayState(
                    indicatorColor = when {
                        record?.status == FastStatus.FASTED -> NimazColors.FastingColors.Fasted
                        record?.status == FastStatus.MAKEUP_DUE -> Color(0xFFEF4444)
                        else -> null
                    },
                    backgroundColor = if (isRamadanDay)
                        NimazColors.FastingColors.Ramadan.copy(alpha = 0.15f) else null,
                    textColor = when {
                        isRamadanDay && !isFuture -> NimazColors.FastingColors.Ramadan
                        isFuture -> futureTextColor
                        else -> null
                    },
                    fontWeight = if (isRamadanDay) FontWeight.SemiBold else null
                )
            } else CalendarDayState()
        },
        legendItems = legendItems
    )
}

@Composable
private fun RecommendedFastsSection(
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    val todayText = stringResource(R.string.fasting_today)

    // Calculate next Monday
    val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    val mondayText = if (nextMonday == today) todayText else stringResource(R.string.fasting_next_format, nextMonday.format(dateFormatter))

    // Calculate next Thursday
    val nextThursday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
    val thursdayText = if (nextThursday == today) todayText else stringResource(R.string.fasting_next_format, nextThursday.format(dateFormatter))

    // Calculate Ayyam al-Beed status (13th, 14th, 15th of lunar month)
    val ayyamDays = calculateAyyamAlBeedDays(today)
    val ayyamText = when {
        ayyamDays == 0 -> todayText
        ayyamDays == 1 -> stringResource(R.string.fasting_tomorrow)
        else -> stringResource(R.string.fasting_in_days_format, ayyamDays)
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.fasting_recommended_fasts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RecommendedFastCard(
                icon = Icons.Default.CalendarMonth,
                iconBgColor = Color(0xFF3B82F6).copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_monday),
                description = stringResource(R.string.fasting_sunnah_desc),
                nextDate = mondayText
            )
            RecommendedFastCard(
                icon = Icons.Default.CalendarMonth,
                iconBgColor = Color(0xFFA855F7).copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_thursday),
                description = stringResource(R.string.fasting_sunnah_desc),
                nextDate = thursdayText
            )
            RecommendedFastCard(
                icon = Icons.Default.NightsStay,
                iconBgColor = NimazColors.FastingColors.Makeup.copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_ayyam_al_beed),
                description = stringResource(R.string.fasting_ayyam_desc),
                nextDate = ayyamText
            )
        }
    }
}

private fun calculateAyyamAlBeedDays(today: LocalDate): Int {
    val hijriDate = HijriDateCalculator.toHijri(today)
    return when (val hijriDay = hijriDate.day) {
        13, 14, 15 -> 0 // Currently Ayyam al-Beed
        in 1..12 -> 13 - hijriDay
        else -> {
            val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(hijriDate.year, hijriDate.month)
            val daysUntilNextMonth = daysInMonth - hijriDay
            daysUntilNextMonth + 13
        }
    }
}

@Composable
private fun RecommendedFastCard(
    icon: ImageVector,
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
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
            text = stringResource(R.string.fasting_log_today),
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
            NimazEmptyState(
                title = stringResource(R.string.fasting_no_makeup),
                message = stringResource(R.string.fasting_all_up_to_date),
                iconTint = GreenAccent
            )
        } else {
            val completedFasts = makeupState.allMakeupFasts.filter {
                it.status == MakeupFastStatus.COMPLETED || it.status == MakeupFastStatus.FIDYA_PAID
            }
            val completedCount = completedFasts.size
            val totalCount = makeupState.allMakeupFasts.size

            // Summary Card
            MakeupSummaryCard(pendingCount = makeupState.pendingCount)

            // Stats Grid
            NimazStatsGrid(
                stats = listOf(
                    NimazStatData("$completedCount", stringResource(R.string.fasting_completed_label), GreenAccent),
                    NimazStatData("${makeupState.pendingCount}", stringResource(R.string.fasting_pending_label), OrangeAccent),
                    NimazStatData("$totalCount", stringResource(R.string.fasting_total_label))
                )
            )

            // Info Banner
            NimazBanner(
                message = stringResource(R.string.fasting_makeup_info),
                variant = NimazBannerVariant.INFO,
                icon = Icons.Default.Info,
                showBorder = true
            )

            // Pending Section
            if (makeupState.pendingMakeupFasts.isNotEmpty()) {
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_pending),
                    trailingText = stringResource(R.string.fasting_pending_count, makeupState.pendingCount),
                    modifier = Modifier.fillMaxWidth()
                )

                makeupState.pendingMakeupFasts.forEach { makeupFast ->
                    MakeupPendingFastCard(
                        makeupFast = makeupFast,
                        onComplete = { onCompleteMakeupFast(makeupFast.id) },
                        onEdit = {
                            Toast.makeText(context, context.getString(R.string.fasting_edit_coming_soon), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Completed Section
            if (completedFasts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                NimazSectionHeader(
                    title = stringResource(R.string.fasting_completed_label),
                    trailingText = stringResource(R.string.fasting_fasts_count, completedFasts.size),
                    modifier = Modifier.fillMaxWidth()
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
                text = stringResource(R.string.fasting_fasts_to_makeup),
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
                text = stringResource(R.string.fasting_remaining_to_complete),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
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
                        text = stringResource(R.string.fasting_pending),
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
                            text = stringResource(R.string.fasting_edit),
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
                            text = stringResource(R.string.fasting_mark_complete),
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
            stringResource(R.string.fasting_fidya_paid_on, date)
        else
            stringResource(R.string.fasting_made_up_on, date)
    } ?: if (makeupFast.status == MakeupFastStatus.FIDYA_PAID) stringResource(R.string.fasting_fidya_paid) else stringResource(R.string.fasting_completed)

    val originalLabel = makeupFast.originalHijriDate?.let {
        stringResource(R.string.fasting_originally, it)
    } ?: stringResource(R.string.fasting_originally, missedDate)

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

// region Previews

@Preview(showBackground = true, widthDp = 400, name = "Stats Grid")
@Composable
private fun StatsGridPreview() {
    NimazTheme {
        NimazStatsGrid(
            stats = listOf(
                NimazStatData("15", "Fasted"),
                NimazStatData("3", "Missed"),
                NimazStatData("12", "Remaining")
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Legend Item")
@Composable
private fun LegendItemPreview() {
    NimazTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            NimazLegendItem(color = Color(0xFF22C55E), label = "Fasted")
            NimazLegendItem(color = Color(0xFFEF4444), label = "Missed")
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Recommended Fasts Section")
@Composable
private fun RecommendedFastsSectionPreview() {
    NimazTheme {
        RecommendedFastsSection()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Log Fast Button")
@Composable
private fun LogFastButtonPreview() {
    NimazTheme {
        LogFastButton(onClick = {})
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Makeup Empty State")
@Composable
private fun MakeupEmptyStatePreview() {
    NimazTheme {
        NimazEmptyState(
            title = "No Makeup Fasts",
            message = "All your fasts are up to date!",
            iconTint = GreenAccent
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Makeup Summary Card")
@Composable
private fun MakeupSummaryCardPreview() {
    NimazTheme {
        MakeupSummaryCard(pendingCount = 5)
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Makeup Stats Grid")
@Composable
private fun MakeupStatsGridPreview() {
    NimazTheme {
        NimazStatsGrid(
            stats = listOf(
                NimazStatData("8", "Completed", GreenAccent),
                NimazStatData("5", "Pending", OrangeAccent),
                NimazStatData("13", "Total")
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Makeup Section Header")
@Composable
private fun MakeupSectionHeaderPreview() {
    NimazTheme {
        NimazSectionHeader(title = "Pending", trailingText = "5")
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Ramadan Countdown Card")
@Composable
private fun RamadanCountdownCardPreview() {
    NimazTheme {
        RamadanCountdownCard()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Ramadan Banner")
@Composable
private fun RamadanBannerPreview() {
    NimazTheme {
        RamadanBanner(fastedDays = 15, totalDays = 30, currentDay = 16)
    }
}

// endregion
