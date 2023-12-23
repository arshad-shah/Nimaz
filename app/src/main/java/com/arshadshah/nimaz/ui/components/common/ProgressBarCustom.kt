package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A determinate circular progress that represents progress by drawing an arc from 0.0
 * to 360.0 degrees
 *
 * @param progressColor The color of the progress
 * @param progressBackgroundColor The background color of the progress
 * @param strokeWidth The progress stroke width
 * @param strokeBackgroundWidth The progress background stroke width
 * @param progress The progress value, float range from 0.0 to 100.0
 * @param progressDirection The progress direction
 * @param roundedBorder Determine whether the progress border is rounded or squared
 *
 */
@Composable
fun ProgressBarCustom(
    modifier: Modifier = Modifier,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    progressBackgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(0.2f),
    strokeWidth: Dp = 10.dp,
    strokeBackgroundWidth: Dp = 5.dp,
    progress: Float = 90f,
    progressDirection: AnimationDirection = AnimationDirection.RIGHT,
    roundedBorder: Boolean = true,
    durationInMilliSecond: Int = 2000,
    startDelay: Int = 1000,
    radius: Dp = 80.dp,
    waveAnimation: Boolean = true,
    label: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {

    val stroke = with(LocalDensity.current) {
        Stroke(
            width = strokeWidth.toPx(),
            cap = if (roundedBorder) StrokeCap.Round else StrokeCap.Square
        )
    }

    val strokeBackground = with(LocalDensity.current) {
        Stroke(width = strokeBackgroundWidth.toPx())
    }

    val strokeReverse = Stroke(strokeBackground.width / 4)

    val currentState = remember {
        MutableTransitionState(AnimatedArcState.START)
            .apply { targetState = AnimatedArcState.END }
    }
    val animatedProgress = updateTransition(currentState, label = "AnimatedProgress")
    var isFinished by remember { mutableStateOf(false) }
    val animatedCircle = rememberInfiniteTransition()

    val progress by animatedProgress.animateFloat(
        transitionSpec = {
            tween(
                durationMillis = durationInMilliSecond,
                easing = LinearEasing,
                delayMillis = startDelay
            )
        }, label = "Progress"
    ) { state ->
        when (state) {
            AnimatedArcState.START -> 0f

            AnimatedArcState.END -> {
                when (progressDirection) {
                    AnimationDirection.RIGHT -> progress
                    AnimationDirection.LEFT -> -progress
                }
            }
        }
    }

    val animatedReverse by animatedCircle.animateFloat(
        initialValue = 1.40f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    val animatedColor by animatedCircle.animateColor(
        initialValue = progressBackgroundColor.copy(0.5f),
        targetValue = progressColor.copy(0.8f),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    DisposableEffect(Unit) {
        isFinished = animatedProgress.currentState == animatedProgress.targetState
        onDispose {}
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(radius * 2)
    ) {
        Canvas(
            modifier.size(radius * 2)
        ) {

            val higherStrokeWidth =
                if (stroke.width > strokeBackground.width) stroke.width else strokeBackground.width
            val radius = (size.minDimension - higherStrokeWidth) / 2
            val halfSize = size / 2.0f
            val topLeft = Offset(
                halfSize.width - radius,
                halfSize.height - radius
            )
            val size = Size(radius * 2, radius * 2)
            val sweep = progress * 360 / 100
            isFinished = animatedProgress.currentState == animatedProgress.targetState

            drawArc(
                startAngle = 0f,
                sweepAngle = 360f,
                color = progressBackgroundColor,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = strokeBackground
            )

            if (waveAnimation && !isFinished) {
                drawCircle(
                    color = animatedColor,
                    style = strokeReverse,
                    radius = radius * animatedReverse,
                )
            }

            drawArc(
                color = progressColor,
                startAngle = 270f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = stroke
            )
        }

        if (label != null) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor
            )
        } else {
            Text(
                text = progress.toInt().toString() + "%",
                color = progressColor,
                fontSize = radius.value.sp / 2,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }


}

private enum class AnimatedArcState {

    START,
    END
}

enum class AnimationDirection {

    LEFT,
    RIGHT
}