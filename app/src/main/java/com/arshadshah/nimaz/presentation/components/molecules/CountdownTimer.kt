package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
