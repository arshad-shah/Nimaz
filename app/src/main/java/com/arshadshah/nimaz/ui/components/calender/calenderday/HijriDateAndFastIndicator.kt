package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

@Composable
fun HijriDateAndFastIndicator(
    todaysFastTracker: LocalFastTracker?,
    isSelectedDay: Boolean,
    today: Boolean,
    importantDay: Pair<Boolean, String>,
    hijriDay: HijrahDate
) {
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            //put a letter scissor ha in front of the day to show that it is a hijri day
            text = "${hijriDay[ChronoField.DAY_OF_MONTH]}/${hijriDay[ChronoField.MONTH_OF_YEAR]}",
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (today) FontWeight.ExtraBold else FontWeight.Normal,
            modifier = Modifier
                .padding(vertical = 1.dp, horizontal = 3.dp),
            color = when (importantDay.first) {
                false -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                else if (today) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurface

                true -> if (isSelectedDay && !today) MaterialTheme.colorScheme.onTertiaryContainer
                else if (today) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onTertiaryContainer
            }
        )
    }
}