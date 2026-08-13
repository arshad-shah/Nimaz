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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.formatFullDate
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
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
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuGroup
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.calendar.CalendarLegendItem
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

    // Spans both the displayed month AND the trailing review window, always -- not just the
    // month's own window. Loading only the displayed month meant paging to a month that didn't
    // include "today" left the review banner and the week rail resolving the last seven real
    // days from an empty record set: every one of them read NOT_RECORDED, so the banner announced
    // a fabricated count and the rail painted warning rings on days the user actually logged.
    LaunchedEffect(displayedMonth, today) {
        val start = minOf(
            displayedMonth.atDay(1).minusDays(REVIEW_WINDOW_DAYS),
            today.minusDays(REVIEW_WINDOW_DAYS),
        )
        val end = maxOf(displayedMonth.atEndOfMonth(), today)
        viewModel.onEvent(PrayerTrackerEvent.LoadHistory(start, end))
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
                DayRail(
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
                    // The row stays open. Closing it on the tap that set the status hid the one
                    // piece of feedback the picker gives -- the chosen segment taking its colour --
                    // and on a tap-to-clear it slammed shut over the note explaining what
                    // "not recorded" means. The header is how a row closes.
                    onSetStatus = { prayer, status ->
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
                        message = pluralStringResource(
                            R.plurals.prayer_unrecorded_banner,
                            unrecordedCount,
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
                // Carded like every other section on this screen, rather than sitting flat on
                // the page background -- NimazMenuGroup is the same FILLED, rounded NimazCard
                // the day card and the month section already use.
                NimazMenuGroup {
                    NimazMenuItem(
                        title = stringResource(R.string.qada_prayers),
                        subtitle = if (qadaState.missedPrayers.isEmpty()) {
                            stringResource(R.string.qada_summary_empty)
                        } else {
                            stringResource(R.string.qada_summary_subtitle)
                        },
                        icon = Icons.Default.Restore,
                        // Qada is purple everywhere else in the tracker (the picker, the
                        // timeline dot) -- NimazColors.StatusColors.Qada, not an invented tone.
                        iconTint = NimazColors.StatusColors.Qada,
                        onClick = onNavigateToQada,
                        trailing = {
                            if (qadaState.missedPrayers.isNotEmpty()) {
                                // A filled purple count badge, not the default neutral/outlined
                                // one -- NimazBadgeDefaults.feature is the same escape hatch
                                // StatusBadge uses for this exact colour.
                                NimazBadge(
                                    text = qadaState.missedPrayers.size.toString(),
                                    colors = NimazBadgeDefaults.feature(
                                        color = NimazColors.StatusColors.Qada,
                                        emphasis = NimazBadgeEmphasis.FILLED,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** How many days either side of the selected date the rail shows. Seven cells total. */
private const val RAIL_HALF_SPAN = 3L

/**
 * A day's overall bucket, for both the rail's dot and its screen-reader description -- computed
 * once so the two can never say different things about the same day.
 */
internal enum class DayBucket {
    ALL_DONE,
    ALL_UNRECORDED,
    PARTIAL,
    HAS_MISSED,
}

/**
 * Classifies a day from what is actually asserted about it, not from whether every slot happens
 * to be [PrayerDisplayStatus.NOT_RECORDED]. [PrayerDisplayStatus.UPCOMING] entries (later prayers
 * on today, which the day hasn't reached yet) must never push a day into [DayBucket.HAS_MISSED]:
 * a red dot is earned only by an actual [PrayerDisplayStatus.MISSED] assertion, never by the mere
 * absence of a done one. A day whose unfulfilled prayers are all `NOT_RECORDED`/`UPCOMING`, with
 * nothing asserted as missed, is [DayBucket.ALL_UNRECORDED] (a ring) -- the whole point of the
 * redesign is that "no record" and "you missed these" are different claims.
 */
internal fun Map<PrayerName, PrayerDisplayStatus>.bucket(): DayBucket {
    val done = values.count { it.isDone() }
    val hasMissed = values.any { it == PrayerDisplayStatus.MISSED }
    return when {
        done == TRACKED_PRAYERS.size -> DayBucket.ALL_DONE
        done > 0 -> DayBucket.PARTIAL
        hasMissed -> DayBucket.HAS_MISSED
        else -> DayBucket.ALL_UNRECORDED
    }
}

private fun DayBucket.dotSpec(): NimazStatusDotSpec = when (this) {
    DayBucket.ALL_DONE -> NimazStatusDotSpec(NimazTone.SUCCESS)
    DayBucket.ALL_UNRECORDED -> NimazStatusDotSpec(NimazTone.WARNING, NimazStatusDotStyle.OUTLINED)
    DayBucket.PARTIAL -> NimazStatusDotSpec(NimazTone.WARNING)
    DayBucket.HAS_MISSED -> NimazStatusDotSpec(NimazTone.ERROR)
}

@Composable
private fun DayRail(
    selectedDate: LocalDate,
    today: LocalDate,
    statusesOn: (LocalDate) -> Map<PrayerName, PrayerDisplayStatus>,
    onSelect: (LocalDate) -> Unit,
) {
    // Centred on the selected date, not anchored to an ISO week -- the month grid below starts
    // its weeks on Sunday, so a Monday-anchored rail put the same seven days in two different
    // weeks on one screen. Centring also means selecting near either end of a calendar week no
    // longer buries the selection under a run of greyed-out future cells.
    val days = remember(selectedDate) {
        (-RAIL_HALF_SPAN..RAIL_HALF_SPAN).map { selectedDate.plusDays(it) }
    }
    // Not Locale.getDefault() -- that reads a JVM-global, not composition state, so the label
    // wouldn't recompose if the user changed the app's language without restarting the process.
    val locale = LocalLocale.current.platformLocale

    NimazDayRail(
        days = days.map { date ->
            val isFuture = date.isAfter(today)
            val bucket = if (isFuture) null else statusesOn(date).bucket()
            val localizedDate = date.formatFullDate()
            NimazDayRailItem(
                weekdayLabel = date.dayOfWeek
                    .getDisplayName(TextStyle.NARROW, locale),
                dayLabel = date.dayOfMonth.toString(),
                marker = bucket?.dotSpec(),
                isToday = date == today,
                enabled = !isFuture,
                // A localized date plus the day's overall state, e.g. "August 13, 2026, prayed" --
                // TalkBack's date-only fallback said "2026-08-13" and nothing about the marker.
                contentDescription = when (bucket) {
                    DayBucket.ALL_DONE -> stringResource(R.string.a11y_prayer_state_prayed, localizedDate)
                    DayBucket.ALL_UNRECORDED ->
                        stringResource(R.string.a11y_prayer_state_not_recorded, localizedDate)
                    DayBucket.PARTIAL -> stringResource(R.string.a11y_prayer_state_partial, localizedDate)
                    DayBucket.HAS_MISSED -> stringResource(R.string.a11y_prayer_state_missed, localizedDate)
                    null -> "$localizedDate, ${stringResource(R.string.upcoming)}"
                },
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
    // Not `remember`ed: it reads `statusesOn`, a fresh lambda closure every recomposition that
    // isn't itself a valid `remember` key, so keying on `(displayedMonth, today)` alone let the
    // count freeze at "0 complete days" for the session (computed before the history load
    // returned) and then read the *previous* month's records after paging. Thirty-odd iterations
    // over already-loaded, already-grouped records is cheap enough to just run every time.
    val completeDays = (1..displayedMonth.lengthOfMonth())
        .map(displayedMonth::atDay)
        .filter { !it.isAfter(today) }
        .count { date -> statusesOn(date).values.count { it.isDone() } == TRACKED_PRAYERS.size }

    val noRecordBarColor = MaterialTheme.colorScheme.outlineVariant
    val notRecordedRingColor = NimazColors.StatusColors.Pending

    Column {
        NimazSectionHeader(
            title = stringResource(R.string.prayer_month_section),
            trailingText = pluralStringResource(
                R.plurals.prayer_complete_days,
                completeDays,
                completeDays,
            ),
        )

        NimazCalendar(
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            onPreviousMonth = { onMonthChange(displayedMonth.minusMonths(1)) },
            onNextMonth = { onMonthChange(displayedMonth.plusMonths(1)) },
            selectionStyle = SelectionStyle.BORDER,
            legendItems = monthLegend(),
            dayStateProvider = { date ->
                if (date.isAfter(today)) {
                    CalendarDayState()
                } else {
                    val statuses = statusesOn(date)
                    val done = statuses.values.count { it.isDone() }
                    // Shared with the rail's bucket() so the calendar ring and the rail dot can
                    // never disagree about the same day -- see bucket()'s doc comment for why
                    // UPCOMING must not be conflated with a genuine MISSED assertion.
                    val bucket = statuses.bucket()
                    CalendarDayState(
                        indicatorBar = done.toFloat() / TRACKED_PRAYERS.size,
                        indicatorBarColor = when (bucket) {
                            DayBucket.ALL_DONE -> NimazColors.StatusColors.Prayed
                            DayBucket.PARTIAL -> NimazColors.StatusColors.Partial
                            DayBucket.HAS_MISSED -> NimazColors.StatusColors.Missed
                            DayBucket.ALL_UNRECORDED -> noRecordBarColor
                        },
                        // The bar alone drew nothing distinctive for a day nobody touched: zero
                        // done means a zero-length bar whichever colour it's given. The dot
                        // channel is independent of the bar (CalendarDayState's own contract), so
                        // a genuinely untouched day also gets a ring here -- the same signal the
                        // rail's ALL_UNRECORDED bucket and the day card's NOT_RECORDED rows use.
                        indicatorColor = if (bucket == DayBucket.ALL_UNRECORDED) notRecordedRingColor else null,
                        indicatorStyle = NimazStatusDotStyle.OUTLINED,
                    )
                }
            },
        )
    }
}

@Composable
private fun monthLegend(): List<CalendarLegendItem> = listOf(
    CalendarLegendItem(
        color = NimazColors.StatusColors.Prayed,
        label = stringResource(R.string.prayer_legend_all_five),
    ),
    CalendarLegendItem(
        color = NimazColors.StatusColors.Partial,
        label = stringResource(R.string.prayer_legend_some),
    ),
    CalendarLegendItem(
        color = NimazColors.StatusColors.Missed,
        label = stringResource(R.string.prayer_legend_none),
    ),
    CalendarLegendItem(
        color = NimazColors.StatusColors.Pending,
        label = stringResource(R.string.prayer_legend_not_recorded),
        indicatorStyle = NimazStatusDotStyle.OUTLINED,
    ),
)
