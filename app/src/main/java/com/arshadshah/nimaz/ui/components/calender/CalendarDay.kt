package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import kotlin.reflect.KFunction1

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarDay(
    dayState: DayState<DynamicSelectionState>,
    handleEvents: KFunction1<LocalDate, Unit>,
    progressForMonth: State<List<LocalPrayersTracker>>,
    fastProgressForMonth: State<List<LocalFastTracker>>,
    modifier: Modifier = Modifier
) {
    // Get Hijri date information
    val hijriDay = HijrahDate.from(dayState.date)
    val currentDate = dayState.date
    val today = dayState.isCurrentDay
    val isSelectedDay = dayState.selectionState.isDateSelected(currentDate)
    val hasDescription = remember { mutableStateOf(false) }

    // Get Hijri calendar specific data
    val hijriMonth = hijriDay[ChronoField.MONTH_OF_YEAR]
    val hijriDayOfMonth = hijriDay[ChronoField.DAY_OF_MONTH]

    // Check for important Islamic day
    val importantDay = IslamicCalendarHelper.isImportantDay(hijriDayOfMonth, hijriMonth)
    val importanceLevel = IslamicCalendarHelper.getImportanceLevel(importantDay.second)

    // Get tracking information
    val todaysTracker = progressForMonth.value.find { it.date == currentDate }
    val todaysFastTracker = fastProgressForMonth.value.find { it.date == currentDate }
    val isMenstruatingToday = todaysTracker?.isMenstruating ?: false
    val isFromCurrentMonth = dayState.isFromCurrentMonth

    // Determine card styling based on state
    val cardColor = when {
        !isFromCurrentMonth -> MaterialTheme.colorScheme.surface
        isSelectedDay -> MaterialTheme.colorScheme.primaryContainer
        today -> MaterialTheme.colorScheme.secondaryContainer
        isMenstruatingToday -> Color(0xFFFFE4E8)
        importanceLevel == ImportanceLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer.copy(
            alpha = 0.3f
        )

        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isSelectedDay && !today && !isMenstruatingToday -> MaterialTheme.colorScheme.primary
        today && !isMenstruatingToday -> MaterialTheme.colorScheme.secondary
        isMenstruatingToday -> Color(0xFFE91E63)
        importanceLevel == ImportanceLevel.HIGH -> IslamicCalendarHelper.getImportantDayColor(
            importantDay
        )

        else -> Color.Transparent
    }

    val elevation = when {
        isSelectedDay || today -> 4.dp
        importanceLevel == ImportanceLevel.HIGH -> 3.dp
        importanceLevel == ImportanceLevel.MEDIUM -> 2.dp
        else -> 1.dp
    }

    Card(
        modifier = modifier
            .aspectRatio(0.6f)
            .padding(2.dp)
            .border(
                width = if (isSelectedDay || today || importanceLevel == ImportanceLevel.HIGH) 2.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                enabled = isFromCurrentMonth,
                onClick = {
                    dayState.selectionState.onDateSelected(dayState.date)
                    handleEvents(dayState.date)
                },
                onLongClick = {
                    if (importantDay.first) {
                        hasDescription.value = !hasDescription.value
                    }
                }
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Gregorian Date
            Text(
                text = dayState.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (today || isSelectedDay || importanceLevel == ImportanceLevel.HIGH)
                        FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelectedDay -> MaterialTheme.colorScheme.onPrimaryContainer
                    today -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Hijri Date
            Text(
                text = hijriDay.format(DateTimeFormatter.ofPattern("d")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Prayer Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                todaysTracker?.let { tracker ->
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> if (tracker.fajr) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline

                                        1 -> if (tracker.dhuhr) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline

                                        2 -> if (tracker.asr) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline

                                        3 -> if (tracker.maghrib) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline

                                        4 -> if (tracker.isha) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline

                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                    }
                }
            }

            // Fast Indicator
            if (todaysFastTracker?.isFasting == true) {
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFCDDC39).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fasted",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }

            // Important Day Indicator
            if (importantDay.first) {
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IslamicCalendarHelper.getImportantDayColor(importantDay)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = importantDay.second,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // Important Day Description Dialog
    if (hasDescription.value) {
        BasicAlertDialog(
            onDismissRequest = { hasDescription.value = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = importantDay.second,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = IslamicCalendarHelper.getImportantDayDescription(importantDay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { hasDescription.value = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}


data class IslamicDay(
    val name: String,
    val description: String,
    val importance: ImportanceLevel
)

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

    // Helper function to get Hijri month name
    fun getHijriMonthName(month: Int): String {
        return when (month) {
            1 -> "Muharram محرم"
            2 -> "Safar صفر"
            3 -> "Rabi' al-Awwal ربيع الأول"
            4 -> "Rabi' al-Thani ربيع الثاني"
            5 -> "Jumada al-Awwal جمادى الأول"
            6 -> "Jumada al-Thani جمادى الثاني"
            7 -> "Rajab رجب"
            8 -> "Sha'ban شعبان"
            9 -> "Ramadan رمضان"
            10 -> "Shawwal شوال"
            11 -> "Dhu al-Qadah ذو القعدة"
            12 -> "Dhu al-Hijjah ذو الحجة"
            else -> "Unknown"
        }
    }
}
