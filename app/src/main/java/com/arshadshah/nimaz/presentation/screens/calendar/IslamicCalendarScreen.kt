package com.arshadshah.nimaz.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator.getHijriMonthName
import com.arshadshah.nimaz.core.util.HijriDateCalculator.getHijriMonthNameArabic
import com.arshadshah.nimaz.domain.model.HijriMonth
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.CalendarDayState
import com.arshadshah.nimaz.presentation.components.molecules.CalendarLegendItem
import com.arshadshah.nimaz.presentation.components.molecules.IslamicEventCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.CalendarEvent
import com.arshadshah.nimaz.presentation.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.calendarState.collectAsState()
    val eventsState by viewModel.eventsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.islamic_calendar),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.LoadToday) }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.today)
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today Card - gradient hero
            item {
                TodayHeroCard(
                    selectedDate = state.selectedDate,
                    hijriDay = state.selectedHijriDate?.day,
                    hijriMonth = state.selectedHijriDate?.month,
                    hijriYear = state.selectedHijriDate?.year
                )
            }

            // Calendar
            item {
                state.currentMonth?.let { month ->
                    val eventMap = remember(month.days) {
                        month.days.associate { day -> day.gregorianDate to day.events }
                    }

                    NimazCalendar(
                        displayedMonth = YearMonth.from(state.selectedDate),
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
                            CalendarDayState(
                                indicatorColor = getEventDotColor(events)
                            )
                        },
                        legendItems = listOf(
                            CalendarLegendItem(Color(0xFFEAB308), stringResource(R.string.eid)),
                            CalendarLegendItem(Color(0xFF22C55E), stringResource(R.string.holy_night)),
                            CalendarLegendItem(Color(0xFFA855F7), stringResource(R.string.fasting))
                        )
                    )
                }
            }

            // Events for Selected Date
            if (eventsState.eventsForSelectedDate.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(eventsState.eventsForSelectedDate) { event ->
                    IslamicEventCard(event = event)
                }
            }

            // Upcoming Events Section
            if (eventsState.upcomingEvents.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.upcoming_events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(eventsState.upcomingEvents.take(5)) { event ->
                    IslamicEventCard(event = event)
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// --- Today Hero Card ---

@Composable
private fun TodayHeroCard(
    selectedDate: LocalDate,
    hijriDay: Int?,
    hijriMonth: Int?,
    hijriYear: Int?,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
    val monthName = hijriMonth?.let { getHijriMonthName(it) } ?: ""
    val monthArabic = hijriMonth?.let { getHijriMonthNameArabic(it) } ?: ""

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(25.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (hijriDay != null && hijriYear != null) "$hijriDay $monthName $hijriYear" else "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (monthArabic.isNotEmpty()) {
                    ArabicText(
                        text = "$hijriDay $monthArabic $hijriYear",
                        size = ArabicTextSize.SMALL,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// --- Helper functions ---

private fun getEventDotColor(events: List<IslamicEvent>): Color? {
    val primaryEvent = events.firstOrNull() ?: return null
    return when (primaryEvent.eventType) {
        IslamicEventType.HOLIDAY -> Color(0xFFEAB308)
        IslamicEventType.NIGHT -> Color(0xFF22C55E)
        IslamicEventType.FAST -> Color(0xFFA855F7)
        IslamicEventType.HISTORICAL -> Color(0xFF22C55E)
    }
}