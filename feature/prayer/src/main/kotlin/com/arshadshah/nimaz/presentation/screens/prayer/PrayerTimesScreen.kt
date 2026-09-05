package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.common.formatWeekdayDayMonth
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazDivider
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarArc
import com.arshadshah.nimaz.presentation.components.atoms.NimazSolarNode
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.atoms.countdownText
import com.arshadshah.nimaz.presentation.components.atoms.rememberCountdownTo
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazDayRail
import com.arshadshah.nimaz.presentation.components.molecules.NimazDayRailItem
import com.arshadshah.nimaz.presentation.components.molecules.NimazPrayerRow
import com.arshadshah.nimaz.presentation.components.molecules.calendar.NimazCalendar
import com.arshadshah.nimaz.presentation.components.organisms.PrayerSkyScene
import com.arshadshah.nimaz.presentation.model.PrayerTimeDisplay
import com.arshadshah.nimaz.presentation.model.withClockState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.PrayerTimesViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** How far a horizontal drag must travel before it counts as paging a day. */
private val PageThreshold = 64.dp

/** Days either side of the selection the rail shows. Seven cells, selection in the middle. */
private const val RailReach = 3

/**
 * An instant as a fraction of its own local day — 0f at midnight, 1f at the next.
 *
 * The arc's x-axis is the clock, so every marker on it is one of these.
 */
private fun kotlin.time.Instant.dayFraction(): Float {
    val local = java.time.Instant.ofEpochMilli(toEpochMilliseconds())
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    return (local.hour * 60 + local.minute) / 1440f
}

/**
 * The clock-derived slice of the screen: live passed/current/next flags plus the sky hero's
 * labels. The ViewModel publishes instants; "now" only enters here, off the shared ticker.
 */
private data class PrayerSky(
    val prayers: List<PrayerTimeDisplay>,
    val timeOfDay: Float,
    val timeLabel: String,
    val statusLabel: String,
)

@Composable
private fun rememberPrayerSky(state: PrayerTimesUiState, selectedDate: LocalDate): PrayerSky {
    val now by rememberNow(TickResolution.MINUTES)
    val prayers = remember(state.prayers, now) { state.prayers.withClockState(now) }
    val next = prayers.firstOrNull { it.isNext }
    val nextAt = if (state.isToday) next?.timeAt ?: state.tomorrowFajrAt else null
    val nextName = if (state.isToday) (next?.type ?: PrayerType.FAJR).displayName else ""

    val nowLocal = remember(now) {
        java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    }
    val timeOfDay = (nowLocal.hour * 60 + nowLocal.minute) / 1440f

    val timeLabel =
        if (state.isToday) clockTimeText(now) else selectedDate.formatWeekdayDayMonth()

    // The sky keeps the *moment*; the solar card owns the *window*. They used to say the same
    // sentence — both wanted "Asr in 2h 41m" — so one of them was always redundant.
    val statusLabel = if (state.isToday && nextAt != null) {
        val parts by rememberCountdownTo(nextAt)
        stringResource(
            R.string.prayer_sky_status_format,
            nextName,
            countdownText(parts, showSeconds = false),
        )
    } else if (state.isToday) {
        nextName
    } else {
        relativeLabel(selectedDate)
    }
    return PrayerSky(prayers, timeOfDay, timeLabel, statusLabel)
}

