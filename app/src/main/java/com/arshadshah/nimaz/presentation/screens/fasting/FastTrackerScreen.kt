package com.arshadshah.nimaz.presentation.screens.fasting

import com.arshadshah.nimaz.core.util.formatLongDate
import com.arshadshah.nimaz.core.util.formatWeekdayDayMonth
import com.arshadshah.nimaz.core.util.formatDayMonth
import com.arshadshah.nimaz.core.util.formatMediumDate
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FastRecord
import com.arshadshah.nimaz.domain.model.FastStatus
import com.arshadshah.nimaz.domain.model.FastType
import com.arshadshah.nimaz.domain.model.MakeupFast
import com.arshadshah.nimaz.domain.model.MakeupFastStatus
import com.arshadshah.nimaz.presentation.components.atoms.NimazSwitch
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconContainerShape
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconType
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazLegendItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.atoms.countdownText
import com.arshadshah.nimaz.presentation.components.atoms.rememberCountdownTo
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.FastingViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import com.arshadshah.nimaz.presentation.viewmodel.tracker.MakeupFastsUiState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import com.arshadshah.nimaz.presentation.theme.ThemeMode
import android.content.res.Configuration
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Restore
import com.arshadshah.nimaz.core.util.formatCurrency
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.screens.resolve

