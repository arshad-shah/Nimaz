package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.formatMonthYear
import com.arshadshah.nimaz.core.util.PrayerTimesPdfExporter
import com.arshadshah.nimaz.core.util.formatClockTime
import com.arshadshah.nimaz.core.util.formatFastLength
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.model.IslamicEvents
import com.arshadshah.nimaz.presentation.components.atoms.NavArrowDirection
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazNavArrowButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.clockTimeText
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.prayer.DayPrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.MonthlyPrayerTimesViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPrayerTimesScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyPrayerTimesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Captured from the composition so the (non-composable) PDF export can honour the preference.
    val use24HourForExport = LocalUse24HourFormat.current
    var exportMenuExpanded by remember { mutableStateOf(false) }
    val canExport = !state.isLoading && state.dayPrayerTimes.isNotEmpty()

    fun shareRows(rows: List<DayPrayerTimes>) {
        if (rows.isEmpty()) return
        AppAnalytics.logFeatureUsed("monthly_prayer_times", "export_pdf")
        runCatching {
            val pdfRows = rows.map {
                PrayerTimesPdfExporter.Row(
                    it.date,
                    listOf(it.fajr, it.sunrise, it.dhuhr, it.asr, it.maghrib, it.isha)
                        .map { t -> t?.let { i -> exportClock(i, use24HourForExport) } ?: "--:--" },
                    fastMinutes = it.fastMinutes,
                )
            }
            val file = PrayerTimesPdfExporter.export(
                context = context,
                locationName = state.locationName,
                methodLabel = state.methodLabel,
                rows = pdfRows,
                latitude = state.latitude,
                longitude = state.longitude,
            )
            ContentShareManager.shareFile(
                context,
                file,
                mimeType = "application/pdf",
            )
        }.onFailure { CrashReporter.recordException(it) }
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.monthly_prayer_times),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { exportMenuExpanded = true }, enabled = canExport) {
                        NimazIcon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.export_as_pdf)
                        )
                    }
                    // Short export chooser → anchored dropdown menu (was a bottom sheet).
                    NimazDropdownMenu(
                        expanded = exportMenuExpanded,
                        onDismissRequest = { exportMenuExpanded = false },
                    ) {
                        NimazDropdownRow(
                            text = stringResource(R.string.monthly_this_month),
                            description = "${state.currentMonth.formatMonthYear()} · ${
                                pluralStringResource(
                                    R.plurals.days_count_format,
                                    state.dayPrayerTimes.size,
                                    state.dayPrayerTimes.size
                                )
                            }",
                            leadingIcon = Icons.Default.CalendarMonth,
                            onClick = {
                                exportMenuExpanded = false
                                shareRows(state.dayPrayerTimes)
                            },
                        )
                        state.ramadanHijriYear?.let { ramadanYear ->
                            NimazDropdownRow(
                                text = stringResource(R.string.ramadan_year_format, ramadanYear),
                                description = stringResource(R.string.monthly_full_month_subtitle),
                                leadingIcon = Icons.Default.DarkMode,
                                onClick = {
                                    exportMenuExpanded = false
                                    shareRows(viewModel.ramadanDays())
                                },
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned month-navigation header — stays put while the list scrolls.
            MonthNavigationHeader(
                monthYear = state.currentMonth.formatMonthYear(),
                hijriLabel = hijriRangeLabel(state.currentMonth),
                locationName = state.locationName,
                isRamadan = state.ramadanHijriYear != null,
                onPrevious = { viewModel.onEvent(MonthlyPrayerTimesEvent.PreviousMonth) },
                onNext = { viewModel.onEvent(MonthlyPrayerTimesEvent.NextMonth) }
            )

            val today = LocalDate.now()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = NimazSpacing.Large,
                    vertical = NimazSpacing.Small
                ),
                verticalArrangement = Arrangement.spacedBy(NimazSpacing.Small)
            ) {
                items(state.dayPrayerTimes, key = { it.date.toEpochDay() }) { dayTimes ->
                    DayPrayerCard(
                        dayTimes = dayTimes,
                        isToday = dayTimes.date == today,
                        isExpanded = dayTimes.date == state.expandedDay,
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
}

/** A Gregorian month spans ~two Hijri months — describe the span compactly. */
private fun hijriRangeLabel(month: YearMonth): String {
    val first = HijriDateCalculator.toHijri(month.atDay(1))
    val last = HijriDateCalculator.toHijri(month.atEndOfMonth())
    val firstName = HijriDateCalculator.getHijriMonthName(first.month)
    return if (first.month == last.month) {
        "$firstName ${first.year}"
    } else {
        "${firstName.take(3)} – ${HijriDateCalculator.getHijriMonthName(last.month)} ${last.year}"
    }
}

@Composable
private fun MonthNavigationHeader(
    monthYear: String,
    hijriLabel: String,
    locationName: String,
    isRamadan: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NimazSpacing.Large, vertical = NimazSpacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazNavArrowButton(
                    direction = NavArrowDirection.PREVIOUS,
                    onClick = onPrevious,
                    contentDescription = stringResource(R.string.cd_previous_month),
                    size = 44.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = monthYear,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = hijriLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            variant = NimazIconVariant.MUTED,
                            size = NimazIconSize.EXTRA_SMALL
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                NimazNavArrowButton(
                    direction = NavArrowDirection.NEXT,
                    onClick = onNext,
                    contentDescription = stringResource(R.string.cd_next_month),
                    size = 44.dp
                )
            }

            if (isRamadan) {
                NimazBadge(
                    text = stringResource(R.string.ramadan_month_label),
                    modifier = Modifier
                        .padding(top = NimazSpacing.Small)
                        .align(Alignment.CenterHorizontally),
                    size = NimazBadgeSize.SMALL,
                    colors = NimazBadgeDefaults.feature(
                        color = NimazColors.Secondary,
                        emphasis = NimazBadgeEmphasis.SOFT
                    )
                )
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
        PrayerTimeEntry(
            stringResource(R.string.prayer_fajr),
            dayTimes.fajr?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Fajr
        ),
        PrayerTimeEntry(
            stringResource(R.string.prayer_sunrise),
            dayTimes.sunrise?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Sunrise
        ),
        PrayerTimeEntry(
            stringResource(R.string.prayer_dhuhr),
            dayTimes.dhuhr?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Dhuhr
        ),
        PrayerTimeEntry(
            stringResource(R.string.prayer_asr),
            dayTimes.asr?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Asr
        ),
        PrayerTimeEntry(
            stringResource(R.string.prayer_maghrib),
            dayTimes.maghrib?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Maghrib
        ),
        PrayerTimeEntry(
            stringResource(R.string.prayer_isha),
            dayTimes.isha?.let { clockTimeText(it) } ?: "--:--",
            NimazColors.PrayerColors.Isha
        )
    )

    val hijri = HijriDateCalculator.toHijri(dayTimes.date)
    val event = IslamicEvents.events
        .filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day }
        .maxByOrNull { it.priority }
    val fast = if (hijri.month == 9) dayTimes.fastMinutes?.let { formatFastLength(it) } else null

    NimazCard(
        onClick = onClick,
        // A day row sits directly on the page background: elevation carries the card
        // boundary (in light mode `surfaceContainer` on `background` is nearly
        // invisible), while the fill carries the "today" selection state.
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(NimazCornerRadius.Large),
        selected = isToday,
        colors = NimazCardDefaults.selectable(
            container = MaterialTheme.colorScheme.surface,
            // Transparent so the "today" gradient drawn inside shows through.
            activeContainer = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        val background = if (isToday) {
            Modifier.background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
                )
            )
        } else {
            Modifier
        }

        Column(
            modifier = Modifier
                .then(background)
                .fillMaxWidth()
        ) {
            DayMetaRow(
                date = dayTimes.date,
                hijriDay = hijri.day,
                hijriMonth = hijri.month,
                hijriYear = hijri.year,
                event = event,
                fast = fast,
                isToday = isToday,
                isExpanded = isExpanded
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExpandedPrayerGrid(prayers = prayers, isToday = isToday)
            }
        }
    }
}

