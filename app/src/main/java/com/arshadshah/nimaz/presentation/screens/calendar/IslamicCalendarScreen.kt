package com.arshadshah.nimaz.presentation.screens.calendar

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.HijriMonth
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.CalendarEvent
import com.arshadshah.nimaz.presentation.viewmodel.CalendarViewModel
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

            // Month Navigation
            item {
                state.currentMonth?.let { month ->
                    CalendarMonthNavigator(
                        monthName = getHijriMonthName(month.hijriMonth),
                        monthNameArabic = getHijriMonthArabicName(month.hijriMonth),
                        year = month.hijriYear,
                        onPrevious = { viewModel.onEvent(CalendarEvent.NavigateToPreviousMonth) },
                        onNext = { viewModel.onEvent(CalendarEvent.NavigateToNextMonth) }
                    )
                }
            }

            // Calendar Grid
            item {
                state.currentMonth?.let { month ->
                    CalendarGridCard(
                        days = month.days,
                        selectedDate = state.selectedDate,
                        onDayClick = { viewModel.onEvent(CalendarEvent.SelectDate(it)) }
                    )
                }
            }

            // Events for Selected Date
            if (eventsState.eventsForSelectedDate.isNotEmpty()) {
                item {
                    Text(
                        text = "Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(eventsState.eventsForSelectedDate) { event ->
                    EventDetailCard(event = event)
                }
            }

            // Upcoming Events Section
            if (eventsState.upcomingEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(eventsState.upcomingEvents.take(5)) { event ->
                    UpcomingEventCard(event = event)
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
    val monthArabic = hijriMonth?.let { getHijriMonthArabicName(it) } ?: ""

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
                    text = "Today",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
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
                    Text(
                        text = "$hijriDay $monthArabic $hijriYear",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- Month Navigator ---

@Composable
private fun CalendarMonthNavigator(
    monthName: String,
    monthNameArabic: String,
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$monthName $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = monthNameArabic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onPrevious,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onNext,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// --- Calendar Grid ---

@Composable
private fun CalendarGridCard(
    days: List<CalendarDay>,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val eidColor = Color(0xFFEAB308)
    val holyColor = Color(0xFF22C55E)
    val fastColor = Color(0xFFA855F7)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Weekday headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar days grid
            val firstDayOffset = days.firstOrNull()?.gregorianDate?.dayOfWeek?.value?.mod(7) ?: 0
            val paddedDays = List(firstDayOffset) { null } + days

            paddedDays.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    week.forEach { day ->
                        if (day != null) {
                            CalendarDayCell(
                                day = day,
                                isSelected = day.gregorianDate == selectedDate,
                                onClick = { onDayClick(day.gregorianDate) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Legend
            HorizontalDivider(
                modifier = Modifier.padding(top = 15.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = eidColor, label = "Eid")
                Spacer(modifier = Modifier.width(20.dp))
                LegendItem(color = holyColor, label = "Holy Night")
                Spacer(modifier = Modifier.width(20.dp))
                LegendItem(color = fastColor, label = "Fasting")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eidColor = Color(0xFFEAB308)
    val holyColor = Color(0xFF22C55E)
    val fastColor = Color(0xFFA855F7)

    val backgroundColor = when {
        day.isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> Color.Transparent
    }

    val textColor = when {
        day.isToday -> MaterialTheme.colorScheme.onPrimary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.gregorianDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                fontSize = 13.sp
            )
        }

        // Event dot indicator at bottom
        if (day.events.isNotEmpty()) {
            val dotColor = getEventDotColor(day.events, eidColor, holyColor, fastColor)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun getEventDotColor(
    events: List<IslamicEvent>,
    eidColor: Color,
    holyColor: Color,
    fastColor: Color
): Color {
    val primaryEvent = events.firstOrNull() ?: return MaterialTheme.colorScheme.primary
    return when (primaryEvent.eventType) {
        IslamicEventType.HOLIDAY -> eidColor
        IslamicEventType.NIGHT -> holyColor
        IslamicEventType.FAST -> fastColor
        IslamicEventType.HISTORICAL -> holyColor
    }
}

// --- Event Detail Card (for selected date events) ---

@Composable
private fun EventDetailCard(
    event: IslamicEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = event.hijriDay.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getHijriMonthName(event.hijriMonth).take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.nameEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                event.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                EventBadge(event = event)
            }
        }
    }
}

// --- Upcoming Event Card ---

@Composable
private fun UpcomingEventCard(
    event: IslamicEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Date column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = event.hijriDay.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getHijriMonthName(event.hijriMonth).take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.nameEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                event.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                EventBadge(event = event)
            }
        }
    }
}

// --- Event Badge ---

@Composable
private fun EventBadge(event: IslamicEvent) {
    val eidColor = Color(0xFFEAB308)
    val holyColor = Color(0xFF22C55E)
    val fastColor = Color(0xFFA855F7)

    val badgeColor: Color
    val badgeLabel: String

    when (event.eventType) {
        IslamicEventType.HOLIDAY -> {
            badgeColor = eidColor
            badgeLabel = "Eid"
        }
        IslamicEventType.NIGHT -> {
            badgeColor = holyColor
            badgeLabel = "Holy Night"
        }
        IslamicEventType.FAST -> {
            badgeColor = fastColor
            badgeLabel = if (event.isFastingDay) "Fasting" else "Fasting Month"
        }
        IslamicEventType.HISTORICAL -> {
            badgeColor = holyColor
            badgeLabel = "Historical"
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp
        )
    }
}

// --- Helper functions ---

private fun getHijriMonthName(month: Int): String {
    return HijriMonth.fromNumber(month)?.displayName() ?: "Unknown"
}

@Composable
private fun getHijriMonthArabicName(month: Int): String {
    return HijriMonth.fromNumber(month)?.arabicName() ?: ""
}
