package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTimeDisplay

/**
 * "Today's Progress" card, drawn as a timeline stepper: a track runs through
 * the five main salat (Fajr → Isha, Sunrise excluded), each a node with its
 * time.
 *
 * Two independent signals share the card:
 *  - [timelineProgress] (0f→1f, from the clock) fills the track up to "now",
 *    so the bar advances through the day regardless of what's been prayed.
 *  - each node's state comes from the tracker: a checkmark when prayed, a ring
 *    for the current prayer, hollow when still pending.
 */
@Composable
fun TodaysProgressCard(
    prayerTimes: List<PrayerTimeDisplay>,
    modifier: Modifier = Modifier,
    timelineProgress: Float = 0f,
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

    NimazCard(
        tone = NimazTone.NEUTRAL,
        style = NimazCardStyle.ELEVATED,
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(16.dp),
            verticalArrangement = if (fillHeight) Arrangement.SpaceBetween else Arrangement.Top
        ) {
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
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            if (!fillHeight) Spacer(modifier = Modifier.height(18.dp))

            PrayerTimeline(
                prayers = mainPrayers,
                timelineProgress = timelineProgress,
            )
        }
    }
}

/**
 * The track + nodes. Nodes are equal-width columns so their dot centres land at
 * even fractions across the width; the track spans the first-to-last dot centre
 * and its fill is driven by [timelineProgress].
 */
@Composable
private fun PrayerTimeline(
    prayers: List<PrayerTimeDisplay>,
    timelineProgress: Float,
) {
    val n = prayers.size
    // Dot centres sit at (i+0.5)/n, so the track runs from 0.5/n to (n-0.5)/n —
    // a width of (n-1)/n centred on the row.
    val trackWidthFraction = if (n > 1) (n - 1f) / n else 1f
    val dotSize = 24.dp
    val trackHeight = 6.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        // Track (behind the dots), aligned to the dots' vertical centre.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = dotSize / 2 - trackHeight / 2)
                .fillMaxWidth(trackWidthFraction)
                .height(trackHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(timelineProgress.coerceIn(0f, 1f))
                    .height(trackHeight)
                    .clip(CircleShape)
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

        Row(modifier = Modifier.fillMaxWidth()) {
            prayers.forEach { prayer ->
                TimelineNode(
                    modifier = Modifier.weight(1f),
                    label = prayer.name.take(5),
                    time = prayer.time,
                    isPrayed = prayer.prayerStatus == PrayerStatus.PRAYED,
                    isCurrent = prayer.isCurrent,
                )
            }
        }
    }
}

@Composable
private fun TimelineNode(
    label: String,
    time: String,
    isPrayed: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isPrayed -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    NimazIcon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        variant = NimazIconVariant.ON_ACCENT,
                        iconSize = 14.dp
                    )
                }

                isCurrent -> Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }

                else -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            fontWeight = FontWeight.SemiBold,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private val samplePrayerTimes = listOf(
    PrayerTimeDisplay(
        PrayerType.FAJR,
        "Fajr",
        "5:23 AM",
        isPassed = true,
        isCurrent = false,
        isNext = false,
        prayerStatus = PrayerStatus.PRAYED
    ),
    PrayerTimeDisplay(
        PrayerType.SUNRISE,
        "Sunrise",
        "6:45 AM",
        isPassed = true,
        isCurrent = false,
        isNext = false
    ),
    PrayerTimeDisplay(
        PrayerType.DHUHR,
        "Dhuhr",
        "1:15 PM",
        isPassed = true,
        isCurrent = false,
        isNext = false,
        prayerStatus = PrayerStatus.PRAYED
    ),
    PrayerTimeDisplay(
        PrayerType.ASR,
        "Asr",
        "4:30 PM",
        isPassed = true,
        isCurrent = false,
        isNext = false,
        prayerStatus = PrayerStatus.PRAYED
    ),
    PrayerTimeDisplay(
        PrayerType.MAGHRIB,
        "Maghrib",
        "6:12 PM",
        isPassed = false,
        isCurrent = true,
        isNext = true
    ),
    PrayerTimeDisplay(
        PrayerType.ISHA,
        "Isha",
        "7:45 PM",
        isPassed = false,
        isCurrent = false,
        isNext = false
    ),
)

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun TodaysProgressCard_Preview() {
    NimazTheme {
        TodaysProgressCard(
            prayerTimes = samplePrayerTimes,
            timelineProgress = 0.72f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 162, name = "Carousel")
@Composable
private fun TodaysProgressCard_Carousel_Preview() {
    NimazTheme {
        TodaysProgressCard(
            prayerTimes = samplePrayerTimes,
            timelineProgress = 0.72f,
            fillHeight = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
