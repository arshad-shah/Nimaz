package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerColor
import com.arshadshah.nimaz.presentation.components.atoms.getPrayerGradientEnd
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme


/**
 * Animated countdown timer display.
 */
@Composable
fun CountdownTimer(
    targetTimeMillis: Long,
    modifier: Modifier = Modifier,
    label: String = "Time until",
    onComplete: (() -> Unit)? = null
) {
    var remainingTimeMillis by remember { mutableLongStateOf(targetTimeMillis - System.currentTimeMillis()) }

    LaunchedEffect(targetTimeMillis) {
        while (remainingTimeMillis > 0) {
            delay(1000)
            remainingTimeMillis = (targetTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
            if (remainingTimeMillis <= 0) {
                onComplete?.invoke()
            }
        }
    }

    val hours = (remainingTimeMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((remainingTimeMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((remainingTimeMillis / 1000) % 60).toInt()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeUnit(value = hours, label = "h")
            TimeSeparator()
            TimeUnit(value = minutes, label = "m")
            TimeSeparator()
            TimeUnit(value = seconds, label = "s")
        }
    }
}

/**
 * Single time unit display with animation.
 */
@Composable
private fun TimeUnit(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith
                        (slideOutVertically { it } + fadeOut())
            },
            label = "time_value"
        ) { targetValue ->
            Text(
                text = targetValue.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Colon separator between time units.
 */
@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/**
 * Prayer countdown timer with prayer-specific styling.
 */
@Composable
fun PrayerCountdownTimer(
    prayerType: PrayerType,
    prayerName: String,
    targetTimeMillis: Long,
    modifier: Modifier = Modifier,
    onComplete: (() -> Unit)? = null
) {
    var remainingTimeMillis by remember { mutableLongStateOf(targetTimeMillis - System.currentTimeMillis()) }

    LaunchedEffect(targetTimeMillis) {
        while (remainingTimeMillis > 0) {
            delay(1000)
            remainingTimeMillis = (targetTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
            if (remainingTimeMillis <= 0) {
                onComplete?.invoke()
            }
        }
    }

    val hours = (remainingTimeMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((remainingTimeMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((remainingTimeMillis / 1000) % 60).toInt()

    val prayerColor = getPrayerColor(prayerType)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Time until $prayerName",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountdownBox(value = hours, label = "Hours", color = prayerColor)
            Spacer(modifier = Modifier.width(8.dp))
            CountdownBox(value = minutes, label = "Min", color = prayerColor)
            Spacer(modifier = Modifier.width(8.dp))
            CountdownBox(value = seconds, label = "Sec", color = prayerColor)
        }
    }
}

/**
 * Individual countdown number box.
 */
@Composable
private fun CountdownBox(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn()) togetherWith
                            (slideOutVertically { it } + fadeOut())
                },
                label = "countdown_value"
            ) { targetValue ->
                Text(
                    text = targetValue.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact inline countdown display.
 */
@Composable
fun InlineCountdown(
    targetTimeMillis: Long,
    modifier: Modifier = Modifier,
    prefix: String = "",
    color: Color = MaterialTheme.colorScheme.primary
) {
    var remainingTimeMillis by remember { mutableLongStateOf(targetTimeMillis - System.currentTimeMillis()) }

    LaunchedEffect(targetTimeMillis) {
        while (remainingTimeMillis > 0) {
            delay(1000)
            remainingTimeMillis = (targetTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    val hours = (remainingTimeMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((remainingTimeMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((remainingTimeMillis / 1000) % 60).toInt()

    val timeString = buildString {
        if (prefix.isNotEmpty()) {
            append(prefix)
            append(" ")
        }
        if (hours > 0) {
            append("${hours}h ")
        }
        append("${minutes}m ${seconds}s")
    }

    Text(
        text = timeString,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

/**
 * Gradient countdown banner for home screen.
 */
@Composable
fun CountdownBanner(
    prayerType: PrayerType,
    prayerName: String,
    targetTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    val gradientColors = listOf(
        getPrayerColor(prayerType),
        getPrayerGradientEnd(prayerType)
    )

    var remainingTimeMillis by remember { mutableLongStateOf(targetTimeMillis - System.currentTimeMillis()) }

    LaunchedEffect(targetTimeMillis) {
        while (remainingTimeMillis > 0) {
            delay(1000)
            remainingTimeMillis = (targetTimeMillis - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }

    val hours = (remainingTimeMillis / (1000 * 60 * 60)).toInt()
    val minutes = ((remainingTimeMillis / (1000 * 60)) % 60).toInt()
    val seconds = ((remainingTimeMillis / 1000) % 60).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Next Prayer",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = prayerName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = buildString {
                    if (hours > 0) append("${hours}h ")
                    append("${minutes}m ${seconds}s")
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Countdown Timer")
@Composable
private fun CountdownTimerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CountdownTimer(
                targetTimeMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000),
                label = "Time until Dhuhr"
            )
        }
    }
}

@Preview(showBackground = true, name = "Prayer Countdown Timer")
@Composable
private fun PrayerCountdownTimerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PrayerCountdownTimer(
                prayerType = PrayerType.ASR,
                prayerName = "Asr",
                targetTimeMillis = System.currentTimeMillis() + (1 * 60 * 60 * 1000)
            )
        }
    }
}

@Preview(showBackground = true, name = "Inline Countdown")
@Composable
private fun InlineCountdownPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            InlineCountdown(
                targetTimeMillis = System.currentTimeMillis() + (30 * 60 * 1000),
                prefix = "in"
            )
        }
    }
}

@Preview(showBackground = true, name = "Countdown Banner")
@Composable
private fun CountdownBannerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CountdownBanner(
                prayerType = PrayerType.MAGHRIB,
                prayerName = "Maghrib",
                targetTimeMillis = System.currentTimeMillis() + (45 * 60 * 1000)
            )
        }
    }
}
