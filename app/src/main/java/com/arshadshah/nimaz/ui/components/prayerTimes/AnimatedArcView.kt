package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.CountDownTime
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    val currentPhase = remember(state.timePoints) {
        getCurrentPhase(state.timePoints)
    }

    val dynamicPositions = remember(state.timePoints) {
        calculateDynamicPositions(state.timePoints)
    }

    // Validate positions before proceeding
    if (dynamicPositions.isEmpty()) {
        return // Early return if no valid positions
    }

    // Calculate initial position with safety checks
    val initialPosition = remember(state.timePoints, currentPhase) {
        calculateInitialPosition(state.timePoints, currentPhase)
    }

    var currentAnimationPhase by remember { mutableIntStateOf(currentPhase) }
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
        try {
            val (nextPosition, nextPhase) = getNextPosition(currentAnimationPhase)

            // Ensure we're not animating to/from NaN values
            if (!nextPosition.isNaN() && !animatablePosition.value.isNaN()) {
                animatablePosition.animateTo(
                    targetValue = nextPosition,
                    animationSpec = tween(
                        durationMillis = durationMillis.toInt(),
                        easing = LinearEasing
                    )
                )
                currentAnimationPhase = nextPhase
            }
        } catch (e: IllegalStateException) {
            // Handle animation errors gracefully
            println("Animation error: ${e.message}")
        }
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
                val position = dynamicPositions.getOrNull(index) ?: return@forEachIndexed
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

            // Draw animated sun with safety checks
            if (!animatablePosition.value.isNaN()) {
                val sunPosition = calculatePointOnArc(
                    animatablePosition.value,
                    size.width / 2,
                    size.width / 2
                )

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

    // Calculate progress between current and next point with safety checks
    val progress = (elapsedDuration / totalDuration).coerceIn(0f, 1f)

    // Get the positions on the arc
    val positions = calculateDynamicPositions(timePoints)
    if (positions.isEmpty()) return 0f

    val currentPos = positions[currentPhase]
    val nextPos = if (currentPhase + 1 < positions.size) {
        positions[currentPhase + 1]
    } else {
        positions[0]
    }

    // Interpolate between current and next position with safety checks
    return (currentPos + (nextPos - currentPos) * progress)
}

private fun calculateDynamicPositions(timePoints: List<LocalDateTime?>): List<Float> {
    val validTimePoints = timePoints.filterNotNull()
    if (validTimePoints.size < 2) return emptyList()

    val firstTime = validTimePoints.first()
    val lastTime = validTimePoints.last()

    // Ensure we don't have zero duration
    val totalDuration = Duration.between(firstTime, lastTime).toMillis().toFloat()

    return timePoints.mapNotNull { timePoint ->
        timePoint?.let {
            val durationFromStart = Duration.between(firstTime, it).toMillis().toFloat()
            ((durationFromStart / totalDuration) * PI.toFloat())
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