@Composable
private fun DayMetaRow(
    date: LocalDate,
    hijriDay: Int,
    hijriMonth: Int,
    hijriYear: Int,
    event: IslamicEvent?,
    fast: String?,
    isToday: Boolean,
    isExpanded: Boolean
) {
    val locale = LocalLocale.current.platformLocale
    val onColor =
        if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val weekdayShort = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
    val hijriShort = "$hijriDay ${HijriDateCalculator.getHijriMonthName(hijriMonth).take(3)}"

    val titleLine =
        (if (isToday) stringResource(R.string.today) + " · " else "") + "$weekdayShort, ${date.dayOfMonth} $monthName"
    val fastLabel = fast?.let { stringResource(R.string.monthly_fast_length_format, it) }.orEmpty()
    val subLine = buildString {
        append("$hijriDay ${HijriDateCalculator.getHijriMonthName(hijriMonth)} $hijriYear")
        if (fast != null) append(" · " + fastLabel)
    }

    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "chevron")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = NimazSpacing.Medium, vertical = NimazSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NimazSpacing.Medium)
    ) {
        DayBadge(
            dayNumber = date.dayOfMonth.toString(),
            dayOfWeek = weekdayShort,
            hijri = hijriShort,
            isToday = isToday
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleLine,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = onColor
            )
            Text(
                text = subLine,
                style = MaterialTheme.typography.labelMedium,
                color = onColor.copy(alpha = 0.7f)
            )
            if (event != null) {
                EventTag(event = event)
            }
        }

        NimazIcon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = stringResource(if (isExpanded) R.string.cd_collapse else R.string.cd_expand),
            tint = onColor.copy(alpha = 0.6f),
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Composable
private fun EventTag(event: IslamicEvent) {
    val color = eventAccent(event.eventType)
    val label =
        if (event.eventType == IslamicEventType.HOLIDAY) "★ ${event.nameEnglish}" else event.nameEnglish
    NimazBadge(
        text = label,
        modifier = Modifier.padding(top = 4.dp),
        size = NimazBadgeSize.SMALL,
        colors = NimazBadgeDefaults.feature(
            color = color,
            emphasis = NimazBadgeEmphasis.SOFT
        )
    )
}

