package com.arshadshah.nimaz.presentation.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator.getHijriMonthName
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator.getHijriMonthNameArabic
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.IslamicEventCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorState
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorVariant
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarDayState
import com.arshadshah.nimaz.presentation.foundation.calendar.CalendarLegendItem
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarEvent
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarUiState
import com.arshadshah.nimaz.presentation.viewmodel.calendar.CalendarViewModel
import com.arshadshah.nimaz.presentation.viewmodel.calendar.EventsUiState
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.calendarState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.islamic_calendar),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.LoadToday) }) {
                        NimazIcon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.today)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = currentWindowSizeClass()

        if (windowSizeClass.isCompact) {
            CalendarCompactContent(
                state = state,
                eventsState = eventsState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            CalendarTabletContent(
                state = state,
                eventsState = eventsState,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun CalendarCompactContent(
    state: CalendarUiState,
    eventsState: EventsUiState,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalendarSection(state = state, viewModel = viewModel)
        }
        if (eventsState.eventsForSelectedDate.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.events),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(eventsState.eventsForSelectedDate, key = { it.id }) { event ->
                IslamicEventCard(event = event)
            }
        }
        if (eventsState.upcomingEvents.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.upcoming_events),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(eventsState.upcomingEvents.take(5), key = { it.id }) { event ->
                IslamicEventCard(event = event)
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CalendarTabletContent(
    state: CalendarUiState,
    eventsState: EventsUiState,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left: Hero card + Calendar
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CalendarSection(state = state, viewModel = viewModel)
        }

        // Right: Events
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (eventsState.eventsForSelectedDate.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(eventsState.eventsForSelectedDate, key = { it.id }) { event ->
                    IslamicEventCard(event = event)
                }
            }
            if (eventsState.upcomingEvents.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.upcoming_events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(eventsState.upcomingEvents.take(5), key = { it.id }) { event ->
                    IslamicEventCard(event = event)
                }
            }
            if (eventsState.eventsForSelectedDate.isEmpty() && eventsState.upcomingEvents.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CalendarSection(
    state: CalendarUiState,
    viewModel: CalendarViewModel
) {
    // The grid draws with or without events, so a content-database fault costs the event
    // markers and nothing else. Before, it cost the whole screen: `loadToday()` ran inside
    // the events `try`, so a throw left `currentMonth` null and this rendered nothing at all.
    state.error?.let { error ->
        // SECTION, not a screen: the grid is still correct and still useful without its
        // event markers, so this sits above it rather than in place of it.
        NimazErrorState(
            title = stringResource(error.message),
            kind = error.kind,
            details = error.details,
            variant = NimazErrorVariant.SECTION,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }

    state.currentMonth?.let { month ->
        val eventMap = remember(month.days) {
            month.days.associate { day -> day.gregorianDate to day.events }
        }

        NimazCalendar(
            // Both the grid and the header title come from `month`, so paging
            // can never move one without the other. Deriving the grid from
            // `selectedDate` here is what left it stuck on the current month.
            displayedMonth = month.displayedMonth ?: YearMonth.from(state.selectedDate),
            selectedDate = state.selectedDate,
            onDateSelected = { viewModel.onEvent(CalendarEvent.SelectDate(it)) },
            onPreviousMonth = { viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth) },
            onNextMonth = { viewModel.onEvent(CalendarEvent.NavigateToNextMonth) },
            headerTitle = "${getHijriMonthName(month.hijriMonth)} ${month.hijriYear}",
            headerSubtitle = {
                ArabicText(
                    text = getHijriMonthNameArabic(month.hijriMonth),
                    size = ArabicTextSize.SMALL,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            },
            dayStateProvider = { date ->
                val events = eventMap[date] ?: emptyList()
                // Dual-date overlay: every cell carries its Hijri day in the
                // top-end corner. The first day of a Hijri month is emphasized
                // (accent + bold) — the layout-stable month-start marker that
                // replaced the old top-stripe + short-name label.
                val hijri = HijriDateCalculator.toHijri(date)
                CalendarDayState(
                    indicatorColor = getEventDotColor(events),
                    secondaryLabel = hijri.day.toString(),
                    emphasizeSecondary = hijri.day == 1
                )
            },
            legendItems = listOf(
                CalendarLegendItem(
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.month_start)
                ),
                CalendarLegendItem(NimazColors.Gold500, stringResource(R.string.eid)),
                CalendarLegendItem(NimazColors.Success, stringResource(R.string.holy_night)),
                CalendarLegendItem(NimazColors.Purple, stringResource(R.string.fasting))
            )
        )
    }
}

// --- Helper functions ---

private fun getEventDotColor(events: List<IslamicEvent>): Color? {
    val primaryEvent = events.firstOrNull() ?: return null
    return when (primaryEvent.eventType) {
        IslamicEventType.HOLIDAY -> NimazColors.Gold500
        IslamicEventType.NIGHT -> NimazColors.Success
        IslamicEventType.FAST -> NimazColors.Purple
        IslamicEventType.HISTORICAL -> NimazColors.Success
    }
}