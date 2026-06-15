package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
 * Combined "today snapshot" card: prayer progress (count + bar + dots) on
 * top, fasting status on the bottom. Designed for use in the home carousel
 * where each page fills the full carousel height — uses [Arrangement.SpaceBetween]
 * so the progress block anchors to the top, fasting anchors to the bottom,
 * and the gradient gap in the middle scales gracefully with available height.
 *
 * For carousel use pass [fillHeight] = true. For standalone use (e.g. an
 * embedded summary widget) leave it false and the card sizes to content.
 */
@Composable
fun DailySummaryCard(
    prayerTimes: List<PrayerTimeDisplay>,
    fastingToday: Boolean,
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
            Column(
                modifier = Modifier.fillMaxSize(),
                // SpaceBetween anchors progress to top, fasting to bottom.
                // In a fixed-height carousel page this makes the layout feel
                // intentionally laid out rather than top-clumped.
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ProgressBlock(
                    completedCount = completedCount,
                    totalCount = totalCount,
                    progress = progress,
                    mainPrayers = mainPrayers,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                FastingRow(fastingToday = fastingToday)
            }
        }
    }
}

@Composable
private fun ProgressBlock(
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    mainPrayers: List<PrayerTimeDisplay>,
) {
    Column {
        // Header
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

        // Progress bar
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

        // Prayer dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            mainPrayers.forEach { prayer ->
                ProgressDot(
                    label = prayer.name.take(5),
                    isCompleted = prayer.prayerStatus == PrayerStatus.PRAYED,
                    isCurrent = prayer.isCurrent
                )
            }
        }
    }
}

@Composable
private fun ProgressDot(
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
private fun FastingRow(fastingToday: Boolean) {
    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (fastingToday) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    // Sun for active fasting (the daylight constraint), moon
                    // for the non-fasting state — visual symmetry with prayer
                    // accents elsewhere in the app.
                    imageVector = if (fastingToday) Icons.Default.LightMode else Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = if (fastingToday) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.fasting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = if (fastingToday) {
                        stringResource(R.string.today_fasting)
                    } else {
                        stringResource(R.string.no_fast_today)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

private val samplePrayerTimes = listOf(
    PrayerTimeDisplay(PrayerType.FAJR, "Fajr", "5:23 AM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.SUNRISE, "Sunrise", "6:45 AM", isPassed = true, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.DHUHR, "Dhuhr", "1:15 PM", isPassed = true, isCurrent = false, isNext = false, prayerStatus = PrayerStatus.PRAYED),
    PrayerTimeDisplay(PrayerType.ASR, "Asr", "4:30 PM", isPassed = false, isCurrent = true, isNext = true),
    PrayerTimeDisplay(PrayerType.MAGHRIB, "Maghrib", "6:12 PM", isPassed = false, isCurrent = false, isNext = false),
    PrayerTimeDisplay(PrayerType.ISHA, "Isha", "7:45 PM", isPassed = false, isCurrent = false, isNext = false),
)

@Preview(showBackground = true, widthDp = 400, name = "Standalone (sized to content)")
@Composable
private fun DailySummaryCard_Standalone_Preview() {
    NimazTheme {
        DailySummaryCard(
            prayerTimes = samplePrayerTimes,
            fastingToday = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 240, name = "Carousel — fasting")
@Composable
private fun DailySummaryCard_Carousel_Fasting_Preview() {
    NimazTheme {
        DailySummaryCard(
            prayerTimes = samplePrayerTimes,
            fastingToday = true,
            fillHeight = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 240, name = "Carousel — not fasting")
@Composable
private fun DailySummaryCard_Carousel_NotFasting_Preview() {
    NimazTheme {
        DailySummaryCard(
            prayerTimes = samplePrayerTimes,
            fastingToday = false,
            fillHeight = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}