/**
 * The Prayer Times screen: a day of prayer times, and nothing else.
 *
 * **It answers *when*.** The prayer tracker answers what the reader did about it, and this screen
 * writes nothing — see `PrayerTimesEvent`. That is why the rows are [NimazPrayerRow] rather than
 * the tracking `PrayerTimeCard` the Home screen uses.
 *
 * One `LazyColumn`, as every redesigned screen in the app is: the living sky is its first item and
 * scrolls, rather than pinning above a nested scroller. Swipe ← → to change day; the rail reaches
 * a week and its month button reaches anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PrayerTimesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Both follow the ViewModel rather than a `remember { LocalDate.now() }`, which froze at
    // whatever day the screen was opened — so across midnight "today" stayed yesterday and the
    // "Today" chip, which renders only when browsing, never appeared to offer a way back.
    val today = state.selectedDate.takeIf { state.isToday } ?: LocalDate.now()
    val selectedDate = state.selectedDate ?: today
    val sky = rememberPrayerSky(state, selectedDate)
    var showMonthSheet by remember { mutableStateOf(false) }

    // The living sky reaches the very top, behind the status bar, so the hero grows by the inset.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Zero content insets, deliberately. The scaffold's default `contentWindowInsets` includes the
    // status bar, and `PrayerSkyScene` already pads its own glass pills down by that same inset so
    // that the sky can bleed behind it. Taking both applies it twice: the pills sit a status bar
    // too low, and the hero's `+ statusBarTop` height compensates for a bleed that is no longer
    // happening. The navigation bar is handled on the scaffold itself.
    NimazScreenScaffold(
        modifier = Modifier.navigationBarsPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            val threshold = PageThreshold.toPx()
                            if (total > threshold) {
                                viewModel.onEvent(PrayerTimesEvent.PreviousDay)
                            } else if (total < -threshold) {
                                viewModel.onEvent(PrayerTimesEvent.NextDay)
                            }
                        },
                    ) { _, dragAmount -> total += dragAmount }
                },
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // 1. The living sky. Full bleed, so it takes no horizontal padding.
            item {
                PrayerSkyScene(
                    timeOfDay = sky.timeOfDay,
                    timeLabel = sky.timeLabel,
                    statusLabel = sky.statusLabel,
                    moonFraction = state.moonFraction,
                    sunriseFraction = state.sunriseFraction,
                    sunsetFraction = state.sunsetFraction,
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    // Never assert a city the reader has not chosen: with no location set the
                    // times below come from FallbackLocation, and the header says so.
                    locationName = if (state.isUsingFallbackLocation) {
                        stringResource(R.string.location_using_default)
                    } else {
                        state.locationName
                    },
                    onBack = onNavigateBack,
                    onSettings = onNavigateToSettings,
                    // The way back to today, as a third glass pill rather than a badge floating
                    // at a hand-measured offset below the bar.
                    trailingAction = if (!state.isToday) {
                        {
                            NimazBadge(
                                text = stringResource(R.string.today),
                                size = NimazBadgeSize.LARGE,
                                colors = NimazBadgeDefaults.colors(
                                    tone = NimazTone.ACCENT,
                                    emphasis = NimazBadgeEmphasis.FILLED,
                                ),
                                onClick = { viewModel.onEvent(PrayerTimesEvent.GoToToday) },
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp + statusBarTop),
                )
            }

            // 2. A week, and a way out of it.
            item {
                DayRailRow(
                    selectedDate = selectedDate,
                    today = today,
                    onSelect = { viewModel.onEvent(PrayerTimesEvent.SelectDate(it)) },
                    onJump = { showMonthSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 3. The day, drawn.
            item {
                SolarDayCard(
                    state = state,
                    prayers = sky.prayers,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            // 4. The six prayers, as rows in one card.
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // No trailing daylight: the card directly above states it in bold, and the
                    // About-this-day table states it again. Three times in one scroll, twice of
                    // them 20dp apart.
                    NimazSectionHeader(
                        title = selectedDate.formatWeekdayDayMonth(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PrayerRowsCard(
                        prayers = sky.prayers,
                        selectedDate = selectedDate,
                        isToday = state.isToday,
                    )
                }
            }

            // 5. The facts that are not times.
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NimazSectionHeader(
                        title = stringResource(R.string.prayer_about_this_day),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DayInfoCard(
                        sunrise = state.sunriseAt?.let { clockTimeText(it) } ?: Placeholder,
                        sunset = state.sunsetAt?.let { clockTimeText(it) } ?: Placeholder,
                        daylight = state.daylight,
                        method = state.methodLabel,
                    )
                }
            }
        }
    }

    if (showMonthSheet) {
        val sheetState = rememberModalBottomSheetState()
        var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
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

private const val Placeholder = "--:--"

/**
 * A week centred on the selection, plus a month button.
 *
 * The rail reaches ±3 days; the button is what keeps every other day reachable, since the old
 * prev/next arrows and their tap-the-date picker went with the redesign.
 */
@Composable
private fun DayRailRow(
    selectedDate: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onJump: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember(selectedDate) {
        (-RailReach..RailReach).map { selectedDate.plusDays(it.toLong()) }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NimazDayRail(
            days = days.map { date ->
                NimazDayRailItem(
                    weekdayLabel = date.narrowWeekday(),
                    dayLabel = date.dayOfMonth.toString(),
                    isToday = date == today,
                    contentDescription = date.formatWeekdayDayMonth(),
                )
            },
            selectedIndex = days.indexOf(selectedDate).takeIf { it >= 0 },
            onSelect = { onSelect(days[it]) },
            modifier = Modifier.weight(1f),
        )
        NimazIconButton(
            icon = Icons.Default.CalendarMonth,
            onClick = onJump,
            contentDescription = stringResource(R.string.cd_pick_month),
        )
    }
}

private fun LocalDate.narrowWeekday(): String =
    dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

/**
 * The day as a drawing: where the sun is, where the prayers sit on its path, and — on today —
 * which prayer's window the reader is currently inside.
 *
 * On any other day there is no "now", so the card leads with the daylight instead and the arc
 * carries no sun. That is the same card in two states rather than two cards.
 */
