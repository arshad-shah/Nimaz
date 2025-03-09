package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.ui.components.dashboard.components.AsrBackground
import com.arshadshah.nimaz.ui.components.dashboard.components.DhuhrBackground
import com.arshadshah.nimaz.ui.components.dashboard.components.FajrBackground
import com.arshadshah.nimaz.ui.components.dashboard.components.IshaBackground
import com.arshadshah.nimaz.ui.components.dashboard.components.MaghribBackground
import com.arshadshah.nimaz.ui.components.dashboard.components.SunriseBackground
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Enhanced Prayer Time Card with realistic animated backgrounds
 * for different prayer times - displays background based on current prayer period
 * while showing information about the next prayer
 */
@Composable
fun DashboardPrayerTimesCard(
    currentPrayerPeriod: String,
    nextPrayerName: String,
    countDownTimer: CountDownTime,
    nextPrayerTime: LocalDateTime,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    timeFormat: DateTimeFormatter,
    height: Dp = 200.dp
) {

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )
    ) {
        // Card content with animated sky background
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated Sky Background based on CURRENT prayer period
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight)
            ) {
                // Prayer backgrounds with crossfade transitions
                val currentPeriod = currentPrayerPeriod.lowercase()

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "fajr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    FajrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "sunrise",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    SunriseBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "dhuhr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    DhuhrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "asr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    AsrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "maghrib",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    MaghribBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = currentPeriod == "isha",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    IshaBackground(modifier = Modifier.fillMaxSize())
                }

                // Subtle gradient overlay for better text visibility
                val overlayGradient = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.01f),
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.1f)
                        )
                    )
                }

                // Light overlay to ensure text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayGradient)
                )

                // Content layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Next prayer pill
                    Surface(
                        color = Color.White.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = formatPrayerName(nextPrayerName),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Time pill
                    Surface(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = nextPrayerTime.format(timeFormat),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Timer pill
                    Surface(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Countdown",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.size(8.dp))

                            // Generate countdown text only when needed
                            val timerText by remember(countDownTimer) {
                                derivedStateOf { getEnhancedTimerText(countDownTimer) }
                            }

                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Optimized timer text generation with fewer conditional checks
 */
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

/**
 * Simple prayer name formatter with no string allocations for lowercase conversion
 */
fun formatPrayerName(name: String): String {
    if (name.equals("loading...", ignoreCase = true)) return name
    return name.replaceFirstChar { it.uppercase() }
}

// Preview functions
@Preview
@Composable
fun DashboardPrayerTimesCardFajrPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Isha",
        nextPrayerName = "Fajr",
        countDownTimer = CountDownTime(1, 1, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardSunrisePreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Fajr",
        nextPrayerName = "Sunrise",
        countDownTimer = CountDownTime(0, 30, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardDhuhrPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Sunrise",
        nextPrayerName = "Dhuhr",
        countDownTimer = CountDownTime(1, 30, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardAsrPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Dhuhr",
        nextPrayerName = "Asr",
        countDownTimer = CountDownTime(0, 45, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardMaghribPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Asr",
        nextPrayerName = "Maghrib",
        countDownTimer = CountDownTime(0, 15, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview(showSystemUi = false)
@Composable
fun DashboardPrayerTimesCardIshaPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Maghrib",
        nextPrayerName = "Isha",
        countDownTimer = CountDownTime(0, 5, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardCompactFajrPreview() {
    DashboardPrayerTimesCard(
        currentPrayerPeriod = "Isha",
        nextPrayerName = "Sunrise",
        countDownTimer = CountDownTime(1, 1, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("hh:mm a"),
        height = 150.dp
    )
}