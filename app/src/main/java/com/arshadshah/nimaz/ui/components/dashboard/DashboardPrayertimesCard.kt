package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.CountDownTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardPrayerTimesCard(
    nextPrayerName: String,
    countDownTimer: CountDownTime,
    nextPrayerTime: LocalDateTime,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    timeFormat: DateTimeFormatter
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header section with prayer name and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnhancedPrayerIcon(
                        prayerName = nextPrayerName,
                        isLoading = isLoading
                    )

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(
                            visible = !isLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = formatPrayerName(nextPrayerName),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = nextPrayerTime.format(timeFormat),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Countdown timer section
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.fajr_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getEnhancedTimerText(countDownTimer),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedPrayerIcon(
    prayerName: String,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoading) {
                Image(
                    painter = painterResource(id = getPrayerIcon(prayerName)),
                    contentDescription = "Prayer Icon",
                    modifier = Modifier.size(48.dp)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

fun getEnhancedTimerText(timeToNextPrayer: CountDownTime?): String {
    if (timeToNextPrayer == null) return "Next prayer time not available"

    return with(timeToNextPrayer) {
        when {
            hours > 0 -> "$hours hours ${minutes}min remaining"
            minutes > 0 -> "${minutes}min ${if (minutes <= 5) "${seconds}s" else ""} remaining"
            seconds > 30 -> "$seconds seconds remaining"
            seconds > 0 -> "Starting momentarily"
            else -> "Time for prayer"
        }
    }
}

private fun getPrayerIcon(prayerName: String): Int {
    return when (prayerName.lowercase()) {
        "fajr" -> R.drawable.fajr_icon
        "sunrise" -> R.drawable.sunrise_icon
        "dhuhr" -> R.drawable.dhuhr_icon
        "asr" -> R.drawable.asr_icon
        "maghrib" -> R.drawable.maghrib_icon
        "isha" -> R.drawable.isha_icon
        else -> R.drawable.fajr_icon
    }
}

fun formatPrayerName(name: String): String {
    return when (name.lowercase()) {
        "loading..." -> name
        else -> name.replaceFirstChar { it.uppercase() }
    }
}