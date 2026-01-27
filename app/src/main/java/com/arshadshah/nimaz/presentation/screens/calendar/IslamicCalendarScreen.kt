package com.arshadshah.nimaz.presentation.screens.calendar

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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.CalendarEvent
import com.arshadshah.nimaz.presentation.viewmodel.CalendarViewModel
import com.arshadshah.nimaz.presentation.viewmodel.CalendarViewMode
import java.time.LocalDate
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
                title = "Islamic Calendar",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(CalendarEvent.LoadToday) }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Today"
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // View Mode Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.viewMode == mode,
                            onClick = { viewModel.onEvent(CalendarEvent.SetViewMode(mode)) },
                            label = {
                                Text(
                                    when (mode) {
                                        CalendarViewMode.GREGORIAN -> "Gregorian"
                                        CalendarViewMode.HIJRI -> "Hijri"
                                        CalendarViewMode.DUAL -> "Dual"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Month Navigator
            item {
                state.currentMonth?.let { month ->
                    MonthNavigator(
                        monthName = getHijriMonthName(month.hijriMonth),
                        year = month.hijriYear,
                        hijriInfo = null,
                        onPrevious = { viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth) },
                        onNext = { viewModel.onEvent(CalendarEvent.NavigateToNextMonth) }
                    )
                }
            }

            // Calendar Grid
            item {
                state.currentMonth?.let { month ->
                    CalendarGrid(
                        days = month.days,
                        selectedDate = state.selectedDate,
                        onDayClick = { viewModel.onEvent(CalendarEvent.SelectDate(it)) }
                    )
                }
            }

            // Selected Date Info
            item {
                state.selectedHijriDate?.let { hijriDate ->
                    SelectedDateCard(
                        gregorianDate = state.selectedDate,
                        hijriDay = hijriDate.day,
                        hijriMonth = hijriDate.monthName,
                        hijriYear = hijriDate.year
                    )
                }
            }

            // Events for Selected Date
            if (eventsState.eventsForSelectedDate.isNotEmpty()) {
                item {
                    Text(
                        text = "Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(eventsState.eventsForSelectedDate) { event ->
                    EventCard(event = event)
                }
            }

            // Upcoming Events Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upcoming Islamic Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(eventsState.upcomingEvents.take(5)) { event ->
                UpcomingEventCard(event = event)
            }
        }
    }
}

@Composable
private fun MonthNavigator(
    monthName: String,
    year: Int,
    hijriInfo: String?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$monthName $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                hijriInfo?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Primary
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month"
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .padding(8.dp)
        ) {
            // Day Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days
            val firstDayOffset = days.firstOrNull()?.gregorianDate?.dayOfWeek?.value?.mod(7) ?: 0
            val paddedDays = List(firstDayOffset) { null } + days

            paddedDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { day ->
                        if (day != null) {
                            DayCell(
                                day = day,
                                isSelected = day.gregorianDate == selectedDate,
                                onClick = { onDayClick(day.gregorianDate) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    // Pad remaining days in week
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> NimazColors.Primary
        day.isToday -> NimazColors.Primary.copy(alpha = 0.2f)
        day.events.isNotEmpty() -> NimazColors.Secondary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val isWeekend = day.gregorianDate.dayOfWeek.value in listOf(6, 7)
    val textColor = when {
        isSelected -> Color.White
        isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        modifier = modifier.padding(2.dp),
        shape = CircleShape,
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.gregorianDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Text(
                text = day.hijriDate.day.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else NimazColors.Primary
            )

            // Event indicator
            if (day.events.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White else NimazColors.Secondary
                        )
                )
            }
        }
    }
}

@Composable
private fun SelectedDateCard(
    gregorianDate: LocalDate,
    hijriDay: Int,
    hijriMonth: String,
    hijriYear: Int,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = gregorianDate.format(formatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$hijriDay $hijriMonth $hijriYear AH",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NimazColors.Primary
            )
        }
    }
}

@Composable
private fun EventCard(
    event: IslamicEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isHoliday) {
                NimazColors.Secondary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NimazColors.Secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = NimazColors.Secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.nameEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.nameArabic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                event.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (event.isHoliday) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NimazColors.Secondary
                ) {
                    Text(
                        text = "Holiday",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventCard(
    event: IslamicEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.nameEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${event.hijriDay} ${getHijriMonthName(event.hijriMonth)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.Primary
                )
            }

            if (event.isFastingDay) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NimazColors.FastingColors.Voluntary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Fast",
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.FastingColors.Voluntary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getMonthName(month: Int): String {
    return listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )[month - 1]
}

private fun getHijriMonthName(month: Int): String {
    return listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah"
    ).getOrElse(month - 1) { "Unknown" }
}
