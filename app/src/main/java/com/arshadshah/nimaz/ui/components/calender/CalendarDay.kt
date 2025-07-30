package com.arshadshah.nimaz.ui.components.calender


import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.canopas.lib.showcase.component.ShowcaseStyle
import com.canopas.lib.showcase.introShowCaseTarget
import java.time.LocalDate
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.WeekFields

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDay(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isFromCurrentMonth: Boolean,
    tracker: LocalPrayersTracker?,
    isMenstruating: Boolean,
    isFasting: Boolean,
    onDateClick: (LocalDate) -> Unit,
    shouldShowShowcase: Boolean = false,
    modifier: Modifier = Modifier
) {

    Log.d("CalendarDay: shouldShowShowcase", shouldShowShowcase.toString())
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

    val showDialog = remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .let { mod ->
                if (shouldShowShowcase) {
                    mod.introShowCaseTarget(
                        index = 0,
                        style = ShowcaseStyle.Default.copy(
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            backgroundAlpha = 0.98f,
                            targetCircleColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        // specify the content to show to introduce app feature
                        content = {
                            CalendarDayShowcase()
                        }
                    )
                } else {
                    mod
                }
            }
            .aspectRatio(0.7f)
            .clip(MaterialTheme.shapes.small)
            .padding(1.dp)
            .border(
                width = 3.dp,
                color = when {
                    !isFromCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    isMenstruating -> Color(0xFFFFBCC2)
                    importanceLevel == ImportanceLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.small
            )
            .combinedClickable(
                enabled = isFromCurrentMonth,
                onClick = { onDateClick(date) },
                onLongClick = {
                    if (importantDay.first) {
                        showDialog.value = true
                    }
                }
            ),
        shape = MaterialTheme.shapes.small,
        contentColor = when {
            !isFromCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.onSecondaryContainer
            isMenstruating -> Color(0xFFFFBCC2)
            importanceLevel == ImportanceLevel.HIGH -> MaterialTheme.colorScheme.onTertiaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                if (importantDay.first || isFasting) {
                    StatusIndicators(
                        importantDay = importantDay,
                        isFasting = isFasting
                    )
                }

                DateDisplay(
                    date = date,
                    hijriDate = hijriDate,
                    isSelected = isSelected,
                    isToday = isToday
                )

                if (tracker != null) {
                    PrayerProgressIndicators(tracker = tracker)
                }
            }
        }
    }

    if (showDialog.value) {
        ImportantDayDialog(
            title = importantDay.second,
            onDismiss = { showDialog.value = false }
        )
    }
}

@Composable
private fun StatusIndicators(
    importantDay: Pair<Boolean, String>,
    isFasting: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (importantDay.first) {
            StatusChip(
                text = importantDay.second,
                color = IslamicCalendarHelper.getImportantDayColor(importantDay),
                modifier = Modifier.weight(1f)
            )
        }
        if (isFasting) {
            StatusChip(
                text = "Fasted",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateDisplay(
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
        Surface(
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isToday || isSelected)
                            FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Text(
            text = hijriDate.format(DateTimeFormatter.ofPattern("d")),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun PrayerProgressIndicators(
    tracker: LocalPrayersTracker,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PrayerDot(completed = tracker.fajr)
        PrayerDot(completed = tracker.dhuhr)
        PrayerDot(completed = tracker.asr)
        PrayerDot(completed = tracker.maghrib)
        PrayerDot(completed = tracker.isha)
    }
}

@Composable
private fun PrayerDot(completed: Boolean) {
    Surface(
        color = if (completed)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.size(width = 4.dp, height = 4.dp),
        tonalElevation = if (completed) 2.dp else 0.dp
    ) {
        Spacer(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ImportantDayDialog(
    title: String,
    onDismiss: () -> Unit
) {
    AlertDialogNimaz(
        title = title,
        onDismissRequest = onDismiss,
        contentDescription = "Important Day Description",
        confirmButtonText = "Close",
        showDismissButton = false,
        onConfirm = onDismiss,
        onDismiss = {},
        contentToShow = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = IslamicCalendarHelper.getImportantDayDescription(
                            Pair(true, title)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

@Preview(showBackground = true)
@Composable
fun CalendarDayPreview() {
    val weekFields = remember { WeekFields.ISO }
    val days = remember { generateDaysForMonth(YearMonth.now(), weekFields) }
    NimazTheme {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(max(100.dp, 500.dp)),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(days, key = { it.date.toString() }) { dayInfo ->
                CalendarDay(
                    date = dayInfo.date,
                    isSelected = dayInfo.date == LocalDate.now(),
                    isToday = dayInfo.date == LocalDate.now(),
                    isFromCurrentMonth = dayInfo.isFromCurrentMonth,
                    tracker = LocalPrayersTracker(
                        date = dayInfo.date,
                        fajr = true,
                        dhuhr = true,
                        asr = true,
                        maghrib = true,
                        isha = true
                    ),
                    isMenstruating = false,
                    isFasting = true,
                    onDateClick = {},
                    shouldShowShowcase = dayInfo.date == LocalDate.now(),
                )
            }
        }
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
            text = "Tap to select date and view prayer progress",
            color = Color.White,
            fontSize = 16.sp
        )
        Text(
            text = "Long press to view details of important days they are highlighted in the calendar",
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}