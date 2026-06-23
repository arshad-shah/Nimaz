package com.arshadshah.nimaz.presentation.screens.prayer

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimesPdfExporter
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.model.IslamicEvents
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius
import com.arshadshah.nimaz.presentation.theme.NimazSpacing
import com.arshadshah.nimaz.presentation.viewmodel.DayPrayerTimes
import com.arshadshah.nimaz.presentation.viewmodel.MonthlyPrayerTimesEvent
import com.arshadshah.nimaz.presentation.viewmodel.MonthlyPrayerTimesViewModel
import com.arshadshah.nimaz.core.util.MONTH_YEAR_FORMATTER
import com.arshadshah.nimaz.core.util.formatFastLength
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPrayerTimesScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonthlyPrayerTimesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }
    val canExport = !state.isLoading && state.dayPrayerTimes.isNotEmpty()

    fun shareRows(rows: List<DayPrayerTimes>) {
        if (rows.isEmpty()) return
        runCatching {
            val pdfRows = rows.map {
                PrayerTimesPdfExporter.Row(
                    it.date,
                    listOf(it.fajr, it.sunrise, it.dhuhr, it.asr, it.maghrib, it.isha),
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
            context.startActivity(
                Intent.createChooser(
                    PrayerTimesPdfExporter.buildShareIntent(context, file),
                    "Share prayer times",
                )
            )
        }
    }

    Scaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.monthly_prayer_times),
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showExportSheet = true }, enabled = canExport) {
                        NimazIcon(Icons.Default.Share, contentDescription = stringResource(R.string.export_as_pdf))
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
                monthYear = state.currentMonth.format(MONTH_YEAR_FORMATTER),
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

    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState()
        NimazBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            scrollable = false,
            contentPadding = PaddingValues(0.dp),
        ) {
            ExportSheet(
                monthLabel = state.currentMonth.format(MONTH_YEAR_FORMATTER),
                dayCount = state.dayPrayerTimes.size,
                ramadanYear = state.ramadanHijriYear,
                onThisMonth = {
                    shareRows(state.dayPrayerTimes)
                    showExportSheet = false
                },
                onRamadan = {
                    shareRows(viewModel.ramadanDays())
                    showExportSheet = false
                },
            )
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
private fun ExportSheet(
    monthLabel: String,
    dayCount: Int,
    ramadanYear: Int?,
    onThisMonth: () -> Unit,
    onRamadan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.export_as_pdf),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )
        ExportOption(
            icon = Icons.Default.CalendarMonth,
            tint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.monthly_this_month),
            subtitle = "$monthLabel · $dayCount days",
            onClick = onThisMonth,
        )
        if (ramadanYear != null) {
            Spacer(modifier = Modifier.height(8.dp))
            ExportOption(
                icon = Icons.Default.DarkMode,
                tint = NimazColors.Secondary,
                title = stringResource(R.string.ramadan_year_format, ramadanYear),
                subtitle = stringResource(R.string.monthly_full_month_subtitle),
                onClick = onRamadan,
                highlight = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportOption(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlight: Boolean = false,
) {
    NimazCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = NimazCardDefaults.colors(
            container = if (highlight) tint.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                NimazIcon(icon, contentDescription = null, tint = tint, size = NimazIconSize.MEDIUM)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NimazIcon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                variant = NimazIconVariant.MUTED
            )
        }
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
                IconButton(
                    onClick = onPrevious,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(R.string.cd_previous_month),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

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

                IconButton(
                    onClick = onNext,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(R.string.cd_next_month),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isRamadan) {
                Box(
                    modifier = Modifier
                        .padding(top = NimazSpacing.Small)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NimazColors.Secondary.copy(alpha = 0.18f))
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ramadan_month_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NimazColors.Secondary
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

    val hijri = HijriDateCalculator.toHijri(dayTimes.date)
    val event = IslamicEvents.events
        .filter { it.hijriMonth == hijri.month && it.hijriDay == hijri.day }
        .maxByOrNull { it.priority }
    val fast = if (hijri.month == 9) dayTimes.fastMinutes?.let { formatFastLength(it) } else null

    NimazCard(
        onClick = onClick,
        shape = RoundedCornerShape(NimazCornerRadius.Large),
        selected = isToday,
        colors = NimazCardDefaults.selectable(
            container = MaterialTheme.colorScheme.surfaceContainer,
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

        Column(modifier = Modifier
            .then(background)
            .fillMaxWidth()) {
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
        (if (isToday) "Today · " else "") + "$weekdayShort, ${date.dayOfMonth} $monthName"
    val subLine = buildString {
        append("$hijriDay ${HijriDateCalculator.getHijriMonthName(hijriMonth)} $hijriYear")
        if (fast != null) append(" · $fast fast")
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
            contentDescription = if (isExpanded) "Collapse" else "Expand",
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
    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
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
    val hijriColor =
        if (isToday) textColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(52.dp)
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
        Text(
            text = hijri,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = hijriColor,
            maxLines = 1,
        )
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
