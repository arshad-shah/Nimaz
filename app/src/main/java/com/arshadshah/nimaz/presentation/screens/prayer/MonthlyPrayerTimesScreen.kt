package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.DayPrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.MonthlyPrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.MonthlyPrayerTimesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPrayerTimesScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyPrayerTimesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Monthly Prayer Times",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                horizontal = NimazSpacing.Large,
                vertical = NimazSpacing.Small
            ),
            verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
        ) {
            // Month navigation card with location
            item {
                MonthNavigationCard(
                    monthYear = state.currentMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy")
                    ),
                    locationName = state.locationName,
                    onPrevious = { viewModel.onEvent(MonthlyPrayerTimesEvent.PreviousMonth) },
                    onNext = { viewModel.onEvent(MonthlyPrayerTimesEvent.NextMonth) }
                )
            }

            // Day cards
            val today = LocalDate.now()
            items(state.dayPrayerTimes, key = { it.date.toEpochDay() }) { dayTimes ->
                val isToday = dayTimes.date == today
                val isExpanded = dayTimes.date == state.expandedDay

                DayPrayerCard(
                    dayTimes = dayTimes,
                    isToday = isToday,
                    isExpanded = isExpanded,
                    onClick = {
                        viewModel.onEvent(
                            MonthlyPrayerTimesEvent.ToggleDayExpanded(dayTimes.date)
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(NimazSpacing.Large))
            }
        }
    }
}

@Composable
private fun MonthNavigationCard(
    monthYear: String,
    locationName: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    NimazCard(
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(NimazCornerRadius.Large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(NimazSpacing.Medium)
        ) {
            // Location row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = NimazSpacing.ExtraSmall,
                    bottom = NimazSpacing.Small
                )
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(NimazSpacing.ExtraSmall))
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Month navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = monthYear,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onNext,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private data class PrayerTimeEntry(
    val name: String,
    val time: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPrayerCard(
    dayTimes: DayPrayerTimes,
    isToday: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val prayers = listOf(
        PrayerTimeEntry("Fajr", dayTimes.fajr, NimazColors.PrayerColors.Fajr),
        PrayerTimeEntry("Sunrise", dayTimes.sunrise, NimazColors.PrayerColors.Sunrise),
        PrayerTimeEntry("Dhuhr", dayTimes.dhuhr, NimazColors.PrayerColors.Dhuhr),
        PrayerTimeEntry("Asr", dayTimes.asr, NimazColors.PrayerColors.Asr),
        PrayerTimeEntry("Maghrib", dayTimes.maghrib, NimazColors.PrayerColors.Maghrib),
        PrayerTimeEntry("Isha", dayTimes.isha, NimazColors.PrayerColors.Isha)
    )

    val dayOfWeek = dayTimes.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayNumber = dayTimes.date.dayOfMonth.toString()

    if (isToday) {
        TodayPrayerCard(
            dayNumber = dayNumber,
            dayOfWeek = dayOfWeek,
            prayers = prayers,
            isExpanded = isExpanded,
            onClick = onClick
        )
    } else {
        RegularDayPrayerCard(
            dayNumber = dayNumber,
            dayOfWeek = dayOfWeek,
            prayers = prayers,
            isExpanded = isExpanded,
            onClick = onClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayPrayerCard(
    dayNumber: String,
    dayOfWeek: String,
    prayers: List<PrayerTimeEntry>,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NimazCornerRadius.Large),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NimazCornerRadius.Large))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Compact row: day badge + top 3 prayers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = NimazSpacing.Medium,
                            vertical = NimazSpacing.Medium
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
                ) {
                    // Day badge
                    DayBadge(
                        dayNumber = dayNumber,
                        dayOfWeek = dayOfWeek,
                        isToday = true
                    )

                    // Top 3 prayer times in compact row
                    CompactPrayerTimes(
                        prayers = prayers,
                        isToday = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Expanded: full prayer grid
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ExpandedPrayerGrid(
                        prayers = prayers,
                        isToday = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularDayPrayerCard(
    dayNumber: String,
    dayOfWeek: String,
    prayers: List<PrayerTimeEntry>,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(NimazCornerRadius.Medium),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Compact row: day badge + top 3 prayers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = NimazSpacing.Medium,
                        vertical = NimazSpacing.Small
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
            ) {
                // Day badge
                DayBadge(
                    dayNumber = dayNumber,
                    dayOfWeek = dayOfWeek,
                    isToday = false
                )

                // Top 3 prayer times in compact row
                CompactPrayerTimes(
                    prayers = prayers,
                    isToday = false,
                    modifier = Modifier.weight(1f)
                )
            }

            // Expanded: full prayer grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExpandedPrayerGrid(
                    prayers = prayers,
                    isToday = false
                )
            }
        }
    }
}

