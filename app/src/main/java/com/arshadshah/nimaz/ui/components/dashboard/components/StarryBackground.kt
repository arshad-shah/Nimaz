package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Immutable data class for stars
private data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val phaseOffset: Float,
    val needsGlow: Boolean // Pre-compute if star needs glow
)

@Composable
fun StarryBackground(
    maxStars: Int,
    modifier: Modifier = Modifier,
    twinkleSpeed: Float = 1f,
    brightnessRange: Pair<Float, Float> = 0.5f to 1f
) {
    // Pre-compute constants
    val brightnessFirst = brightnessRange.first
    val brightnessDelta = brightnessRange.second - brightnessRange.first
    val twoPi = 2f * PI.toFloat()

    // Generate star positions and sizes only once
    val stars = remember {
        List(maxStars) {
            val x = Random.nextFloat()
            val y = Random.nextFloat() * 0.95f // Keep stars in upper 95% of sky
            val size = (1.5f + Random.nextFloat() * 3f)
            val phaseOffset = Random.nextFloat() * twoPi
            // Pre-compute which stars need glow
            val needsGlow = size > 3f
            Star(x, y, size, phaseOffset, needsGlow)
        }
    }

    // Cache the white color
    val whiteColor = remember { Color.White }

    // Animation for twinkling effect - single animation for all stars
    val infiniteTransition = rememberInfiniteTransition(label = "twinkle")
    val twinkleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = twoPi,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
        ),
        label = "twinkle animation"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Optimize by reusing calculations
        stars.forEach { star ->
            // Calculate brightness with sine wave for twinkling
            val sinValue = (0.5f + 0.5f * sin(twinkleProgress * twinkleSpeed + star.phaseOffset))
            val brightness = brightnessFirst + brightnessDelta * sinValue

            // Center calculation done once per star
            val centerX = star.x * width
            val centerY = star.y * height
            val center = Offset(centerX, centerY)

            // Draw star with calculated brightness
            drawCircle(
                color = whiteColor.copy(alpha = brightness),
                radius = star.size * (0.8f + 0.2f * brightness),
                center = center
            )

            // Add subtle glow only to stars that need it
            if (star.needsGlow) {
                drawCircle(
                    color = whiteColor.copy(alpha = brightness * 0.3f),
                    radius = star.size * 2.5f,
                    center = center
                )
            }
        }
    }
}