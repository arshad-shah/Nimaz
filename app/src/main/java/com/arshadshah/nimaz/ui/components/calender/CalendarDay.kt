package com.arshadshah.nimaz.ui.components.calender

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.calender.calenderday.DayTextGreg
import com.arshadshah.nimaz.ui.components.calender.calenderday.HijriDateAndFastIndicator
import com.arshadshah.nimaz.ui.components.calender.calenderday.ImportantDayDescriptionPopup
import com.arshadshah.nimaz.ui.components.calender.calenderday.PrayerDots
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.YearMonth
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.reflect.KFunction1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalenderDay(
    dayState: DayState<DynamicSelectionState>,
    handleEvents: KFunction1<TrackerViewModel.TrackerEvent, Unit>?,
    progressForMonth: State<List<LocalPrayersTracker>>,
    fastProgressForMonth: State<List<LocalFastTracker>>,
) {

    //get the day for the hijri calendar
    val hijriDay = HijrahDate.from(dayState.date)
    val currentDate = dayState.date
    val today = dayState.isCurrentDay

    val isSelectedDay = dayState.selectionState.isDateSelected(currentDate)

    val hasDescription = remember { mutableStateOf(false) }

    val hijriMonth = hijriDay[ChronoField.MONTH_OF_YEAR]
    val hijriDayOfMonth = hijriDay[ChronoField.DAY_OF_MONTH]
    //check if today is an important day and if so, display the description of the day and highlight the day
    val importantDay = isImportantDay(hijriDayOfMonth, hijriMonth)

    //find todays tracker in the list of trackers from progressForMonth
    val todaysTracker = progressForMonth.value.find { it.date == currentDate }
    val todaysFastTracker = fastProgressForMonth.value.find { it.date == currentDate }
    val isMenstratingToday = todaysTracker?.isMenstruating ?: false

    val isFromCurrentMonth = dayState.isFromCurrentMonth

    Log.d("CalenderDay", "isFromCurrentMonth: $currentDate $isFromCurrentMonth")

    Card(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth()
            .alpha(if (isFromCurrentMonth) 1f else 0.4f)
            .border(
                width = if (isSelectedDay || today) 2.dp else 0.dp,
                color = if (isSelectedDay && !today && !isMenstratingToday) MaterialTheme.colorScheme.tertiary.copy(
                    alpha = 0.5f
                )
                else if (today && !isMenstratingToday) MaterialTheme.colorScheme.secondary.copy(
                    alpha = 0.5f
                )
                else if (isMenstratingToday) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp)
                ,
                shape = MaterialTheme.shapes.medium
            )
            .combinedClickable(
                enabled = isFromCurrentMonth,
                onClick = {
                    dayState.selectionState.onDateSelected(dayState.date)
                    if (handleEvents == null) return@combinedClickable
                    handleEvents(
                        TrackerViewModel.TrackerEvent.SET_DATE(
                            dayState.date
                        )
                    )
                    handleEvents(
                        TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
                            dayState.date
                        )
                    )
                    handleEvents(
                        TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
                            dayState.date
                        )
                    )
                    handleEvents(
                        TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
                            dayState.date
                        )
                    )
                    handleEvents(
                        TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
                            YearMonth.from(dayState.date)
                        )
                    )
                },
                onLongClick = {
                    if (importantDay.first) {
                        hasDescription.value = !hasDescription.value
                    }
                }
            ),
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DayTextGreg(
                dayState = dayState,
                isSelectedDay = isSelectedDay,
                today = today,
                importantDay = importantDay
            )
            HijriDateAndFastIndicator(
                todaysFastTracker = todaysFastTracker,
                isSelectedDay = isSelectedDay,
                today = today,
                importantDay = importantDay,
                hijriDay = hijriDay
            )

            PrayerDots(todaysTracker = todaysTracker)

            if (todaysFastTracker?.isFasting == true){
                Card(
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFCDDC39),
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Fasted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            if (importantDay.first) {
                Card(
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = getImportantDayColor(importantDay),
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = importantDay.second,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

    }

    if (hasDescription.value) {
        ImportantDayDescriptionPopup(
            description = importantDay.second,
            hasDescription = hasDescription
        )
    }
}


fun getImportantDayColor(
    importantDay: Pair<Boolean, String>,
) = when (importantDay.first) {
    true -> if (importantDay.second == "Ramadan") Color(0xFF4CAF50)
    else if (importantDay.second == "Eid Al Fitr") Color(0xFF4CAF50)
    else if (importantDay.second == "Eid Al Adha") Color(0xFF4CAF50)
    else if (importantDay.second == "Islamic New Year") Color(0xFF4CAF50)
    else if (importantDay.second == "Day of Ashura") Color(0xFF4CAF50)
    else if (importantDay.second == "Mawlid An Nabawi") Color(0xFF4CAF50)
    else if (importantDay.second == "Start of Dul Hijjah") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 2") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 3") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 4") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 5") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 6") Color(0xFF4CAF50)
    else if (importantDay.second == "Dul Hijjah 7") Color(0xFF4CAF50)
    else if (importantDay.second == "Hajj Day 1") Color(0xFF4CAF50)
    else if (importantDay.second == "Day of Arafah") Color(0xFF4CAF50)
    else if (importantDay.second == "Ramadan Last Day") Color(0xFF4CAF50)
    else if (importantDay.second == "Shab-e-Barat") Color(0xFF4CAF50)
    else if (importantDay.second == "Al Isra Wal Mi'raj") Color(0xFF4CAF50)
    else Color(0xFFE91E63)

    true -> Color.Transparent
    false -> TODO()
}

