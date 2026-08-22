package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.common.CountdownParts
import com.arshadshah.nimaz.core.common.countdownTo
import com.arshadshah.nimaz.presentation.components.atoms.TickResolution
import com.arshadshah.nimaz.presentation.components.atoms.rememberNow
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * HH:MM:SS countdown timer rendered as three boxed digit groups separated by
 * colons. Counts down to [target] itself via the shared ticker — the caller
 * passes an instant, so nothing upstream has to hold or reformat a string.
 *
 * This replaces the old string-parsing signature, which split on spaces and
 * matched the literal Latin suffixes `h`/`m`/`s`; that broke the moment the
 * countdown strings were localized or rendered with Arabic-Indic digits. The
 * numbers now travel as numbers.
 *
 * The boxes pulse subtly when [LocalAnimationsEnabled] is true; when it is off
 * they hold a plain `1f` rather than running an idle infinite tween that keeps
 * the frame clock awake.
 */
@Composable
fun CountdownTimer(
    target: Instant?,
    modifier: Modifier = Modifier
) {
    val now by rememberNow(TickResolution.SECONDS)
    val parts = remember(target, now) {
        if (target == null) CountdownParts.ZERO else countdownTo(target, now)
    }
    val hours = parts.hours.toString().padStart(2, '0')
    val minutes = parts.minutes.toString().padStart(2, '0')
    val seconds = parts.seconds.toString().padStart(2, '0')

    val animationsEnabled = LocalAnimationsEnabled.current
    val infiniteTransition = rememberInfiniteTransition(label = "countdown_pulse")
    val alpha by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CountdownUnit(value = hours, label = stringResource(R.string.hours), alpha = alpha)
        CountdownSeparator()
        CountdownUnit(value = minutes, label = stringResource(R.string.minutes), alpha = alpha)
        CountdownSeparator()
        CountdownUnit(value = seconds, label = stringResource(R.string.seconds), alpha = alpha)
    }
}

@Composable
private fun CountdownUnit(
    value: String,
    label: String,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CountdownSeparator(modifier: Modifier = Modifier) {
    Text(
        text = ":",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 0.dp)
    )
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun CountdownTimer_Preview() {
    NimazTheme {
        CountdownTimer(
            target = Clock.System.now() + 2.hours + 15.minutes + 30.seconds,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Imminent (45s)")
@Composable
private fun CountdownTimer_Imminent_Preview() {
    NimazTheme {
        CountdownTimer(
            target = Clock.System.now() + 45.seconds,
            modifier = Modifier.padding(16.dp)
        )
    }
}
