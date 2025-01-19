package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val prayers = listOf(
    "Fajr",
    "Dhuhr",
    "Asr",
    "Maghrib",
    "Isha"
)

@Composable
fun PrayerTrackerGrid(
    progressForMonth: State<List<LocalPrayersTracker>>,
    dateState: State<LocalDate>
) {
    val yearMonth = YearMonth.of(dateState.value.year, dateState.value.month)
    val daysInMonth = yearMonth.lengthOfMonth()
    val completedDays = progressForMonth.value.count { it.progress > 0 }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
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
                            text = dateState.value.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
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
                        text = "$completedDays/$daysInMonth days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Prayer Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prayers.forEach { prayer ->
                        PrayerRowRedesigned(
                            prayer = prayer,
                            yearMonth = yearMonth,
                            daysInMonth = daysInMonth,
                            currentDate = dateState.value,
                            progressForMonth = progressForMonth
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRowRedesigned(
    prayer: String,
    yearMonth: YearMonth,
    daysInMonth: Int,
    currentDate: LocalDate,
    progressForMonth: State<List<LocalPrayersTracker>>
) {
    val prayerColor = getPrayerColor(prayer)
    val completedCount = progressForMonth.value.count { it.isPrayerCompleted(prayer) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Prayer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = prayerColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = prayer,
                    style = MaterialTheme.typography.labelMedium,
                    color = prayerColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Surface(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$completedCount/$daysInMonth",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Dots Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until daysInMonth) {
                val date = yearMonth.atDay(i + 1)
                val prayerTracker = progressForMonth.value.find { it.date == date }
                DayDotRedesigned(
                    date = date,
                    isHighlighted = prayerTracker?.isPrayerCompleted(prayer) == true,
                    isMenstruating = prayerTracker?.isMenstruating == true,
                    currentDate = currentDate,
                    prayerColor = prayerColor
                )
            }
        }
    }
}

@Composable
private fun DayDotRedesigned(
    date: LocalDate,
    isHighlighted: Boolean,
    isMenstruating: Boolean,
    currentDate: LocalDate,
    prayerColor: Color
) {
    val isCurrentDay = date == currentDate
    val dotSize by animateDpAsState(
        targetValue = if (isCurrentDay) 8.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .size(dotSize)
            .background(
                color = when {
                    isMenstruating -> Color(0xFFE91E63)
                    isHighlighted -> prayerColor
                    isCurrentDay -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .border(
                width = if (isCurrentDay) 1.5.dp else 0.dp,
                color = if (isHighlighted)
                    prayerColor.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape
            )
    )
}

@Composable
private fun getPrayerColor(prayer: String): Color = when (prayer) {
    AppConstants.PRAYER_NAME_FAJR -> Color(0xFF2196F3)    // Stronger blue
    AppConstants.PRAYER_NAME_DHUHR -> Color(0xFFF57C00)   // Stronger orange
    AppConstants.PRAYER_NAME_ASR -> Color(0xFF43A047)     // Stronger green
    AppConstants.PRAYER_NAME_MAGHRIB -> Color(0xFFE53935) // Stronger red
    AppConstants.PRAYER_NAME_ISHA -> Color(0xFF5E35B1)    // Stronger purple
    else -> MaterialTheme.colorScheme.primary
}