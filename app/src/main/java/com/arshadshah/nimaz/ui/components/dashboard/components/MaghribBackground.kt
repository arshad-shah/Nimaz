package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MaghribBackground(modifier: Modifier = Modifier) {
    // Pre-compute and cache mathematical constants
    val piFloat = remember { PI.toFloat() }

    // Cache color components for reuse
    val white = remember { Color(0xFFFFFFFF) }
    val transparent = remember { Color.Transparent }
    val sunYellow = remember { Color(0xFFFFD54F) }
    val sunOrange1 = remember { Color(0xFFFF9800) }
    val sunOrange2 = remember { Color(0xFFFF7043) }
    val sunOrange3 = remember { Color(0xFFFF5722) }

    // Single transition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "maghrib animations")

    // Primary color animation for the dynamic sunset palette
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color transition"
    )

    // Sun glow animation
    val sunGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun glow pulse"
    )

    // Secondary timing for sun position - fixed value since it doesn't animate
    val sunsetProgress = 0.5f

    // Pre-compute sine values for the dynamic color calculations
    val sinColorShift = sin(colorShift * piFloat)
    val sinColorShift2 = sin(colorShift * piFloat * 0.7f)
    val sinColorShift3 = sin(colorShift * piFloat * 0.5f)
    val sinColorShift4 = sin(colorShift * piFloat * 1.3f)
    val sinColorShift5 = sin(colorShift * piFloat * 0.8f)
    val sinColorShift6 = sin(colorShift * piFloat * 1.1f)
    val sinColorShift7 = sin(colorShift * piFloat * 0.9f)
    val sinColorShift8 = sin(colorShift * piFloat * 1.2f)
    val sinColorShift9 = sin(colorShift * piFloat * 0.6f)

    // Calculate dynamic color variations with smooth transitions
    val topBlue = remember(sinColorShift, sinColorShift2, sinColorShift3) {
        Color(
            red = (30 + 15 * sinColorShift).toInt().coerceIn(0, 255),
            green = (40 + 10 * sinColorShift2).toInt().coerceIn(0, 255),
            blue = (100 + 25 * sinColorShift3).toInt().coerceIn(0, 255)
        )
    }

    val midPurple = remember(sinColorShift4, sinColorShift5, sinColorShift6) {
        Color(
            red = (120 + 15 * sinColorShift4).toInt().coerceIn(0, 255),
            green = (50 + 15 * sinColorShift5).toInt().coerceIn(0, 255),
            blue = (140 + 15 * sinColorShift6).toInt().coerceIn(0, 255)
        )
    }

    val horizonOrange = remember(sinColorShift7, sinColorShift8, sinColorShift9) {
        Color(
            red = (230 + 25 * sinColorShift7).toInt().coerceIn(0, 255),
            green = (70 + 20 * sinColorShift8).toInt().coerceIn(0, 255),
            blue = (50 + 20 * sinColorShift9).toInt().coerceIn(0, 255)
        )
    }

    // Create the sky gradient only when colors change
    val skyGradient = remember(topBlue, midPurple, horizonOrange) {
        Brush.verticalGradient(
            colors = listOf(topBlue, midPurple, horizonOrange)
        )
    }

    // Pre-calculate alpha values for glow effects
    val largeGlowAlpha1 = 0.15f * sunGlowPulse
    val largeGlowAlpha2 = 0.1f * sunGlowPulse
    val mediumGlowAlpha1 = 0.4f * sunGlowPulse
    val mediumGlowAlpha2 = 0.2f * sunGlowPulse
    val innerGlowAlpha1 = 0.7f * sunGlowPulse
    val innerGlowAlpha2 = 0.5f * sunGlowPulse

    // Cache the horizon glow gradient parameters
    val horizonGlowAlpha = remember(sunsetProgress) {
        0.7f + (sunsetProgress * 0.3f)
    }

    // Create the density value once
    val density = LocalDensity.current

    Box(modifier = modifier.background(skyGradient)) {
        // Combined canvas for all sun elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Calculate sun position once
            val sunY =
                size.height + 40f * density.density - (100f * density.density * (1 - sunsetProgress))
            val sunCenter = Offset(size.width / 2, sunY)

            // Pre-compute radii in pixels
            val largeGlowRadius = 350f * density.density
            val mediumGlowRadius = 180f * density.density
            val innerGlowRadius = 100f * density.density
            val sunRadius = 60f * density.density

            // Large atmospheric glow
            val largeGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunOrange1.copy(alpha = largeGlowAlpha1),
                    sunOrange3.copy(alpha = largeGlowAlpha2),
                    transparent
                ),
                center = sunCenter,
                radius = largeGlowRadius
            )

            drawCircle(
                brush = largeGlowBrush,
                center = sunCenter,
                radius = largeGlowRadius
            )

            // Medium sun glow
            val mediumGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunOrange2.copy(alpha = mediumGlowAlpha1),
                    sunOrange3.copy(alpha = mediumGlowAlpha2),
                    transparent
                ),
                center = sunCenter,
                radius = 200f * density.density // Slightly larger than the draw radius for better gradient
            )

            drawCircle(
                brush = mediumGlowBrush,
                center = sunCenter,
                radius = mediumGlowRadius
            )

            // Inner sun glow
            val innerGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunYellow.copy(alpha = innerGlowAlpha1),
                    sunOrange2.copy(alpha = innerGlowAlpha2),
                    transparent
                ),
                center = sunCenter,
                radius = 120f * density.density // Slightly larger than the draw radius
            )

            drawCircle(
                brush = innerGlowBrush,
                center = sunCenter,
                radius = innerGlowRadius
            )

            // Sun disc - create a clipping rectangle once
            val clipRect = Rect(0f, 0f, size.width, size.height)
            val clipPath = Path().apply {
                addRect(clipRect)
            }

            // Sun core gradient
            val sunCoreBrush = Brush.radialGradient(
                colors = listOf(
                    white,
                    sunYellow,
                    sunOrange3
                ),
                center = sunCenter,
                radius = sunRadius
            )

            // Clip and draw the sun
            clipPath(clipPath) {
                drawCircle(
                    brush = sunCoreBrush,
                    center = sunCenter,
                    radius = sunRadius
                )
            }
        }

        // Horizon atmospheric glow - optimized with remembered gradient
        val horizonGlowGradient = remember(horizonOrange, sunsetProgress) {
            Brush.verticalGradient(
                colors = listOf(
                    horizonOrange.copy(alpha = 0.5f),
                    sunOrange1.copy(alpha = 0.3f),
                    transparent
                ),
                startY = Float.POSITIVE_INFINITY,
                endY = 0f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
                .alpha(horizonGlowAlpha)
                .background(horizonGlowGradient)
        )
    }
}