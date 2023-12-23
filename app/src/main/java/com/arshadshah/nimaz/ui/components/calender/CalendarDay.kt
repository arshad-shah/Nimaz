package com.arshadshah.nimaz.ui.components.calender

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import io.github.boguszpawlowski.composecalendar.day.Day
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import io.github.boguszpawlowski.composecalendar.selection.SelectionMode
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import kotlin.reflect.KFunction1

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalenderDay(
    dayState: DayState<DynamicSelectionState>,
    handleEvents: KFunction1<TrackerViewModel.TrackerEvent, Unit>?,
    progressForMonth: State<MutableList<PrayerTracker>>,
    fastProgressForMonth: State<MutableList<FastTracker>>,
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
                                dayState.date.toString()
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
            Text(
                text = dayState.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                //if today then bolden the text
                fontWeight = if (today) FontWeight.ExtraBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
                color = when (importantDay.first) {
                    false -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                    else if (today) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface

                    true -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                    else if (today) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                //fajr
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (todaysTracker?.fajr == true) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
                //dhuhr
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (todaysTracker?.dhuhr == true) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
                //asr
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (todaysTracker?.asr == true) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
                //maghrib
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (todaysTracker?.maghrib == true) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
                //isha
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (todaysTracker?.isha == true) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
            Row(
                modifier = Modifier,
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    //put a letter scissor ha in front of the day to show that it is a hijri day
                    text = "ه" + hijriDay[ChronoField.DAY_OF_MONTH].toString(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (today) FontWeight.ExtraBold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(vertical = 3.dp, horizontal = 3.dp),
                    color = when (importantDay.first) {
                        false -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                        else if (today) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface

                        true -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                        else if (today) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (todaysFastTracker?.isFasting == true) MaterialTheme.colorScheme.error
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

    }

    if (hasDescription.value) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, -120),
            onDismissRequest = { hasDescription.value = false }
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        hasDescription.value = !hasDescription.value
                    },
            ) {
                Text(
                    text = importantDay.second,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
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

@Preview
@Composable
fun CalenderDayPreview() {
    class DayOfWeek : Day {

        override val date: LocalDate = LocalDate.now()
        override val isCurrentDay: Boolean = true
        override val isFromCurrentMonth: Boolean = true
    }

    val dayState = DayState(
        day = DayOfWeek(), selectionState = DynamicSelectionState(
            selection = listOf(),
            confirmSelectionChange = { true },
            selectionMode = SelectionMode.Single
        )
    )
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(48.dp)
            .background(color = MaterialTheme.colorScheme.surface),
    ) {
        CalenderDay(
            dayState = dayState,
            handleEvents = null,
            progressForMonth = remember {
                mutableStateOf(
                    mutableListOf(
                        PrayerTracker(
                            date = LocalDate.now().toString(),
                            fajr = true,
                            dhuhr = true,
                            asr = true,
                            maghrib = true,
                            isha = true
                        )
                    )
                )
            },
            fastProgressForMonth = remember {
                mutableStateOf(
                    mutableListOf(
                        FastTracker(
                            date = LocalDate.now().toString(),
                            isFasting = true
                        )
                    )
                )
            }
        )
    }
}