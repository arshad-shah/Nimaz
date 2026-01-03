package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Date Controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Day Button
                    DateChangeButton(
                        iconId = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
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
                        iconId = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        description = "Next Day",
                        onClick = { updateDate(date, 1, updateDate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChangeButton(iconId: ImageVector, description: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = iconId,
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
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (isToday || showFutureIcon || showPastIcon) {
            TodayIndicator(isToday, showFutureIcon, showPastIcon)
        }

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = formattedHijrahDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun TodayIndicator(isToday: Boolean, showFutureIcon: Boolean, showPastIcon: Boolean) {
    Surface(
        color = if (isToday)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showFutureIcon) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Day",
                    tint = if (isToday)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = if (isToday) "Today" else "Return to Today",
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (showPastIcon) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Day",
                    tint = if (isToday)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
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