@Composable
private fun DayBadge(
    dayNumber: String,
    dayOfWeek: String,
    isToday: Boolean
) {
    val bgColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(44.dp)
            .clip(RoundedCornerShape(NimazCornerRadius.Medium))
            .background(bgColor)
            .padding(vertical = NimazSpacing.Small, horizontal = NimazSpacing.ExtraSmall)
    ) {
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CompactPrayerTimes(
    prayers: List<PrayerTimeEntry>,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    // Show Fajr, Dhuhr, Maghrib as the 3 key times
    val keyPrayers = listOf(prayers[0], prayers[2], prayers[4]) // Fajr, Dhuhr, Maghrib

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        keyPrayers.forEach { prayer ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NimazSpacing.ExtraSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(prayer.color)
                    )
                    Text(
                        text = prayer.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = prayer.time,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ExpandedPrayerGrid(
    prayers: List<PrayerTimeEntry>,
    isToday: Boolean
) {
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val dividerColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = NimazSpacing.Medium,
                end = NimazSpacing.Medium,
                bottom = NimazSpacing.Medium
            )
    ) {
        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        Spacer(modifier = Modifier.height(NimazSpacing.Medium))

        // 2-column grid of all 6 prayer times
        for (rowIndex in 0..2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = NimazSpacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
            ) {
                val leftPrayer = prayers[rowIndex * 2]
                val rightPrayer = prayers[rowIndex * 2 + 1]

                PrayerTimeItem(
                    entry = leftPrayer,
                    textColor = textColor,
                    modifier = Modifier.weight(1f)
                )
                PrayerTimeItem(
                    entry = rightPrayer,
                    textColor = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrayerTimeItem(
    entry: PrayerTimeEntry,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(NimazCornerRadius.Small))
            .background(textColor.copy(alpha = 0.05f))
            .padding(horizontal = NimazSpacing.Small, vertical = NimazSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
    ) {
        // Color indicator bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(entry.color)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
            Text(
                text = entry.time,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

// --- Previews ---

private val sampleDayPrayerTimes = DayPrayerTimes(
    date = LocalDate.of(2026, 1, 15),
    fajr = "5:45 AM",
    sunrise = "7:12 AM",
    dhuhr = "12:30 PM",
    asr = "3:15 PM",
    maghrib = "5:48 PM",
    isha = "7:18 PM"
)

private val sampleTodayPrayerTimes = DayPrayerTimes(
    date = LocalDate.now(),
    fajr = "5:42 AM",
    sunrise = "7:10 AM",
    dhuhr = "12:28 PM",
    asr = "3:12 PM",
    maghrib = "5:45 PM",
    isha = "7:15 PM"
)

@Preview(showBackground = true, name = "Month Navigation Card")
@Composable
private fun MonthNavigationCardPreview() {
    MaterialTheme {
        MonthNavigationCard(
            monthYear = "January 2026",
            locationName = "Dublin, Ireland",
            onPrevious = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true, name = "Day Card - Regular")
@Composable
private fun DayPrayerCardPreview() {
    MaterialTheme {
        DayPrayerCard(
            dayTimes = sampleDayPrayerTimes,
            isToday = false,
            isExpanded = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Day Card - Today")
@Composable
private fun DayPrayerCardTodayPreview() {
    MaterialTheme {
        DayPrayerCard(
            dayTimes = sampleTodayPrayerTimes,
            isToday = true,
            isExpanded = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Day Card - Expanded")
@Composable
private fun DayPrayerCardExpandedPreview() {
    MaterialTheme {
        DayPrayerCard(
            dayTimes = sampleDayPrayerTimes,
            isToday = false,
            isExpanded = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Day Card - Today Expanded")
@Composable
private fun DayPrayerCardTodayExpandedPreview() {
    MaterialTheme {
        DayPrayerCard(
            dayTimes = sampleTodayPrayerTimes,
            isToday = true,
            isExpanded = true,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Full Screen Content", showSystemUi = true)
@Composable
private fun MonthlyPrayerTimesContentPreview() {
    val sampleDays = (1..31).map { day ->
        DayPrayerTimes(
            date = LocalDate.of(2026, 1, day),
            fajr = "5:${40 + (day % 10)} AM",
            sunrise = "7:${5 + (day % 10)} AM",
            dhuhr = "12:${25 + (day % 5)} PM",
            asr = "3:${10 + (day % 8)} PM",
            maghrib = "5:${40 + (day % 10)} PM",
            isha = "7:${10 + (day % 10)} PM"
        )
    }
    val today = LocalDate.now()

    MaterialTheme {
        Scaffold(
            topBar = {
                Surface(tonalElevation = 2.dp) {
                    Text(
                        text = "Monthly Prayer Times",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(NimazSpacing.Large)
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.Small
                ),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
            ) {
                item {
                    MonthNavigationCard(
                        monthYear = "January 2026",
                        locationName = "Dublin, Ireland",
                        onPrevious = {},
                        onNext = {}
                    )
                }

                items(sampleDays, key = { it.date.toEpochDay() }) { dayTimes ->
                    DayPrayerCard(
                        dayTimes = dayTimes,
                        isToday = dayTimes.date == today,
                        isExpanded = dayTimes.date.dayOfMonth == 15,
                        onClick = {}
                    )
                }
            }
        }
    }
}
