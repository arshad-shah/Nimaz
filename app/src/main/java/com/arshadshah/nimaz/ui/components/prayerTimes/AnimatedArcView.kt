package com.arshadshah.nimaz.ui.components.prayerTimes

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.ui.theme.NimazTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.*

private const val FULL_ARC_ANGLE = 180f
private const val START_ANGLE = 180f
private const val ANIMATION_DURATION = 300

@Stable
data class ArcViewState(
    val timePoints: List<LocalDateTime?>,
    val countDownTime: CountDownTime,
)

@Composable
fun AnimatedArcView(
    state: ArcViewState,
    modifier: Modifier = Modifier
) {
    Log.d("ArcView - state", state.toString())
    val currentPhase = remember(state.timePoints) {
        getCurrentPhase(state.timePoints)
    }

    val dynamicPositions = remember(state.timePoints) {
        calculateDynamicPositions(state.timePoints)
    }

    // Calculate initial position based on current time
    val initialPosition = remember(state.timePoints, currentPhase) {
        calculateInitialPosition(state.timePoints, currentPhase)
    }

    var currentAnimationPhase by remember { mutableIntStateOf(currentPhase) }
    // Initialize animatable with calculated initial position
    val animatablePosition = remember { Animatable(initialPosition) }

    val getNextPosition = { phase: Int ->
        if (phase + 1 < dynamicPositions.size) {
            dynamicPositions[phase + 1] to (phase + 1)
        } else {
            dynamicPositions[0] to 0
        }
    }

    val durationMillis = remember(state.countDownTime) {
        with(state.countDownTime) {
            (hours * 3600 + minutes * 60 + seconds) * 1000
        }
    }

    // Enhanced colors and styling
    val arcBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(state.countDownTime, currentAnimationPhase) {
        val (nextPosition, nextPhase) = getNextPosition(currentAnimationPhase)

        animatablePosition.animateTo(
            targetValue = nextPosition,
            animationSpec = tween(
                durationMillis = durationMillis.toInt(),
                easing = LinearEasing
            )
        )
        // Update the phase after animation completes
        currentAnimationPhase = nextPhase
    }


    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("EnhancedArcView")
        ) {
            // Draw main arc background with gradient
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        arcBackgroundColor,
                        arcBackgroundColor.copy(alpha = 0.1f)
                    )
                ),
                startAngle = START_ANGLE,
                sweepAngle = FULL_ARC_ANGLE,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.width)
            )

            // Draw progress arc
            drawArc(
                color = primaryColor,
                startAngle = START_ANGLE,
                sweepAngle = animatablePosition.value * (FULL_ARC_ANGLE / PI.toFloat()),
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.width)
            )

            // Draw prayer time indicators
            state.timePoints.filterNotNull().forEachIndexed { index, _ ->
                val position = dynamicPositions[index]
                val point = calculatePointOnArc(
                    position,
                    size.width / 2,
                    size.width / 2
                )

                // Draw indicator circles with shadow
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = point,
                    style = Fill
                )
                drawCircle(
                    color = if (index == currentPhase) accentColor else primaryColor,
                    radius = 10.dp.toPx(),
                    center = point,
                    style = Fill
                )
            }

            // Draw animated sun
            val sunPosition = calculatePointOnArc(
                animatablePosition.value,
                size.width / 2,
                size.width / 2
            )
            Log.d("ArcView - sun position", sunPosition.toString())

            // Enhanced sun drawing with glow effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE4B5),
                        Color(0xFFFFD700),
                        Color(0xFFFFA500)
                    ),
                    center = sunPosition,
                    radius = 18.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = sunPosition,
                style = Fill
            )
        }
    }
}