// Color constants for makeup fasts
// Shared with MakeupFastsTab.kt, which was cut out of this file — hence `internal` rather
// than `private`. They are aliases onto the palette, not literals (CLAUDE.md rule 7).
internal val OrangeAccent = NimazColors.PrayerColors.Asr
internal val OrangeDark = NimazColors.OrangeDark
internal val GreenAccent = NimazColors.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    val makeupState by viewModel.makeupState.collectAsStateWithLifecycle()
    val ramadanState by viewModel.ramadanState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarState.collectAsStateWithLifecycle()
    val sheetState by viewModel.sheetState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    // The two long sections start folded. The tracker tab was a very long scroll with the
    // calendar and the recommended list always open; the Go deeper rows now report what is in
    // each and reveal it on demand, which is what "go deeper" should mean.
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var showRecommended by rememberSaveable { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.fasting_tab_tracker),
        stringResource(R.string.fasting_tab_makeup)
    )

    // Fast management bottom sheet
    FastManagementBottomSheet(
        isVisible = sheetState.isVisible,
        date = sheetState.date,
        existingRecord = sheetState.existingRecord,
        initialStatus = sheetState.selectedStatus,
        initialFastType = sheetState.selectedFastType,
        initialExemptionReason = sheetState.selectedExemptionReason,
        initialNote = sheetState.note,
        onSave = { status, fastType, exemptionReason, note ->
            viewModel.onEvent(
                FastingEvent.SaveFastForDate(
                    sheetState.date,
                    status,
                    fastType,
                    exemptionReason,
                    note
                )
            )
        },
        onDelete = { viewModel.onEvent(FastingEvent.DeleteFastRecord(sheetState.date)) },
        onDismiss = { viewModel.onEvent(FastingEvent.DismissFastSheet) }
    )

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.fasting_title),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs — full-width, fixed above the scrolling content
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        // Data for both tabs is loaded in init and kept live via
                        // Flow collectors — no need to re-dispatch a load here.
                        onClick = { selectedTab = index },
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // Tracker — merged Ramadan + Voluntary, context-aware
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
                                        NimazStatData(
                                            ramadanState.fastedDays.toString(),
                                            stringResource(R.string.fasting_fasted)
                                        ),
                                        NimazStatData(
                                            ramadanState.missedDays.toString(),
                                            stringResource(R.string.fasting_missed)
                                        ),
                                        NimazStatData(
                                            ramadanState.remainingDays.toString(),
                                            stringResource(R.string.fasting_remaining)
                                        )
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
                        } else if (HijriDateCalculator.daysUntilNextRamadan() <= RamadanCardWindowDays) {
                            // Ramadan approaching (within 30 days) - show countdown
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
                                suhoorAt = state.suhoorAt,
                                iftarAt = state.iftarAt,
                                canToggle = state.canToggleToday,
                                onToggleFast = { viewModel.onEvent(FastingEvent.ToggleTodayFast) }
                            )
                        }

                        // Calendar with Ramadan indicators — revealed by its Go deeper row.
                        if (showCalendar) item {
                            FastingCalendarSection(
                                records = calendarState.records,
                                selectedMonth = calendarState.selectedMonth,
                                selectedYear = calendarState.selectedYear,
                                onPreviousMonth = {
                                    val newMonth =
                                        if (calendarState.selectedMonth == 1) 12 else calendarState.selectedMonth - 1
                                    val newYear =
                                        if (calendarState.selectedMonth == 1) calendarState.selectedYear - 1 else calendarState.selectedYear
                                    viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                                },
                                onNextMonth = {
                                    val newMonth =
                                        if (calendarState.selectedMonth == 12) 1 else calendarState.selectedMonth + 1
                                    val newYear =
                                        if (calendarState.selectedMonth == 12) calendarState.selectedYear + 1 else calendarState.selectedYear
                                    viewModel.onEvent(FastingEvent.SelectMonth(newMonth, newYear))
                                },
                                onSelectDate = { date ->
                                    viewModel.onEvent(FastingEvent.SelectDate(date))
                                    viewModel.onEvent(FastingEvent.OpenFastSheet(date))
                                },
                                showRamadanIndicators = ramadanState.isRamadan
                            )
                        }

                        // Recommended voluntary fasts - only outside Ramadan
                        if (!ramadanState.isRamadan && showRecommended) {
                            item {
                                RecommendedFastsSection(
                                    records = calendarState.records,
                                    onLogFast = { date ->
                                        viewModel.onEvent(FastingEvent.SelectDate(date))
                                        viewModel.onEvent(FastingEvent.OpenFastSheet(date))
                                    },
                                    daysUntilAyyamAlBeed = ramadanState.daysUntilAyyamAlBeed,
                                )
                            }
                        }

                        // Go deeper — one group of rows that report, replacing subtitles that
                        // restated their own titles. The makeup count is a badge rather than
                        // prose: it is the number someone came here to check.
                        item {
                            FastingGoDeeperGroup(
                                fastedThisMonth = calendarState.records.count {
                                    it.status == FastStatus.FASTED
                                },
                                daysUntilRecommended = ramadanState.daysUntilAyyamAlBeed,
                                showRecommendedRow = !ramadanState.isRamadan,
                                pendingMakeup = makeupState.pendingCount,
                                fidyaPaid = makeupState.totalFidyaPaid,
                                currency = makeupState.currency,
                                isRamadan = ramadanState.isRamadan,
                                ramadanDay = ramadanState.currentDay,
                                daysUntilRamadan = HijriDateCalculator.daysUntilNextRamadan(),
                                calendarExpanded = showCalendar,
                                recommendedExpanded = showRecommended,
                                onToggleCalendar = { showCalendar = !showCalendar },
                                onToggleRecommended = { showRecommended = !showRecommended },
                                onOpenMakeup = { selectedTab = 1 },
                                onOpenRamadanCalendar = onNavigateToCalendar,
                            )
                        }

                        // Log a fast for the selected day. Kept as its own action: the calendar
                        // reaches any date, but "log today" should not require finding today on a
                        // grid that may be folded away.
                        item {
                            LogFastButton(
                                onClick = {
                                    viewModel.onEvent(FastingEvent.OpenFastSheet(state.selectedDate))
                                }
                            )
                        }
                    }

                    1 -> {
                        // Makeup Tab - Show makeup fasts inline
                        item {
                            MakeupFastsContent(
                                makeupState = makeupState,
                                onCompleteMakeupFast = { makeupFastId ->
                                    viewModel.onEvent(FastingEvent.CompleteMakeupFast(makeupFastId))
                                },
                                onUpdateMakeupFast = { updatedFast ->
                                    viewModel.onEvent(FastingEvent.UpdateMakeupFast(updatedFast))
                                },
                                onPayFidya = { id, amount ->
                                    viewModel.onEvent(FastingEvent.PayFidya(id, amount))
                                }
                            )
                        }
                    }
                }

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
    GradientCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        gradientColors = listOf(
            NimazColors.FastingColors.Ramadan,
            NimazColors.FastingColors.Ramadan.copy(alpha = 0.85f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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

    GradientCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        gradientColors = listOf(
            NimazColors.FastingColors.Ramadan,
            NimazColors.FastingColors.Ramadan.copy(alpha = 0.8f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                text = if (daysUntilRamadan == 1) stringResource(R.string.fasting_day) else stringResource(
                    R.string.fasting_days
                ),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = ramadanStart.formatLongDate(),
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
        NimazCard(
            modifier = modifier.fillMaxWidth(),
            style = NimazCardStyle.OUTLINED,
            shape = RoundedCornerShape(14.dp),
            colors = NimazCardDefaults.colors(
                container = NimazColors.PrayerColors.Maghrib.copy(alpha = 0.1f),
                border = NimazColors.PrayerColors.Maghrib.copy(alpha = 0.3f),
                borderWidth = 1.dp
            )
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
                        .background(NimazColors.PrayerColors.Maghrib.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$unloggedDays",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.PrayerColors.Maghrib
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (unloggedDays == 1) stringResource(R.string.fasting_unlogged_day) else stringResource(
                            R.string.fasting_unlogged_days
                        ),
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
    suhoorAt: kotlin.time.Instant?,
    iftarAt: kotlin.time.Instant?,
    canToggle: Boolean,
    onToggleFast: () -> Unit,
    modifier: Modifier = Modifier
) {
    // "Are we still before Fajr" is a function of now — derived here off the shared ticker
    // instead of being frozen into state when prayer times happened to load.
    val now by rememberNow(TickResolution.MINUTES)
    val isSuhoorTime = suhoorAt != null && now < suhoorAt
    val suhoorTime = suhoorAt?.let { clockTimeText(it) } ?: "--:--"
    val iftarTime = iftarAt?.let { clockTimeText(it) } ?: "--:--"

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.fasting_today),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.ELEVATED,
            shape = RoundedCornerShape(16.dp),
            tone = NimazTone.NEUTRAL
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with date and status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = selectedDate.formatWeekdayDayMonth(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // The Hijri date, because a fast is a Hijri-calendar act: "13 Sha'ban" is
                        // what tells someone *which* recommended fast today is, and the Gregorian
                        // line above cannot. Inside Ramadan the day number is the better fact, so
                        // it takes the slot instead of stacking three dates.
                        Text(
                            text = if (ramadanDay != null) {
                                stringResource(R.string.fasting_ramadan_day, ramadanDay)
                            } else {
                                HijriDateCalculator.toHijri(selectedDate).formatted()
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

                Spacer(modifier = Modifier.height(16.dp))

                // The one-tap log for the day.
                //
                // `onToggleFast` was previously passed into this composable and never
                // invoked, so FastingEvent.ToggleTodayFast had no producer at all and
                // the day could only be logged through the management sheet.
                //
                // Disabled for EXEMPTED and MAKEUP_DUE: those are considered states
                // recorded in the sheet with a reason attached, and a single tap must
                // not silently overwrite one. The subtitle says where to change it
                // rather than leaving a control that looks live and does nothing.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fasting_log_today),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                !canToggle -> stringResource(R.string.fasting_log_managed_in_sheet)
                                isFasting -> stringResource(R.string.fasting_log_on_subtitle)
                                else -> stringResource(R.string.fasting_log_off_subtitle)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    NimazSwitch(
                        checked = isFasting,
                        enabled = canToggle,
                        onCheckedChange = { onToggleFast() },
                        contentDescription = stringResource(R.string.fasting_log_today)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Suhoor and Iftar times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    // Suhoor card
                    NimazCard(
                        modifier = Modifier.weight(1f),
                        style = NimazCardStyle.FILLED,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp)
                        ) {
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
                                color = NimazColors.IndigoLight // Indigo for suhoor
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (isSuhoorTime && suhoorAt != null) {
                                    countdownText(
                                        rememberCountdownTo(suhoorAt).value,
                                        showSeconds = false,
                                    )
                                } else stringResource(R.string.fasting_completed),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Iftar card
                    NimazCard(
                        modifier = Modifier.weight(1f),
                        style = NimazCardStyle.FILLED,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp)
                        ) {
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
                                text = if (isSuhoorTime) {
                                    stringResource(R.string.fasting_waiting)
                                } else if (iftarAt != null && now < iftarAt) {
                                    countdownText(
                                        rememberCountdownTo(iftarAt).value,
                                        showSeconds = false,
                                    )
                                } else stringResource(R.string.fasting_completed),
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
            add(CalendarLegendItem(NimazColors.PrayerColors.Maghrib, missedLabel))
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
                        record?.status == FastStatus.MAKEUP_DUE -> NimazColors.PrayerColors.Maghrib
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
    records: List<FastRecord> = emptyList(),
    onLogFast: (LocalDate) -> Unit = {},
    daysUntilAyyamAlBeed: Int = 0,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    val todayText = stringResource(R.string.fasting_today)

    // Build a set of fasted dates for quick lookup
    val fastedDates = remember(records) {
        records.filter { it.status == FastStatus.FASTED }
            .map { LocalDate.ofEpochDay(it.date / (24 * 60 * 60 * 1000)) }
            .toSet()
    }

    // Calculate next Monday
    val nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
    val mondayText = if (nextMonday == today) todayText else stringResource(
        R.string.fasting_next_format,
        nextMonday.formatDayMonth()
    )

    // Calculate next Thursday
    val nextThursday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
    val thursdayText = if (nextThursday == today) todayText else stringResource(
        R.string.fasting_next_format,
        nextThursday.formatDayMonth()
    )

    // Ayyam al-Beed (13th, 14th, 15th of the lunar month) — counted by the ViewModel, which
    // is where the clock and the user's Hijri offset both live.
    val ayyamDays = daysUntilAyyamAlBeed
    val ayyamText = when {
        ayyamDays == 0 -> todayText
        ayyamDays == 1 -> stringResource(R.string.fasting_tomorrow)
        else -> pluralStringResource(R.plurals.fasting_in_days_format, ayyamDays, ayyamDays)
    }

    // Islamic calendar recommended fasts
    val hijriToday = remember { HijriDateCalculator.today() }
    val ashuraName = stringResource(R.string.fasting_event_ashura_name)
    val ashuraDesc = stringResource(R.string.fasting_event_ashura_description)
    val arafahName = stringResource(R.string.fasting_event_arafah_name)
    val arafahDesc = stringResource(R.string.fasting_event_arafah_description)
    val shawwalName = stringResource(R.string.fasting_event_shawwal_name)
    val shawwalDesc = stringResource(R.string.fasting_event_shawwal_description)
    val midShabanName = stringResource(R.string.fasting_event_mid_shaban_name)
    val midShabanDesc = stringResource(R.string.fasting_event_mid_shaban_description)
    val islamicFasts = remember(hijriToday.year) {
        val events = HijriDateCalculator.getIslamicEvents(hijriToday.year) +
                HijriDateCalculator.getIslamicEvents(hijriToday.year + 1)

        data class RecommendedIslamicFast(
            val name: String,
            val date: LocalDate,
            val description: String
        )

        buildList {
            // Day of Ashura (10 Muharram) — events span two hijri years, so keep
            // only the single nearest upcoming occurrence (avoids duplicate cards).
            events.filter { it.name == "Day of Ashura" }
                .map { it.toGregorianDate() }
                .filter { !it.isBefore(today) }
                .minOrNull()
                ?.let { date ->
                    add(
                        RecommendedIslamicFast(
                            name = ashuraName,
                            date = date,
                            description = ashuraDesc
                        )
                    )
                }
            // Day of Arafah (9 Dhul Hijjah) — nearest upcoming occurrence only.
            events.filter { it.name == "Day of Arafah" }
                .map { it.toGregorianDate() }
                .filter { !it.isBefore(today) }
                .minOrNull()
                ?.let { date ->
                    add(
                        RecommendedIslamicFast(
                            name = arafahName,
                            date = date,
                            description = arafahDesc
                        )
                    )
                }
            // 6 Days of Shawwal
            try {
                val shawwalStart = HijriDateCalculator.toGregorian(2, 10, hijriToday.year)
                if (!shawwalStart.isBefore(today)) {
                    add(
                        RecommendedIslamicFast(
                            name = shawwalName,
                            date = shawwalStart,
                            description = shawwalDesc
                        )
                    )
                }
            } catch (_: Exception) {
            }
            // Mid-Sha'ban (15 Sha'ban)
            try {
                val midShaban = HijriDateCalculator.toGregorian(15, 8, hijriToday.year)
                if (!midShaban.isBefore(today)) {
                    add(
                        RecommendedIslamicFast(
                            name = midShabanName,
                            date = midShaban,
                            description = midShabanDesc
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }.take(3) // Show at most 3 upcoming
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
                iconBgColor = NimazColors.Info.copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_monday),
                description = stringResource(R.string.fasting_sunnah_desc),
                nextDate = mondayText,
                isFasted = nextMonday in fastedDates,
                onClick = { onLogFast(nextMonday) }
            )
            RecommendedFastCard(
                icon = Icons.Default.CalendarMonth,
                iconBgColor = NimazColors.Purple.copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_thursday),
                description = stringResource(R.string.fasting_sunnah_desc),
                nextDate = thursdayText,
                isFasted = nextThursday in fastedDates,
                onClick = { onLogFast(nextThursday) }
            )
            RecommendedFastCard(
                icon = Icons.Default.NightsStay,
                iconBgColor = NimazColors.FastingColors.Makeup.copy(alpha = 0.2f),
                name = stringResource(R.string.fasting_ayyam_al_beed),
                description = stringResource(R.string.fasting_ayyam_desc),
                nextDate = ayyamText,
                onClick = {
                    // Open sheet for the next Ayyam al-Beed day
                    val hijri = HijriDateCalculator.toHijri(today)
                    val targetDay = if (hijri.day <= 15) 13 else {
                        // Next month's 13th
                        13
                    }
                    try {
                        val nextMonth = if (hijri.day > 15) {
                            if (hijri.month == 12) 1 else hijri.month + 1
                        } else hijri.month
                        val nextYear =
                            if (hijri.day > 15 && hijri.month == 12) hijri.year + 1 else hijri.year
                        val ayyamDate =
                            HijriDateCalculator.toGregorian(targetDay, nextMonth, nextYear)
                        onLogFast(ayyamDate)
                    } catch (_: Exception) {
                        onLogFast(today)
                    }
                }
            )

            // Islamic calendar fasts
            islamicFasts.forEach { fast ->
                val fastDate = fast.date
                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, fastDate).toInt()
                val dateText = when {
                    daysUntil == 0 -> todayText
                    daysUntil == 1 -> stringResource(R.string.fasting_tomorrow)
                    else -> stringResource(
                        R.string.fasting_next_format,
                        fastDate.formatDayMonth()
                    )
                }
                RecommendedFastCard(
                    icon = Icons.Default.NightsStay,
                    iconBgColor = NimazColors.Emerald.copy(alpha = 0.2f),
                    name = fast.name,
                    description = fast.description,
                    nextDate = dateText,
                    isFasted = fastDate in fastedDates,
                    onClick = { onLogFast(fastDate) }
                )
            }
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
    isFasted: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(14.dp),
        tone = NimazTone.NEUTRAL
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                type = NimazIconType.CONTAINED,
                containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = iconBgColor,
                containerSize = 44.dp,
                iconSize = 22.dp,
                cornerRadius = 12.dp,
            )

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

            if (isFasted) {
                NimazCheckbox(
                    checked = true,
                    onCheckedChange = null,
                    variant = NimazCheckboxVariant.SUCCESS,
                    size = NimazCheckboxSize.LARGE,
                    type = NimazCheckboxType.CIRCLE,
                    contentDescription = null,
                )
            } else {
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
    }
}

/**
 * The rows under the day's card: where to go next, each reporting rather than describing.
 *
 * Every row does something **different**, which is the constraint that shaped this. Three rows
 * that all opened the day sheet would be one row wearing three hats. So the calendar and the
 * recommended list fold and unfold in place, makeup switches to its tab, and Ramadan — which has
 * no destination on this screen — opens the Islamic calendar, the one place that does own it.
 *
 * Makeup fasts carries a [NimazBadge] rather than putting its count in the subtitle. The count is
 * the reason someone opens that row, and a number in prose is a number you read a sentence to find.
 *
 * The Ramadan row appears **only beyond thirty days**. Inside that window the countdown card at
 * the top of this screen says the same thing far better, and during Ramadan the banner does —
 * a row repeating either would state one fact twice on one screen.
 */
@Composable
private fun FastingGoDeeperGroup(
    fastedThisMonth: Int,
    daysUntilRecommended: Int,
    showRecommendedRow: Boolean,
    pendingMakeup: Int,
    fidyaPaid: Double,
    currency: String,
    isRamadan: Boolean,
    ramadanDay: Int,
    daysUntilRamadan: Int,
    calendarExpanded: Boolean,
    recommendedExpanded: Boolean,
    onToggleCalendar: () -> Unit,
    onToggleRecommended: () -> Unit,
    onOpenMakeup: () -> Unit,
    onOpenRamadanCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.fasting_go_deeper),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        NimazMenuGroup {
            NimazMenuItem(
                title = stringResource(R.string.fasting_row_calendar),
                subtitle = FastingSubtitles.calendar(fastedThisMonth).resolve(),
                icon = Icons.Default.CalendarMonth,
                onClick = onToggleCalendar,
                // A chevron, not an arrow: this reveals a section on this screen rather than
                // going somewhere, and the two should not look the same.
                trailingIcon = if (calendarExpanded) Icons.Default.ExpandLess
                else Icons.Default.ExpandMore,
            )
            if (showRecommendedRow) {
                NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                NimazMenuItem(
                    title = stringResource(R.string.fasting_row_recommended),
                    subtitle = FastingSubtitles.recommended(daysUntilRecommended).resolve(),
                    icon = Icons.Default.EventAvailable,
                    onClick = onToggleRecommended,
                    trailingIcon = if (recommendedExpanded) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                )
            }
            NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
            NimazMenuItem(
                title = stringResource(R.string.fasting_row_makeup),
                subtitle = FastingSubtitles.makeup(
                    pending = pendingMakeup,
                    // Only the paid figure, and only once nothing is outstanding — the mapper
                    // decides which of the two facts the row leads with.
                    fidyaPaid = fidyaPaid.takeIf { it > 0.0 }
                        ?.let { formatCurrency(it, currency) },
                ).resolve(),
                icon = Icons.Default.Restore,
                onClick = onOpenMakeup,
                trailingIcon = null,
                trailing = {
                    if (pendingMakeup > 0) {
                        NimazBadge(
                            text = pendingMakeup.toString(),
                            tone = NimazTone.WARNING,
                            size = NimazBadgeSize.SMALL,
                        )
                    }
                },
            )
            if (!isRamadan && daysUntilRamadan > RamadanCardWindowDays) {
                NimazDivider(modifier = Modifier.padding(start = 56.dp), alpha = 0.5f)
                NimazMenuItem(
                    title = stringResource(R.string.fasting_row_ramadan),
                    subtitle = FastingSubtitles.ramadan(
                        isRamadan = isRamadan,
                        currentDay = ramadanDay,
                        daysUntil = daysUntilRamadan,
                    ).resolve(),
                    icon = Icons.Default.NightsStay,
                    onClick = onOpenRamadanCalendar,
                )
            }
        }
    }
}

/**
 * How close Ramadan has to be before the countdown card takes over from the Go deeper row.
 *
 * Named because it is used in two places that must agree — the card's own gate and the row's —
 * and the failure mode of them disagreeing is either both showing or neither.
 */
private const val RamadanCardWindowDays = 30

/** Opens the day sheet for the selected date. */
@Composable
private fun LogFastButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NimazButton(
        text = stringResource(R.string.fasting_log_a_fast),
        onClick = onClick,
        leadingIcon = Icons.Default.Add,
        fullWidth = true,
        modifier = modifier,
    )
}


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
            NimazLegendItem(color = NimazColors.Success, label = "Fasted")
            NimazLegendItem(color = NimazColors.PrayerColors.Maghrib, label = "Missed")
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

@Composable
private fun GoDeeperShowcase() {
    FastingGoDeeperGroup(
        fastedThisMonth = 18,
        daysUntilRecommended = 4,
        showRecommendedRow = true,
        pendingMakeup = 3,
        fidyaPaid = 24.0,
        currency = "GBP",
        isRamadan = false,
        ramadanDay = 0,
        daysUntilRamadan = 96,
        calendarExpanded = false,
        recommendedExpanded = false,
        onToggleCalendar = {},
        onToggleRecommended = {},
        onOpenMakeup = {},
        onOpenRamadanCalendar = {},
        modifier = Modifier.padding(20.dp),
    )
}

@Preview(showBackground = true, widthDp = 400, name = "Go deeper — Light")
@Composable
private fun FastingGoDeeperLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        GoDeeperShowcase()
    }
}

@Preview(
    showBackground = true, widthDp = 400, name = "Go deeper — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun FastingGoDeeperDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        GoDeeperShowcase()
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
