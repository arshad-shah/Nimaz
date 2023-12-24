package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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