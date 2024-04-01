package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.CountDownTime
import com.arshadshah.nimaz.ui.theme.NimazTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedArcView(timePoints: List<LocalDateTime?>, countDownTime: CountDownTime) {
    val currentPhase = getCurrentPhase(timePoints)
    // calculate the positions of prayer times on the arc using the timePoints
    // first timePoint is the start of the arc and the last timePoint is the end of the arc
    val dynamicPositions = timePoints.mapIndexed { index, timePoint ->
        val totalDuration = Duration.between(timePoints.first(), timePoints.last()).toMillis().toFloat()
        val durationFromStart = Duration.between(timePoints.first(), timePoint).toMillis().toFloat()
        val calculatedDuration = 180f * (durationFromStart / totalDuration)
        Math.toRadians(calculatedDuration.toDouble()).toFloat()
    }
    val animatablePosition = remember { Animatable(dynamicPositions[currentPhase]) }

    val endPosition =
        if (currentPhase + 1 < dynamicPositions.size) dynamicPositions[currentPhase + 1] else dynamicPositions[0]
    LaunchedEffect(key1 = currentPhase, key2 = timePoints, key3 = countDownTime) {
        //change the countDownTime to milliseconds
        val hoursMillis = countDownTime.hours * 60 * 60 * 1000
        val minutesMillis = countDownTime.minutes * 60 * 1000
        val secondsMillis = countDownTime.seconds * 1000
        val durationMillis = hoursMillis + minutesMillis + secondsMillis
        animatablePosition.animateTo(
            targetValue = endPosition,
            animationSpec = tween(
                durationMillis = durationMillis.toInt(),
            )
        )
    }

    val internalLineColour = MaterialTheme.colorScheme.onSurface

    val prayerTimesLocationOnArcColour = MaterialTheme.colorScheme.primary

    //get device width
    val deviceWidth = LocalConfiguration.current.screenWidthDp.dp

    val sunColorArray: Array<Pair<Float, Color>> = arrayOf(
    0.3f to Color(0xFFF74822),
    0.4f to Color(0xFFF5836A),
    0.5f to Color(0xFFF5A962),
    0.6f to Color(0xFFF5C151),
    0.7f to Color(0xFFF5D94A),
    0.8f to Color(0xFFF5E93A),
    0.9f to Color(0xFFFAF1D1),
    1f to Color(0xFFFFFBEE)
    )
    Canvas(
        modifier = Modifier
            .width(deviceWidth)
            .testTag("AnimatedArcView")
            .height(200.dp)) {
        inset(horizontal = 100f) {
            translate(top = 280f) {
                val maxWidth = size.width
                val arcRadius = maxWidth / 2
                val centerY = size.height / 2

                // Draw the animated object
                val animX = arcRadius + arcRadius * cos(animatablePosition.value)
                val animY = centerY - arcRadius * sin(animatablePosition.value)

                val path = Path()
                if (timePoints.isNotEmpty()) {
                    // Drawing the dashed arc
                    val sweepAngle = 180f
                    val startAngle = 180f
                    path.addArc(
                        oval = Rect(0f, (centerY - arcRadius), maxWidth, (centerY + arcRadius)),
                        startAngleDegrees = startAngle,
                        sweepAngleDegrees = sweepAngle
                    )

                    drawPath(
                        path = path,
                        color = internalLineColour,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                    dynamicPositions.forEach { position ->
                        val xCoord = arcRadius + arcRadius * cos(position)
                        val yCoord = centerY - arcRadius * sin(position)
                        drawCircle(
                            color = prayerTimesLocationOnArcColour,
                            center = Offset(xCoord, yCoord),
                            radius = 20f
                        )
                    }
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = sunColorArray,
                        center = Offset(maxWidth - animX, animY),
                        radius = (50 + 10).toFloat()
                    ),
                    radius = 40f,
                    center = Offset(maxWidth - animX, animY)
                )
            }
        }
    }
}

fun calculateDurationMillis(timePoints: List<LocalDateTime?>, currentPhase: Int): Int {
    val now = LocalDateTime.now()
    val nextPoint =
        if (currentPhase + 1 < timePoints.size) timePoints[currentPhase + 1] else timePoints[0]
    val duration = ChronoUnit.MILLIS.between(now, nextPoint).toInt()
    return maxOf(duration, 0) // Ensure duration is not negative
}

fun getCurrentPhase(timePoints: List<LocalDateTime?>): Int {
    val now = LocalDateTime.now()

    // Ensure all timePoints are non-null for easier processing
    val nonNullTimePoints = timePoints.filterNotNull()

    // If the current time is before the first point, return 0 as we're in the first phase
    if (nonNullTimePoints.isNotEmpty() && now.isBefore(nonNullTimePoints.first())) {
        return 0
    }

    // Find the first time point that is after the current time
    val nextPointIndex = nonNullTimePoints.indexOfFirst { it.isAfter(now) }

    // If all points are before now, it means we are in the last phase
    if (nextPointIndex == -1) {
        return nonNullTimePoints.size - 1
    }

    // Otherwise, we're in the phase before the next point
    return nextPointIndex - 1
}
