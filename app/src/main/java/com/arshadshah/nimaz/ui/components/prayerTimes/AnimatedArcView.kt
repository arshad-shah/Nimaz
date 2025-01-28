package com.arshadshah.nimaz.ui.components.prayerTimes

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
private const val TAG = "Nimaz: AnimatedArcView"

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
    Log.d(TAG, "Rendering with ${state.timePoints.size} time points")
    Log.d(TAG, "Raw timePoints: ${state.timePoints}")

    val currentPhase = remember(state.timePoints) {
        Log.d(TAG, "Calculating current phase for time points: ${state.timePoints}")
        getCurrentPhase(state.timePoints).also { phase ->
            Log.d(TAG, "Calculated phase: $phase")
        }
    }

    val dynamicPositions = remember(state.timePoints) {
        Log.d(TAG, "Calculating dynamic positions for time points: ${state.timePoints}")
        calculateDynamicPositions(state.timePoints).also { positions ->
            Log.d(TAG, "Calculated positions: $positions")
        }
    }

    // Validate positions before proceeding
    if (dynamicPositions.isEmpty()) {
        Log.w(TAG, "No valid positions calculated, returning early")
        return // Early return if no valid positions
    }

    // Calculate initial position with safety checks and validation
    val initialPosition = remember(state.timePoints, currentPhase) {
        Log.d(TAG, "Starting initial position calculation for phase $currentPhase")
        val validTimePoints = state.timePoints.filterNotNull()
        Log.d(TAG, "Valid time points for initial position: $validTimePoints")

        if (currentPhase >= validTimePoints.size) {
            Log.e(
                TAG,
                "Current phase ($currentPhase) is invalid for timepoints size (${validTimePoints.size})"
            )
            0f
        } else {
            calculateInitialPosition(state.timePoints, currentPhase).also { pos ->
                if (pos.isNaN()) {
                    Log.e(TAG, "Initial position calculation returned NaN!")
                    Log.d(
                        TAG,
                        "Calculation details - Phase: $currentPhase, TimePoints: ${state.timePoints}"
                    )
                }
                Log.d(TAG, "Calculated initial position: $pos")
            }
        }
    }

    var currentAnimationPhase by remember { mutableIntStateOf(currentPhase) }

    // Debug initialization values
    Log.d(TAG, "Before Animatable creation - Initial position value: $initialPosition")
    Log.d(TAG, "Before Animatable creation - Current phase: $currentPhase")
    Log.d(TAG, "Before Animatable creation - Dynamic positions: $dynamicPositions")

    val animatablePosition = remember(initialPosition) {
        Log.d(TAG, "Creating Animatable with initial position: $initialPosition")
        if (initialPosition.isNaN()) {
            Log.e(TAG, "Initial position is NaN - Defaulting to 0f")
            Animatable(0f)
        } else {
            Animatable(initialPosition)
        }
    }

    LaunchedEffect(initialPosition) {
        Log.d(TAG, "Updating animatable position to: $initialPosition")
        if (!initialPosition.isNaN() && initialPosition != animatablePosition.value) {
            animatablePosition.snapTo(initialPosition)
        }
    }

    Log.d(TAG, "Current animatable position value: ${animatablePosition.value}")

    val getNextPosition = { phase: Int ->
        Log.d(TAG, "Getting next position for phase: $phase")
        if (phase + 1 < dynamicPositions.size) {
            val nextPhase = phase + 1
            val nextPosition = dynamicPositions[nextPhase]
            Log.d(TAG, "Moving to next phase: $nextPhase with position $nextPosition")
            nextPosition to nextPhase
        } else {
            Log.d(TAG, "Wrapping back to phase 0 with position ${dynamicPositions[0]}")
            dynamicPositions[0] to 0
        }
    }

    val durationMillis = remember(state.countDownTime) {
        with(state.countDownTime) {
            Log.d(TAG, "Calculating duration for countdown: ${hours}h ${minutes}m ${seconds}s")
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
            Log.d(TAG, "Animation target: position=$nextPosition, phase=$nextPhase")

            // Ensure we're not animating to/from NaN values
            if (!nextPosition.isNaN() && !animatablePosition.value.isNaN()) {
                Log.d(
                    TAG,
                    "Starting animation to position: $nextPosition with duration: ${durationMillis}ms"
                )
                animatablePosition.animateTo(
                    targetValue = nextPosition,
                    animationSpec = tween(
                        durationMillis = durationMillis.toInt(),
                        easing = LinearEasing
                    )
                )
                currentAnimationPhase = nextPhase
                Log.d(TAG, "Animation completed, new phase: $nextPhase")
            } else {
                Log.w(
                    TAG,
                    "Invalid animation values detected - current: ${animatablePosition.value}, target: $nextPosition"
                )
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Animation error", e)
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
                .size(320.dp)
                .fillMaxSize()
                .testTag("EnhancedArcView")
        ) {
            Log.v(TAG, "Canvas drawing - size: ${size.width}x${size.height}")

            // Draw main arc background with gradient
            drawArc(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        arcBackgroundColor,
                        arcBackgroundColor.copy(alpha = 0.6f)
                    )
                ),
                startAngle = START_ANGLE,
                sweepAngle = FULL_ARC_ANGLE,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.width)
            )

            // Draw progress arc
            val sweepAngle = animatablePosition.value * (FULL_ARC_ANGLE / PI.toFloat())
            Log.v(TAG, "Drawing progress arc - sweep angle: $sweepAngle")
            drawArc(
                color = primaryColor,
                startAngle = START_ANGLE,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.width)
            )

            // Draw prayer time indicators
            state.timePoints.filterNotNull().forEachIndexed { index, timePoint ->
                Log.v(TAG, "Drawing indicator for timepoint[$index]: $timePoint")
                val position = dynamicPositions.getOrNull(index)
                if (position == null) {
                    Log.w(TAG, "No position found for index $index")
                    return@forEachIndexed
                }
                val point = calculatePointOnArc(
                    position,
                    size.width / 2,
                    size.width / 2
                )
                Log.v(TAG, "Indicator point for index $index: $point")

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
                Log.v(TAG, "Drawing sun at position: $sunPosition")

                // Enhanced sun drawing with richer gradient and glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),  // Bright white core
                            Color(0xFFFFF7E6),  // Warm white
                            Color(0xFFFFE4B5),  // Light goldenrod
                            Color(0xFFFFD700),  // Golden yellow
                            Color(0xFFFFA500),  // Orange
                            Color(0x99FF8C00),  // Semi-transparent dark orange
                            Color(0x66FF4500)   // More transparent red-orange
                        ),
                        center = sunPosition,
                        radius = 22.dp.toPx()
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
    Log.d(TAG, "calculateInitialPosition: phase=$currentPhase, timePoints=${timePoints.size}")
    Log.d(TAG, "Raw timePoints: $timePoints")

    val validTimePoints = timePoints.filterNotNull()
    Log.d(TAG, "Valid timePoints: $validTimePoints")

    if (validTimePoints.size < 2) {
        Log.w(TAG, "Insufficient valid time points")
        return 0f
    }

    val now = LocalDateTime.now()
    val currentPoint = validTimePoints[currentPhase]
    val nextPoint = if (currentPhase + 1 < validTimePoints.size) {
        validTimePoints[currentPhase + 1]
    } else {
        validTimePoints[0]
    }

    Log.d(TAG, "Current point: $currentPoint, Next point: $nextPoint, Now: $now")

    val totalDuration = Duration.between(currentPoint, nextPoint).toMillis().toFloat()
    val elapsedDuration = Duration.between(currentPoint, now).toMillis().toFloat()

    Log.d(TAG, "Duration calculation details:")
    Log.d(TAG, "- Current point: $currentPoint")
    Log.d(TAG, "- Next point: $nextPoint")
    Log.d(TAG, "- Current time: $now")
    Log.d(TAG, "- Total duration: $totalDuration")
    Log.d(TAG, "- Elapsed duration: $elapsedDuration")

    if (totalDuration <= 0) {
        Log.e(TAG, "Total duration is zero or negative: $totalDuration")
        return 0f
    }

    if (elapsedDuration < 0) {
        Log.e(TAG, "Elapsed duration is negative: $elapsedDuration")
        return 0f
    }

    // Calculate progress between current and next point with safety checks
    val rawProgress = elapsedDuration / totalDuration
    Log.d(TAG, "Raw progress before clamping: $rawProgress")
    val progress = rawProgress.coerceIn(0f, 1f)
    Log.d(TAG, "Progress calculation after clamping: $progress")

    // Get the positions on the arc
    val positions = calculateDynamicPositions(timePoints)
    if (positions.isEmpty()) {
        Log.w(TAG, "No positions calculated")
        return 0f
    }

    val currentPos = positions[currentPhase]
    val nextPos = if (currentPhase + 1 < positions.size) {
        positions[currentPhase + 1]
    } else {
        positions[0]
    }

    Log.d(TAG, "Position calculation: current=$currentPos, next=$nextPos, progress=$progress")

    // Interpolate between current and next position with safety checks
    val result = (currentPos + (nextPos - currentPos) * progress)
    Log.d(TAG, "Initial position calculated: $result")
    return result
}

