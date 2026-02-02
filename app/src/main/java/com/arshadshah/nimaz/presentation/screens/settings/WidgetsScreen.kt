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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.core.util.HijriDateCalculator
import com.arshadshah.nimaz.core.util.PrayerTimeCalculator
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration

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

    // State for dynamic widget preview data
    var previewData by remember { mutableStateOf(WidgetPreviewData()) }

    // Load real prayer times data
    LaunchedEffect(Unit) {
        while (true) {
            previewData = loadWidgetPreviewData(context)
            delay(1000) // Update every second for countdown
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NimazBackTopAppBar(
                title = "Widgets",
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
                    text = "Add Nimaz widgets to your home screen for quick access to prayer times without opening the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            // Next Prayer Widget (2x2)
            item {
                WidgetSection(
                    title = "Next Prayer Widget (2\u00D72)",
                    infoName = "Next Prayer",
                    infoSize = "2\u00D72 \u2022 Shows countdown to next prayer",
                    infoIcon = Icons.Default.Schedule,
                    preview = { NextPrayerWidgetPreview(previewData) }
                )
            }

            // Prayer Times Widget (4x1)
            item {
                WidgetSection(
                    title = "Prayer Times Widget (4\u00D71)",
                    infoName = "Prayer Times",
                    infoSize = "4\u00D71 \u2022 All prayers + countdown",
                    infoIcon = Icons.Default.ListAlt,
                    preview = { PrayerTimesWidgetPreview(previewData) }
                )
            }

            // Hijri Date Widget (2x2)
            item {
                WidgetSection(
                    title = "Hijri Date Widget (2\u00D72)",
                    infoName = "Hijri Date",
                    infoSize = "2\u00D72 \u2022 Islamic calendar date",
                    infoIcon = Icons.Default.CalendarMonth,
                    preview = { HijriDateWidgetPreview(previewData) }
                )
            }

            // Prayer Tracker Widget (4x1)
            item {
                WidgetSection(
                    title = "Prayer Tracker Widget (4\u00D71)",
                    infoName = "Prayer Tracker",
                    infoSize = "4\u00D71 \u2022 Tap to mark prayers completed",
                    infoIcon = Icons.Default.CheckCircle,
                    preview = { PrayerTrackerWidgetPreview() }
                )
            }

            // Hijri Calendar Widget (4x2)
            item {
                WidgetSection(
                    title = "Hijri Calendar Widget (4\u00D72)",
                    infoName = "Hijri Calendar",
                    infoSize = "4\u00D72 \u2022 Full month calendar + today\u2019s events",
                    infoIcon = Icons.Default.CalendarMonth,
                    preview = { HijriCalendarWidgetPreview(previewData) }
                )
            }

            // How to Add Widgets
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How to Add Widgets",
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            preview()
        }

        // Widget info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp)
                )
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
                Icon(
                    imageVector = infoIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
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
                text = "Next Prayer",
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
                    text = "in ${data.countdown}",
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
                        text = "in ${data.countdown}",
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
                data.prayers.filter { it.name != "Rise" }.forEach { prayer ->
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
    val checkedColor = Color(0xFF22C55E) // Green
    val uncheckedColor = MaterialTheme.colorScheme.surfaceVariant

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
                    text = "Today",
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
                    "F" to true,
                    "D" to true,
                    "A" to true,
                    "M" to false,
                    "I" to false
                ).forEach { (name, isChecked) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) checkedColor else uncheckedColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Text(
                                    text = "\u2713",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
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

                // Day-of-week labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
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
                    text = "Today",
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
                        text = "No events",
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val steps = listOf(
            "Long press on an empty area of your home screen",
            "Tap \"Widgets\" from the menu that appears",
            "Search for \"Nimaz\" and select your preferred widget",
            "Drag the widget to your desired location"
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

private suspend fun loadWidgetPreviewData(context: android.content.Context): WidgetPreviewData {
    return try {
        val prefs = PreferencesDataStore(context)
        val userPrefs = prefs.userPreferences.first()

        val latitude = userPrefs.latitude.takeIf { it != 0.0 } ?: 53.3498
        val longitude = userPrefs.longitude.takeIf { it != 0.0 } ?: -6.2603
        val locationName = userPrefs.locationName.takeIf { it.isNotBlank() }?.split(",")?.firstOrNull()?.trim()
            ?: "Dublin"

        val calculator = PrayerTimeCalculator()
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
                name = getShortPrayerName(name),
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
        val nextPrayerName = nextPrayer?.type?.displayName ?: "Fajr"
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
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        } else "—"

        // Mark next prayer in the list
        val prayersWithNext = prayers.map { prayer ->
            prayer.copy(isNext = prayer.name == getShortPrayerName(nextPrayerName))
        }

        // Get dates
        val hijriDate = HijriDateCalculator.today()
        val today = LocalDate.now()
        val gregorianDate = "${today.dayOfMonth} ${today.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
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
            .map { it.name to it.type.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } }

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
            nextPrayerName = "Maghrib",
            nextPrayerTime = "6:15 PM",
            countdown = "2h 30m",
            prayers = listOf(
                PrayerPreview("Fajr", "5:30", isPassed = true),
                PrayerPreview("Dhuhr", "12:45", isPassed = true),
                PrayerPreview("Asr", "3:30", isPassed = true),
                PrayerPreview("Mgrb", "6:15", isNext = true),
                PrayerPreview("Isha", "7:45")
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

private fun getShortPrayerName(name: String): String {
    return when (name.lowercase()) {
        "fajr" -> "Fajr"
        "sunrise" -> "Rise"
        "dhuhr" -> "Dhuhr"
        "asr" -> "Asr"
        "maghrib" -> "Mgrb"
        "isha" -> "Isha"
        else -> name.take(5)
    }
}

private fun formatWidgetTime(hour: Int, minute: Int): String {
    val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
    return String.format("%d:%02d", h, minute)
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
