package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KFunction1

@Composable
fun DateSelector(
    dateState: State<LocalDate>,
    updateDate: KFunction1<LocalDate, Unit>,
) {
    val date = remember(dateState.value) { mutableStateOf(dateState.value) }
    val hijrahDate = remember(date.value) { HijrahDate.from(date.value) }
    val formattedDate =
        remember(date.value) { date.value.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) }
    val formattedHijrahDate =
        remember(hijrahDate) { hijrahDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day Button
            DateChangeButton(iconId = R.drawable.angle_left_icon, description = "Previous Day") {
                updateDate(date, -1, updateDate)
            }

            // Date Display
            DateDisplay(date, formattedDate, formattedHijrahDate) {
                date.value = LocalDate.now()
                updateDate(date, 0, updateDate)
            }

            // Next Day Button
            DateChangeButton(iconId = R.drawable.angle_right_icon, description = "Next Day") {
                updateDate(date, 1, updateDate)
            }
        }
    }
}

@Composable
private fun DateChangeButton(iconId: Int, description: String, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = iconId),
            contentDescription = description,
        )
    }
}

@Composable
private fun DateDisplay(
    date: MutableState<LocalDate>,
    formattedDate: String,
    formattedHijrahDate: String,
    onClick: () -> Unit
) {
    val isToday = date.value == LocalDate.now()
    val showFutureIcon = date.value.isAfter(LocalDate.now())
    val showPastIcon = date.value.isBefore(LocalDate.now())

    Column(
        modifier = Modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isToday) {
            Text("Today", style = MaterialTheme.typography.titleSmall)
        } else {
            TodayIndicator(showFutureIcon, showPastIcon)
        }
        Text(formattedDate, style = MaterialTheme.typography.titleMedium)
        Text(formattedHijrahDate, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TodayIndicator(showFutureIcon: Boolean, showPastIcon: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showFutureIcon) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.angle_small_left_icon),
                contentDescription = "Previous Day",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(
            "Today",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
        if (showPastIcon) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.angle_small_right_icon),
                contentDescription = "Next Day",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private fun updateDate(
    date: MutableState<LocalDate>,
    daysToAdd: Long,
    updateDateAndFetchData: KFunction1<LocalDate, Unit>
) {
    date.value = date.value.plusDays(daysToAdd)
    updateDateAndFetchData(date.value)
}