private fun calculateDynamicPositions(timePoints: List<LocalDateTime?>): List<Float> {
    Log.d(TAG, "calculateDynamicPositions: input size=${timePoints.size}")
    Log.d(TAG, "Raw timePoints for dynamic positions: $timePoints")

    val validTimePoints = timePoints.filterNotNull()
    Log.d(TAG, "Valid timePoints for dynamic positions: $validTimePoints")

    if (validTimePoints.size < 2) {
        Log.w(TAG, "Insufficient valid time points for dynamic positions")
        return emptyList()
    }

    val firstTime = validTimePoints.first()
    val lastTime = validTimePoints.last()

    // Ensure we don't have zero duration
    val totalDuration = Duration.between(firstTime, lastTime).toMillis().toFloat()
    Log.d(TAG, "Total duration for positions: $totalDuration")

    if (totalDuration <= 0) {
        Log.w(TAG, "Warning: Total duration for dynamic positions is zero or negative")
    }

    return timePoints.mapNotNull { timePoint ->
        timePoint?.let {
            val durationFromStart = Duration.between(firstTime, it).toMillis().toFloat()
            Log.v(TAG, "Duration from start for $timePoint: $durationFromStart")
            val position = ((durationFromStart / totalDuration) * PI.toFloat())
            Log.v(TAG, "Calculated position for $timePoint: $position")
            position
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
    Log.d(TAG, "getCurrentPhase: input size=${timePoints.size}")
    Log.d(TAG, "Raw timePoints for phase calculation: $timePoints")

    val now = LocalDateTime.now()
    val validTimePoints = timePoints.filterNotNull()
    Log.d(TAG, "Valid timePoints for phase calculation: $validTimePoints")

    return when {
        validTimePoints.isEmpty() -> {
            Log.w(TAG, "No valid time points, returning phase 0")
            0
        }

        now.isBefore(validTimePoints.first()) -> {
            Log.d(TAG, "Current time is before first point, returning phase 0")
            0
        }

        else -> {
            val nextPointIndex = validTimePoints.indexOfFirst { it.isAfter(now) }
            val result =
                if (nextPointIndex == -1) validTimePoints.size - 1 else maxOf(0, nextPointIndex - 1)
            Log.d(TAG, "Calculated current phase: $result")
            result
        }
    }
}