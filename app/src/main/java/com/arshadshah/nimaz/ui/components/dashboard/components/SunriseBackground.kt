package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun SunriseBackground(modifier: Modifier = Modifier) {
    // Cache colors to avoid recreation
    val skyBlue1 = remember { Color(0xFF1A365D) }
    val skyBlue2 = remember { Color(0xFF294D7F) }
    val skyPurple1 = remember { Color(0xFF5D4777) }
    val skyPurple2 = remember { Color(0xFF7E5686) }
    val skyPink1 = remember { Color(0xFFD05E6A) }
    val skyPink2 = remember { Color(0xFFD76F77) }
    val skyOrange1 = remember { Color(0xFFFF9E6F) }
    val skyOrange2 = remember { Color(0xFFFFAB7C) }

    // Sun and glow colors
    val white = remember { Color.White }
    val transparent = remember { Color.Transparent }
    val sunYellow1 = remember { Color(0xFFFFE082) }
    val sunYellow2 = remember { Color(0xFFFFD54F) }
    val sunYellow3 = remember { Color(0xFFFFCA28) }
    val sunYellow4 = remember { Color(0xFFFFEE58) }
    val sunYellow5 = remember { Color(0xFFFFF59D) }

    // Single transition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "sunrise animations")

    // Main color shift animation
    val colorProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color transition"
    )

    // Sun position animation
    val sunProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun position"
    )

    // Glow pulse animation
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow pulse"
    )

    // Sun ray rotation - not used in optimized version to reduce calculations
    val sunRayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ray rotation"
    )

    // Compute sky colors only when colorProgress changes
    val skyColor1 = lerp(skyBlue1, skyBlue2, colorProgress)
    val skyColor2 = lerp(skyPurple1, skyPurple2, colorProgress)
    val skyColor3 = lerp(skyPink1, skyPink2, colorProgress)
    val skyColor4 = lerp(skyOrange1, skyOrange2, colorProgress)

    // Create gradient once per composition update
    val skyGradient = remember(skyColor1, skyColor2, skyColor3, skyColor4) {
        Brush.verticalGradient(colors = listOf(skyColor1, skyColor2, skyColor3, skyColor4))
    }

    // Precompute glow alpha values
    val outerGlowAlpha1 = 0.4f * glowPulse
    val outerGlowAlpha2 = 0.25f * glowPulse
    val outerGlowAlpha3 = 0.1f * glowPulse

    // Reflection glow alpha values
    val reflectionAlpha1 = 0.7f * glowPulse
    val reflectionAlpha2 = 0.5f * glowPulse
    val reflectionAlpha3 = 0.3f * glowPulse

    Box(modifier = modifier.background(skyGradient)) {
        // Combine all Canvas operations to reduce recompositions
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Horizon atmospheric perspective - create once
            val hazeGradient = Brush.verticalGradient(
                colors = listOf(
                    white.copy(alpha = 0.0f),
                    white.copy(alpha = 0.05f),
                    white.copy(alpha = 0.15f)
                ),
                startY = 0f,
                endY = height
            )
            drawRect(brush = hazeGradient)

            // Calculate sun position once
            val sunCenterY = height * (1.0f - sunProgress)
            val sunCenter = Offset(width / 2, sunCenterY)

            // Convert dp to px once
            val outerGlowRadius = 250f * density
            val innerGlowRadius = 100f * density
            val sunRadius = 60f * density

            // Create brushes only once per draw
            val outerGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunYellow1.copy(alpha = outerGlowAlpha1),
                    sunYellow2.copy(alpha = outerGlowAlpha2),
                    sunYellow3.copy(alpha = outerGlowAlpha3),
                    transparent
                ),
                center = sunCenter,
                radius = outerGlowRadius
            )

            val innerGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunYellow4.copy(alpha = 0.9f),
                    sunYellow2.copy(alpha = 0.7f),
                    sunYellow3.copy(alpha = 0.0f)
                ),
                center = sunCenter,
                radius = innerGlowRadius
            )

            val sunBrush = Brush.radialGradient(
                colors = listOf(
                    white,
                    sunYellow5,
                    sunYellow4
                ),
                center = sunCenter,
                radius = sunRadius
            )

            // Draw sun elements
            drawCircle(
                brush = outerGlowBrush,
                center = sunCenter,
                radius = outerGlowRadius
            )

            drawCircle(
                brush = innerGlowBrush,
                center = sunCenter,
                radius = innerGlowRadius
            )

            drawCircle(
                brush = sunBrush,
                center = sunCenter,
                radius = sunRadius
            )

            // Calculate reflection position based on sun position
            // Only if we're in the bottom part of the canvas where reflection would be visible
            if (sunCenterY > height * 0.7f) {
                // Reflection path - create only once
                val reflectionPath = Path().apply {
                    moveTo(width * 0.3f, height)
                    quadraticTo(
                        width * 0.5f, height * 0.7f,
                        width * 0.7f, height
                    )
                }

                // Calculate reflection width based on pulse
                val reflectionWidth = 8f * density * (0.8f + 0.2f * glowPulse)

                val reflectionBrush = Brush.verticalGradient(
                    colors = listOf(
                        sunYellow4.copy(alpha = reflectionAlpha1),
                        sunYellow2.copy(alpha = reflectionAlpha2),
                        sunYellow3.copy(alpha = reflectionAlpha3)
                    )
                )

                // Draw reflection
                drawPath(
                    path = reflectionPath,
                    brush = reflectionBrush,
                    style = Stroke(
                        width = reflectionWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}