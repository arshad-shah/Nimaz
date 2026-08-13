package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBanner
import com.arshadshah.nimaz.presentation.components.atoms.NimazBannerVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazDayRail
import com.arshadshah.nimaz.presentation.components.atoms.NimazDayRailItem
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotSpec
import com.arshadshah.nimaz.presentation.components.atoms.NimazStatusDotStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.molecules.calendar.SelectionStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.tracker.PrayerTrackerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** How far back the review banner looks. One week is a period a user can actually remember. */
private const val REVIEW_WINDOW_DAYS = 7L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTrackerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToQada: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel(),
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    val statsState by viewModel.statsState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val qadaState by viewModel.qadaState.collectAsStateWithLifecycle()

    // Read the clock through the shared ticker rather than calling LocalDateTime.now() directly:
    // a bare now() is not observable state, so the "passed / not recorded" flag would only flip
    // when something *else* happened to recompose this screen -- sit on the tracker as a prayer
    // time arrives and nothing would change. Minute resolution is the granularity of the decision.
    val nowInstant by rememberNow(TickResolution.MINUTES)
    val now = remember(nowInstant) {
        LocalDateTime.ofInstant(
            Instant.ofEpochMilli(nowInstant.toEpochMilliseconds()),
            ZoneId.systemDefault(),
        )
    }
    val today = now.toLocalDate()

    var displayedMonth by remember(state.selectedDate) {
        mutableStateOf(YearMonth.from(state.selectedDate))
    }
    var expandedPrayer by rememberSaveable { mutableStateOf<PrayerName?>(null) }

    LaunchedEffect(displayedMonth) {
        viewModel.onEvent(
            PrayerTrackerEvent.LoadHistory(
                displayedMonth.atDay(1).minusDays(REVIEW_WINDOW_DAYS),
                displayedMonth.atEndOfMonth(),
            )
        )
    }

    val recordsByDate = remember(historyState.records) {
        historyState.records.groupBy { it.date }
    }

    fun statusesOn(date: LocalDate) = resolvePrayerStatuses(
        records = recordsByDate[date.toUtcMidnightMillis()].orEmpty(),
        // The month view has records but not schedules, so a past day resolves from the day
        // being over. Only the selected day gets real times, which is the only day that needs
        // per-prayer precision.
        times = if (date == state.selectedDate) state.prayerTimes else null,
        date = date,
        now = now,
    )

    val unrecordedCount = remember(recordsByDate, now) {
        (1..REVIEW_WINDOW_DAYS).sumOf { back ->
            statusesOn(today.minusDays(back))
                .values.count { it == PrayerDisplayStatus.NOT_RECORDED }
        }
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.prayer_tracker_title),
                onBackClick = onNavigateBack,
                actions = {
                    NimazIconButton(
                        onClick = onNavigateToStats,
                        icon = Icons.Default.BarChart,
                        contentDescription = stringResource(R.string.view_statistics),
                    )
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                WeekRail(
                    selectedDate = state.selectedDate,
                    today = today,
                    statusesOn = ::statusesOn,
                    onSelect = { date ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(date))
                    },
                )
            }

            item {
                PrayerTrackerDayCard(
                    selectedDate = state.selectedDate,
                    statuses = statusesOn(state.selectedDate),
                    times = state.prayerTimes,
                    now = now,
                    streak = statsState.currentStreak,
                    expandedPrayer = expandedPrayer,
                    onExpandedChange = { expandedPrayer = it },
                    onSetStatus = { prayer, status ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SetPrayerStatus(prayer, status))
                    },
                    onBackToToday = {
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(today))
                    },
                )
            }

            if (unrecordedCount > 0) {
                item {
                    NimazBanner(
                        message = stringResource(
                            R.string.prayer_unrecorded_banner_format,
                            unrecordedCount,
                        ),
                        variant = NimazBannerVariant.WARNING,
                        actionLabel = stringResource(R.string.prayer_unrecorded_banner_action),
                        onAction = {
                            viewModel.onEvent(
                                PrayerTrackerEvent.ConfirmUnrecordedAsMissed(
                                    from = today.minusDays(REVIEW_WINDOW_DAYS),
                                    to = today.minusDays(1),
                                )
                            )
                        },
                    )
                }
            }

            item {
                MonthSection(
                    displayedMonth = displayedMonth,
                    selectedDate = state.selectedDate,
                    today = today,
                    statusesOn = ::statusesOn,
                    onMonthChange = { displayedMonth = it },
                    onDateSelected = { date ->
                        expandedPrayer = null
                        viewModel.onEvent(PrayerTrackerEvent.SelectDate(date))
                    },
                )
            }

            item {
                NimazMenuItem(
                    title = stringResource(R.string.qada_prayers),
                    subtitle = if (qadaState.missedPrayers.isEmpty()) {
                        stringResource(R.string.qada_summary_empty)
                    } else {
                        stringResource(R.string.qada_summary_subtitle)
                    },
                    icon = Icons.Default.Restore,
                    onClick = onNavigateToQada,
                    trailing = {
                        if (qadaState.missedPrayers.isNotEmpty()) {
                            NimazBadge(text = qadaState.missedPrayers.size.toString())
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekRail(
    selectedDate: LocalDate,
    today: LocalDate,
    statusesOn: (LocalDate) -> Map<PrayerName, PrayerDisplayStatus>,
    onSelect: (LocalDate) -> Unit,
) {
    val weekStart = remember(selectedDate) {
        selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    }
    val days = remember(weekStart, today) { (0L..6L).map { weekStart.plusDays(it) } }

    NimazDayRail(
        days = days.map { date ->
            val statuses = statusesOn(date)
            NimazDayRailItem(
                weekdayLabel = date.dayOfWeek
                    .getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                dayLabel = date.dayOfMonth.toString(),
                marker = if (date.isAfter(today)) null else statuses.railMarker(),
                isToday = date == today,
                enabled = !date.isAfter(today),
                contentDescription = date.toString(),
            )
        },
        selectedIndex = days.indexOf(selectedDate).takeIf { it >= 0 },
        onSelect = { onSelect(days[it]) },
    )
}

@Composable
private fun MonthSection(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    statusesOn: (LocalDate) -> Map<PrayerName, PrayerDisplayStatus>,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val completeDays = remember(displayedMonth, today) {
        (1..displayedMonth.lengthOfMonth())
            .map(displayedMonth::atDay)
            .filter { !it.isAfter(today) }
            .count { date -> statusesOn(date).values.count { it.isDone() } == TRACKED_PRAYERS.size }
    }
    val noRecordBarColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        NimazSectionHeader(
            title = stringResource(R.string.prayer_month_section),
            trailingText = stringResource(R.string.prayer_complete_days_format, completeDays),
        )

        NimazCalendar(
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            onPreviousMonth = { onMonthChange(displayedMonth.minusMonths(1)) },
            onNextMonth = { onMonthChange(displayedMonth.plusMonths(1)) },
            selectionStyle = SelectionStyle.BORDER,
            dayStateProvider = { date ->
                if (date.isAfter(today)) {
                    CalendarDayState()
                } else {
                    val statuses = statusesOn(date)
                    val done = statuses.values.count { it.isDone() }
                    CalendarDayState(
                        indicatorBar = done.toFloat() / TRACKED_PRAYERS.size,
                        indicatorBarColor = when {
                            done == TRACKED_PRAYERS.size -> NimazColors.StatusColors.Prayed
                            done > 0 -> NimazColors.StatusColors.Partial
                            statuses.values.any { it == PrayerDisplayStatus.MISSED } ->
                                NimazColors.StatusColors.Missed
                            else -> noRecordBarColor
                        },
                    )
                }
            },
        )
    }
}

/** One dot summarising a whole day for the week rail. */
private fun Map<PrayerName, PrayerDisplayStatus>.railMarker(): NimazStatusDotSpec {
    val done = values.count { it.isDone() }
    val allUnrecorded = values.all { it == PrayerDisplayStatus.NOT_RECORDED }
    return when {
        done == TRACKED_PRAYERS.size -> NimazStatusDotSpec(NimazTone.SUCCESS)
        // A day nobody touched gets a ring, not a red dot -- the whole point of the redesign is
        // that "no record" and "you missed these" are different claims.
        allUnrecorded -> NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED)
        done > 0 -> NimazStatusDotSpec(NimazTone.WARNING)
        else -> NimazStatusDotSpec(NimazTone.ERROR)
    }
}
