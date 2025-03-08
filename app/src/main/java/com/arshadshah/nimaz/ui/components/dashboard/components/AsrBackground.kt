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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Precomputed data for dust particles
private data class DustParticle(
    val relativeX: Float,
    val relativeY: Float,
    val size: Float,
    val phaseOffset: Float
)

@Composable
fun AsrBackground(modifier: Modifier = Modifier) {
    // Cache colors to avoid recreating them during recompositions
    val skyBlue1 = remember { Color(0xFF4B95D6) }
    val skyBlue2 = remember { Color(0xFF5A9BDA) }
    val skyMidBlue1 = remember { Color(0xFF87CEEB) }
    val skyMidBlue2 = remember { Color(0xFF90D2EE) }
    val skyCyan1 = remember { Color(0xFFE0F7FA) }
    val skyCyan2 = remember { Color(0xFFEDF9FB) }

    val goldenColor1 = remember { Color(0xFFFFF9C4) }
    val goldenColor2 = remember { Color(0xFFFFECB3) }
    val goldenColor3 = remember { Color(0xFFFFE082) }

    val sunYellow1 = remember { Color(0xFFFFD54F) }
    val sunYellow2 = remember { Color(0xFFFFE082) }
    val sunYellow3 = remember { Color(0xFFFFF176) }

    val shadowColor = remember { Color(0xFF333333) }
    val transparent = remember { Color.Transparent }
    val white = remember { Color.White }

    // Pre-compute math constants
    val piFloat = remember { PI.toFloat() }
    val piQuarter = remember { piFloat * 0.25f }
    val piThird = remember { piFloat * 0.3f }
    val piTwo = remember { piFloat * 2f }

    // Single transition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "asr animations")

    // Dynamic color shift for late afternoon atmosphere
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color dynamics"
    )

    // Shadow animations
    val shadowLength by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow length"
    )

    val shadowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow intensity"
    )

    // Sun glow animation
    val sunGlowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun glow"
    )

    // Dynamic sky colors based on color shift
    val skyColor1 = lerp(skyBlue1, skyBlue2, colorShift)
    val skyColor2 = lerp(skyMidBlue1, skyMidBlue2, colorShift)
    val skyColor3 = lerp(skyCyan1, skyCyan2, colorShift)

    // Create sky gradient once per composition update
    val skyGradient = remember(skyColor1, skyColor2, skyColor3) {
        Brush.verticalGradient(colors = listOf(skyColor1, skyColor2, skyColor3))
    }

    // Golden hour haze gradient - doesn't change with animations so can be remembered
    val goldenHazeGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                transparent,
                goldenColor1.copy(alpha = 0.04f),
                goldenColor3.copy(alpha = 0.08f)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }

    // Get the density once
    val density = LocalDensity.current

    // Pre-calculate sun position values
    val sunPositionXStart = remember(density) { density.run { 120.dp.toPx() } }
    val sunPositionXEnd = remember(density) { density.run { 60.dp.toPx() } }
    val sunPositionYStart = remember(density) { density.run { 100.dp.toPx() } }
    val sunPositionYEnd = remember(density) { density.run { 180.dp.toPx() } }

    // Pre-computed dust particles
    val dustParticles = remember {
        List(40) {
            val x = 0.3f + 0.4f * kotlin.random.Random.nextFloat()
            val y = 0.3f + 0.4f * kotlin.random.Random.nextFloat()
            val size = 0.5f + kotlin.random.Random.nextFloat()
            val phaseOffset = it * 0.3f  // Fixed offset for consistent animation
            DustParticle(x, y, size, phaseOffset)
        }
    }

    Box(modifier = modifier.background(skyGradient)) {

        StaticCloudsWithDepth(
            modifier = Modifier.fillMaxSize()
        )

        // Golden hour atmospheric light
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val atmosphericBrush = Brush.linearGradient(
                colors = listOf(
                    goldenColor1.copy(alpha = 0.03f),
                    goldenColor2.copy(alpha = 0.08f),
                    goldenColor1.copy(alpha = 0.03f)
                ),
                start = Offset(0f, height * 0.7f),
                end = Offset(width, height * 0.7f)
            )

            drawRect(brush = atmosphericBrush)
        }

        // Calculate current sun position based on animation
        val sunPositionX = remember(colorShift, sunPositionXStart, sunPositionXEnd) {
            lerp(sunPositionXStart, sunPositionXEnd, colorShift)
        }

        val sunPositionY = remember(colorShift, sunPositionYStart, sunPositionYEnd) {
            lerp(sunPositionYStart, sunPositionYEnd, colorShift)
        }

        // Sun and glow effects combined
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sunCenter = Offset(sunPositionX, sunPositionY)

            // Pre-compute radii and alpha values
            val wideGlowRadius = 200f * density.density
            val midGlowRadius = 80f * density.density
            val sunRadius = 25f * density.density

            val wideGlowAlpha1 = 0.15f * sunGlowIntensity
            val wideGlowAlpha2 = 0.08f * sunGlowIntensity
            val midGlowAlpha1 = 0.3f * sunGlowIntensity
            val midGlowAlpha2 = 0.2f * sunGlowIntensity

            // Wide sun glow - atmospheric scattering
            val wideGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunYellow1.copy(alpha = wideGlowAlpha1),
                    sunYellow1.copy(alpha = wideGlowAlpha2),
                    transparent
                ),
                center = sunCenter,
                radius = wideGlowRadius
            )

            drawCircle(
                brush = wideGlowBrush,
                center = sunCenter,
                radius = wideGlowRadius
            )

            // Medium sun glow
            val midGlowBrush = Brush.radialGradient(
                colors = listOf(
                    sunYellow2.copy(alpha = midGlowAlpha1),
                    sunYellow1.copy(alpha = midGlowAlpha2),
                    transparent
                ),
                center = sunCenter,
                radius = midGlowRadius
            )

            drawCircle(
                brush = midGlowBrush,
                center = sunCenter,
                radius = midGlowRadius
            )

            // Sun core with late afternoon golden color
            val sunCoreBrush = Brush.radialGradient(
                colors = listOf(
                    white,
                    sunYellow3,
                    sunYellow1
                ),
                center = sunCenter,
                radius = sunRadius
            )

            drawCircle(
                brush = sunCoreBrush,
                center = sunCenter,
                radius = sunRadius
            )
        }

        // Long shadows characteristic of Asr time
        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = shadowIntensity)) {

            val width = size.width
            val height = size.height

            // Shadow parameters
            val shadowDirection = 2.5f
            val shadowStartX1 = width * 0.2f
            val shadowStartX2 = width * 0.6f

            // Main shadow path
            val shadowPath = Path().apply {
                moveTo(shadowStartX1, height)
                lineTo(shadowStartX1 + width * 0.1f, height)

                // Calculate end points once
                val endX1 = shadowStartX1 + width * 0.1f + cos(piThird) * width * shadowLength * shadowDirection
                val endY1 = height - sin(piThird) * height * 0.3f * shadowLength
                val endX2 = shadowStartX1 + cos(piThird) * width * shadowLength * shadowDirection
                val endY2 = height - sin(piThird) * height * 0.3f * shadowLength

                lineTo(endX1, endY1)
                lineTo(endX2, endY2)
                close()
            }

            // Create main shadow brush
            val mainShadowBrush = Brush.linearGradient(
                colors = listOf(
                    shadowColor.copy(alpha = 0.3f),
                    shadowColor.copy(alpha = 0.1f),
                    transparent
                ),
                start = Offset(width * 0.2f, height),
                end = Offset(width * 0.7f, height * 0.7f)
            )

            // Draw main long shadow
            drawPath(
                path = shadowPath,
                brush = mainShadowBrush
            )

            // Additional smaller shadow
            val smallShadowPath = Path().apply {
                moveTo(shadowStartX2, height)
                lineTo(shadowStartX2 + width * 0.05f, height)

                // Calculate small shadow end points
                val smallEndX1 = shadowStartX2 + width * 0.05f + cos(piQuarter) * width * 0.2f * shadowLength
                val smallEndY1 = height - sin(piQuarter) * height * 0.2f * shadowLength
                val smallEndX2 = shadowStartX2 + cos(piQuarter) * width * 0.2f * shadowLength
                val smallEndY2 = height - sin(piQuarter) * height * 0.2f * shadowLength

                lineTo(smallEndX1, smallEndY1)
                lineTo(smallEndX2, smallEndY2)
                close()
            }

            // Create small shadow brush
            val smallShadowBrush = Brush.linearGradient(
                colors = listOf(
                    shadowColor.copy(alpha = 0.25f),
                    shadowColor.copy(alpha = 0.1f),
                    transparent
                ),
                start = Offset(width * 0.6f, height),
                end = Offset(width * 0.8f, height * 0.8f)
            )

            drawPath(
                path = smallShadowPath,
                brush = smallShadowBrush
            )
        }

        // Golden hour atmospheric haze
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(goldenHazeGradient)
        )

        // Subtle dust particles in sunlight
        Canvas(modifier = Modifier
            .fillMaxSize()
            .alpha(0.4f)) {

            val width = size.width
            val height = size.height
            val particleBaseSize = 1f * density.density

            // Process all pre-computed dust particles
            dustParticles.forEach { particle ->
                val x = width * particle.relativeX
                val y = height * particle.relativeY

                // Only draw particles in the sunbeam area
                if (x > sunPositionX * 0.7f && x < sunPositionX * 1.3f) {
                    // Calculate particle alpha with animation based on phase offset
                    val particleAlpha = 0.2f + 0.3f * (0.5f + 0.5f * sin(colorShift * piTwo + particle.phaseOffset))

                    drawCircle(
                        color = goldenColor1.copy(alpha = particleAlpha),
                        radius = particleBaseSize * particle.size,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}