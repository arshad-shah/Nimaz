package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.CalendarDay
import com.arshadshah.nimaz.domain.model.HijriMonth
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.presentation.components.molecules.CalendarDayCell
import com.arshadshah.nimaz.presentation.components.molecules.IslamicEventCard
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.domain.model.HijriDate
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Complete Islamic calendar grid with month navigation.
 */
@Composable
fun CalendarGrid(
    days: List<CalendarDay>,
    currentMonth: Int,
    currentYear: Int,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelect: (CalendarDay) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onTodayClick: () -> Unit = {}
) {
    var animationDirection by remember { mutableStateOf(1) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Month navigation header
        CalendarHeader(
            hijriMonth = currentMonth,
            hijriYear = currentYear,
            onPreviousMonth = {
                animationDirection = -1
                onPreviousMonth()
            },
            onNextMonth = {
                animationDirection = 1
                onNextMonth()
            },
            onTodayClick = onTodayClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Day of week headers
        DayOfWeekHeader()

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid with animation
        AnimatedContent(
            targetState = days,
            transitionSpec = {
                (slideInHorizontally { width -> width * animationDirection } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width * animationDirection } + fadeOut())
            },
            label = "calendar_animation"
        ) { targetDays ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(targetDays) { day ->
                    CalendarDayCell(
                        gregorianDay = day.gregorianDate.dayOfMonth,
                        hijriDay = day.hijriDate.day,
                        isToday = day.isToday,
                        isSelected = selectedDate == day.gregorianDate,
                        isCurrentMonth = day.isCurrentMonth,
                        hasEvent = day.events.isNotEmpty(),
                        isHoliday = day.events.any { it.isHoliday },
                        isFastingDay = day.events.any { it.isFastingDay },
                        onClick = { onDateSelect(day) },
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }
        }
    }
}

/**
 * Calendar header with month/year and navigation.
 */
@Composable
private fun CalendarHeader(
    hijriMonth: Int,
    hijriYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = HijriMonth.fromNumber(hijriMonth)?.displayName() ?: "Unknown"
    val monthNameArabic = HijriMonth.fromNumber(hijriMonth)?.arabicName() ?: ""

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onTodayClick)
        ) {
            Text(
                text = monthNameArabic,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$monthName $hijriYear AH",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Day of week header row.
 */
@Composable
private fun DayOfWeekHeader(
    modifier: Modifier = Modifier,
    startFromSunday: Boolean = true
) {
    val daysOfWeek = if (startFromSunday) {
        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (day == "Fri") {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Calendar with events list below.
 */
@Composable
fun CalendarWithEvents(
    days: List<CalendarDay>,
    events: List<IslamicEvent>,
    currentMonth: Int,
    currentYear: Int,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelect: (CalendarDay) -> Unit = {},
    onEventClick: (IslamicEvent) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Calendar
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                CalendarGrid(
                    days = days,
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    selectedDate = selectedDate,
                    onDateSelect = onDateSelect,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Events section
        if (events.isNotEmpty()) {
            item {
                Text(
                    text = "Events This Month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(events) { event ->
                IslamicEventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

/**
 * Compact calendar view for widgets or inline displays.
 */
@Composable
fun CompactCalendarGrid(
    days: List<CalendarDay>,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDateSelect: (CalendarDay) -> Unit = {}
) {
    Column(modifier = modifier) {
        // Mini day of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Mini calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(days) { day ->
                CompactCalendarDayCell(
                    day = day.gregorianDate.dayOfMonth,
                    isToday = day.isToday,
                    isSelected = selectedDate == day.gregorianDate,
                    isCurrentMonth = day.isCurrentMonth,
                    hasEvent = day.events.isNotEmpty(),
                    onClick = { onDateSelect(day) },
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactCalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasEvent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (hasEvent) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )
            }
        }
    }
}

/**
 * Horizontal scrolling events strip.
 */
@Composable
fun EventsStrip(
    events: List<IslamicEvent>,
    modifier: Modifier = Modifier,
    onEventClick: (IslamicEvent) -> Unit = {}
) {
    if (events.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Upcoming Events",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(events) { event ->
                CompactEventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun CompactEventCard(
    event: IslamicEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventColor = when (event.eventType) {
        IslamicEventType.HOLIDAY -> MaterialTheme.colorScheme.secondary
        IslamicEventType.FAST -> NimazColors.FastingColors.Fasted
        IslamicEventType.NIGHT -> MaterialTheme.colorScheme.tertiary
        IslamicEventType.HISTORICAL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = eventColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(eventColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${event.hijriDay} ${HijriMonth.fromNumber(event.hijriMonth)?.displayName()?.take(3) ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = eventColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = event.nameEnglish,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (event.isHoliday) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Holiday",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Year overview with all 12 months.
 */
@Composable
fun YearOverviewGrid(
    year: Int,
    monthsData: List<Pair<Int, List<CalendarDay>>>,
    modifier: Modifier = Modifier,
    onMonthClick: (Int) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(monthsData) { (month, days) ->
            MiniMonthCard(
                month = month,
                year = year,
                days = days,
                onClick = { onMonthClick(month) }
            )
        }
    }
}

@Composable
private fun MiniMonthCard(
    month: Int,
    year: Int,
    days: List<CalendarDay>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = HijriMonth.fromNumber(month)?.displayName() ?: "Unknown"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini calendar representation
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                val weeks = days.chunked(7)
                weeks.take(5).forEach { week ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            day.isToday -> MaterialTheme.colorScheme.primary
                                            day.events.isNotEmpty() -> MaterialTheme.colorScheme.secondary
                                            day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> Color.Transparent
                                        }
                                    )
                            )
                        }
                        // Fill remaining spaces if week is incomplete
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "EventsStrip")
@Composable
private fun EventsStripPreview() {
    val today = LocalDate.now()
    val sampleEvents = listOf(
        IslamicEvent(
            id = "1",
            nameArabic = "\u0639\u064a\u062f \u0627\u0644\u0641\u0637\u0631",
            nameEnglish = "Eid al-Fitr",
            description = "Festival of Breaking the Fast",
            hijriMonth = 10,
            hijriDay = 1,
            eventType = IslamicEventType.HOLIDAY,
            isHoliday = true,
            isFastingDay = false,
            isNightOfPower = false,
            gregorianDate = today,
            year = 1446,
            notes = null,
            priority = 1
        ),
        IslamicEvent(
            id = "2",
            nameArabic = "\u0644\u064a\u0644\u0629 \u0627\u0644\u0642\u062f\u0631",
            nameEnglish = "Laylat al-Qadr",
            description = "Night of Power",
            hijriMonth = 9,
            hijriDay = 27,
            eventType = IslamicEventType.NIGHT,
            isHoliday = false,
            isFastingDay = false,
            isNightOfPower = true,
            gregorianDate = today.plusDays(5),
            year = 1446,
            notes = null,
            priority = 1
        ),
        IslamicEvent(
            id = "3",
            nameArabic = "\u064a\u0648\u0645 \u0639\u0631\u0641\u0629",
            nameEnglish = "Day of Arafah",
            description = "Recommended fasting day",
            hijriMonth = 12,
            hijriDay = 9,
            eventType = IslamicEventType.FAST,
            isHoliday = false,
            isFastingDay = true,
            isNightOfPower = false,
            gregorianDate = today.plusDays(10),
            year = 1446,
            notes = null,
            priority = 2
        )
    )
    NimazTheme {
        EventsStrip(events = sampleEvents)
    }
}

@Preview(showBackground = true, name = "CalendarGrid")
@Composable
private fun CalendarGridPreview() {
    val today = LocalDate.now()
    val sampleDays = (0 until 30).map { offset ->
        val date = today.withDayOfMonth(1).plusDays(offset.toLong())
        CalendarDay(
            gregorianDate = date,
            hijriDate = HijriDate(day = offset + 1, month = 7, year = 1446),
            events = if (offset == 14) listOf(
                IslamicEvent(
                    id = "1",
                    nameArabic = "\u0639\u064a\u062f",
                    nameEnglish = "Sample Event",
                    description = null,
                    hijriMonth = 7,
                    hijriDay = offset + 1,
                    eventType = IslamicEventType.HOLIDAY,
                    isHoliday = true,
                    isFastingDay = false,
                    isNightOfPower = false,
                    gregorianDate = date,
                    year = 1446,
                    notes = null,
                    priority = 1
                )
            ) else emptyList(),
            isToday = date == today,
            isCurrentMonth = true
        )
    }
    NimazTheme {
        CalendarGrid(
            days = sampleDays,
            currentMonth = 7,
            currentYear = 1446,
            modifier = Modifier.padding(16.dp)
        )
    }
}
