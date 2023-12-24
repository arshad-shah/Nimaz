package com.arshadshah.nimaz.ui.components.calender

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
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
    progressForMonth: State<List<PrayerTracker>>,
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
    val todaysTracker = progressForMonth.value.find { it.date == currentDate.toString() }
    val todaysFastTracker = fastProgressForMonth.value.find { it.date == currentDate.toString() }
    val isMenstratingToday = todaysTracker?.isMenstruating ?: false

    val isFromCurrentMonth = dayState.isFromCurrentMonth

    Log.d("CalenderDay", "isFromCurrentMonth: $currentDate $isFromCurrentMonth")

    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isFromCurrentMonth) 2.dp else 0.dp,
        ),
        modifier = Modifier
            .padding(2.dp)
            .alpha(if (isFromCurrentMonth) 1f else 0.2f)
            .border(
                width = if (isSelectedDay || today) 2.dp else 0.75.dp,
                color = when (importantDay.first) {
                    false -> if (isSelectedDay && !today && !isMenstratingToday) MaterialTheme.colorScheme.tertiary.copy(
                        alpha = 0.5f
                    )
                    else if (today && !isMenstratingToday) MaterialTheme.colorScheme.secondary.copy(
                        alpha = 0.5f
                    )
                    else if (isMenstratingToday) Color(0xFFE91E63)
                    else MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp)

                    true -> if (isSelectedDay && !today && !isMenstratingToday) MaterialTheme.colorScheme.tertiary.copy(
                        alpha = 0.5f
                    )
                    else if (today && !isMenstratingToday) MaterialTheme.colorScheme.secondary.copy(
                        alpha = 0.5f
                    )
                    else if (isMenstratingToday) Color(0xFFE91E63)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (importantDay.first) {
                false -> if (isSelectedDay && !today) MaterialTheme.colorScheme.tertiaryContainer
                else if (today) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 32.dp)

                true -> if (isSelectedDay && !today) MaterialTheme.colorScheme.tertiaryContainer
                else if (today) MaterialTheme.colorScheme.surfaceColorAtElevation(
                    elevation = 32.dp
                )
                else MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {

        Column(
            modifier = Modifier
                .combinedClickable(
                    enabled = isFromCurrentMonth,
                    onClick = {
                        dayState.selectionState.onDateSelected(dayState.date)
                        if (handleEvents == null) return@combinedClickable
                        handleEvents(
                            TrackerViewModel.TrackerEvent.SET_DATE(
                                dayState.date.toString()
                            )
                        )
                        handleEvents(
                            TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
                                dayState.date.toString()
                            )
                        )
                        handleEvents(
                            TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
                                dayState.date.toString()
                            )
                        )
                        handleEvents(
                            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
                                dayState.date.toString()
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DayTextGreg(
                dayState = dayState,
                isSelectedDay = isSelectedDay,
                today = today,
                importantDay = importantDay
            )
            PrayerDots(todaysTracker = todaysTracker)
            HijriDateAndFastIndicator(
                todaysFastTracker = todaysFastTracker,
                isSelectedDay = isSelectedDay,
                today = today,
                importantDay = importantDay,
                hijriDay = hijriDay
            )
        }

    }

    if (hasDescription.value) {
        ImportantDayDescriptionPopup(
            description = importantDay.second,
            hasDescription = hasDescription
        )
    }
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