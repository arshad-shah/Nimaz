package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DhuhrBackground(modifier: Modifier = Modifier) {
    // Cache colors to avoid recreating them on each recomposition
    val skyBlue1 = remember { Color(0xFF4B95D6) }
    val skyBlue2 = remember { Color(0xFF5BA0E0) }
    val skyMidBlue1 = remember { Color(0xFF7FB3E3) }
    val skyMidBlue2 = remember { Color(0xFF8BBFF0) }
    val skyLightBlue1 = remember { Color(0xFF9DCBF5) }
    val skyLightBlue2 = remember { Color(0xFFB5DBFF) }

    // Cache sun and ray colors
    val white = remember { Color.White }
    val transparent = remember { Color.Transparent }
    val sunYellow1 = remember { Color(0xFFFFE082) }
    val sunYellow2 = remember { Color(0xFFFFD54F) }
    val sunYellow3 = remember { Color(0xFFFFECB3) }
    val sunYellow4 = remember { Color(0xFFFFEE58) }
    val sunYellow5 = remember { Color(0xFFFDD835) }
    val sunYellow6 = remember { Color(0xFFFFF59D) }

    // Pre-calculate math constants
    val piDivided180 = remember { (Math.PI / 180f).toFloat() }

    // Single transition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "background animation")

    // Time-based animation progress for dynamic color shifts
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color shift"
    )

    // Sun animations
    val sunScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun pulse"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow pulse"
    )

    val rayAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ray alpha"
    )

    // Compute sky colors only when colorShift changes
    val skyColor1 = lerp(skyBlue1, skyBlue2, colorShift)
    val skyColor2 = lerp(skyMidBlue1, skyMidBlue2, colorShift)
    val skyColor3 = lerp(skyLightBlue1, skyLightBlue2, colorShift)

    // Create gradient once per composition update
    val skyGradient = remember(skyColor1, skyColor2, skyColor3) {
        Brush.verticalGradient(colors = listOf(skyColor1, skyColor2, skyColor3))
    }

    // Cache the atmospheric haze gradient
    val hazeGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                white.copy(alpha = 0.03f),
                white.copy(alpha = 0.12f)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }

    // Get the density once
    val density = LocalDensity.current

    // Pre-calculate sun position
    val sunPositionX = remember(density) { density.run { 140.dp.toPx() } }
    val sunPositionY = remember(density) { density.run { 20.dp.toPx() } }

    Box(modifier = modifier.background(skyGradient)) {
        // Subtle atmospheric haze layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hazeGradient)
        )

        StaticCloudsWithDepth(
            modifier = Modifier.fillMaxSize()
        )

        // Combined canvas for all sun elements to reduce composable count
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sunPosition = Offset(sunPositionX, sunPositionY)

            // Pre-compute radii in pixels
            val outerGlowRadius = 80f * density.density
            val midGlowRadius = 55f * density.density
            val sunRadius = 30f * density.density
            val rayCount = 12
            val rayInnerRadius = 30f * density.density
            val rayOuterRadius = 70f * density.density
            val strokeWidth = 3f * density.density * sunScale

            // Ray alpha adjustment
            val adjustedRayAlpha = rayAlpha * 0.8f

            // Outermost sun glow
            scale(glowScale, pivot = sunPosition) {
                val outerGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        sunYellow1.copy(alpha = 0.2f),
                        sunYellow2.copy(alpha = 0.1f),
                        transparent
                    ),
                    center = sunPosition,
                    radius = outerGlowRadius
                )

                drawCircle(
                    brush = outerGlowBrush,
                    center = sunPosition,
                    radius = outerGlowRadius
                )
            }

            // Mid sun glow
            scale(glowScale * 0.95f, pivot = sunPosition) {
                val midGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        sunYellow3.copy(alpha = 0.6f),
                        sunYellow1.copy(alpha = 0.3f),
                        transparent
                    ),
                    center = sunPosition,
                    radius = midGlowRadius + 5f * density.density // Slightly larger than radius for proper gradient
                )

                drawCircle(
                    brush = midGlowBrush,
                    center = sunPosition,
                    radius = midGlowRadius
                )
            }

            // Sun core
            scale(sunScale, pivot = sunPosition) {
                val sunCoreBrush = Brush.radialGradient(
                    colors = listOf(
                        white,
                        sunYellow4,
                        sunYellow5
                    ),
                    center = sunPosition,
                    radius = sunRadius
                )

                drawCircle(
                    brush = sunCoreBrush,
                    center = sunPosition,
                    radius = sunRadius
                )
            }

            // Corona effect - rays around the sun
            // Create a single line brush color to reuse
            val rayColorEnd = sunYellow6.copy(alpha = 0f)

            for (i in 0 until rayCount) {
                val angle = i * (360f / rayCount) * piDivided180
                val cosAngle = cos(angle)
                val sinAngle = sin(angle)

                val rayStart = Offset(
                    sunPosition.x + cosAngle * rayInnerRadius,
                    sunPosition.y + sinAngle * rayInnerRadius
                )

                val rayEnd = Offset(
                    sunPosition.x + cosAngle * rayOuterRadius,
                    sunPosition.y + sinAngle * rayOuterRadius
                )

                // Create the brush for each ray (can't be reused due to start/end positions)
                val rayBrush = Brush.linearGradient(
                    colors = listOf(
                        sunYellow6.copy(alpha = adjustedRayAlpha),
                        rayColorEnd
                    ),
                    start = rayStart,
                    end = rayEnd
                )

                drawLine(
                    brush = rayBrush,
                    start = rayStart,
                    end = rayEnd,
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}