@Composable
private fun SolarDayCard(
    state: PrayerTimesUiState,
    prayers: List<PrayerTimeDisplay>,
    modifier: Modifier = Modifier,
) {
    val now by rememberNow(TickResolution.MINUTES)
    val isToday = state.isToday
    val current = prayers.lastOrNull { it.isPassed }
    val next = prayers.firstOrNull { it.isNext }

    val nodes = remember(prayers) {
        prayers.mapNotNull { prayer ->
            val at = prayer.timeAt ?: return@mapNotNull null
            NimazSolarNode(
                position = at.dayFraction(),
                // Sunrise and Maghrib are the horizon crossings and the card states their times
                // below, so they are drawn as bare dots — six labels do not fit.
                label = when (prayer.type) {
                    PrayerType.SUNRISE, PrayerType.MAGHRIB -> null
                    else -> prayer.type.displayName
                },
                tone = if (prayer.isNext) NimazTone.WARNING else NimazTone.MUTED,
                contentDescription = prayer.type.displayName,
            )
        }
    }

    // NEUTRAL, not ACCENT. An accent card fills with the primary hue in dark, and the arc's
    // daylight limb is drawn from that same hue — so the rising limb vanished into its own
    // container. The card is the ground; the arc is the figure.
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.MUTED,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (isToday && current != null) {
                Text(
                    text = stringResource(R.string.prayer_window_lede),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = current.type.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val until = next?.timeAt
                    if (until != null) {
                        Text(
                            text = stringResource(
                                R.string.prayer_window_until,
                                clockTimeText(until),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.prayer_daylight_lede),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.prayer_daylight_amount, state.daylight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val sunriseText = state.sunriseAt?.let { clockTimeText(it) } ?: Placeholder
            val sunsetText = state.sunsetAt?.let { clockTimeText(it) } ?: Placeholder

            NimazSolarArc(
                nodes = nodes,
                sunriseFraction = state.sunriseFraction,
                sunsetFraction = state.sunsetFraction,
                contentDescription = stringResource(
                    R.string.prayer_arc_cd,
                    sunriseText,
                    sunsetText,
                ),
                sunPosition = if (isToday) now.dayFraction() else null,
                // Kotlin will not smart-cast a `val` from another module, so both ends are bound
                // locally before the range is built.
                litSpan = if (isToday) {
                    val from = current?.timeAt
                    val to = next?.timeAt
                    if (from != null && to != null) {
                        from.dayFraction()..to.dayFraction()
                    } else {
                        null
                    }
                } else {
                    null
                },
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sunriseText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = sunsetText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrayerRowsCard(
    prayers: List<PrayerTimeDisplay>,
    selectedDate: LocalDate,
    isToday: Boolean,
) {
    NimazCard(
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.MUTED,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            prayers.forEachIndexed { index, prayer ->
                if (index > 0) NimazDivider()
                NimazPrayerRow(
                    type = prayer.type,
                    name = prayer.type.displayName,
                    time = prayer.timeAt?.let { clockTimeText(it) } ?: Placeholder,
                    qualifier = jumuahQualifier(prayer.type, selectedDate),
                    isPassed = prayer.isPassed,
                    isNext = prayer.isNext && isToday,
                    showArabic = prayer.type != PrayerType.SUNRISE,
                )
            }
        }
    }
}

/** Friday's Dhuhr is Jumu'ah, and saying so is the one thing a prayer row can add to a time. */
@Composable
private fun jumuahQualifier(type: PrayerType, date: LocalDate): String? =
    if (type == PrayerType.DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY) {
        stringResource(R.string.prayer_jumuah)
    } else {
        null
    }

@Composable
private fun DayInfoCard(sunrise: String, sunset: String, daylight: String, method: String) {
    NimazCard(
        modifier = Modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        shape = RoundedCornerShape(20.dp),
        tone = NimazTone.MUTED,
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

/**
 * "Tomorrow", "3 days ago" — localised.
 *
 * Replaces a hardcoded-English `daysFromToday` that sat four lines from this function and did the
 * same job. It gains a `diff == 0` branch, which the old caller never needed and this one does.
 */
@Composable
private fun relativeLabel(date: LocalDate): String {
    val diff = date.toEpochDay() - LocalDate.now().toEpochDay()
    return when {
        diff == 0L -> stringResource(R.string.today)
        diff == 1L -> stringResource(R.string.fasting_tomorrow)
        diff == -1L -> stringResource(R.string.relative_yesterday)
        diff > 0 -> pluralStringResource(R.plurals.relative_in_days_format, diff.toInt(), diff)
        else -> pluralStringResource(R.plurals.relative_days_ago_format, (-diff).toInt(), -diff)
    }
}
