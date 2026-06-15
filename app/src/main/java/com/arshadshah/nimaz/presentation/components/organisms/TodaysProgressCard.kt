package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay

/**
 * "Today's Progress" card: a thin gradient progress bar over the five main
 * salat (Fajr, Dhuhr, Asr, Maghrib, Isha — Sunrise excluded), with a row of
 * dots beneath showing each prayer's state (current / completed / pending).
 */
@Composable
fun TodaysProgressCard(
    prayerTimes: List<PrayerTimeDisplay>,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val mainPrayers = prayerTimes.filter {
        it.type in listOf(
            PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR,
            PrayerType.MAGHRIB, PrayerType.ISHA
        )
    }
    val completedCount = mainPrayers.count { it.prayerStatus == PrayerStatus.PRAYED }
    val totalCount = mainPrayers.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.todays_progress),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.prayers_count_format, completedCount, totalCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    mainPrayers.forEach { prayer ->
                        ProgressPrayerDot(
                            label = prayer.name.take(5),
                            isCompleted = prayer.prayerStatus == PrayerStatus.PRAYED,
                            isCurrent = prayer.isCurrent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPrayerDot(
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> MaterialTheme.colorScheme.secondary
                        isCompleted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val samplePrayerTimes = listOf(
    PrayerTimeDisplay(PrayerType.FAJR, "Fajr", "5:23 AM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.SUNRISE, "Sunrise", "6:45 AM", isPassed = true, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.DHUHR, "Dhuhr", "1:15 PM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.ASR, "Asr", "4:30 PM", isPassed = false, isCurrent = true, isNext = true),
    PrayerTimeDisplay(PrayerType.MAGHRIB, "Maghrib", "6:12 PM", isPassed = false, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.ISHA, "Isha", "7:45 PM", isPassed = false, isCurrent = false, isNext = false),
)

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun TodaysProgressCard_Preview() {
    NimazTheme {
        TodaysProgressCard(
            prayerTimes = samplePrayerTimes,
            modifier = Modifier.padding(16.dp)
        )
    }
}
