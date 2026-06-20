package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerSkyScene
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimesViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM")
private val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

/**
 * Dedicated Prayer Times screen: a day pager with a living-sky hero. The top
 * bar, sky and date row stay pinned while the reused [PrayerTimeCard] list
 * scrolls. Swipe ← → (or use the arrows) to change day; tap the date to jump
 * to any month.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PrayerTimesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val today = remember { LocalDate.now() }
    var showMonthSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = state.locationName,
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Prayer settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Living sky hero (pinned) with a "Today" shortcut when browsing.
            Box(modifier = Modifier.fillMaxWidth()) {
                PrayerSkyScene(
                    timeOfDay = state.timeOfDay,
                    timeLabel = state.skyTimeLabel,
                    statusLabel = state.skyStatusLabel,
                    moonFraction = state.moonFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                )
                if (!state.isToday) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clickable { viewModel.onEvent(PrayerTimesEvent.GoToToday) },
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Day navigation row (pinned).
            DayNavBar(
                selectedDate = state.selectedDate,
                isToday = state.isToday,
                onPrev = { viewModel.onEvent(PrayerTimesEvent.PreviousDay) },
                onNext = { viewModel.onEvent(PrayerTimesEvent.NextDay) },
                onPickDate = { showMonthSheet = true },
            )

            // Day content: swipe horizontally to change day; list scrolls within.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                val threshold = 64.dp.toPx()
                                if (total > threshold) {
                                    viewModel.onEvent(PrayerTimesEvent.PreviousDay)
                                } else if (total < -threshold) {
                                    viewModel.onEvent(PrayerTimesEvent.NextDay)
                                }
                            },
                        ) { _, dragAmount -> total += dragAmount }
                    },
            ) {
                AnimatedContent(
                    targetState = state.selectedDate,
                    transitionSpec = {
                        val dir = if (targetState.isAfter(initialState)) 1 else -1
                        (slideInHorizontally(tween(260)) { w -> dir * w } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally(tween(260)) { w -> -dir * w } + fadeOut(tween(260)))
                    },
                    label = "day",
                ) { _ ->
                    DayList(
                        prayers = state.prayers,
                        isFuture = state.selectedDate.isAfter(today),
                        sunrise = state.sunrise,
                        sunset = state.sunset,
                        daylight = state.daylight,
                        method = state.methodLabel,
                        onToggle = { viewModel.onEvent(PrayerTimesEvent.TogglePrayer(it)) },
                    )
                }
            }
        }
    }

    if (showMonthSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMonthSheet = false },
            sheetState = sheetState,
        ) {
            MonthPicker(
                selected = state.selectedDate,
                onPick = {
                    viewModel.onEvent(PrayerTimesEvent.SelectDate(it))
                    showMonthSheet = false
                },
            )
        }
    }
}

@Composable
private fun DayNavBar(
    selectedDate: LocalDate,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(
            modifier = Modifier.clickable(onClick = onPickDate),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (isToday) "Today" else relativeLabel(selectedDate),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = selectedDate.format(DATE_FMT),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun DayList(
    prayers: List<PrayerTimeDisplay>,
    isFuture: Boolean,
    sunrise: String,
    sunset: String,
    daylight: String,
    method: String,
    onToggle: (PrayerType) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(prayers, key = { it.type }) { prayer ->
            PrayerTimeCard(
                prayer = prayer,
                isActive = prayer.isNext,
                onClick = { onToggle(prayer.type) },
                onToggle = { onToggle(prayer.type) },
                showToggle = !isFuture,
            )
        }
        item {
            DayInfoCard(sunrise = sunrise, sunset = sunset, daylight = daylight, method = method)
        }
    }
}

@Composable
private fun DayInfoCard(sunrise: String, sunset: String, daylight: String, method: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Daylight", daylight)
            InfoRow("Sunrise / Sunset", "$sunrise — $sunset")
            InfoRow("Method", method)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MonthPicker(selected: LocalDate, onPick: (LocalDate) -> Unit) {
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }
    val today = remember { LocalDate.now() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = month.atDay(1).format(MONTH_FMT),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Cells: leading blanks (Monday-based) then the month's days.
        val leading = month.atDay(1).dayOfWeek.value - 1
        val cells = buildList<LocalDate?> {
            repeat(leading) { add(null) }
            for (d in 1..month.lengthOfMonth()) add(month.atDay(d))
        }
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        isSelected = date == selected,
                        isToday = date == today,
                        onClick = { date?.let(onPick) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            )
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

private fun relativeLabel(date: LocalDate): String {
    val diff = date.toEpochDay() - LocalDate.now().toEpochDay()
    return when {
        diff == 1L -> "Tomorrow"
        diff == -1L -> "Yesterday"
        diff > 0 -> "In $diff days"
        else -> "${-diff} days ago"
    }
}
