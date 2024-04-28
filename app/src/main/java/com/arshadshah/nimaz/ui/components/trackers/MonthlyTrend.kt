package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter


// Moved outside the composable
val prayers = listOf(
    AppConstants.PRAYER_NAME_FAJR,
    AppConstants.PRAYER_NAME_DHUHR,
    AppConstants.PRAYER_NAME_ASR,
    AppConstants.PRAYER_NAME_MAGHRIB,
    AppConstants.PRAYER_NAME_ISHA
)


@Composable
fun PrayerTrackerGrid(
    progressForMonth: State<List<LocalPrayersTracker>>,
    dateState: State<LocalDate>
) {
    val yearMonth = YearMonth.of(dateState.value.year, dateState.value.month)
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        //month name
        Text(
            modifier = Modifier
                .padding(bottom = 2.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = dateState.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        )
        prayers.forEach { prayer ->
            PrayerRow(
                prayer,
                yearMonth,
                daysInMonth,
                dateState.value,
                progressForMonth
            )
        }
    }
}

@Composable
fun PrayerRow(
    prayer: String,
    yearMonth: YearMonth,
    daysInMonth: Int,
    currentDate: LocalDate,
    progressForMonth: State<List<LocalPrayersTracker>>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prayer,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp)
        )

        for (i in 0 until daysInMonth) {
            val date = yearMonth.atDay(i + 1)
            val prayerTracker = progressForMonth.value.find { it.date == date }
            val isHighlighted = prayerTracker?.isPrayerCompleted(prayer) == true
            val isMenstruating = prayerTracker?.isMenstruating == true

            DayDot(date, isHighlighted, isMenstruating, currentDate)
        }
    }
}

@Composable
fun DayDot(
    date: LocalDate,
    isHighlighted: Boolean,
    isMenstruating: Boolean,
    currentDate: LocalDate,
) {
    // Determine border and background color based on conditions
    val borderColor =
        determineBorderColor(date, isHighlighted, isMenstruating, currentDate)
    val backgroundColor =
        determineBackgroundColor(date, isHighlighted, isMenstruating, currentDate)

    Box(
        modifier = Modifier
            .size(if (date == currentDate || date == LocalDate.now()) 10.dp else 8.dp)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .background(color = backgroundColor, shape = CircleShape)
    )
}

@Composable
fun determineBorderColor(
    date: LocalDate,
    isHighlighted: Boolean,
    isMenstruating: Boolean,
    currentDate: LocalDate,
): Color {
    return when {
        date == currentDate -> MaterialTheme.colorScheme.tertiary
        date == LocalDate.now() -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> {
            if (isHighlighted && !isMenstruating) {
                MaterialTheme.colorScheme.primary
            } else if (isMenstruating) {
                Color(0xFFE91E63) // Pink
            } else {
                Color.Gray
            }
        }
    }
}

@Composable
fun determineBackgroundColor(
    date: LocalDate,
    isHighlighted: Boolean,
    isMenstruating: Boolean,
    currentDate: LocalDate,
): Color {
    return when {
        date == currentDate || date == LocalDate.now() -> {
            if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else if (isMenstruating) {
                Color(0xFFE91E63) // Pink
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        }

        else -> {
            if (isHighlighted) {
                MaterialTheme.colorScheme.primary
            } else if (isMenstruating) {
                Color(0xFFE91E63) // Pink
            } else {
                Color.Gray
            }
        }
    }
}

@Preview
@Composable
fun PrayerTrackerGrid2Preview() {
    val progressForMonth = remember {
        mutableStateOf(
            listOf(
                LocalPrayersTracker(
                    date = LocalDate.of(2021, 1, 1),
                    progress = 0,
                    isMenstruating = true
                )
            )
        )
    }
    val dateState = remember { mutableStateOf(LocalDate.now()) }
    PrayerTrackerGrid(progressForMonth, dateState)
}
