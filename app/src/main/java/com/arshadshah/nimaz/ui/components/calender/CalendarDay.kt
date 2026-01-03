package com.arshadshah.nimaz.ui.components.calender


import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.canopas.lib.showcase.IntroShowcase
import com.canopas.lib.showcase.component.ShowcaseStyle
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDay(
    showcaseState: Boolean,
    onShowcaseDismiss: () -> Unit,
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFromCurrentMonth: Boolean,
    tracker: LocalPrayersTracker?,
    isMenstruating: Boolean,
    isFasting: Boolean,
    onDateClick: (LocalDate) -> Unit,
    onPrayerUpdate: (LocalDate, String, Boolean) -> Unit,
    onFastingUpdate: (LocalFastTracker) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("CalendarDay: showCaseState", showcaseState.toString())
    val hijriDate = remember(date) {
        HijrahDate.from(date)
    }

    val hijriDay = remember(hijriDate) {
        hijriDate[ChronoField.DAY_OF_MONTH]
    }

    val hijriMonth = remember(hijriDate) {
        hijriDate[ChronoField.MONTH_OF_YEAR]
    }

    val importantDay = remember(hijriDay, hijriMonth) {
        IslamicCalendarHelper.isImportantDay(hijriDay, hijriMonth)
    }

    val importanceLevel = remember(importantDay.second) {
        IslamicCalendarHelper.getImportanceLevel(importantDay.second)
    }

    // Dialog state for trackers and day details
    var showDetailsDialog by remember { mutableStateOf(false) }

    IntroShowcase(
        showIntroShowCase = isToday && !showcaseState,
        dismissOnClickOutside = true,
        onShowCaseCompleted = {
            onShowcaseDismiss()
        },
    ) {
        Surface(
            modifier = modifier
                .introShowCaseTarget(
                    index = 0,
                    style = ShowcaseStyle.Default.copy(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        backgroundAlpha = 0.98f,
                        targetCircleColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                    content = {
                        CalendarDayShowcase()
                    }
                )
                .clip(RoundedCornerShape(8.dp))
                .padding(1.dp)
                .border(
                    width = 1.dp,
                    color = when {
                        !isFromCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isToday -> MaterialTheme.colorScheme.secondaryContainer
                        isMenstruating -> Color(0xFFFFBCC2)
                        importanceLevel == ImportanceLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = isFromCurrentMonth) {
                    onDateClick(date)
                    showDetailsDialog = true
                },
            shape = RoundedCornerShape(8.dp),
            contentColor = when {
                !isFromCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                isMenstruating -> Color(0xFFFFBCC2)
                importanceLevel == ImportanceLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp), // Reduced padding
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status indicators at the top
                if (importantDay.first || isFasting) {
                    CompactStatusIndicators(
                        importantDay = importantDay,
                        isFasting = isFasting
                    )
                } else {
                    // Add a small spacer when no indicators to maintain consistent spacing
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Date display (Gregorian and Hijri)
                CompactDateDisplay(
                    date = date,
                    hijriDate = hijriDate,
                    isSelected = isSelected,
                    isToday = isToday
                )

                // Prayer progress indicators at the bottom
                if (tracker != null) {
                    CompactPrayerProgressIndicators(tracker = tracker)
                    Spacer(modifier = Modifier.height(2.dp))
                } else {
                    // Add a small spacer when no tracker to maintain spacing
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }


    // Detailed day tracker dialog
    if (showDetailsDialog && isFromCurrentMonth) {
        DayDetailsDialog(
            date = date,
            hijriDate = hijriDate,
            prayerTracker = tracker,
            isMenstruating = isMenstruating,
            isFasting = isFasting,
            importantDay = importantDay,
            onPrayerUpdate = onPrayerUpdate,
            onFastingUpdate = onFastingUpdate,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
fun DayDetailsDialog(
    date: LocalDate,
    hijriDate: HijrahDate,
    prayerTracker: LocalPrayersTracker?,
    isMenstruating: Boolean,
    isFasting: Boolean,
    importantDay: Pair<Boolean, String>,
    onPrayerUpdate: (LocalDate, String, Boolean) -> Unit,
    onFastingUpdate: (LocalFastTracker) -> Unit,
    onDismiss: () -> Unit
) {
    LocalContext.current
    date.isAfter(LocalDate.now())

    // Format dates
    val gregFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val hijriFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
    val formattedGregorianDate = date.format(gregFormatter)
    val formattedHijriDate = hijriDate.format(hijriFormatter)

    // Tab selection state
    val tabOptions = listOf("Prayers", "Fasting", "Info")
    var selectedTabIndex by remember { mutableStateOf(0) }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .scale(scale),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with dates and important day indicator
                DateHeaderSection(
                    gregorianDate = formattedGregorianDate,
                    hijriDate = formattedHijriDate,
                    importantDay = importantDay
                )

                // Tabs
                TabSelector(
                    options = tabOptions,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    showInfoTab = importantDay.first
                )

                // Content based on selected tab
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        when (selectedTabIndex) {
                            0 -> { // Prayers tab
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    EnhancedPrayersTracker(
                                        selectedDate = date,
                                        tracker = prayerTracker ?: LocalPrayersTracker(date),
                                        isMenstruating = isMenstruating,
                                        onPrayerUpdate = onPrayerUpdate
                                    )
                                }
                            }

                            1 -> { // Fasting tab
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    EnhancedFastingTracker(
                                        selectedDate = date,
                                        isMenstruating = isMenstruating,
                                        isFasting = isFasting,
                                        onFastingUpdate = onFastingUpdate
                                    )
                                }
                            }

                            2 -> { // Info tab (only visible for important days)
                                if (importantDay.first) {
                                    ImportantDayInfoContent(importantDay = importantDay)
                                }
                            }
                        }
                    }
                }

                // Actions row
                ActionsRow(onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun TabSelector(
    options: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    showInfoTab: Boolean
) {
    val visibleOptions = if (showInfoTab) options else options.dropLast(1)

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleOptions.forEachIndexed { index, tabName ->
                val isSelected = index == selectedTabIndex

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) },
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (index) {
                            0 -> painterResource(R.drawable.person_praying_icon)
                            1 -> painterResource(R.drawable.dark_icon)
                            2 -> painterResource(R.drawable.info_icon)
                            else -> null
                        }

                        if (icon != null) {
                            Icon(
                                painter = icon,
                                contentDescription = tabName,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = tabName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
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
private fun DateHeaderSection(
    gregorianDate: String,
    hijriDate: String,
    importantDay: Pair<Boolean, String>
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = hijriDate,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = gregorianDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ImportantDayInfoContent(importantDay: Pair<Boolean, String>) {
    val description = IslamicCalendarHelper.getImportantDayDescription(importantDay)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "About ${importantDay.second}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Suggested acts of worship for this day
        val suggestedActs = getSuggestedActsForDay(importantDay.second)
        if (suggestedActs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Recommended Acts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            suggestedActs.forEach { act ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rating_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = act,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

// Helper function to get suggested acts for each important day
private fun getSuggestedActsForDay(dayName: String): List<String> {
    return when {
        dayName.contains("Eid al-Fitr") -> listOf(
            "Give Zakat al-Fitr before Eid prayer",
            "Take a bath (ghusl) before prayer",
            "Wear your best clothes",
            "Recite takbeer on the way to prayer",
            "Use different routes to and from prayer"
        )

        dayName.contains("Eid al-Adha") -> listOf(
            "Perform Qurbani (sacrifice)",
            "Take a bath (ghusl) before prayer",
            "Recite takbeer on the way to prayer",
            "Distribute meat to family, neighbors and poor"
        )

        dayName.contains("Arafah") -> listOf(
            "Fast if not performing Hajj",
            "Make abundant dua",
            "Seek forgiveness"
        )

        dayName.contains("Ashura") -> listOf(
            "Fast on the 9th and 10th of Muharram",
            "Be generous to your family",
            "Increase in voluntary acts of worship"
        )

        dayName.contains("Laylatul Qadr") -> listOf(
            "Pray Tahajjud/night prayer",
            "Recite Quran",
            "Make abundant dua",
            "Recite the dua for Laylatul Qadr"
        )

        dayName.contains("Ramadan") -> listOf(
            "Fast from dawn to sunset",
            "Pray Taraweeh",
            "Read Quran",
            "Give charity"
        )

        else -> emptyList()
    }
}

@Composable
private fun ActionsRow(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Preview
@Composable
fun TabbedDayDetailsDialogPreview() {
    val date = LocalDate.now()
    val hijriDate = HijrahDate.from(date)
    val importantDay = Pair(true, "Day of Arafah")

    NimazTheme {
        DayDetailsDialog(
            date = date,
            hijriDate = hijriDate,
            prayerTracker = LocalPrayersTracker(
                date = date,
                fajr = true,
                dhuhr = true,
                asr = false,
                maghrib = false,
                isha = false
            ),
            isMenstruating = false,
            isFasting = true,
            importantDay = importantDay,
            onPrayerUpdate = { _, _, _ -> },
            onFastingUpdate = { },
            onDismiss = { }
        )
    }
}

@Preview
@Composable
fun DayDetailsDialogPreview() {
    val date = LocalDate.now()
    val hijriDate = HijrahDate.from(date)
    val importantDay = Pair(true, "Day of Arafah")

    NimazTheme {
        DayDetailsDialog(
            date = date,
            hijriDate = hijriDate,
            prayerTracker = LocalPrayersTracker(
                date = date,
                fajr = true,
                dhuhr = true,
                asr = false,
                maghrib = false,
                isha = false
            ),
            isMenstruating = false,
            isFasting = true,
            importantDay = importantDay,
            onPrayerUpdate = { _, _, _ -> },
            onFastingUpdate = { },
            onDismiss = { }
        )
    }
}

@Preview
@Composable
fun DayDetailsDialogRegularDayPreview() {
    val date = LocalDate.now()
    val hijriDate = HijrahDate.from(date)
    val importantDay = Pair(false, "")

    NimazTheme {
        DayDetailsDialog(
            date = date,
            hijriDate = hijriDate,
            prayerTracker = LocalPrayersTracker(
                date = date,
                fajr = true,
                dhuhr = false,
                asr = false,
                maghrib = false,
                isha = false
            ),
            isMenstruating = false,
            isFasting = false,
            importantDay = importantDay,
            onPrayerUpdate = { _, _, _ -> },
            onFastingUpdate = { },
            onDismiss = { }
        )
    }
}

@Composable
fun EnhancedPrayersTracker(
    selectedDate: LocalDate,
    tracker: LocalPrayersTracker,
    isMenstruating: Boolean,
    onPrayerUpdate: (LocalDate, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val prayers = listOf(
        PRAYER_NAME_FAJR to tracker.fajr,
        PRAYER_NAME_DHUHR to tracker.dhuhr,
        PRAYER_NAME_ASR to tracker.asr,
        PRAYER_NAME_MAGHRIB to tracker.maghrib,
        PRAYER_NAME_ISHA to tracker.isha
    )

    val progress = prayers.count { it.second }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prayer Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$progress/5",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Prayer checkbox grid
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                prayers.forEach { (name, completed) ->
                    EnhancedPrayerCheckbox(
                        name = name,
                        isCompleted = completed,
                        enabled = !isMenstruating,
                        onStatusChange = { isChecked ->
                            onPrayerUpdate(selectedDate, name, isChecked)
                        }
                    )
                }
            }

            if (isMenstruating) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Prayers tracking is disabled during menstruation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPrayerCheckbox(
    name: String,
    isCompleted: Boolean,
    enabled: Boolean,
    onStatusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onStatusChange(!isCompleted) },
        enabled = enabled,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isCompleted) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Surface(
                shape = CircleShape,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    isCompleted -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (!isCompleted && enabled)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                else null,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedFastingTracker(
    selectedDate: LocalDate,
    isMenstruating: Boolean,
    isFasting: Boolean,
    onFastingUpdate: (LocalFastTracker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAfterToday = selectedDate.isAfter(LocalDate.now())
    val formatter = DateTimeFormatter.ofPattern("dd MMM")

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Fasting Status",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                onClick = {
                    when {
                        isMenstruating -> {
                            Toasty.info(
                                context,
                                "Cannot track fasting during menstruation",
                                Toasty.LENGTH_SHORT
                            ).show()
                        }

                        isAfterToday -> {
                            Toasty.warning(
                                context,
                                "Cannot track fasting for future dates",
                                Toasty.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            onFastingUpdate(
                                LocalFastTracker(
                                    date = selectedDate,
                                    isFasting = !isFasting
                                )
                            )
                        }
                    }
                },
                enabled = !isMenstruating && !isAfterToday,
                color = when {
                    !(!isMenstruating && !isAfterToday) -> MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.5f
                    )

                    isFasting -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (isFasting) 4.dp else 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                selectedDate.isBefore(LocalDate.now()) -> if (isFasting)
                                    "Fasted on ${formatter.format(selectedDate)}"
                                else
                                    "Did not fast"

                                selectedDate.isEqual(LocalDate.now()) -> if (isFasting)
                                    "Fasting today"
                                else
                                    "Not fasting today"

                                else -> "Cannot track future dates"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isFasting) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isAfterToday -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                isFasting -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (isMenstruating) {
                            Text(
                                text = "Tracking disabled during menstruation",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }

                    EnhancedFastingIndicator(
                        isFasting = isFasting,
                        enabled = !isMenstruating && !isAfterToday
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedFastingIndicator(
    isFasting: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            isFasting -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (!isFasting && enabled)
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        else null,
        modifier = modifier.size(width = 60.dp, height = 32.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = if (isFasting) "Yes" else "No",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    isFasting -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun CompactStatusIndicators(
    importantDay: Pair<Boolean, String>,
    isFasting: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .height(if (importantDay.first && isFasting) 24.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (importantDay.first) {
            CompactStatusChip(
                text = importantDay.second,
                color = IslamicCalendarHelper.getImportantDayColor(importantDay),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isFasting) {
            CompactStatusChip(
                text = "Fasted",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CompactStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val displayText = when {
        text.length > 12 -> text.take(10) + ".."
        else -> text
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            width = 0.5.dp,
            color = color.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.sp
            ),
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactDateDisplay(
    date: LocalDate,
    hijriDate: HijrahDate,
    isSelected: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Gregorian day
        Surface(
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday || isSelected)
                        FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Hijri day
        Text(
            text = hijriDate.format(DateTimeFormatter.ofPattern("d")),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp
            ),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun CompactPrayerProgressIndicators(
    tracker: LocalPrayersTracker,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactPrayerDot(completed = tracker.fajr)
        CompactPrayerDot(completed = tracker.dhuhr)
        CompactPrayerDot(completed = tracker.asr)
        CompactPrayerDot(completed = tracker.maghrib)
        CompactPrayerDot(completed = tracker.isha)
    }
}

@Composable
private fun CompactPrayerDot(completed: Boolean) {
    Surface(
        color = if (completed)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(4.dp),
        tonalElevation = if (completed) 1.dp else 0.dp
    ) {
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun CalendarDayShowcase(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Calendar Day",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap to view prayer and fasting details",
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = "See important Islamic dates and track your worship",
            color = Color.White,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

enum class ImportanceLevel {
    HIGH, // For days like Eid, Laylatul Qadr
    MEDIUM, // For days like Ashura, Islamic New Year
    REGULAR // For regular days in Ramadan, Dhul Hijjah
}

object IslamicCalendarHelper {

    // Function to check if a day is an important Islamic day
    fun isImportantDay(day: Int, month: Int): Pair<Boolean, String> {
        return when (month) {
            1 -> when (day) { // Muharram
                1 -> Pair(true, "Islamic New Year")
                10 -> Pair(true, "Day of Ashura")
                else -> Pair(false, "")
            }

            3 -> when (day) { // Rabi' al-Awwal
                12 -> Pair(true, "Mawlid an-Nabi ﷺ")
                else -> Pair(false, "")
            }

            7 -> when (day) { // Rajab
                27 -> Pair(true, "Al-Isra' wal-Mi'raj")
                else -> Pair(false, "")
            }

            8 -> when (day) { // Sha'ban
                15 -> Pair(true, "Laylat al-Bara'at")
                else -> Pair(false, "")
            }

            9 -> getRamadanDay(day) // Ramadan
            10 -> when (day) { // Shawwal
                1 -> Pair(true, "Eid al-Fitr")
                else -> Pair(false, "")
            }

            12 -> getDhulHijjahDay(day) // Dhul Hijjah
            else -> Pair(false, "")
        }
    }

    // Helper function to get Ramadan day description
    private fun getRamadanDay(day: Int): Pair<Boolean, String> {
        return when (day) {
            1 -> Pair(true, "Ramadan Begins")
            in 2..20 -> Pair(true, "Ramadan Day $day")
            in 21..30 -> when (day) {
                21, 23, 25, 27, 29 -> Pair(true, "Laylatul Qadr")
                else -> Pair(true, "Last 10 Days of Ramadan")
            }

            else -> Pair(false, "")
        }
    }

    // Helper function to get Dhul Hijjah day description
    private fun getDhulHijjahDay(day: Int): Pair<Boolean, String> {
        return when (day) {
            1 -> Pair(true, "Dhul Hijjah Begins")
            in 2..7 -> Pair(true, "Dhul Hijjah Day $day")
            8 -> Pair(true, "Yawm at-Tarwiyah")
            9 -> Pair(true, "Day of Arafah")
            10 -> Pair(true, "Eid al-Adha")
            11, 12, 13 -> Pair(true, "Days of Tashreeq")
            else -> Pair(false, "")
        }
    }

    // Get detailed description for important days
    @Composable
    fun getImportantDayDescription(importantDay: Pair<Boolean, String>): String {
        return when (importantDay.second) {
            // Major Festivals
            "Eid al-Fitr" -> "The blessed festival marking the end of Ramadan. A day of celebration, gratitude, and giving (Zakat al-Fitr). The takbeer is recited, and Muslims gather for Eid prayer followed by festivities with family and community."

            "Eid al-Adha" -> "The festival of sacrifice commemorating Prophet Ibrahim's (AS) devotion to Allah. Muslims who can afford it sacrifice an animal and distribute the meat to family, neighbors, and those in need. The day begins with Eid prayer and takbeer."

            // Ramadan Related
            "Ramadan Begins" -> "The blessed month of fasting begins. Muslims fast from dawn (Fajr) to sunset (Maghrib), increase in worship, recite Quran, and give in charity. The nights are marked by special Taraweeh prayers."

            "Laylatul Qadr" -> "The Night of Power, better than a thousand months (Surah Al-Qadr). It commemorates the night when the Quran's revelation began. Muslims spend this night in prayer, seeking Allah's forgiveness and mercy."

            // Hajj Related
            "Dhul Hijjah Begins" -> "The sacred month of Hajj begins. Muslims not performing Hajj are encouraged to fast the first 9 days, especially the Day of Arafah."

            "Yawm at-Tarwiyah" -> "The first day of Hajj when pilgrims begin their journey to Mina, preparing for the Day of Arafah."

            "Day of Arafah" -> "The most virtuous day of the year. Pilgrims gather at Mount Arafah, while other Muslims are encouraged to fast. It's a day of acceptance of duas and forgiveness of sins."

            "Days of Tashreeq" -> "The days following Eid al-Adha when pilgrims complete their Hajj rites. These are days of remembrance of Allah and celebration."

            // Sacred Days and Nights
            "Islamic New Year" -> "Marks the beginning of the Islamic lunar calendar (Hijri year), commemorating the Prophet's ﷺ migration (Hijra) from Makkah to Madinah."

            "Day of Ashura" -> "The 10th of Muharram, commemorating Allah saving Prophet Musa (AS) and his followers. Prophet Muhammad ﷺ recommended fasting on this day and the day before or after."

            "Mawlid an-Nabi ﷺ" -> "Commemorating the birth of Prophet Muhammad ﷺ. A time to learn about and reflect on his noble character, teachings, and the mercy he brought to mankind."

            "Al-Isra' wal-Mi'raj" -> "The miraculous night journey of Prophet Muhammad ﷺ from Makkah to Jerusalem and his ascension through the heavens, where the five daily prayers were prescribed."

            "Laylat al-Bara'at" -> "The night of forgiveness in mid-Sha'ban. Muslims seek Allah's forgiveness and mercy, as it's said that Allah descends to the lowest heaven and accepts repentance."

            // Regular Days
            "Last 10 Days of Ramadan" -> "The most blessed nights of Ramadan, which include Laylatul Qadr. Muslims increase in worship, many perform I'tikaf (spiritual retreat in the mosque)."

            else -> when {
                importantDay.second.startsWith("Ramadan Day") -> "A blessed day of fasting, increased worship, and spiritual growth in the month of Ramadan."
                importantDay.second.startsWith("Dhul Hijjah Day") -> "One of the blessed first ten days of Dhul Hijjah, recommended for increased worship and voluntary fasting."
                else -> "A blessed day in the Islamic calendar."
            }
        }
    }

    // Get color scheme for important days
    @Composable
    fun getImportantDayColor(importantDay: Pair<Boolean, String>): Color {
        return when {
            // Eid Days - Gold/Primary
            importantDay.second.contains("Eid") ->
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)

            // Ramadan - Purple/Tertiary
            importantDay.second.contains("Ramadan") ||
                    importantDay.second.contains("Laylatul Qadr") ->
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)

            // Hajj and Dhul Hijjah - Green
            importantDay.second.contains("Dhul Hijjah") ||
                    importantDay.second.contains("Arafah") ||
                    importantDay.second.contains("Tashreeq") ||
                    importantDay.second.contains("Tarwiyah") ->
                Color(0xFF4CAF50).copy(alpha = 0.8f)

            // Sacred Nights - Deep Blue
            importantDay.second.contains("Bara'at") ||
                    importantDay.second.contains("Mi'raj") ->
                Color(0xFF1976D2).copy(alpha = 0.8f)

            // Historical Days - Orange
            importantDay.second.contains("Ashura") ||
                    importantDay.second.contains("Islamic New Year") ||
                    importantDay.second.contains("Mawlid") ->
                Color(0xFFFF9800).copy(alpha = 0.8f)

            // Default
            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
        }
    }

    // Get importance level of the day
    fun getImportanceLevel(dayName: String): ImportanceLevel {
        return when {
            // HIGH importance days
            dayName.contains("Eid") ||
                    dayName.contains("Laylatul Qadr") ||
                    dayName == "Day of Arafah" -> ImportanceLevel.HIGH

            // MEDIUM importance days
            dayName.contains("Islamic New Year") ||
                    dayName.contains("Ashura") ||
                    dayName.contains("Mi'raj") ||
                    dayName.contains("Mawlid") ||
                    dayName.contains("Bara'at") ||
                    dayName == "Ramadan Begins" -> ImportanceLevel.MEDIUM

            // REGULAR importance days
            else -> ImportanceLevel.REGULAR
        }
    }

}

//EnhancedPrayersTracker
@Preview
@Composable
fun EnhancedPrayersTrackerPreview() {
    NimazTheme {
        EnhancedPrayersTracker(
            selectedDate = LocalDate.now(),
            tracker = LocalPrayersTracker(
                date = LocalDate.now(),
                fajr = true,
                dhuhr = true,
                asr = true,
                maghrib = true,
                isha = true
            ),
            isMenstruating = false,
            onPrayerUpdate = { _, _, _ -> }
        )
    }
}

//EnhancedFastingTracker
@Preview
@Composable
fun EnhancedFastingTrackerPreview() {
    NimazTheme {
        EnhancedFastingTracker(
            selectedDate = LocalDate.now(),
            isMenstruating = false,
            isFasting = true,
            onFastingUpdate = { }
        )
    }
}

//day
@Preview
@Composable
fun CalendarDayPreview() {
    NimazTheme {
        Column(
            modifier = Modifier
                .height(68.dp)
                .width(48.dp)
        ) {
            CalendarDay(
                showcaseState = false,
                onShowcaseDismiss = { },
                date = LocalDate.now(),
                isSelected = false,
                isToday = false,
                isFromCurrentMonth = true,
                tracker = LocalPrayersTracker(
                    date = LocalDate.now(),
                    fajr = true,
                    dhuhr = true,
                    asr = true,
                    maghrib = true,
                    isha = true
                ),
                isMenstruating = false,
                isFasting = true,
                onDateClick = { },
                onPrayerUpdate = { _, _, _ -> },
                onFastingUpdate = { }
            )
        }
    }
}