package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarWeekHeader(
    modifier: Modifier = Modifier,
    locale: Locale = Locale.getDefault(),
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY // Default to Sunday as first day
) {
    val daysOfWeek = remember(locale, firstDayOfWeek) {
        (0..6).map { i ->
            val day = firstDayOfWeek.plus(i.toLong())
            day to day.getDisplayName(TextStyle.SHORT, locale)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            daysOfWeek.forEach { (day, label) ->
                WeekDayLabel(
                    day = day,
                    label = label,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeekDayLabel(
    day: DayOfWeek,
    label: String,
    modifier: Modifier = Modifier
) {
    val isWeekend = remember(day) {
        day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
    }

    Surface(
        color = if (isWeekend)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isWeekend)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}