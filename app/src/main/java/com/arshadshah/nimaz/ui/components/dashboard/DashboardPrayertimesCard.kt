package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardPrayerTimesCard(
    nextPrayerName: String,
    countDownTimer: CountDownTime,
    nextPrayerTime: LocalDateTime,
    isLoading: Boolean,
) {
    val context = LocalContext.current

    val deviceTimeFormat = android.text.format.DateFormat.is24HourFormat(context)

    val formatter = if (deviceTimeFormat) {
        DateTimeFormatter.ofPattern("HH:mm")
    } else {
        DateTimeFormatter.ofPattern("hh:mm a")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrayerIcon(nextPrayerName)

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatPrayerName(nextPrayerName),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.placeholder(
                                visible = isLoading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )

                        Text(
                            text = nextPrayerTime.format(formatter),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.placeholder(
                                visible = isLoading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = getTimerText(countDownTimer),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .placeholder(
                                        visible = isLoading,
                                        highlight = PlaceholderHighlight.shimmer()
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerIcon(prayerName: String) {
    val iconRes = when (prayerName) {
        "Sunrise" -> R.drawable.sunrise_icon
        "Fajr" -> R.drawable.fajr_icon
        "Dhuhr" -> R.drawable.dhuhr_icon
        "Asr" -> R.drawable.asr_icon
        "Maghrib" -> R.drawable.maghrib_icon
        "Isha" -> R.drawable.isha_icon
        else -> R.drawable.sunrise_icon
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "Next Prayer Icon",
            modifier = Modifier
                .size(80.dp),
            contentScale = ContentScale.Fit
        )
    }
}

private fun formatPrayerName(name: String): String {
    return if (name == "Loading...") name
    else name.replaceFirstChar { it.uppercase() }
}

fun getTimerText(timeToNextPrayer: CountDownTime?): String {
    if (timeToNextPrayer == null) return "Next prayer time not available"

    with(timeToNextPrayer) {
        return when {
            hours > 0 -> buildString {
                append("Next prayer in ${hours}h")
                if (minutes > 0) append(" ${minutes}m")
            }

            minutes > 0 -> buildString {
                append("Coming up in ${minutes}m")
                if (seconds > 0 && minutes <= 5) append(" ${seconds}s")
            }

            seconds > 30 -> "Starting in ${seconds}s"
            seconds > 0 -> "Starting now"

            else -> "Time for prayer"
        }
    }
}