//function to check if a day is an important day
fun isImportantDay(day: Int, month: Int): Pair<Boolean, String> {
    val importantDays = mapOf(
        7 to mapOf(27 to "Al Isra Wal Mi'raj"),
        8 to mapOf(15 to "Shab-e-Barat"),
        9 to mapOf(
            1 to "Ramadan Starts (estimated)",
            2 to "Ramadan day 2",
            3 to "Ramadan day 3",
            4 to "Ramadan day 4",
            5 to "Ramadan day 5",
            6 to "Ramadan day 6",
            7 to "Ramadan day 7",
            8 to "Ramadan day 8",
            9 to "Ramadan day 9",
            10 to "Ramadan day 10",
            11 to "Ramadan day 11",
            12 to "Ramadan day 12",
            13 to "Ramadan day 13",
            14 to "Ramadan day 14",
            15 to "Ramadan day 15",
            16 to "Ramadan day 16",
            17 to "Ramadan day 17",
            18 to "Ramadan day 18",
            19 to "Ramadan day 19",
            20 to "Ramadan day 20",
            21 to "Ramadan day 21",
            22 to "Ramadan day 22",
            23 to "Ramadan day 23",
            24 to "Ramadan day 24",
            25 to "Ramadan day 25",
            26 to "Ramadan day 26",
            27 to "Laylatul Qadr",
            28 to "Laylatul Qadr",
            29 to "Laylatul Qadr",
            30 to "Laylatul Qadr"
        ),
        10 to mapOf(1 to "Eid Al Fitr"),
        12 to mapOf(
            1 to "Start of Dul Hijjah",
            2 to "Dul Hijjah 2",
            3 to "Dul Hijjah 3",
            4 to "Dul Hijjah 4",
            5 to "Dul Hijjah 5",
            6 to "Dul Hijjah 6",
            7 to "Dul Hijjah 7",
            8 to "Hajj Day 1",
            9 to "Day of Arafah",
            10 to "Eid Al Adha"
        ),
        1 to mapOf(1 to "Islamic New Year", 10 to "Day of Ashura"),
        3 to mapOf(12 to "Mawlid An Nabawi")
    )
    return if (importantDays[month]?.contains(day) == true) {
        //structure of the map is month (key) -> day (key) -> description (value)
        Pair(true, importantDays[month]!![day] ?: "")
    } else {
        Pair(false, "")
    }
}