private fun calculateInitialPosition(
    timePoints: List<LocalDateTime?>,
    currentPhase: Int
): Float {
    val validTimePoints = timePoints.filterNotNull()
    if (validTimePoints.size < 2) return 0f

    val now = LocalDateTime.now()
    val currentPoint = validTimePoints[currentPhase]
    val nextPoint = if (currentPhase + 1 < validTimePoints.size) {
        validTimePoints[currentPhase + 1]
    } else {
        validTimePoints[0]
    }

    val totalDuration = Duration.between(currentPoint, nextPoint).toMillis().toFloat()
    val elapsedDuration = Duration.between(currentPoint, now).toMillis().toFloat()

    // Calculate progress between current and next point
    val progress = (elapsedDuration / totalDuration).coerceIn(0f, 1f)

    // Get the positions on the arc
    val positions = calculateDynamicPositions(timePoints)
    val currentPos = positions[currentPhase]
    val nextPos = if (currentPhase + 1 < positions.size) {
        positions[currentPhase + 1]
    } else {
        positions[0]
    }

    // Interpolate between current and next position
    return currentPos + (nextPos - currentPos) * progress
}


// Helper functions remain the same
private fun calculateDynamicPositions(timePoints: List<LocalDateTime?>): List<Float> {
    val validTimePoints = timePoints.filterNotNull()
    if (validTimePoints.size < 2) return emptyList()

    Log.d("ArcView - timePoints", timePoints.toString())

    val totalDuration = Duration.between(validTimePoints.first(), validTimePoints.last()).toMillis().toFloat()

    return timePoints.mapNotNull { timePoint ->
        timePoint?.let {
            val durationFromStart = Duration.between(validTimePoints.first(), it).toMillis().toFloat()
            (durationFromStart / totalDuration) * PI.toFloat()
        }
    }
}

private fun calculatePointOnArc(
    angle: Float,
    radius: Float,
    centerY: Float
): Offset {
    val x = radius + (radius * cos(angle + PI.toFloat()))
    val y = centerY + (radius * sin(angle + PI.toFloat()))
    return Offset(x, y)
}

private fun getCurrentPhase(timePoints: List<LocalDateTime?>): Int {
    val now = LocalDateTime.now()
    val validTimePoints = timePoints.filterNotNull()

    return when {
        validTimePoints.isEmpty() -> 0
        now.isBefore(validTimePoints.first()) -> 0
        else -> {
            val nextPointIndex = validTimePoints.indexOfFirst { it.isAfter(now) }
            if (nextPointIndex == -1) validTimePoints.size - 1 else maxOf(0, nextPointIndex - 1)
        }
    }
}


@Preview(
    showBackground = false,
    widthDp = 360,
    heightDp = 400, showSystemUi = false
)
@Composable
fun ArcViewPreview() {
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Create test time points spread across 2 minutes (120 seconds)
    val baseTime = LocalDateTime.now()
    val timePoints = listOf(
        baseTime,                      // Start (0s)
        baseTime.plusSeconds(24),      // 24s
        baseTime.plusSeconds(48),      // 48s
        baseTime.plusSeconds(72),      // 72s
        baseTime.plusSeconds(96),      // 96s
        baseTime.plusSeconds(120)      // 120s (2 minutes)
    )

    // Update timer every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds = (elapsedSeconds + 1) % 121 // Reset after 120 seconds
        }
    }

    // Calculate remaining time until next prayer
    val currentTime = baseTime.plusSeconds(elapsedSeconds.toLong())
    val nextPrayerIndex = timePoints.indexOfFirst { it.isAfter(currentTime) }
    val countDownTime = if (nextPrayerIndex != -1) {
        val remainingSeconds = Duration.between(currentTime, timePoints[nextPrayerIndex]).seconds
        CountDownTime(
            hours = (remainingSeconds / 3600),
            minutes = ((remainingSeconds % 3600) / 60),
            seconds = (remainingSeconds % 60)
        )
    } else {
        CountDownTime(0, 0, 0)
    }

    val state = ArcViewState(
        timePoints = timePoints,
        countDownTime = countDownTime,
    )

    NimazTheme{
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AnimatedArcView(state = state)

            // Display timer for demo purposes
            Text(
                text = "Demo Time: ${elapsedSeconds}s / 120s",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}