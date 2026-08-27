package com.arshadshah.nimaz.presentation.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.calendar.HijriDateCalculator
import com.arshadshah.nimaz.domain.model.FallbackLocation
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.model.resolveLocation
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckbox
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCheckboxVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.settings.SettingsViewModel
import com.arshadshah.nimaz.core.common.formatWidgetTime
import com.arshadshah.nimaz.presentation.foundation.tokens.prayerShortName
import java.time.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Data classes for widget preview
private data class WidgetPreviewData(
    val nextPrayerName: String = "—",
    val nextPrayerTime: String = "—",
    val countdown: String = "—",
    val prayers: List<PrayerPreview> = emptyList(),
    val hijriDate: String = "—",
    val hijriDay: Int = 1,
    val hijriMonth: String = "—",
    val hijriYear: Int = 1446,
    val gregorianDate: String = "—",
    val dayOfWeek: String = "—",
    val locationName: String = "—",
    val daysInMonth: Int = 30,
    val firstDayOfWeekOffset: Int = 0,
    val todayEvents: List<Pair<String, String>> = emptyList()
)

private data class PrayerPreview(
    val name: String,
    val time: String,
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // State for dynamic widget preview data.
    //
    // The preferences come from the ViewModel's `LocationSettings` seam. This used to call a
    // helper that constructed `PreferencesDataStore(context)` itself — a second instance of a
    // `@Singleton`, built outside Hilt — which since #559 would also be a screen depending on
    // `:core:datastore`'s implementation rather than on the seam. Same values, same fallback,
    // one owner.
    val previewPreferences by viewModel.widgetPreviewPreferences.collectAsStateWithLifecycle()
    val previewLocation = remember(previewPreferences) {
        previewPreferences?.let(::previewLocationOf)
    }

    // Only the countdown moves per second; derive it from the one shared ticker
    // instead of a private 1s loop. buildWidgetPreviewData is pure (no I/O), so it
    // is safe to recompute on each second boundary.
    val nowTick by rememberNow(TickResolution.SECONDS)
    val previewData = remember(previewLocation, nowTick) {
        previewLocation?.let {
            buildWidgetPreviewData(context, it, viewModel.prayerTimeCalculator)
        } ?: WidgetPreviewData()
    }

    NimazScreenScaffold(
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.widgets),
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro text
            item {
                Text(
                    text = stringResource(R.string.widgets_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            // Next Prayer Widget (2x2)
            item {
                WidgetSection(
                    title = stringResource(R.string.widget_next_prayer_title),
                    infoName = stringResource(R.string.widget_next_prayer),
                    infoSize = stringResource(R.string.widget_next_prayer_size),
                    infoIcon = Icons.Default.Schedule,
                    preview = { NextPrayerWidgetPreview(previewData) }
                )
            }

            // Prayer Times Widget (4x1)
            item {
                WidgetSection(
                    title = stringResource(R.string.widget_prayer_times_title),
                    infoName = stringResource(R.string.widget_prayer_times),
                    infoSize = stringResource(R.string.widget_prayer_times_size),
                    infoIcon = Icons.AutoMirrored.Filled.ListAlt,
                    preview = { PrayerTimesWidgetPreview(previewData) }
                )
            }

            // Hijri Date Widget (2x2)
            item {
                WidgetSection(
                    title = stringResource(R.string.widget_hijri_date_title),
                    infoName = stringResource(R.string.widget_hijri_date),
                    infoSize = stringResource(R.string.widget_hijri_date_size),
                    infoIcon = Icons.Default.CalendarMonth,
                    preview = { HijriDateWidgetPreview(previewData) }
                )
            }

            // Prayer Tracker Widget (4x1)
            item {
                WidgetSection(
                    title = stringResource(R.string.widget_prayer_tracker_title),
                    infoName = stringResource(R.string.widget_prayer_tracker),
                    infoSize = stringResource(R.string.widget_prayer_tracker_size),
                    infoIcon = Icons.Default.CheckCircle,
                    preview = { PrayerTrackerWidgetPreview() }
                )
            }

            // Hijri Calendar Widget (4x2)
            item {
                WidgetSection(
                    title = stringResource(R.string.widget_hijri_calendar_title),
                    infoName = stringResource(R.string.widget_hijri_calendar),
                    infoSize = stringResource(R.string.widget_hijri_calendar_size),
                    infoIcon = Icons.Default.CalendarMonth,
                    preview = { HijriCalendarWidgetPreview(previewData) }
                )
            }

            // Khatam Widget (4x2)
            item {
                WidgetSection(
                    // Reuses the localised widget strings rather than adding
                    // screen-only copy that would exist in English alone.
                    title = stringResource(R.string.khatam_widget_label),
                    infoName = stringResource(R.string.khatam_widget_label),
                    infoSize = stringResource(R.string.khatam_widget_description),
                    infoIcon = Icons.Default.MenuBook,
                    preview = { KhatamWidgetPreview() }
                )
            }

            // How to Add Widgets
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widgets_how_to),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                HowToAddCard()
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WidgetSection(
    title: String,
    infoName: String,
    infoSize: String,
    infoIcon: ImageVector,
    preview: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Widget preview container
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            style = NimazCardStyle.GRADIENT,
            shape = RoundedCornerShape(24.dp),
            gradient = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                preview()
            }
        }

        // Widget info row
        NimazCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            tone = NimazTone.NEUTRAL,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    NimazIcon(
                        imageVector = infoIcon,
                        contentDescription = null,
                        variant = NimazIconVariant.MUTED,
                        iconSize = 22.dp
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = infoName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = infoSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NextPrayerWidgetPreview(
    data: WidgetPreviewData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(160.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.widget_next_prayer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.nextPrayerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = data.nextPrayerTime,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.widget_countdown_format, data.countdown),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PrayerTimesWidgetPreview(
    data: WidgetPreviewData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.locationName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${data.hijriDay} ${data.hijriMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = data.nextPrayerName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.widget_countdown_format, data.countdown),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prayer times row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val sunriseShort = stringResource(R.string.widget_prayer_short_sunrise)
                data.prayers.filter { it.name != sunriseShort }.forEach { prayer ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = prayer.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prayer.isPassed)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = prayer.time,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (prayer.isPassed)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else if (prayer.isNext)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HijriDateWidgetPreview(
    data: WidgetPreviewData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(160.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.dayOfWeek,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.hijriDay.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${data.hijriMonth} ${data.hijriYear}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.gregorianDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PrayerTrackerWidgetPreview(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "3/5",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Checkboxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Sample data: Fajr, Dhuhr, Asr checked; Maghrib, Isha unchecked
                listOf(
                    stringResource(R.string.widget_prayer_initial_fajr) to true,
                    stringResource(R.string.widget_prayer_initial_dhuhr) to true,
                    stringResource(R.string.widget_prayer_initial_asr) to true,
                    stringResource(R.string.widget_prayer_initial_maghrib) to false,
                    stringResource(R.string.widget_prayer_initial_isha) to false
                ).forEach { (name, isChecked) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NimazCheckbox(
                            checked = isChecked,
                            onCheckedChange = null,
                            variant = NimazCheckboxVariant.SUCCESS,
                            size = NimazCheckboxSize.LARGE,
                            type = NimazCheckboxType.CIRCLE
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChecked)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KhatamWidgetPreview(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.khatam_widget_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "42%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { 0.42f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.khatam_juz_position, 13),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.khatam_ayahs_remaining, 3617, 3617),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun HijriCalendarWidgetPreview(
    data: WidgetPreviewData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left side: Calendar grid
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .padding(end = 8.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${data.hijriMonth} ${data.hijriYear}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = data.gregorianDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Day-of-week labels (locale-aware narrow names, Sunday first).
                // Read the locale observably via LocalConfiguration so the labels
                // recompose when the app language changes, rather than reading a
                // static Locale.getDefault() (lint: NonObservableLocale).
                val locale = LocalConfiguration.current.locales[0]
                val weekdayInitials = remember(locale) {
                    listOf(
                        java.time.DayOfWeek.SUNDAY,
                        java.time.DayOfWeek.MONDAY,
                        java.time.DayOfWeek.TUESDAY,
                        java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY,
                        java.time.DayOfWeek.FRIDAY,
                        java.time.DayOfWeek.SATURDAY,
                    ).map { it.getDisplayName(java.time.format.TextStyle.NARROW, locale) }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdayInitials.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Calendar grid — each row uses weight(1f) to fill vertical space
                val totalCells = data.firstDayOfWeekOffset + data.daysInMonth
                val totalRows = (totalCells + 6) / 7
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - data.firstDayOfWeekOffset + 1

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..data.daysInMonth) {
                                    val isToday = dayNumber == data.hijriDay
                                    if (isToday) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Right side: Events panel
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.today),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = data.hijriDay.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (data.todayEvents.isEmpty()) {
                    Text(
                        text = stringResource(R.string.widget_no_events),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    data.todayEvents.forEach { (name, type) ->
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HowToAddCard(modifier: Modifier = Modifier) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tone = NimazTone.NEUTRAL,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val steps = listOf(
                stringResource(R.string.widgets_how_to_step_1),
                stringResource(R.string.widgets_how_to_step_2),
                stringResource(R.string.widgets_how_to_step_3),
                stringResource(R.string.widgets_how_to_step_4)
            )

            steps.forEachIndexed { index, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/** The stored location the widget preview is rendered for. Read once per screen. */
private data class PreviewLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String,
)

/**
 * The location backing the widget previews, from the stored preferences.
 *
 * Pure, so it can be `remember`ed against the preferences rather than run in a `LaunchedEffect`.
 * The `try` is kept: `resolveLocation` and the name split are the parts that could throw on
 * malformed stored values, and a preview is not worth taking the screen down for.
 */
private fun previewLocationOf(userPrefs: UserPreferences): PreviewLocation {
    return try {
        val resolved = resolveLocation(userPrefs.latitude, userPrefs.longitude)
        PreviewLocation(
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            name = userPrefs.locationName.takeIf { it.isNotBlank() }
                ?.split(",")?.firstOrNull()?.trim() ?: "Dublin",
        )
    } catch (_: Exception) {
        PreviewLocation(FallbackLocation.LATITUDE, FallbackLocation.LONGITUDE, "Dublin")
    }
}

/**
 * Build the widget preview for "now". Pure and non-suspending: it runs once a
 * second to advance the countdown, so it must not touch DataStore.
 */
private fun buildWidgetPreviewData(
    context: android.content.Context,
    location: PreviewLocation,
    calculator: PrayerTimeCalculator,
): WidgetPreviewData {
    return try {
        val latitude = location.latitude
        val longitude = location.longitude
        val locationName = location.name

        val prayerTimes = calculator.getPrayerTimes(latitude, longitude)

        val currentTime = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val localTime = currentTime.toLocalDateTime(timeZone)

        // Build prayer list (excluding Sunrise for the main 5 prayers display)
        val prayers = prayerTimes.mapNotNull { prayerTime ->
            val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
            val isPassed = prayerLocalTime.time < localTime.time
            val name = prayerTime.type.displayName

            // Skip sunrise for main prayer list
            if (name.lowercase() == "sunrise") return@mapNotNull null

            PrayerPreview(
                name = context.prayerShortName(name),
                time = formatWidgetTime(prayerLocalTime.hour, prayerLocalTime.minute),
                isPassed = isPassed,
                isNext = false
            )
        }

        // Find next prayer
        val nextPrayerIndex = prayerTimes.indexOfFirst { prayerTime ->
            val prayerLocalTime = prayerTime.time.toLocalDateTime(timeZone)
            prayerLocalTime.time > localTime.time
        }

        val nextPrayer = prayerTimes.getOrNull(nextPrayerIndex)
        val nextPrayerName = nextPrayer?.type?.displayName
            ?: context.getString(R.string.prayer_fajr)
        val nextPrayerLocalTime = nextPrayer?.time?.toLocalDateTime(timeZone)
        val nextPrayerTimeStr = nextPrayerLocalTime?.let {
            formatWidgetTime(it.hour, it.minute)
        } ?: "—"

        // Calculate countdown
        val countdown = if (nextPrayer != null) {
            val diff: Duration = nextPrayer.time - currentTime
            val totalSeconds = diff.inWholeSeconds
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            when {
                hours > 0 -> context.getString(R.string.widget_countdown_hm_format, hours, minutes)
                minutes > 0 -> context.getString(
                    R.string.widget_countdown_ms_format,
                    minutes,
                    seconds
                )

                else -> context.getString(R.string.widget_countdown_s_format, seconds)
            }
        } else "—"

        // Mark next prayer in the list
        val prayersWithNext = prayers.map { prayer ->
            prayer.copy(isNext = prayer.name == context.prayerShortName(nextPrayerName))
        }

        // Get dates
        val hijriDate = HijriDateCalculator.today()
        val today = LocalDate.now()
        val gregorianDate = "${today.dayOfMonth} ${
            today.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        }"
        val dayOfWeek = today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // Calendar grid data
        val daysInMonth = HijriDateCalculator.getDaysInHijriMonth(hijriDate.year, hijriDate.month)
        val firstOfMonth = HijriDateCalculator.toGregorian(1, hijriDate.month, hijriDate.year)
        val javaDow = firstOfMonth.dayOfWeek.value // 1=Mon..7=Sun
        val firstDayOfWeekOffset = if (javaDow == 7) 0 else javaDow // Sun=0, Mon=1..Sat=6

        // Today's events
        val allEvents = HijriDateCalculator.getIslamicEvents(hijriDate.year)
        val todayEvents = allEvents
            .filter { it.day == hijriDate.day && it.month == hijriDate.month }
            .map {
                it.name to it.type.name.replace("_", " ").lowercase()
                    .replaceFirstChar { c -> c.uppercase() }
            }

        WidgetPreviewData(
            nextPrayerName = nextPrayerName,
            nextPrayerTime = nextPrayerTimeStr,
            countdown = countdown,
            prayers = prayersWithNext,
            hijriDate = "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year}",
            hijriDay = hijriDate.day,
            hijriMonth = hijriDate.monthName,
            hijriYear = hijriDate.year,
            gregorianDate = gregorianDate,
            dayOfWeek = dayOfWeek,
            locationName = locationName,
            daysInMonth = daysInMonth,
            firstDayOfWeekOffset = firstDayOfWeekOffset,
            todayEvents = todayEvents
        )
    } catch (e: Exception) {
        // Return fallback data
        WidgetPreviewData(
            nextPrayerName = context.getString(R.string.prayer_maghrib),
            nextPrayerTime = "6:15 PM",
            countdown = "2h 30m",
            prayers = listOf(
                PrayerPreview(
                    context.getString(R.string.widget_prayer_short_fajr),
                    "5:30",
                    isPassed = true
                ),
                PrayerPreview(
                    context.getString(R.string.widget_prayer_short_dhuhr),
                    "12:45",
                    isPassed = true
                ),
                PrayerPreview(
                    context.getString(R.string.widget_prayer_short_asr),
                    "3:30",
                    isPassed = true
                ),
                PrayerPreview(
                    context.getString(R.string.widget_prayer_short_maghrib),
                    "6:15",
                    isNext = true
                ),
                PrayerPreview(context.getString(R.string.widget_prayer_short_isha), "7:45")
            ),
            hijriDate = "15 Rajab 1446",
            hijriDay = 15,
            hijriMonth = "Rajab",
            hijriYear = 1446,
            gregorianDate = "28 Jan",
            dayOfWeek = "Tuesday",
            locationName = "Dublin"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Next Prayer Widget Preview")
@Composable
private fun NextPrayerWidgetPreviewDemo() {
    NimazTheme {
        NextPrayerWidgetPreview(
            data = WidgetPreviewData(
                nextPrayerName = "Maghrib",
                nextPrayerTime = "6:15 PM",
                countdown = "2h 30m"
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Prayer Times Widget Preview")
@Composable
private fun PrayerTimesWidgetPreviewDemo() {
    NimazTheme {
        PrayerTimesWidgetPreview(
            data = WidgetPreviewData(
                locationName = "Dublin",
                hijriDay = 15,
                hijriMonth = "Rajab",
                nextPrayerName = "Maghrib",
                countdown = "2h 30m",
                prayers = listOf(
                    PrayerPreview("Fajr", "5:30", isPassed = true),
                    PrayerPreview("Dhuhr", "12:45", isPassed = true),
                    PrayerPreview("Asr", "3:30", isPassed = true),
                    PrayerPreview("Mgrb", "6:15", isNext = true),
                    PrayerPreview("Isha", "7:45")
                )
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Hijri Date Widget Preview")
@Composable
private fun HijriDateWidgetPreviewDemo() {
    NimazTheme {
        HijriDateWidgetPreview(
            data = WidgetPreviewData(
                dayOfWeek = "Tuesday",
                hijriDay = 15,
                hijriMonth = "Rajab",
                hijriYear = 1446,
                gregorianDate = "28 Jan"
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Prayer Tracker Widget Preview")
@Composable
private fun PrayerTrackerWidgetPreviewDemo() {
    NimazTheme {
        PrayerTrackerWidgetPreview()
    }
}

@Preview(showBackground = true, widthDp = 400, name = "How To Add Card")
@Composable
private fun HowToAddCardPreview() {
    NimazTheme {
        HowToAddCard()
    }
}
