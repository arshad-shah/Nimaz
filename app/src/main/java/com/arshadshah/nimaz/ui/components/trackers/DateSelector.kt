package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun DateSelector(
    handleEvent: (TrackerViewModel.TrackerEvent) -> Unit,
    dateState: State<String>,
) {
    val date = remember(dateState.value) { mutableStateOf(LocalDate.parse(dateState.value)) }
    val hijrahDate = remember(date.value) { HijrahDate.from(date.value) }
    val formattedDate =
        remember(date.value) { date.value.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) }
    val formattedHijrahDate =
        remember(hijrahDate) { hijrahDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
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
                updateDate(date, -1, handleEvent)
            }

            // Date Display
            DateDisplay(date, formattedDate, formattedHijrahDate) {
                handleEvent(TrackerViewModel.TrackerEvent.SET_DATE(LocalDate.now().toString()))
                handleEvent(
                    TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
                        LocalDate.now().toString()
                    )
                )
                handleEvent(
                    TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
                        LocalDate.now().toString()
                    )
                )
            }

            // Next Day Button
            DateChangeButton(iconId = R.drawable.angle_right_icon, description = "Next Day") {
                updateDate(date, 1, handleEvent)
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
        Text(formattedHijrahDate, style = MaterialTheme.typography.bodySmall)
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
    handleEvent: (TrackerViewModel.TrackerEvent) -> Unit
) {
    date.value = date.value.plusDays(daysToAdd)
    handleEvent(TrackerViewModel.TrackerEvent.SET_DATE(date.value.toString()))
    handleEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(date.value.toString()))
    handleEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(date.value.toString()))
}