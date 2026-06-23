package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.PrayerTimeCard
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerSkyScene
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimesViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMMM")

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
                        NimazIcon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_prayer_settings))
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
            // Living sky banner (pinned) — square top, rounded bottom, with a
            // "Today" shortcut when browsing.
            Box(modifier = Modifier.fillMaxWidth()) {
                PrayerSkyScene(
                    timeOfDay = state.timeOfDay,
                    timeLabel = state.skyTimeLabel,
                    statusLabel = state.skyStatusLabel,
                    moonFraction = state.moonFraction,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
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
                            text = stringResource(R.string.today),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Day navigation, in a card overlapping the sky's curved bottom.
            // A custom layout pulls the card up by `overlap` AND shrinks the
            // space it reserves by the same amount, so its bottom is flush with
            // the day list (offset alone would leave an equal-sized empty gap).
            NimazCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val overlap = 28.dp.roundToPx()
                        layout(placeable.width, (placeable.height - overlap).coerceAtLeast(0)) {
                            placeable.place(0, -overlap)
                        }
                    },
                style = NimazCardStyle.FILLED,
                shape = RoundedCornerShape(20.dp),
                colors = NimazCardDefaults.colors(container = MaterialTheme.colorScheme.surface),
                elevation = 4.dp,
            ) {
                DayNavBar(
                    selectedDate = state.selectedDate,
                    isToday = state.isToday,
                    onPrev = { viewModel.onEvent(PrayerTimesEvent.PreviousDay) },
                    onNext = { viewModel.onEvent(PrayerTimesEvent.NextDay) },
                    onPickDate = { showMonthSheet = true },
                )
            }

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
                                (slideOutHorizontally(tween(260)) { w -> -dir * w } + fadeOut(
                                    tween(
                                        260
                                    )
                                ))
                    },
                    label = "day",
                ) { date ->
                    DayList(
                        prayers = state.prayers,
                        isFuture = date.isAfter(today),
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
        var displayedMonth by remember { mutableStateOf(YearMonth.from(state.selectedDate)) }
        NimazBottomSheet(
            onDismissRequest = { showMonthSheet = false },
            sheetState = sheetState,
            scrollable = false,
            contentPadding = PaddingValues(0.dp),
        ) {
            NimazCalendar(
                displayedMonth = displayedMonth,
                selectedDate = state.selectedDate,
                onDateSelected = {
                    viewModel.onEvent(PrayerTimesEvent.SelectDate(it))
                    showMonthSheet = false
                },
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = { displayedMonth = displayedMonth.plusMonths(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .navigationBarsPadding(),
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrev) {
            NimazIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_day))
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onPickDate)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isToday) stringResource(R.string.today_uppercase) else relativeLabel(selectedDate).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedDate.format(DATE_FMT),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    NimazIcon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.cd_pick_date),
                        variant = NimazIconVariant.MUTED,
                        size = NimazIconSize.SMALL,
                        modifier = Modifier
                            .padding(start = 6.dp),
                    )
                }
            }
        }
        FilledTonalIconButton(onClick = onNext) {
            NimazIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_day))
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
    NimazCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(16.dp),
        colors = NimazCardDefaults.colors(container = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(stringResource(R.string.prayer_info_daylight), daylight)
            InfoRow(stringResource(R.string.prayer_info_sun), "$sunrise — $sunset")
            InfoRow(stringResource(R.string.prayer_info_method), method)
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
private fun relativeLabel(date: LocalDate): String {
    val diff = date.toEpochDay() - LocalDate.now().toEpochDay()
    return when {
        diff == 1L -> stringResource(R.string.fasting_tomorrow)
        diff == -1L -> stringResource(R.string.relative_yesterday)
        diff > 0 -> stringResource(R.string.relative_in_days_format, diff)
        else -> stringResource(R.string.relative_days_ago_format, -diff)
    }
}
