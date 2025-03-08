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
import androidx.compose.ui.text.style.TextAlign
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
 * for different prayer times - displays automatically based on current time
 */
@Composable
fun DashboardPrayerTimesCard(
    nextPrayerName: String,
    countDownTimer: CountDownTime,
    nextPrayerTime: LocalDateTime,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    timeFormat: DateTimeFormatter,
    height: Dp = 200.dp
) {

    val isCompact = height < 200.dp
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp
        )
    ) {
        // Card content with animated sky background
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {

            // Animated Sky Background based on prayer time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight)
            ) {
                // Prayer backgrounds with crossfade transitions
                val prayerName = nextPrayerName.lowercase()

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "fajr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    FajrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "sunrise",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    SunriseBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "dhuhr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    DhuhrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "asr",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    AsrBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "maghrib",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    MaghribBackground(modifier = Modifier.fillMaxSize())
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = prayerName == "isha",
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(500)),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
                ) {
                    IshaBackground(modifier = Modifier.fillMaxSize())
                }

                // Single gradient brush creation instead of creating it for each recomposition
                val overlayGradient = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                }

                // Semi-transparent overlay to ensure text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayGradient)
                )

                // Content overlay with adjusted spacing for smaller heights
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 4.dp else 8.dp),
                    verticalArrangement = if (isCompact)
                        Arrangement.SpaceEvenly
                    else
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (!isCompact) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Prayer Name and Time Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isLoading,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = formatPrayerName(nextPrayerName),
                                style = if (isCompact)
                                    MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        shadow = MaterialTheme.typography.titleLarge.shadow
                                    )
                                else
                                    MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        shadow = MaterialTheme.typography.displaySmall.shadow
                                    ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Prayer Time Pill
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text(
                                text = nextPrayerTime.format(timeFormat),
                                style = if (isCompact)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(if (isCompact) 6.dp else 8.dp)
                            )
                        }
                    }

                    // Countdown Timer Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompact) 12.dp else 20.dp,
                                    vertical = if (isCompact) 8.dp else 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Countdown",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                                )

                                // Generate countdown text only when needed
                                val timerText by remember(countDownTimer) {
                                    derivedStateOf { getEnhancedTimerText(countDownTimer) }
                                }

                                Text(
                                    text = timerText,
                                    style = if (isCompact)
                                        MaterialTheme.typography.bodyLarge
                                    else
                                        MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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

@Preview
@Composable
fun DashboardPrayerTimesCardFajrPreview() {

    DashboardPrayerTimesCard(
        nextPrayerName = "Fajr",
        countDownTimer = CountDownTime(1, 1, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardSunrisePreview() {
    DashboardPrayerTimesCard(
        nextPrayerName = "Sunrise",
        countDownTimer = CountDownTime(0, 30, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardDhuhrPreview() {
    DashboardPrayerTimesCard(
        nextPrayerName = "Dhuhr",
        countDownTimer = CountDownTime(1, 30, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardAsrPreview() {
    DashboardPrayerTimesCard(
        nextPrayerName = "Asr",
        countDownTimer = CountDownTime(0, 45, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

@Preview
@Composable
fun DashboardPrayerTimesCardMaghribPreview() {
    DashboardPrayerTimesCard(
        nextPrayerName = "Maghrib",
        countDownTimer = CountDownTime(0, 15, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

@Preview(showSystemUi = false)
@Composable
fun DashboardPrayerTimesCardIshaPreview() {

    DashboardPrayerTimesCard(
        nextPrayerName = "Isha",
        countDownTimer = CountDownTime(0, 5, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    )
}

//compact
@Preview
@Composable
fun DashboardPrayerTimesCardCompactFajrPreview() {
    DashboardPrayerTimesCard(
        nextPrayerName = "Sunrise",
        countDownTimer = CountDownTime(1, 1, 0),
        nextPrayerTime = LocalDateTime.now(),
        isLoading = false,
        modifier = Modifier.fillMaxWidth(),
        timeFormat = DateTimeFormatter.ofPattern("HH:mm"),
        height = 150.dp
    )
}