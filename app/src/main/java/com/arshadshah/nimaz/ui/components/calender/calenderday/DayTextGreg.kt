package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState

@Composable
fun DayTextGreg(
    dayState: DayState<DynamicSelectionState>,
    isSelectedDay: Boolean,
    today: Boolean,
    importantDay: Pair<Boolean, String>
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
}