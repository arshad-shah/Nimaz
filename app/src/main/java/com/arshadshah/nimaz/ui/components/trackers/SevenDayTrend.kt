package com.arshadshah.nimaz.ui.components.trackers

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import com.arshadshah.nimaz.viewModel.PrayerTrakerForWeek
import java.time.LocalDate

@Composable
fun SevenDayTrend(
    trackersForWeek: State<List<PrayerTrakerForWeek>>,
    dateState: State<LocalDate>
) {

    Log.d("SevenDayTrend", "trackersForWeek: ${trackersForWeek.value}")
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${trackersForWeek.value.count { it.progress > 0 }}/7",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bars
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
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
                            progressColor = determineColor(prayerTracker),
                            radius = 24.dp,
                            label = prayerTracker.date.dayOfWeek.name.take(1),
                            strokeWidth = 6.dp,
                            strokeBackgroundWidth = 3.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun determineColor(prayerTracker: PrayerTrakerForWeek): Color {
    return when {
        prayerTracker.progress == 0 && !prayerTracker.isMenstruating ->
            MaterialTheme.colorScheme.surfaceVariant

        prayerTracker.isMenstruating -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Preview
@Composable
fun SevenDayTrendPreview() {
    val trackers = remember {
        listOf(
            PrayerTrakerForWeek(LocalDate.now(), 0, false),
            PrayerTrakerForWeek(LocalDate.now(), 1, false),
            PrayerTrakerForWeek(LocalDate.now(), 2, false),
            PrayerTrakerForWeek(LocalDate.now(), 3, false),
            PrayerTrakerForWeek(LocalDate.now(), 4, false),
            PrayerTrakerForWeek(LocalDate.now(), 5, false),
            PrayerTrakerForWeek(LocalDate.now(), 6, false),
        )
    }
    val date = remember { mutableStateOf(LocalDate.now()) }
    SevenDayTrend(
        trackersForWeek = remember { mutableStateOf(trackers) },
        dateState = date
    )
}