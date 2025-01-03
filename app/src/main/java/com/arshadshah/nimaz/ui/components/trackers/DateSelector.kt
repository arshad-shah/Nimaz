package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
    val formattedDate = remember(date.value) {
        date.value.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
    }
    val formattedHijrahDate = remember(hijrahDate) {
        hijrahDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // Decorative top pattern
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day Button
            DateChangeButton(
                iconId = R.drawable.angle_left_icon,
                description = "Previous Day",
                onClick = { updateDate(date, -1, updateDate) }
            )

            // Date Display
            DateDisplay(
                date = date,
                formattedDate = formattedDate,
                formattedHijrahDate = formattedHijrahDate,
                onClick = {
                    date.value = LocalDate.now()
                    updateDate(date, 0, updateDate)
                }
            )

            // Next Day Button
            DateChangeButton(
                iconId = R.drawable.angle_right_icon,
                description = "Next Day",
                onClick = { updateDate(date, 1, updateDate) }
            )
        }
    }
}

@Composable
private fun DateChangeButton(iconId: Int, description: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
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
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isToday || showFutureIcon || showPastIcon) {
            TodayIndicator(isToday, showFutureIcon, showPastIcon)
        }

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = formattedHijrahDate,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun TodayIndicator(isToday: Boolean, showFutureIcon: Boolean, showPastIcon: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = if (isToday)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFutureIcon) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(id = R.drawable.angle_small_left_icon),
                contentDescription = "Previous Day",
                tint = if (isToday)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = if (isToday) "Today" else "Return to Today",
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.primary
        )

        if (showPastIcon) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(id = R.drawable.angle_small_right_icon),
                contentDescription = "Next Day",
                tint = if (isToday)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary
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