package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import java.time.LocalDate

//composable to show the prayers for this week using 7 circular progress indicators
@Composable
fun SevenDayTrend(trackersForWeek: State<List<LocalPrayersTracker>>, dateState: State<LocalDate>) {

    Column(
        modifier = Modifier.padding(
            vertical = 8.dp,
            horizontal = 4.dp
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            trackersForWeek.value.forEachIndexed { index, prayerTracker ->

                ProgressBarCustom(
                    progress = prayerTracker.progress.toFloat(),
                    progressColor = if (prayerTracker.progress == 0 && !prayerTracker.isMenstruating) Color.Gray
                    else if (prayerTracker.isMenstruating) Color(0xFFE91E63)
                    else MaterialTheme.colorScheme.primary,
                    radius = 20.dp,
                    label = prayerTracker.date.dayOfWeek.name.first().toString(),
                    strokeWidth = 6.dp,
                    strokeBackgroundWidth = 3.dp,
                    startDelay = 0,
                    labelColor = if (prayerTracker.progress == 0 && !prayerTracker.isMenstruating) Color.Gray
                    else if (prayerTracker.isMenstruating) Color(0xFFE91E63)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