private fun eventAccent(type: IslamicEventType): Color = when (type) {
    IslamicEventType.HOLIDAY -> NimazColors.Gold500
    IslamicEventType.NIGHT -> NimazColors.PrayerColors.Isha
    IslamicEventType.FAST -> NimazColors.Primary
    IslamicEventType.HISTORICAL -> NimazColors.PrayerColors.Fajr
}

@Composable
private fun DayBadge(
    dayNumber: String,
    dayOfWeek: String,
    hijri: String,
    isToday: Boolean
) {
    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val hijriColor =
        if (isToday) textColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary

    // Nested inside the day card → outlined, no elevation. The primary fill is
    // reserved for the "today" selection.
    NimazCard(
        modifier = Modifier.width(52.dp),
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(NimazCornerRadius.Medium),
        selected = isToday,
        colors = NimazCardDefaults.selectable(
            container = MaterialTheme.colorScheme.surface,
            activeContainer = MaterialTheme.colorScheme.primary,
            activeBorder = MaterialTheme.colorScheme.primary,
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
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
            Text(
                text = hijri,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = hijriColor,
                maxLines = 1,
            )
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
    // A cell nested inside the day card → outlined, no fill. A 5% tint was invisible
    // in light mode, and a transparent container lets the "today" gradient show through.
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.OUTLINED,
        shape = RoundedCornerShape(NimazCornerRadius.Small),
        colors = NimazCardDefaults.colors(
            container = Color.Transparent,
            content = textColor,
            border = textColor.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
}

// --- Previews ---

private val sampleDayPrayerTimes = DayPrayerTimes(
    date = LocalDate.of(2026, 1, 15),
    fajr = previewInstant(5, 45),
    sunrise = previewInstant(7, 12),
    dhuhr = previewInstant(12, 30),
    asr = previewInstant(15, 15),
    maghrib = previewInstant(17, 48),
    isha = previewInstant(19, 18)
)

private val sampleTodayPrayerTimes = DayPrayerTimes(
    date = LocalDate.now(),
    fajr = previewInstant(5, 42),
    sunrise = previewInstant(7, 10),
    dhuhr = previewInstant(12, 28),
    asr = previewInstant(15, 12),
    maghrib = previewInstant(17, 45),
    isha = previewInstant(19, 15)
)

@Preview(showBackground = true, name = "Month Navigation Header")
@Composable
private fun MonthNavigationHeaderPreview() {
    MaterialTheme {
        MonthNavigationHeader(
            monthYear = "March 2026",
            hijriLabel = "Sha – Ramaḍān 1447",
            locationName = "Dublin, Ireland",
            isRamadan = true,
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

/**
 * The PDF export runs outside composition, so it cannot read `LocalUse24HourFormat`; the caller
 * captures the flag and formats with it here.
 */
private fun exportClock(instant: kotlin.time.Instant, use24Hour: Boolean): String {
    val local = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
    return formatClockTime(local.hour, local.minute, use24Hour)
}

/** Fixed wall-clock instants for previews. */
private fun previewInstant(hour: Int, minute: Int): kotlin.time.Instant =
    kotlin.time.Instant.fromEpochMilliseconds(
        java.time.LocalDate.now().atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
